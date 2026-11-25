package com.alpoyy.pulse.music;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.request.SongRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlaybackController implements PlaybackEngine.PlaybackListener {
    private static PlaybackController instance;
    
    private PlaybackEngine currentEngine;
    private LocalAudioEngine localEngine;
    // Future: AuthorizedPlaybackEngine authorizedEngine;
    
    private final List<Song> queue = new CopyOnWriteArrayList<>();
    private int currentQueueIndex = -1;
    private boolean shuffle = false;
    private boolean repeat = false; // false = off, true = repeat one, 2 = repeat all
    private float masterVolume = 1.0f;
    
    private final List<PlaybackControllerListener> listeners = new CopyOnWriteArrayList<>();
    
    private PlaybackController() {
        this.localEngine = new LocalAudioEngine();
        this.currentEngine = localEngine;
        this.currentEngine.setPlaybackListener(this);
    }
    
    public static PlaybackController getInstance() {
        if (instance == null) {
            instance = new PlaybackController();
        }
        return instance;
    }
    
    public void initialize() {
        ConfigManager config = ConfigManager.getInstance();
        setMasterVolume(config.getConfig().masterVolume);
        setMusicVolume(config.getConfig().musicVolume);
        Pulse.LOGGER.info("PlaybackController initialized");
    }
    
    @Override
    public void onTrackStart(Song song) {
        notifyTrackStart(song);
    }
    
    @Override
    public void onTrackEnd(Song song) {
        notifyTrackEnd(song);
        playNext();
    }
    
    @Override
    public void onTrackError(Song song, Throwable error) {
        Pulse.LOGGER.error("Playback error for {}: {}", song != null ? song.getTitle() : "unknown", error.getMessage());
        notifyTrackError(song, error);
        playNext();
    }
    
    @Override
    public void onPlaybackStateChanged(PlaybackEngine.PlaybackState state) {
        notifyStateChanged(state);
    }
    
    @Override
    public void onPositionUpdate(long positionMs, long durationMs) {
        notifyPositionUpdate(positionMs, durationMs);
    }
    
    public void play(Song song) {
        if (song == null) return;
        
        currentEngine.play(song);
    }
    
    public void playLocal(Song song) {
        if (currentEngine != localEngine) {
            stop();
            currentEngine = localEngine;
        }
        play(song);
    }
    
    public void playFromQueue(int index) {
        if (index >= 0 && index < queue.size()) {
            currentQueueIndex = index;
            play(queue.get(index));
        }
    }
    
    public void playNext() {
        if (queue.isEmpty()) return;
        
        int nextIndex;
        if (shuffle) {
            nextIndex = (int) (Math.random() * queue.size());
        } else {
            nextIndex = currentQueueIndex + 1;
            if (nextIndex >= queue.size()) {
                if (repeat) { // repeat all
                    nextIndex = 0;
                } else {
                    stop();
                    return;
                }
            }
        }
        
        playFromQueue(nextIndex);
    }
    
    public void playPrevious() {
        if (queue.isEmpty()) return;
        
        int prevIndex = currentQueueIndex - 1;
        if (prevIndex < 0) {
            if (repeat) { // repeat all
                prevIndex = queue.size() - 1;
            } else {
                return;
            }
        }
        
        playFromQueue(prevIndex);
    }
    
    @Override
    public void pause() {
        currentEngine.pause();
    }
    
    @Override
    public void resume() {
        currentEngine.resume();
    }
    
    public void togglePlayPause() {
        if (currentEngine.isPlaying()) {
            pause();
        } else if (currentEngine.isPaused()) {
            resume();
        } else if (currentQueueIndex >= 0 && currentQueueIndex < queue.size()) {
            playFromQueue(currentQueueIndex);
        }
    }
    
    @Override
    public void stop() {
        currentEngine.stop();
        currentQueueIndex = -1;
    }
    
    @Override
    public void seek(long positionMs) {
        currentEngine.seek(positionMs);
    }
    
    @Override
    public void setVolume(float volume) {
        float effectiveVolume = volume * masterVolume;
        currentEngine.setVolume(effectiveVolume);
    }
    
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        setVolume(currentEngine.getVolume());
    }
    
    public void setMusicVolume(float volume) {
        currentEngine.setVolume(volume * masterVolume);
    }
    
    @Override
    public float getVolume() {
        return currentEngine.getVolume();
    }
    
    @Override
    public boolean isPlaying() {
        return currentEngine.isPlaying();
    }
    
    @Override
    public boolean isPaused() {
        return currentEngine.isPaused();
    }
    
    @Override
    public long getCurrentPosition() {
        return currentEngine.getCurrentPosition();
    }
    
    @Override
    public long getDuration() {
        return currentEngine.getDuration();
    }
    
    @Override
    public Song getCurrentSong() {
        return currentEngine.getCurrentSong();
    }
    
    public void addToQueue(Song song) {
        if (song != null) {
            queue.add(song);
            if (currentQueueIndex == -1) {
                currentQueueIndex = 0;
            }
            notifyQueueChanged();
        }
    }
    
    public void addToQueue(List<Song> songs) {
        if (songs != null && !songs.isEmpty()) {
            queue.addAll(songs);
            if (currentQueueIndex == -1) {
                currentQueueIndex = 0;
            }
            notifyQueueChanged();
        }
    }
    
    public void removeFromQueue(int index) {
        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            if (index <= currentQueueIndex && currentQueueIndex > 0) {
                currentQueueIndex--;
            } else if (queue.isEmpty()) {
                currentQueueIndex = -1;
            }
            notifyQueueChanged();
        }
    }
    
    public void clearQueue() {
        queue.clear();
        currentQueueIndex = -1;
        notifyQueueChanged();
    }
    
    public List<Song> getQueue() {
        return new ArrayList<>(queue);
    }
    
    public int getCurrentQueueIndex() {
        return currentQueueIndex;
    }
    
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }
    
    public boolean isShuffle() {
        return shuffle;
    }
    
    public void setRepeatMode(int mode) {
        this.repeat = mode != 0;
    }
    
    public boolean isRepeat() {
        return repeat;
    }
    
    public void addListener(PlaybackControllerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(PlaybackControllerListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyTrackStart(Song song) {
        for (PlaybackControllerListener listener : listeners) {
            listener.onTrackStart(song);
        }
    }
    
    private void notifyTrackEnd(Song song) {
        for (PlaybackControllerListener listener : listeners) {
            listener.onTrackEnd(song);
        }
    }
    
    private void notifyTrackError(Song song, Throwable error) {
        for (PlaybackControllerListener listener : listeners) {
            listener.onTrackError(song, error);
        }
    }
    
    private void notifyStateChanged(PlaybackEngine.PlaybackState state) {
        for (PlaybackControllerListener listener : listeners) {
            listener.onStateChanged(state);
        }
    }
    
    private void notifyPositionUpdate(long positionMs, long durationMs) {
        for (PlaybackControllerListener listener : listeners) {
            listener.onPositionUpdate(positionMs, durationMs);
        }
    }
    
    private void notifyQueueChanged() {
        for (PlaybackControllerListener listener : listeners) {
            listener.onQueueChanged(new ArrayList<>(queue), currentQueueIndex);
        }
    }
    
    public void shutdown() {
        if (localEngine != null) {
            localEngine.shutdown();
        }
        // if (authorizedEngine != null) authorizedEngine.shutdown();
        listeners.clear();
    }
    
    public interface PlaybackControllerListener {
        void onTrackStart(Song song);
        void onTrackEnd(Song song);
        void onTrackError(Song song, Throwable error);
        void onStateChanged(PlaybackEngine.PlaybackState state);
        void onPositionUpdate(long positionMs, long durationMs);
        void onQueueChanged(List<Song> queue, int currentIndex);
    }
}