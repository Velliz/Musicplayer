package dfd.pbol.GUI;

import dfd.pbol.utils.BackgroundExecutor;
import dfd.pbol.utils.Timer;
import dfd.pbol.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executor;

public class WaveformParallelFrame extends JFrame {

    private int WIDTH = 450;
    private int HEIGHT = 100;
    private int N_TASK = 2;
    private int NTOTLINES = 0;
    private int DISCARD_FACTOR = 4; //must be %2=0 ( < 8 )
    private WaveformPanel wfp;
    private int[] polyliney, polylinex;    //contains positions for the polyline

    //Executor/Other
    Executor executor = BackgroundExecutor.get();
    boolean updatedOnScreen = true;
    private boolean canWriteOnScreen = false;

    //Icons
    ImageIcon frameIcon = new ImageIcon(getClass().getResource("/res/waveicon.png"));

    //Check correctness/performance
    int taskCount = 0;
    Timer timer = new Timer(); //timer for max/min drawtime

    public WaveformParallelFrame() {
        super();
        setIconImage(frameIcon.getImage());
        setSize(WIDTH, HEIGHT + 20);
        setTitle("Waveform Frame");
        setName("Main FRAME");
        wfp = new WaveformPanel();
        wfp.setSize(WIDTH, HEIGHT);
        wfp.setName("Waveform PANEL");
        wfp.setDoubleBuffered(false);
        add(wfp);
        setLocationRelativeTo(null);
    }

    public void updateWave(byte[] pcmdata) {
        wfp.updateWave(pcmdata);
    }

    /**
     * This panel gets a signal with updateWave and each time
     * he can (not busy), he asks the executor to run
     * N_TASK tasks that calculate the absolute position of the
     * polyline relative to part of the signal (that is pcmdata)
     *
     * @author Pierluigi
     */
    class WaveformPanel extends JPanel {
        byte[] pcmdata = null;
        Label cdtlmx; //Label per il drawtime massimo
        Label cdtlmn; //Label per il drawtime minimo
        Label cdtlavg; //Label per il drawtime medio

        public WaveformPanel() {
            super();
            setLayout(null);
            cdtlmx = new Label("DrawTime max");
            cdtlmx.setBounds(0, 0, 80, 10);
            add(cdtlmx);
            cdtlmn = new Label("DrawTime min");
            cdtlmn.setBounds(0, 10, 80, 10);
            add(cdtlmn);
            cdtlavg = new Label("DrawTime avg");
            cdtlavg.setBounds(160, 0, 80, 10);
            add(cdtlavg);
        }

        /**
         * Refresh the wave every times a new pcmdata arrives
         */
        public void updateWave(byte[] pcmdata) {
            //ignore all other pcmdata until we draw something
            //repaint();
            synchronized (wfp) {
                if (!updatedOnScreen)
                    return;
                updatedOnScreen = false;
            }
            this.pcmdata = pcmdata;
            callTask();
        }

        /**
         * This makes the executor run some task
         * each task calculate position for part of the signal
         */
        private void callTask() {
            timer.start();
            int numLines = pcmdata.length / 4; // half because 2 points = 1 line, other half because we use 16 bit samples
            numLines /= DISCARD_FACTOR; //Discard other lines for performance (no quality but speed).

            //Instantiate the array if the number of total lines changes
            //This might happen due to different pcm lenght from song to song
            if (NTOTLINES != numLines) {
                NTOTLINES = numLines;
                instantiateEmptyLinesArray();
                log("Lines we are drawing: " + numLines);
            }
            //Let multiple task do the math
            int diff = pcmdata.length / N_TASK;
            for (int i = 0; i < N_TASK; i++)
                executor.execute(new WaveformTask(getWidth(), getHeight(), i * diff, (i + 1) * diff, i));
        }

