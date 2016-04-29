package edu.maranatha.pbol.view;

import edu.maranatha.pbol.utils.BackgroundExecutor;
import edu.maranatha.pbol.utils.Complex;
import edu.maranatha.pbol.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class FreqDiagFrame extends JFrame {

    private int WIDTH = 450;
    private int HEIGHT = 100;
    private FreqDiagPanel fdp;
    private ArrayList<Rectangle2D.Float> recs;

    //Icons
    ImageIcon frameIcon = new ImageIcon(getClass().getResource("/res/waveicon.png"));


    public FreqDiagFrame() {
        super();
        setIconImage(frameIcon.getImage());
        setSize(WIDTH, HEIGHT + 20); //+20 is the title gap
        setTitle("Frequency Diagram Frame");
        setName("Main FRAME");
        fdp = new FreqDiagPanel();
        fdp.setSize(WIDTH, HEIGHT);
        fdp.setName("FreqDiag PANEL");
        add(fdp);

    }

    public void updateWave(byte[] pcmdata) {
        fdp.updateWave(pcmdata);
        //wfp.doDrawing(getGraphics());
        //repaint();
    }

    class FreqDiagPanel extends JPanel {
        byte[] pcmdata = null;

        /**
         * Refresh the wave every times a new pcmdata arrives
         */
        public void updateWave(byte[] pcmdata) {
            //log("pcmdata received");
            this.pcmdata = pcmdata;
            repaint();
        }

        /**
         * Handle the refresh of the diagram
         *
         * @param g
         */
        private void doDrawing(Graphics g) {

            //log("ThreadSwing: " + Thread.currentThread());

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int HEIGHT = getHeight();
            int WIDTH = getWidth();

            if (pcmdata == null) {
                //Render something
                Rectangle rect0 = new Rectangle(new Point(10, 10), new Dimension(WIDTH - 20, HEIGHT - 20));
                g2d.draw(rect0);
                g2d.fill(rect0);
                return;
            }
            //Let swing handle the drawing
            BackgroundExecutor.get().execute(new FftTask(WIDTH, HEIGHT));
            //Let swing handle the drawing
            if (recs != null) {
                drawRects(g2d);
            }
        }

        class FftTask implements Runnable {
            Graphics2D g2d;
            int HEIGHT;
            int WIDTH;

            public FftTask(int width, int height) {
                HEIGHT = height;
                WIDTH = width;
            }

            private final static int FREQ_N = 32;

            @Override
            public void run() {
                //must do conversion to class
                int div = pcmdata.length / FREQ_N;
                Complex[] complexData = new Complex[FREQ_N];
                //Let's calculate a medium value for each set of pcmdata
                //relative to the FREQ_N
                for (int i = 0; i < complexData.length; i++) {
                    int val = 0;
                    for (int j = 0; j < div; j++)
                        val += pcmdata[(i * div) + j];
                    complexData[i] = new Complex(val / div, 0);
                }
                //Transform data
                Complex[] res = Utils.fft(complexData);
                calcRects(res);
            }

            /**
             * Calculate positions of the rectangles
             */
            private void calcRects(Complex[] freqs) {
                int N = freqs.length;
                float diff = (float) WIDTH / N;
                //log("N: " + N + " l: " + diff);
                ArrayList<Rectangle2D.Float> recs = new ArrayList<Rectangle2D.Float>();
                for (int i = 0; i < N; i++) {
                    float val = (float) ((freqs[i].abs() / 1024) * HEIGHT);
                    recs.add(new Rectangle2D.Float(i * diff, HEIGHT - val, diff, val));
                }
                synchronized (FreqDiagPanel.this) {
                    FreqDiagFrame.this.recs = recs;
                }
            }
        }

        /**
         * This should draw rectangles
         *
         * @param g2d
         */
        synchronized void drawRects(Graphics2D g2d) {
            for (Rectangle2D.Float rec : recs) {
                g2d.draw(rec);
                g2d.fill(rec);
            }
        }

        /**
         * Called each time the UI is rendered
         */

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            doDrawing(g);
        }
        /*
        @Override
		public void paint(Graphics g) {
			super.paint(g);
			doDrawing(g);
		}*/
    }

    /// END OF JPANEL CLASS
    private void log(String line) {
        System.out.println("FD out] " + line);
    }

}
