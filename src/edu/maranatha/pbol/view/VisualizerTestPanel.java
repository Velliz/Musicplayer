package dfd.pbol.GUI;

import javax.swing.*;
import java.awt.*;

public class VisualizerTestPanel extends JPanel {

    int WIDTH = getWidth();
    int HEIGHT = getHeight();
    byte[] pcmdata = null;
    int intensity = 10;

    public void updateWireframe(byte[] pcmdata) {
        this.pcmdata = pcmdata;
        repaint();
        //updateUI();
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        if (pcmdata == null) {
            g2d.drawLine(0, 75, 500, 75);
        } else {
            WIDTH = getWidth();
            HEIGHT = getHeight();
            float scale = (float) HEIGHT / 65536;
            int nlines = pcmdata.length / 2;
            float lineLen = (float) WIDTH / (nlines - 1);
            if (lineLen < 1)
                lineLen = 1;
            int sumData = 0;
            for (int i = 0; i < nlines - 2; i += 2) {
                int sample0 = getSixteenBitSample(pcmdata[i + 1], pcmdata[i]);
                int sample1 = getSixteenBitSample(pcmdata[i + 3], pcmdata[i + 2]);
                int val0 = (int) (sample0 * scale) + HEIGHT / 2;
                int val1 = (int) (sample1 * scale) + HEIGHT / 2;
                int diffx0 = Math.round(lineLen * i);
                int diffx1 = Math.round(lineLen * i + 2);
                g2d.drawLine(diffx0, val0, diffx1, val1);
                sumData = val0 + val1;
            }
            //System.out.println("Updated GUI ( " + sumData + ") " + lineLen +  " " + WIDTH + " " + HEIGHT + " nlines: " +nlines + " Scale: "+scale );

        }
    }

    private int getSixteenBitSample(int high, int low) {
        return (high << 8) + (low & 0x00ff);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}
