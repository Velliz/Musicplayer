package dfd.pbol.main;

import de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;
import dfd.pbol.GUI.FFTParallelFrame;
import dfd.pbol.GUI.SeekBar;
import dfd.pbol.GUI.StatusFrame;
import dfd.pbol.GUI.WaveformParallelFrame;
import dfd.pbol.audio.AudioPlayer;
import dfd.pbol.utils.BackgroundExecutor;
import dfd.pbol.utils.Utils;
import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;
import java.text.ParseException;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

/**
 *
 * @author Dwi Fadhil Didit
 */
public class Mp3Window extends JFrame {

    private double volume = 0.1;

    //Other
    private DefaultListModel<String> songList = new DefaultListModel<>();
    private ScheduledExecutorService timersExec = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService titleExec = Executors.newSingleThreadScheduledExecutor();
    private float currentAudioDurationSec = 0;

    //AudioPlayer
    private AudioPlayer player = AudioPlayer.getInstance();

    private JFileChooser fc = new JFileChooser();

    //Frames
    private WaveformParallelFrame wff = null;
    private FFTParallelFrame fdf = null;
    public static StatusFrame stf = new StatusFrame();

    //Icons
    private ImageIcon frameIcon = new ImageIcon(getClass().getResource("/res/frameicon.png"));
    private ImageIcon playIcon = new ImageIcon(getClass().getResource("/res/playicon.png"));
    private ImageIcon pauseIcon = new ImageIcon(getClass().getResource("/res/pauseicon.png"));

    private SeekBar seekbar = new SeekBar();

    public Mp3Window() {
        initComponents();
        setIconImage(frameIcon.getImage());
        setTitle("Music Player - Java - 1.0");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        jPanel1.add(seekbar);
        uiBehaviour();

    }

    private void triggerVolume() {
        try {
            player.setVolume(((double) volslide.getValue()) / 100);
        } catch (BasicPlayerException e1) {
            e1.printStackTrace();
        }
    }

