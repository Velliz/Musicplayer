package dfd.pbol.GUI;

import javax.swing.*;
import java.awt.*;

public class StatusFrame extends JFrame {

    private JTextArea txtArea = new JTextArea();
    int WIDTH = 500;
    int HEIGHT = 400;

    public StatusFrame() {
        super();
        setTitle("Status Frame");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        txtArea.setBounds(0,0,WIDTH,HEIGHT);
        JScrollPane sp = new JScrollPane(txtArea);
        sp.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(sp);
        
        setLocationRelativeTo(null);
    }

    public void addText(String text) {
        String app = txtArea.getText() + "\n";
        txtArea.setText(app + text);
    }

}
