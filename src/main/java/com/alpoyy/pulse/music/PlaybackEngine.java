package com.alpoyy.pulse.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public interface PlaybackEngine {
    
    void play(Song song);
    
    void pause();
    
    void resume();
    
    void stop();
    
    void seek(long positionMs);
    
    void setVolume(float volume);
    
    float getVolume();
    
    boolean isPlaying();
    
    boolean isPaused();
    
    long getCurrentPosition();
    
    long getDuration();
    
    Song getCurrentSong();
    
    void setPlaybackListener(PlaybackListener listener);
    
    void shutdown();
    
    enum PlaybackState {
        STOPPED,
        PLAYING,
        PAUSED,
        BUFFERING,
        ERROR
    }
    
    interface PlaybackListener {
        void onTrackStart(Song song);
        void onTrackEnd(Song song);
        void onTrackError(Song song, Throwable error);
        void onPlaybackStateChanged(PlaybackState state);
        void onPositionUpdate(long positionMs, long durationMs);
    }
}