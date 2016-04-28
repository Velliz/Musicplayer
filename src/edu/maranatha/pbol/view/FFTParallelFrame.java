package dfd.pbol.GUI;

import dfd.pbol.utils.BackgroundExecutor;
import dfd.pbol.utils.Complex;
import dfd.pbol.utils.Timer;
import dfd.pbol.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class FFTParallelFrame extends JFrame {

    private int N_TASK = 2;
    private int pcmLenght = 0;
    private int pow2FreqLenght; //max power of 2 we can select
    private boolean canWriteOnScreen = false;
    private boolean updatedOnScreen = true;

    private int WIDTH = 450;
    private int HEIGHT = 100;
    private FreqDiagPanel fdp;
    private Rectangle2D[] recs;
    private Complex[][] frequencies; //Frequencies calculated for every task at a given time

    private int taskCount = 0; //Necessary for ugly way to coordinate tasks over frequencies

    //Icons
    ImageIcon frameIcon = new ImageIcon(getClass().getResource("/res/waveicon.png"));

    //
    Timer timer = new Timer(); //timer for max/min drawtime

    public FFTParallelFrame() {
        super();
        setIconImage(frameIcon.getImage());
        setSize(WIDTH, HEIGHT + 20); //+20 is the title gap
        setTitle("Frequency Diagram Frame");
        setName("Main FRAME");
        fdp = new FreqDiagPanel();
        fdp.setSize(WIDTH, HEIGHT);
        fdp.setName("FreqDiag PANEL");
        add(fdp);
        
        setLocationRelativeTo(null);
    }

    public void updateWave(byte[] pcmdata) {
        fdp.updateWave(pcmdata);
    }

    /**
     * JPanel that contains the frequency spectrum every frequency is drawn on
     * screen as a rectangle every times a new pcmdata is received, N_TASK tasks
     * process a portion of the signal (that is pcmdata)
     *
     * @author Pierluigi
     */
    class FreqDiagPanel extends JPanel {

        byte[] pcmdata = null;
        Label cdtlmx; //Label per il drawtime massimo
        Label cdtlmn; //Label per il drawtime minimo
        Label cdtlavg; //Label per il drawtime medio

        public FreqDiagPanel() {
            super();
            ///
            frequencies = new Complex[N_TASK][]; //Instantiate first array
            initAmbient(); //Instantiate the arrays

            //Label for drawing time
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
            //log("pcmdata received");
            synchronized (fdp) {
                if (!updatedOnScreen) //scarta tutti i pcm che non posso disegnare
                {
                    return;
                }
                updatedOnScreen = false;
            }
            this.pcmdata = pcmdata;
            callTask();
        }

        /**
         * Calls all the task with the canon executor
         */
        private void callTask() {
            timer.start();
            if (pcmdata.length == 0) {
                //May happen when we seek
                updatedOnScreen = true;
                return;
            }

            //Instantiate arrays every time pcmdata change length
            if (pcmdata.length != pcmLenght) {
                initAmbient(); //Reinstantiate the arrays
            }
            int HEIGHT = getHeight();
            int WIDTH = getWidth();

            //Let more tasks do the math
            for (int i = 0; i < N_TASK; i++) {
                BackgroundExecutor.get().execute(new FftTask(WIDTH, HEIGHT, i));
            }
        }

        /**
         * Handle the refresh of the diagram
         *
         * @param g
         */
        private void doDrawing(Graphics g) {

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            if (pcmdata == null || pcmdata.length == 0) {
                //Render something
                Rectangle rect0 = new Rectangle(new Point(10, 10), new Dimension(WIDTH - 20, HEIGHT - 20));
                g2d.draw(rect0);
                g2d.fill(rect0);
                return;
            }
            //Let swing handle the drawing
            drawRects(g2d);
        }

        /**
         * Splitted computation for each part of the signal Each task calculate
         * the frequencies using a different part of the signal One task do the
         * math for the rectangles positions and lengths
         *
         * @author Pierluigi
         */
        class FftTask implements Runnable {

            Graphics2D g2d;
            int HEIGHT;
            int WIDTH;
            int N;

            public FftTask(int width, int height, int n) {
                HEIGHT = height;
                WIDTH = width;
                N = n; //to identify wich part of the pcmdata it should process
            }

            @Override
            public void run() {
                calcFrequency(); //set freqencies[N] with the result of the fft
                synchronized (FFTParallelFrame.this) {
                    //Verify.beginAtomic();
                    taskCount++;
                    //Only one will calculate the rectangle position relative to the frequencies
                    if (taskCount == N_TASK) {
                        taskCount = 0;
                        calcRects();
                        canWriteOnScreen = true;
                        repaint();
                    }
                    //Verify.endAtomic();
                }
            }

            /**
             * Calculates frequencies[N] using fft on part of the signal
             */
            private void calcFrequency() {
                //int windowSize = pcmdata.length / N_TASK;
                int windowSize = pow2FreqLenght;
                int winSizeHalf = windowSize / 2;
                int from = (windowSize) * N;
                int to = windowSize * (N + 1);
                Complex[] data = new Complex[winSizeHalf]; //2channel to 1 wave rappresentation for the task
                int j = 0;
                for (int i = from; i < to; i += 2) {
                    data[j] = new Complex(Utils.getSixteenBitSample(pcmdata[i + 1], pcmdata[i]), 0);
                    j++;
                }
                Complex[] freqs = Utils.fft(data);
                frequencies[N] = freqs;
            }

            /**
             * Calculate positions of the rectangles on screen
             */
            private void calcRects() {
                //log("CalcRects called " + Thread.currentThread().getName());
                int nRects = frequencies[0].length / 2; //Number of data (rectangles) on screen, only half of the fft returned frequencies are useful
                //log("STAMPA: " + nRects  + "  " + frequencies[0][0]);
                float recWidth = (float) WIDTH / nRects;
                float scale = (float) HEIGHT / 1000000;
                for (int i = 0; i < nRects; i++) {
                    double value = 0;
                    for (int j = 0; j < N_TASK; j++) {
                        assert (frequencies[j][i] != null);
                        if (frequencies[j][i] != null) {
                            value += frequencies[j][i].abs(); //take the value from every vector frequency
                        }                            //(calculated from different part of the wave by the tasks)
                    }
                    value = (value / N_TASK) * scale; //avarege value between calcs, scaled & inverted
                    float posx = recWidth * i;
                    Rectangle2D r = new Rectangle();
                    r.setRect(posx, HEIGHT - value, recWidth, value);
                    recs[i] = r; //
                }
            }
        }

        /**
         * This should draw rectangles stored in recs
         *
         * @param g2d
         */
        void drawRects(Graphics2D g2d) {
            assert (recs != null);
            if (canWriteOnScreen) { //repaint() might be called by something else
                for (int i = 0; i < recs.length; i++) {
                    g2d.draw(recs[i]);
                    g2d.fill(recs[i]);
                    if (i % 2 == 0) {
                        g2d.setColor(Color.darkGray);
                    } else {
                        g2d.setColor(Color.lightGray);
                    }
                }
                g2d.dispose();
                timer.stop(); //stop the timer for the draw time
                synchronized (fdp) {
                    canWriteOnScreen = false;
                    updatedOnScreen = true;
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
         * Instantiate all the needs of the panel, like the arrays
         */
        private void initAmbient() {
            if (pcmdata != null) {
                pcmLenght = pcmdata.length;
            } else {
                pcmLenght = 4608;
            }
            int freqLenght = (pcmLenght / N_TASK) / 2;
            int log2 = Utils.log(freqLenght, 2);
            pow2FreqLenght = (int) Math.pow(2, log2);
            log("Total rectangles/frequencies to draw: " + pow2FreqLenght + "/2");
            for (int i = 0; i < N_TASK; i++) {
                frequencies[i] = new Complex[pow2FreqLenght];
            }
            recs = new Rectangle2D[pow2FreqLenght / 4]; //we use 16bit data and only half of the fft freqencies are useful
            canWriteOnScreen = false; //don't write until we have recalculated the frequencies
        }
    }
    /// END OF JPANEL CLASS

    private void log(String line) {
        System.out.println("FD out] " + line);
    }
}
