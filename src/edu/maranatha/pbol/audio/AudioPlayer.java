package edu.maranatha.pbol.audio;

import edu.maranatha.pbol.view.MainView;
import javazoom.jlgui.basicplayer.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class AudioPlayer extends BasicPlayer {

    //Extension of BasicPlayer
    private static AudioPlayer instance = null;

    // BasicPlayer is a BasicController
    private BasicController control = null;
    private ArrayList<String> playlist = new ArrayList<String>();
    private int index = 0;
    private boolean paused = true;
    private boolean opened = false;
    private boolean isSeeking = false;

    //Current Audio Properties
    private float audioDurationInSeconds = 0;
    private int audioFrameSize = 0;
    private float audioFrameRate = 0;
    //Stream info/status
    private byte[] cpcmdata;
    private long csms = 0; //Current Song microseconds
    private int lastSeekMs = 0; //Every time we seek, basic player returns microseconds are resetted
    //we need a var to mantain the position we seeked to

    //Want to use a singleton
    private AudioPlayer() {
        super();
        control = this;
        //Wanna give the AudioPlayer class a basic behaviour
        this.addBasicPlayerListener(new BasicPlayerListener() {

            @Override
            public void stateUpdated(BasicPlayerEvent event) {
                if (event.getCode() == BasicPlayerEvent.EOM) {
                    lastSeekMs = 0;
                    paused = true; //reset player state
                    opened = false;
                    log("EOM event catched, player resetted.");
                }
                if (event.getCode() == BasicPlayerEvent.SEEKING) {
                    isSeeking = true;
                }
                if (event.getCode() == BasicPlayerEvent.SEEKED) {
                    isSeeking = false;
                }
            }

            @Override
            public void setController(BasicController args) {
                //No need to use this
                System.out.println("hehehehehehe mateng");
            }

            @Override
            public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
                csms = microseconds;
                cpcmdata = pcmdata;
            }

            @Override
            public void opened(Object stream, Map properties) {
                log("Opened event caught");
                Object[] e = properties.entrySet().toArray();
                Object[] k = properties.keySet().toArray();
                String line = "Stream properties:";
                for (int i = 0; i < properties.size(); i++) {
                    line += "\n\t" + k[i] + ":" + e[i];
                }
                log(line);
                //Set Audio Properties
                File file = new File(playlist.get(index));
                long audioFileLength = file.length();
                int frameSize = (int) properties.get("mp3.framesize.bytes");
                float frameRate = (float) properties.get("mp3.framerate.fps");
                audioFrameSize = frameSize;
                audioFrameRate = frameRate;
                audioDurationInSeconds = (audioFileLength / (frameSize * frameRate));
                log("\tframesize " + frameSize + " framerate " + frameRate);
                log("\tAudio File lenght in seconds is: " + audioDurationInSeconds);
            }
        });
    }

    public static AudioPlayer getInstance() {
        if (instance == null) {
            instance = new AudioPlayer();
        }
        return instance;
    }
    /////////////////////////////////////

    @Override
    public void play() throws BasicPlayerException {
        if (playlist.size() == 0) {
            return;
        }
        if (!paused || !opened) {
            File f = new File(playlist.get(index));
            log("Opening file... " + f.getAbsolutePath());
            open(f);
            opened = true;
            super.play();
        }
        if (paused) {
            super.resume();
        }
        paused = false;
    }

    @Override
    public void pause() throws BasicPlayerException {
        log("Paused");
        paused = true;
        super.pause();
    }

    @Override
    public void stop() throws BasicPlayerException {
        paused = false;
        super.stop();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isOpenFile() {
        return opened;
    }

    public ArrayList<String> getPlaylist() {
        return playlist;
    }

    public int getIndexSong() {
        return index;
    }

    public void setIndexSong(int index) {
        this.index = index;
        lastSeekMs = 0;
    }

    public boolean isSeeking() {
        return isSeeking;
    }

    /**
     * goes to the next song in playlist and plays it
     *
     * @throws BasicPlayerException
     */
    public void nextSong() throws BasicPlayerException {
        if (playlist.size() == 0) {
            return;
        }
        lastSeekMs = 0;
        paused = false;
        index = (index + 1) % playlist.size();
        play();
    }

    /**
     * goes to the previous song and plays it
     *
     * @throws BasicPlayerException
     */
    public void prvSong() throws BasicPlayerException {
        if (playlist.size() == 0) {
            return;
        }
        lastSeekMs = 0;
        paused = false;
        index = (index - 1) % playlist.size();
        play();
    }

    public void setVolume(double volume) throws BasicPlayerException {
        control.setGain(volume);
    }

    /**
     * Adds a song to the playlist
     *
     * @param songPath
     */
    public void addSong(String songPath) {
        playlist.add(songPath);
    }

    /**
     * Remove a song by index
     *
     * @param index
     */
    public void removeSong(int index) {
        playlist.remove(index);
    }

    /**
     * Remove a song by songPath
     *
     * @param songPath
     */
    public void removeSong(String songPath) {
        playlist.remove(songPath);
    }

    public byte[] getPcmData() {
        return cpcmdata;
    }

    public long getProgressMicroseconds() {
        return csms + lastSeekMs;
    }

    public float getAudioDurationSeconds() {
        return audioDurationInSeconds;
    }

    public float getAudioFrameRate() {
        return audioFrameRate;
    }

    public float getAudioFrameSize() {
        return audioFrameSize;
    }

    /**
     * Remembers what's the last position relative to the playing song when
     * seeking
     */
    public void setLastSeekPositionInMs(int seekMs) {
        lastSeekMs = seekMs;
    }

    /**
     * For logging
     *
     * @param line
     */
    private void log(String line) {
        System.out.println("AudioPlayer] " + line);
        MainView.stf.addText("AudioPlayer] " + line);
    }
}
