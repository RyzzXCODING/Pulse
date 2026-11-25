package com.alpoyy.pulse.request;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.music.MusicLibrary;
import com.alpoyy.pulse.music.Song;
import com.alpoyy.pulse.platform.PlatformManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SongResolver {
    private final MusicLibrary musicLibrary;
    private final PlatformManager platformManager;
    
    public SongResolver() {
        this.musicLibrary = new MusicLibrary();
        this.platformManager = PlatformManager.getInstance();
    }
    
    public CompletableFuture<Song> resolve(SongRequest request) {
        request.setStatus(SongRequest.RequestStatus.SEARCHING);
        
        // First, try local library
        return searchLocal(request)
            .thenCompose(song -> {
                if (song != null) {
                    return CompletableFuture.completedFuture(song);
                }
                
                // Then try connected platforms
                return searchPlatforms(request);
            })
            .exceptionally(ex -> {
                Pulse.LOGGER.error("Error resolving song request: {}", ex.getMessage());
                request.setStatus(SongRequest.RequestStatus.FAILED);
                request.setValidationResult("Resolution error: " + ex.getMessage());
                return null;
            });
    }
    
    private CompletableFuture<Song> searchLocal(SongRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<Song> results = musicLibrary.searchSongs(request.getSongName());
            
            // Try exact match first
            for (Song song : results) {
                if (matchesRequest(song, request)) {
                    request.setStatus(SongRequest.RequestStatus.AVAILABLE);
                    request.setValidationResult("Found in local library");
                    return song;
                }
            }
            
            // Try fuzzy match
            if (!results.isEmpty()) {
                Song song = results.get(0);
                request.setStatus(SongRequest.RequestStatus.AVAILABLE);
                request.setValidationResult("Found similar in local library");
                return song;
            }
            
            return null;
        });
    }
    
    private CompletableFuture<Song> searchPlatforms(SongRequest request) {
        // Search YouTube if connected
        PlatformConnector youtube = platformManager.getConnector("youtube");
        if (youtube != null && youtube.isAuthenticated()) {
            return youtube.searchTracks(request.getSongName())
                .thenApply(results -> {
                    if (results != null && !results.isEmpty()) {
                        Song song = results.get(0);
                        request.setStatus(SongRequest.RequestStatus.AVAILABLE);
                        request.setValidationResult("Found on YouTube (metadata only)");
                        return song;
                    }
                    return null;
                })
                .exceptionally(ex -> {
                    Pulse.LOGGER.debug("YouTube search failed: {}", ex.getMessage());
                    return null;
                });
        }
        
        // No platform available
        return CompletableFuture.completedFuture(null);
    }
    
    private boolean matchesRequest(Song song, SongRequest request) {
        String songTitle = song.getTitle().toLowerCase();
        String requestTitle = request.getSongName().toLowerCase();
        
        if (songTitle.equals(requestTitle)) return true;
        if (songTitle.contains(requestTitle) || requestTitle.contains(songTitle)) return true;
        
        if (request.getArtist() != null) {
            String songArtist = song.getArtist().toLowerCase();
            String requestArtist = request.getArtist().toLowerCase();
            if (songArtist.equals(requestArtist) || songArtist.contains(requestArtist)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void resolveAsync(SongRequest request, ResolutionCallback callback) {
        resolve(request).thenAccept(song -> {
            if (song != null) {
                request.setResolvedSong(song);
                callback.onResolved(request);
            } else {
                request.setStatus(SongRequest.RequestStatus.UNAVAILABLE);
                request.setValidationResult("Song not found in available sources");
                callback.onUnavailable(request);
            }
        }).exceptionally(ex -> {
            request.setStatus(SongRequest.RequestStatus.FAILED);
            request.setValidationResult("Resolution failed: " + ex.getMessage());
            callback.onError(request, ex);
            return null;
        });
    }
    
    public interface ResolutionCallback {
        void onResolved(SongRequest request);
        void onUnavailable(SongRequest request);
        void onError(SongRequest request, Throwable error);
    }
}