package edu.maranatha.pbol.view;

import edu.maranatha.pbol.audio.AudioPlayer;
import edu.maranatha.pbol.utils.BackgroundExecutor;
import edu.maranatha.pbol.utils.Utils;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainView extends JFrame {

    double volume = 0.1;
    //Other
    DefaultListModel<String> songList = new DefaultListModel<String>();
    ScheduledExecutorService timersExec = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService titleExec = Executors.newSingleThreadScheduledExecutor();
    float currentAudioDurationSec = 0;
    //AudioPlayer
    AudioPlayer player = AudioPlayer.getInstance();
    //Components
    JPanel container = new JPanel();
    JButton btnPlay = new JButton();
    JButton btnAdd = new JButton();
    JButton btnNext = new JButton();
    JButton btnPrev = new JButton();
    JButton btnShSt = new JButton();
    JButton btnShWf = new JButton();
    JButton btnShDi = new JButton();
    JButton btnDel = new JButton();
    JButton btnDelAll = new JButton();
    JMenuBar topMenu = new JMenuBar();
    JList<String> jSongList = new JList<String>(songList);
    JLabel lblplaying = new JLabel();
    JLabel lblst = new JLabel();
    JLabel lblet = new JLabel();
    SeekBar seekbar = new SeekBar();
    JSlider volslide = new JSlider();
    JFileChooser fc = new JFileChooser();
    //Frames
    WaveformParallelFrame wff = null;
    FFTParallelFrame fdf = null;
    public static StatusFrame stf = new StatusFrame();
    //Icons
    ImageIcon frameIcon = new ImageIcon(getClass().getResource("/res/frameicon.png"));
    ImageIcon playIcon = new ImageIcon(getClass().getResource("/res/playicon.png"));
    ImageIcon pauseIcon = new ImageIcon(getClass().getResource("/res/pauseicon.png"));

    /**
     * Class/Frame constructor
     */
    public MainView() {
        init();
        initMenu();
        uiBehaviour();
    }

    /**
     * Holds and init menu functionality
     */
    private void initMenu() {

    }

    /**
     * Init Swing graphics UI
     */
    private void init() {
        //MainView
        setIconImage(frameIcon.getImage());
        setTitle("Music Player - Java - 1.0");
        int _H = 300;
        int _W = 330;
        setSize(_W, _H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        //setResizable(false);
        //Container
        container.setLayout(null);
        getContentPane().add(container);
        //Buttons
        int btn_h = 35;
        int line1 = 80;
        JPanel contBtns = new JPanel();
        contBtns.setBounds(0, line1, 180, btn_h);
        btnPrev.setText("<<");
        btnPrev.setBounds(0, 0, 50, btn_h);
        //btnPlay.setText(">");
        btnPlay.setIcon(playIcon);
        btnPlay.setMnemonic(KeyEvent.VK_SPACE);
        btnPlay.setBounds(0, 0, 50, btn_h);
        btnNext.setText(">>");
        btnNext.setBounds(0, 0, 50, btn_h);
        btnAdd.setText("Add Song");
        btnAdd.setBounds(_W - 80, line1, 70, btn_h);
        contBtns.add(btnPrev);
        contBtns.add(btnPlay);
        contBtns.add(btnNext);
        container.add(contBtns);
        container.add(btnAdd);
        //Now Playing Panel
        JPanel panelNP = new JPanel();
        panelNP.setLayout(new BoxLayout(panelNP, BoxLayout.PAGE_AXIS));
        panelNP.setToolTipText("Now Playing");
        panelNP.setBorder(BorderFactory.createMatteBorder(1, 0, 2, 0, Color.gray));
        panelNP.setBounds(5, line1 - 25, _W - 15, 20);
        //JLabel lblnp = new JLabel("Now Playing:");
        lblplaying.setText("Now Playing: ");
        lblplaying.setBounds(5, 0, 100, 40);
        //panelNP.add(lblnp);
        panelNP.add(lblplaying);
        container.add(panelNP);
        //SongList
        int h_list = 100;
        //jSongList.setBounds(0, line1+50, _W, h_list);
        JScrollPane listScroller = new JScrollPane(jSongList);
        listScroller.setPreferredSize(new Dimension(_W - 10, h_list));
        listScroller.setBounds(0, line1 + 50, _W - 10, h_list);
        container.add(listScroller);
        //container.add(jSongList);
        //2Row Buttons
        int line2 = line1 + h_list + 50;
        JPanel contBtns2 = new JPanel();
        //contBtns2.setLayout(new BoxLayout(contBtns2, BoxLayout.PAGE_AXIS));
        contBtns2.setBounds(0, line2, 220, 50);
        //contBtns2.setBackground(Color.lightGray);
        btnShSt.setText("STAT");
        //btnShSt.setBounds(0, 0, 30, btn_h);
        btnShWf.setText("ShWf");
        //btnShWf.setBounds(35, 0, 30, btn_h);
        btnShDi.setText("ShDi");
        contBtns2.add(btnShSt);
        contBtns2.add(btnShWf);
        contBtns2.add(btnShDi);
        container.add(contBtns2);
        //DelBtns
        btnDel.setBounds(_W - 55, line2 + 5, 45, 30);
        btnDel.setText("X");
        container.add(btnDel);

        //volume
        volslide.setBounds(5, _H, _W - 15, 10);
        volslide.setMinimum(0);
        volslide.setMaximum(100);
        container.add(volslide);

        //SeekBar
        seekbar.setBounds(5, 10, _W - 15, 10);
        container.add(seekbar);
        //Labels song time
        JPanel contSlbl = new JPanel();
        contSlbl.setBounds(10, 15, _W - 20, 20);
        contSlbl.add(lblst);
        contSlbl.add(lblet);
        lblst.setText("00:00");
        lblst.setBorder(new EmptyBorder(0, 0, 0, 200));
        lblet.setText("00:00");
        container.add(contSlbl);
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
        btnAdd.addActionListener(new ActionListener() {

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
        btnDel.addActionListener(new ActionListener() {

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
                    //seekbar.resetLastSeek();
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
                        player.nextSong();
                    } catch (BasicPlayerException e) {
                        e.printStackTrace();
                    }
                    log("EOM event catched, calling next song.");
                }
                if (event.getCode() == BasicPlayerEvent.PAUSED) {
                    //btnPlay.setText(">");
                    btnPlay.setIcon(playIcon);
                }
                if (event.getCode() == BasicPlayerEvent.RESUMED) {
                    //btnPlay.setText("||");
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
                //btnPlay.setText("||");
                btnPlay.setIcon(pauseIcon);
                jSongList.setSelectedIndex(player.getIndexSong());
                lblplaying.setText("Now Playing: " + songList.get(player.getIndexSong()));
                currentAudioDurationSec = player.getAudioDurationSeconds();
            }
        });
        //END LISTENER

        //Timers Executor / Every 1 Second
        timersExec.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updateTimers();
                //updatePlayingText();
            }
        }, 0, 1, TimeUnit.SECONDS);

        titleExec.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updatePlayingText();
            }
        }, 0, 1, TimeUnit.SECONDS);

        //Btn Waveform
        btnShWf.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        wff = new WaveformParallelFrame();
                        wff.setVisible(true);
                    }
                });
            }
        });
        //Btn that show freq diagram
        btnShDi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                fdf = new FFTParallelFrame();
                fdf.setVisible(true);
            }
        });
        //Open status window frame
        btnShSt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                //stf = new StatusFrame();
                stf.setVisible(true);
            }
        });

        volslide.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                triggerVolume();
            }
        });

    }

    private void triggerVolume() {
        try {
            player.setVolume(((double) volslide.getValue()) / 100);
        } catch (BasicPlayerException e1) {
            e1.printStackTrace();
        }
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
            //btnPlay.setText(">");
            btnPlay.setIcon(playIcon);
        } else {
            player.play();
        }
        triggerVolume();
    }
    ///////////////////////////////

    private void updateTimers() {
        if (!player.isPaused()) {
            long lms = player.getProgressMicroseconds();
            //log("Update tm" + lms + "  pla" + (player.getProgressMicroseconds()/1000000));
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
            lblplaying.setText("Now Playing: " + cutStr);
            //log(" out lbl play: " + dispIndex + " " + cutStr);
            if (!goback) {
                dispIndex++;
            } else {
                dispIndex--;
            }
        }
    }
    /////////////////////////////////

    //MAIN
//    public static void main(String[] args) {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (UnsupportedLookAndFeelException e) {
//            e.printStackTrace();
//        }
//
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                MainView mv = new MainView();
//                mv.setVisible(true);
//            }
//        });
//    }
    private void log(String line) {
        System.out.println("UI-Main] " + line);
        stf.addText("UI-Main] " + line);
    }
}