    private void uiBehaviour() {
        //File chooser
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "only supported audio files (mp3, wav)";
            }

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                if (f.getName().endsWith(".mp3")) {
                    return true;
                }
                if (f.getName().endsWith(".wav")) {
                    return true;
                }
                return false;
            }
        });

        menuAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fc.showOpenDialog(btnAdd);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = fc.getSelectedFiles();
                    for (File f : files) {
                        player.addSong(f.getAbsolutePath());
                        songList.addElement(f.getName());
                        log("Added file " + f.getName() + " to playlist");
                    }
                } else {
                    log("No file selected");
                }
            }
        });

        //Song List
        jSongList.setModel(songList);
        jSongList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jSongList.setLayoutOrientation(JList.VERTICAL);
        //Event that triggers at double click
        jSongList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    log("Double click detected, moving to selected item.");
                    int index = list.locationToIndex(evt.getPoint());
                    player.setIndexSong(index);
                    try {
                        player.play();
                    } catch (BasicPlayerException ev) {
                        ev.printStackTrace();
                    }
                }
            }
        });

        //Btn Delete
        menuDelete.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //Executed Outside UI Thread
                BackgroundExecutor.get().execute(new Runnable() {

                    @Override
                    public void run() {
                        int[] indexes = jSongList.getSelectedIndices();
                        int removed = 0;
                        for (int i : indexes) {
                            log("Removed Song (" + (i - removed) + ")" + songList.get(i - removed));
                            player.removeSong(i - removed);
                            songList.remove(i - removed);
                            removed++;
                        }
                    }
                });
            }
        });
        //Play Btn
        btnPlay.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    tooglePlay();
                } catch (BasicPlayerException e1) {
                    e1.printStackTrace();
                }
            }
        });
        //Next and Previous btns
        btnNext.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    player.nextSong();
                } catch (BasicPlayerException e) {
                    log("Error calling the next song");
                    e.printStackTrace();
                }
            }
        });

        btnPrev.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    player.prvSong();
                    //seekbar.resetLastSeek();
                } catch (BasicPlayerException e) {
                    log("Error calling the previous song");
                    e.printStackTrace();
                }
            }
        });

        //Player related behaviour
        player.addBasicPlayerListener(new BasicPlayerListener() {

            @Override
            public void stateUpdated(BasicPlayerEvent event) {
                if (event.getCode() == BasicPlayerEvent.EOM) {
                    //seekbar.resetLastSeek();
                    try {
                        if (!repeat.isSelected()) {
                            player.nextSong();
                        } else {
                            player.play();
                        }
                    } catch (BasicPlayerException e) {
                        e.printStackTrace();
                    }
                    log("EOM event catched, calling next song.");
                }
                if (event.getCode() == BasicPlayerEvent.PAUSED) {
                    btnPlay.setIcon(playIcon);
                }
                if (event.getCode() == BasicPlayerEvent.RESUMED) {
                    btnPlay.setIcon(pauseIcon);
                }
            }

            @Override
            public void setController(BasicController arg0) {
            }

            @Override
            public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
                //we don't want to use microseconds directly because it gets resetted on seeking
                seekbar.updateSeekBar(player.getProgressMicroseconds(), currentAudioDurationSec);
                if (wff != null) {
                    wff.updateWave(pcmdata);
                }
                if (fdf != null) {
                    fdf.updateWave(pcmdata);
                }
            }

            @Override
            public void opened(Object arg0, Map arg1) {
                btnPlay.setIcon(pauseIcon);
                jSongList.setSelectedIndex(player.getIndexSong());
                //jTextArea1.setText("Now Playing: " + songList.get(player.getIndexSong()));
                currentAudioDurationSec = player.getAudioDurationSeconds();
            }
        });

        timersExec.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updateTimers();
            }
        }, 0, 1, TimeUnit.SECONDS);

        titleExec.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updatePlayingText();
            }
        }, 0, 1, TimeUnit.SECONDS);

        volslide.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                triggerVolume();
            }
        });

    }

    /**
     * Used by the Play/Pause button
     */
    private void tooglePlay() throws BasicPlayerException {
        if (songList.size() == 0) {
            return;
        }
        if (!player.isPaused()) {
            player.pause();
            btnPlay.setIcon(playIcon);
        } else {
            player.play();
        }
    }

    private void updateTimers() {
        if (!player.isPaused()) {
            long lms = player.getProgressMicroseconds();
            String timer0 = Utils.getMinutesRapp(player.getProgressMicroseconds());
            String timer1 = Utils.getMinutesRapp((long) (currentAudioDurationSec * 1000000) - player.getProgressMicroseconds());
            lblst.setText(timer0);
            lblet.setText(timer1);
        }
    }

    int dispIndex = 0;
    boolean goback = false;
    final static int MAXLblPChar = 36;

    private void updatePlayingText() {
        if (player.isPaused()) {
            return;
        }
        if (songList == null || (songList.size() == 0)) {
            return;
        }
        String currentSong = songList.get(player.getIndexSong());
        if (currentSong.length() > MAXLblPChar) {
            if ((MAXLblPChar + dispIndex) >= currentSong.length()) {
                goback = true;
            }
            if (dispIndex == 0) {
                goback = false;
            }
            String cutStr = currentSong.substring(dispIndex, MAXLblPChar + dispIndex);
            //jTextArea1.setText("Now Playing: " + cutStr);
            if (!goback) {
                dispIndex++;
            } else {
                dispIndex--;
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new SyntheticaSkyMetallicLookAndFeel());
        } catch (ParseException ex) {
            Logger.getLogger(Mp3Window.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Mp3Window.class.getName()).log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Mp3Window mv = new Mp3Window();
                mv.setVisible(true);
            }
        });
    }

    private void log(String line) {
        //jTextArea1.append("UI-Main] " + line + "\n");
        stf.addText("UI-Main] " + line);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenu6 = new javax.swing.JMenu();
        buttonGroup1 = new javax.swing.ButtonGroup();
        backgrounD = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jComboBox1 = new javax.swing.JComboBox<String>();
        jScrollPane2 = new javax.swing.JScrollPane();
        jSongList = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        btnPrev = new javax.swing.JButton();
        btnNext = new javax.swing.JButton();
        btnPlay = new javax.swing.JButton();
        lblst = new javax.swing.JLabel();
        lblet = new javax.swing.JLabel();
        repeat = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        volslide = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        jRadioButton4 = new javax.swing.JRadioButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        btnAdd = new javax.swing.JMenu();
        menuAdd = new javax.swing.JMenuItem();
        menuDelete = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();

        jMenu6.setText("jMenu6");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Papyrus", 0, 24)); // NOI18N
        jLabel1.setText("DFD MP3 PLAYER");

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("Kuning");
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("Hijau");
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText("Merah");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Status", "Ekualizer Frekuensi", "Wave Frekuensi" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jSongList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(jSongList);

        jPanel1.setBackground(new java.awt.Color(153, 255, 153));

        btnPrev.setText("<");

        btnNext.setText(">");

        btnPlay.setText("PLAY");

        lblst.setText("start");

        lblet.setText("end");

        repeat.setText("Repeat");

        jButton2.setText("MUTE");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel2.setText("Daftar Putar");

        buttonGroup1.add(jRadioButton4);
        jRadioButton4.setText("Abu-abu");
        jRadioButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgrounDLayout = new javax.swing.GroupLayout(backgrounD);
        backgrounD.setLayout(backgrounDLayout);
        backgrounDLayout.setHorizontalGroup(
            backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgrounDLayout.createSequentialGroup()
                .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgrounDLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(backgrounDLayout.createSequentialGroup()
                        .addGap(109, 109, 109)
                        .addComponent(jLabel2)))
                .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgrounDLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(backgrounDLayout.createSequentialGroup()
                                .addComponent(btnPrev)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblst)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 153, Short.MAX_VALUE)
                                .addComponent(lblet)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnNext)
                                .addGap(37, 37, 37))
                            .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(backgrounDLayout.createSequentialGroup()
                                    .addComponent(jRadioButton2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jRadioButton3)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jRadioButton1)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jRadioButton4)
                                    .addGap(86, 86, 86))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgrounDLayout.createSequentialGroup()
                                        .addComponent(btnPlay)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(repeat)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(volslide, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(backgrounDLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgrounDLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(209, 209, 209))
        );
        backgrounDLayout.setVerticalGroup(
            backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgrounDLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnPlay)
                        .addComponent(jButton2)
                        .addComponent(jLabel2)
                        .addComponent(repeat))
                    .addComponent(volslide, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgrounDLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnNext)
                            .addComponent(btnPrev)
                            .addComponent(lblet)
                            .addComponent(lblst))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(backgrounDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jRadioButton2)
                            .addComponent(jRadioButton3)
                            .addComponent(jRadioButton1)
                            .addComponent(jRadioButton4)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        btnAdd.setText("File");

        menuAdd.setText("Tambah Lagu");
        btnAdd.add(menuAdd);

        menuDelete.setText("Hapus Lagu");
        btnAdd.add(menuDelete);

        jMenuBar1.add(btnAdd);

        jMenu2.setText("About");

        jMenuItem3.setText("Tentang Aplikasi");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuItem2.setText("Tentang Pengembang");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Exit");
        jMenu3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jMenu3MouseClicked(evt);
            }
        });
        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgrounD, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgrounD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenu3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMenu3MouseClicked
        System.exit(0);
    }//GEN-LAST:event_jMenu3MouseClicked

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        JOptionPane.showMessageDialog(this, "Aplikasi ini adalah aplikasi untuk memutar lagu \n"
                + "Dibuat untuk tugas PBO Lanjut - 25 Februari 2016 \n"
                + "Pilih File - Tambah Lagu - Ke lokasi File lagu Anda, Lalu OK", "Tentang Aplikasi", 1);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        JOptionPane.showMessageDialog(this,
                "------------Pengembang Aplikasi-----------\n"
                + "1575004      Nurcholid Achmad     \n"
                + "1575010 Fadhil Hafizh Ardiansyah  \n"
                + "1575002    Dwi Paulina Brahmana   \n"
                + "------------DFD MP3 Player--------------\n"
                + "----------Didit - Fadhil - Dwi----------",
                "Tentang Pengembang", 1);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            if (volume != 0) {
                player.setVolume(0.0);
                volume = 0;
            } else {
                volume = ((double) volslide.getValue()) / 100;
                player.setVolume(volume);
            }
        } catch (BasicPlayerException ex) {
            Logger.getLogger(Mp3Window.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        switch (jComboBox1.getSelectedIndex()) {
            case 0:
            stf.setVisible(true);
            break;
            case 1:
            fdf = new FFTParallelFrame();
            fdf.setVisible(true);
            break;
            case 2:
            wff = new WaveformParallelFrame();
            wff.setVisible(true);
            break;
            default:
            break;
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
        backgrounD.setBackground(Color.RED);
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        backgrounD.setBackground(Color.GREEN);        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        // TODO add your handling code here:
        backgrounD.setBackground(Color.YELLOW);
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void jRadioButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton4ActionPerformed
backgrounD.setBackground(Color.LIGHT_GRAY);
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton4ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgrounD;
    private javax.swing.JMenu btnAdd;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnPlay;
    private javax.swing.JButton btnPrev;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList jSongList;
    private javax.swing.JLabel lblet;
    private javax.swing.JLabel lblst;
    private javax.swing.JMenuItem menuAdd;
    private javax.swing.JMenuItem menuDelete;
    private javax.swing.JCheckBox repeat;
    private javax.swing.JSlider volslide;
    // End of variables declaration//GEN-END:variables
}