        /**
         * Handle the refresh of the waveform
         *
         * @param g
         */
        private void doDrawing(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            if (pcmdata == null) {
                int HEIGHT = getHeight();
                int WIDTH = getWidth();
                //Render a straight line
                g2d.drawLine(0, HEIGHT / 2, WIDTH, HEIGHT / 2);
                return;
            }
            //Let swing handle the drawing
            if (polylinex != null) {
                drawLines(g2d);
            }
        }

        /**
         * This task calculates part of the polyline, relative to a portion
         * of the signal (pcmdata.lenght/N_TASK)
         *
         * @author Pierluigi
         */
        class WaveformTask implements Runnable {
            int HEIGHT;
            int WIDTH;
            int from;
            int to;
            int N;

            public WaveformTask(int width, int height, int from, int to, int n) {
                HEIGHT = height;
                WIDTH = width;
                this.to = to;
                this.from = from;
                this.N = n;
            }

            @Override
            public void run() {
                //log("Task " + N + " inizia l'esecuzione");
                calcLine2d();
                //The last thread synch with the drawing arrays
                //log("Task " + N + " ha completato l'esecuzione");
                synchronized (polylinex) {
                    taskCount++;
                    if (taskCount == N_TASK) {
                        taskCount = 0;
                        canWriteOnScreen = true;
                        repaint(); //If I'm the last one, then repaint
                    }
                }
            }

            void calcLine2d() {
                float scale = (float) HEIGHT / 65536; //h/2^16
                int npoints = (to - from) / (2 * DISCARD_FACTOR); //16bit, half gone
                //log( "from: " + from + " to: " + to);
                float pwidth = (float) WIDTH / N_TASK;
                float dx = (pwidth) * N;
                int dy = HEIGHT / 2;
                float lineLen = (float) pwidth / npoints;
                int ix = 0; //relative x position
                int absi; //absolute index of the arrays
                int inc = DISCARD_FACTOR * 2;
                for (int i = from; i < to; i += inc) {
                    int sample0 = Utils.getSixteenBitSample(pcmdata[i + 1], pcmdata[i]);
                    int val0 = (int) (sample0 * scale) + dy;
                    int diffx0 = Math.round(lineLen * ix + dx);
                    absi = ix + (N * npoints);
                    WaveformParallelFrame.this.polylinex[absi] = diffx0;
                    WaveformParallelFrame.this.polyliney[absi] = val0;
                    ix++;
                    //log("x vals: " + diffx0 + " --" + nlines + " from: " + from + " to: " + to+ " DX: " + dx);
                    //log("Updated GUI ( " + sumData + ") " + lineLen +  " " + WIDTH + " " + HEIGHT + " nlines: " +nlines + " Scale: "+scale );
                }
            }
        }
        //TASK DEFINITION END

        /**
         * This should draw lines
         *
         * @param g2d
         */
        void drawLines(Graphics2D g2d) {
            assert (polylinex != null); //Was everything instantiated with success ?
            assert (taskCount == 0); //Have all the task processed their wave for the cycle?

            if (canWriteOnScreen) { //repaint() might be called from something else (window resize, etc)
                //log("Inizio a disegnare...");
                g2d.drawPolyline(polylinex, polyliney, polylinex.length);
                g2d.dispose();
                timer.stop();
                //log("Disegno eseguito.");
                synchronized (wfp) {
                    canWriteOnScreen = false;
                    updatedOnScreen = true; //sync with pcmdata input
                }
            }

        }

        /**
         * Called each time the UI is rendered
         */

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            doDrawing(g);
            cdtlmx.setText(timer.getMax() + "");
            cdtlmn.setText(timer.getMin() + "");
            cdtlavg.setText(timer.getAvg() + "");
        }

        /**
         * Initialize arrays
         */
        private void instantiateEmptyLinesArray() {
            polylinex = new int[NTOTLINES * 2];
            polyliney = new int[NTOTLINES * 2];
            for (int i = 0; i < NTOTLINES * 2; i++) {
                polylinex[i] = 0;
                polyliney[i] = 0;
            }
        }
    }

    /// END OF JPANEL CLASS
    private void log(String line) {
        System.out.println("WF out] " + line);
    }
}
