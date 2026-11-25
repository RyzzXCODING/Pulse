package com.alpoyy.pulse.platform;

import com.alpoyy.pulse.music.Playlist;
import com.alpoyy.pulse.music.Song;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PlatformConnector {
    
    String getPlatformId();
    
    String getPlatformName();
    
    ConnectionStatus getConnectionStatus();
    
    CompletableFuture<AuthenticationResult> authenticate();
    
    void disconnect();
    
    boolean isAuthenticated();
    
    // Library & Playlist
    CompletableFuture<List<Playlist>> getPlaylists();
    
    CompletableFuture<List<Song>> getPlaylistTracks(String playlistId);
    
    CompletableFuture<List<Song>> getLibraryTracks();
    
    CompletableFuture<List<Song>> searchTracks(String query);
    
    // Metadata
    CompletableFuture<Song> getTrackMetadata(String trackId);
    
    CompletableFuture<Playlist> getPlaylistMetadata(String playlistId);
    
    // Playback
    boolean supportsPlayback();
    
    CompletableFuture<PlaybackInfo> getPlaybackInfo(String trackId);
    
    // OAuth
    String getAuthorizationUrl();
    
    CompletableFuture<TokenResult> exchangeCode(String code);
    
    CompletableFuture<TokenResult> refreshToken(String refreshToken);
    
    // Configuration
    void saveCredentials(TokenResult tokens);
    
    TokenResult loadCredentials();
    
    void clearCredentials();
    
    enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        AUTHENTICATED,
        ERROR,
        TOKEN_EXPIRED
    }
    
    record AuthenticationResult(boolean success, String message, TokenResult tokens) {}
    
    record TokenResult(String accessToken, String refreshToken, long expiresAt, String scope) {}
    
    record PlaybackInfo(String streamUrl, String format, boolean drmProtected, long expiresAt) {}
}