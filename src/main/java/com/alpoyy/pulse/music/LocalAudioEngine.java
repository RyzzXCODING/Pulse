package com.alpoyy.pulse.music;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class LocalAudioEngine implements PlaybackEngine {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer audioPlayer;
    private final LocalAudioListener listener;
    
    private Song currentSong;
    private PlaybackListener playbackListener;
    private float volume = 1.0f;
    private PlaybackState state = PlaybackState.STOPPED;
    
    public LocalAudioEngine() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        
        this.audioPlayer = playerManager.createPlayer();
        this.listener = new LocalAudioListener();
        this.audioPlayer.addListener(listener);
    }
    
    @Override
    public void play(Song song) {
        if (song == null || song.getFilePath() == null) {
            Pulse.LOGGER.warn("Cannot play null song or song without file path");
            if (playbackListener != null) {
                playbackListener.onTrackError(song, new IllegalArgumentException("Invalid song"));
            }
            return;
        }
        
        File file = new File(song.getFilePath());
        if (!file.exists() || !file.isFile()) {
            Pulse.LOGGER.warn("Audio file not found: {}", song.getFilePath());
            if (playbackListener != null) {
                playbackListener.onTrackError(song, new IllegalArgumentException("File not found: " + song.getFilePath()));
            }
            return;
        }
        
        stop();
        
        this.currentSong = song;
        this.state = PlaybackState.BUFFERING;
        
        if (playbackListener != null) {
            playbackListener.onPlaybackStateChanged(state);
        }
        
        playerManager.loadItem(file.toURI().toString(), new com.sedmelluq.discord.lavaplayer.tools.FriendlyException(
            "Loading audio file",
            com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON,
            null
        ) {
            @Override
            public void trackLoaded(AudioTrack track) {
                audioPlayer.playTrack(track);
                state = PlaybackState.PLAYING;
                
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(state);
                    playbackListener.onTrackStart(currentSong);
                }
                
                Pulse.LOGGER.info("Started playing: {}", currentSong.getTitle());
            }
            
            @Override
            public void loadFailed(Throwable throwable) {
                state = PlaybackState.ERROR;
                
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(state);
                    playbackListener.onTrackError(currentSong, throwable);
                }
                
                Pulse.LOGGER.error("Failed to load audio file: {}", currentSong.getFilePath(), throwable);
            }
        });
    }
    
    @Override
    public void pause() {
        if (state == PlaybackState.PLAYING) {
            audioPlayer.setPaused(true);
            state = PlaybackState.PAUSED;
            
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(state);
            }
        }
    }
    
    @Override
    public void resume() {
        if (state == PlaybackState.PAUSED) {
            audioPlayer.setPaused(false);
            state = PlaybackState.PLAYING;
            
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(state);
            }
        }
    }
    
    @Override
    public void stop() {
        audioPlayer.stopTrack();
        currentSong = null;
        state = PlaybackState.STOPPED;
        
        if (playbackListener != null) {
            playbackListener.onPlaybackStateChanged(state);
        }
    }
    
    @Override
    public void seek(long positionMs) {
        if (audioPlayer.getPlayingTrack() != null) {
            audioPlayer.getPlayingTrack().setPosition(positionMs);
        }
    }
    
    @Override
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        audioPlayer.setVolume((int) (this.volume * 100));
    }
    
    @Override
    public float getVolume() {
        return volume;
    }
    
    @Override
    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }
    
    @Override
    public boolean isPaused() {
        return state == PlaybackState.PAUSED;
    }
    
    @Override
    public long getCurrentPosition() {
        AudioTrack track = audioPlayer.getPlayingTrack();
        return track != null ? track.getPosition() : 0;
    }
    
    @Override
    public long getDuration() {
        AudioTrack track = audioPlayer.getPlayingTrack();
        return track != null ? track.getDuration() : 0;
    }
    
    @Override
    public Song getCurrentSong() {
        return currentSong;
    }
    
    @Override
    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    @Override
    public void shutdown() {
        stop();
        audioPlayer.destroy();
        playerManager.shutdown();
    }
    
    private class LocalAudioListener extends com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer 
            implements com.sedmelluq.discord.lavaplayer.track.AudioEventListener {
        
        @Override
        public void onTrackStart(AudioPlayer player, AudioTrack track) {
            // Handled in loadItem callback
        }
        
        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (currentSong != null && playbackListener != null) {
                if (endReason.mayStartNext) {
                    playbackListener.onTrackEnd(currentSong);
                } else {
                    playbackListener.onTrackError(currentSong, new Exception("Track ended: " + endReason));
                }
            }
            currentSong = null;
            state = PlaybackState.STOPPED;
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(state);
            }
        }
        
        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            if (currentSong != null && playbackListener != null) {
                playbackListener.onTrackError(currentSong, exception);
            }
            state = PlaybackState.ERROR;
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(state);
            }
        }
        
        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            if (playbackListener != null) {
                playbackListener.onTrackError(currentSong, new Exception("Track stuck"));
            }
        }
    }
}