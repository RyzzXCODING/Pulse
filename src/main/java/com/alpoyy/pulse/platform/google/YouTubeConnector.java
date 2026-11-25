package com.alpoyy.pulse.platform.google;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.music.Playlist;
import com.alpoyy.pulse.music.Song;
import com.alpoyy.pulse.platform.PlatformConnector;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class YouTubeConnector implements PlatformConnector {
    private static final String PLATFORM_ID = "youtube";
    private static final String PLATFORM_NAME = "YouTube";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";
    
    private static final String CLIENT_ID = System.getenv("PULSE_YOUTUBE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("PULSE_YOUTUBE_CLIENT_SECRET");
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    
    private static final String[] SCOPES = {
        "https://www.googleapis.com/auth/youtube.readonly",
        "https://www.googleapis.com/auth/youtubepartner"
    };
    
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    private TokenResult tokens;
    private final HttpClient httpClient;
    private final ConfigManager configManager;
    
    public YouTubeConnector() {
        this.httpClient = HttpClient.newHttpClient();
        this.configManager = ConfigManager.getInstance();
        this.tokens = loadCredentials();
        
        if (tokens != null && tokens.accessToken() != null && tokens.expiresAt() > System.currentTimeMillis()) {
            this.status = ConnectionStatus.AUTHENTICATED;
        }
    }
    
    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }
    
    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        if (tokens != null && tokens.expiresAt() <= System.currentTimeMillis()) {
            status = ConnectionStatus.TOKEN_EXPIRED;
        }
        return status;
    }
    
    @Override
    public CompletableFuture<AuthenticationResult> authenticate() {
        return CompletableFuture.supplyAsync(() -> {
            if (CLIENT_ID == null || CLIENT_SECRET == null) {
                return new AuthenticationResult(false, 
                    "YouTube API credentials not configured. Set PULSE_YOUTUBE_CLIENT_ID and PULSE_YOUTUBE_CLIENT_SECRET environment variables.", 
                    null);
            }
            
            status = ConnectionStatus.CONNECTING;
            
            // In a real implementation, this would open a browser for OAuth
            // For now, return the authorization URL for manual completion
            String authUrl = getAuthorizationUrl();
            return new AuthenticationResult(true, 
                "Please open this URL in your browser to authorize: " + authUrl, 
                null);
        });
    }
    
    @Override
    public void disconnect() {
        this.tokens = null;
        this.status = ConnectionStatus.DISCONNECTED;
        clearCredentials();
    }
    
    @Override
    public boolean isAuthenticated() {
        return tokens != null && tokens.accessToken() != null && tokens.expiresAt() > System.currentTimeMillis();
    }
    
    @Override
    public CompletableFuture<List<Playlist>> getPlaylists() {
        return authenticatedRequest(() -> {
            String url = API_BASE + "/playlists?part=snippet,contentDetails&mine=true&maxResults=50";
            JsonObject response = sendGetRequest(url).get();
            return parsePlaylists(response);
        });
    }
    
    @Override
    public CompletableFuture<List<Song>> getPlaylistTracks(String playlistId) {
        return authenticatedRequest(() -> {
            String url = API_BASE + "/playlistItems?part=snippet,contentDetails&playlistId=" + playlistId + "&maxResults=50";
            JsonObject response = sendGetRequest(url).get();
            return parsePlaylistTracks(response);
        });
    }
    
    @Override
    public CompletableFuture<List<Song>> getLibraryTracks() {
        // YouTube doesn't have a traditional "library" - return liked videos or uploads
        return authenticatedRequest(() -> {
            String url = API_BASE + "/playlistItems?part=snippet,contentDetails&playlistId=LL&maxResults=50";
            JsonObject response = sendGetRequest(url).get();
            return parsePlaylistTracks(response);
        });
    }
    
    @Override
    public CompletableFuture<List<Song>> searchTracks(String query) {
        return authenticatedRequest(() -> {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = API_BASE + "/search?part=snippet&q=" + encodedQuery + "&type=video&maxResults=20";
            JsonObject response = sendGetRequest(url).get();
            return parseSearchResults(response);
        });
    }
    
    @Override
    public CompletableFuture<Song> getTrackMetadata(String trackId) {
        return authenticatedRequest(() -> {
            String url = API_BASE + "/videos?part=snippet,contentDetails,statistics&id=" + trackId;
            JsonObject response = sendGetRequest(url).get();
            return parseSongFromVideo(response.getAsJsonArray("items").get(0).getAsJsonObject());
        });
    }
    
    @Override
    public CompletableFuture<Playlist> getPlaylistMetadata(String playlistId) {
        return authenticatedRequest(() -> {
            String url = API_BASE + "/playlists?part=snippet,contentDetails&id=" + playlistId;
            JsonObject response = sendGetRequest(url).get();
            return parsePlaylist(response.getAsJsonArray("items").get(0).getAsJsonObject());
        });
    }
    
    @Override
    public boolean supportsPlayback() {
        // YouTube Data API does not provide direct playback URLs
        // YouTube Music playback would require a different integration
        return false;
    }
    
    @Override
    public CompletableFuture<PlaybackInfo> getPlaybackInfo(String trackId) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Direct playback not supported via YouTube Data API. Use YouTube Music API or authorized player."));
    }
    
    @Override
    public String getAuthorizationUrl() {
        StringBuilder url = new StringBuilder(AUTH_URL);
        url.append("?client_id=").append(CLIENT_ID);
        url.append("&redirect_uri=").append(java.net.URLEncoder.encode(REDIRECT_URI, java.nio.charset.StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&scope=").append(java.net.URLEncoder.encode(String.join(" ", SCOPES), java.nio.charset.StandardCharsets.UTF_8));
        url.append("&access_type=offline");
        url.append("&prompt=consent");
        return url.toString();
    }
    
    @Override
    public CompletableFuture<TokenResult> exchangeCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = "code=" + java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8) +
                    "&client_id=" + java.net.URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8) +
                    "&client_secret=" + java.net.URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8) +
                    "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, java.nio.charset.StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    TokenResult result = new TokenResult(
                        json.get("access_token").getAsString(),
                        json.has("refresh_token") ? json.get("refresh_token").getAsString() : null,
                        System.currentTimeMillis() + (json.get("expires_in").getAsLong() * 1000),
                        json.get("scope").getAsString()
                    );
                    this.tokens = result;
                    this.status = ConnectionStatus.AUTHENTICATED;
                    saveCredentials(result);
                    return result;
                } else {
                    throw new IOException("Token exchange failed: " + response.body());
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<TokenResult> refreshToken(String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = "client_id=" + java.net.URLEncoder.encode(CLIENT_ID, java.nio.charset.StandardCharsets.UTF_8) +
                    "&client_secret=" + java.net.URLEncoder.encode(CLIENT_SECRET, java.nio.charset.StandardCharsets.UTF_8) +
                    "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, java.nio.charset.StandardCharsets.UTF_8) +
                    "&grant_type=refresh_token";
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    TokenResult result = new TokenResult(
                        json.get("access_token").getAsString(),
                        refreshToken,
                        System.currentTimeMillis() + (json.get("expires_in").getAsLong() * 1000),
                        json.get("scope").getAsString()
                    );
                    this.tokens = result;
                    this.status = ConnectionStatus.AUTHENTICATED;
                    saveCredentials(result);
                    return result;
                } else {
                    throw new IOException("Token refresh failed: " + response.body());
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public void saveCredentials(TokenResult tokens) {
        if (tokens != null) {
            configManager.getConfig().platformTokens.put(PLATFORM_ID, 
                new com.google.gson.Gson().toJson(tokens));
            configManager.saveAsync();
        }
    }
    
    @Override
    public TokenResult loadCredentials() {
        String json = configManager.getConfig().platformTokens.get(PLATFORM_ID);
        if (json != null && !json.isEmpty()) {
            try {
                return new com.google.gson.Gson().fromJson(json, TokenResult.class);
            } catch (Exception e) {
                Pulse.LOGGER.warn("Failed to parse stored YouTube credentials", e);
            }
        }
        return null;
    }
    
    @Override
    public void clearCredentials() {
        configManager.getConfig().platformTokens.remove(PLATFORM_ID);
        configManager.saveAsync();
    }
    
    private <T> CompletableFuture<T> authenticatedRequest(java.util.function.Supplier<CompletableFuture<T>> request) {
        if (!isAuthenticated()) {
            // Try to refresh token
            if (tokens != null && tokens.refreshToken() != null) {
                return refreshToken(tokens.refreshToken())
                    .thenCompose(v -> request.get())
                    .exceptionally(ex -> {
                        status = ConnectionStatus.ERROR;
                        throw new CompletionException(ex);
                    });
            }
            return CompletableFuture.failedFuture(
                new IllegalStateException("Not authenticated with YouTube"));
        }
        
        return request.get().exceptionally(ex -> {
            if (ex.getCause() instanceof IOException ioEx && ioEx.getMessage().contains("401")) {
                status = ConnectionStatus.TOKEN_EXPIRED;
            }
            throw new CompletionException(ex);
        });
    }
    
    private CompletableFuture<JsonObject> sendGetRequest(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + tokens.accessToken())
                    .header("Accept", "application/json")
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                } else {
                    throw new IOException("API request failed: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    private List<Playlist> parsePlaylists(JsonObject response) {
        List<Playlist> result = new ArrayList<>();
        JsonArray items = response.getAsJsonArray("items");
        if (items != null) {
            for (var item : items) {
                result.add(parsePlaylist(item.getAsJsonObject()));
            }
        }
        return result;
    }
    
    private Playlist parsePlaylist(JsonObject item) {
        Playlist playlist = new Playlist();
        playlist.setId(item.get("id").getAsString());
        playlist.setSource(Song.Source.YOUTUBE);
        playlist.setSourceId(item.get("id").getAsString());
        
        JsonObject snippet = item.getAsJsonObject("snippet");
        playlist.setName(snippet.get("title").getAsString());
        playlist.setDescription(snippet.get("description").getAsString());
        
        JsonObject thumbnails = snippet.getAsJsonObject("thumbnails");
        if (thumbnails != null) {
            JsonObject high = thumbnails.getAsJsonObject("high");
            if (high != null) {
                playlist.setArtworkUrl(high.get("url").getAsString());
            }
        }
        
        JsonObject contentDetails = item.getAsJsonObject("contentDetails");
        if (contentDetails != null) {
            int itemCount = contentDetails.get("itemCount").getAsInt();
            // Note: We don't fetch all tracks here, just metadata
        }
        
        return playlist;
    }
    
    private List<Song> parsePlaylistTracks(JsonObject response) {
        List<Song> result = new ArrayList<>();
        JsonArray items = response.getAsJsonArray("items");
        if (items != null) {
            for (var item : items) {
                JsonObject snippet = item.getAsJsonObject().getAsJsonObject("snippet");
                JsonObject contentDetails = item.getAsJsonObject().getAsJsonObject("contentDetails");
                
                Song song = parseSongFromVideo(snippet);
                song.setSourceId(contentDetails.get("videoId").getAsString());
                result.add(song);
            }
        }
        return result;
    }
    
    private List<Song> parseSearchResults(JsonObject response) {
        List<Song> result = new ArrayList<>();
        JsonArray items = response.getAsJsonArray("items");
        if (items != null) {
            for (var item : items) {
                JsonObject id = item.getAsJsonObject().getAsJsonObject("id");
                if ("youtube#video".equals(id.get("kind").getAsString())) {
                    JsonObject snippet = item.getAsJsonObject().getAsJsonObject("snippet");
                    Song song = parseSongFromVideo(snippet);
                    song.setSourceId(id.get("videoId").getAsString());
                    result.add(song);
                }
            }
        }
        return result;
    }
    
    private Song parseSongFromVideo(JsonObject snippet) {
        Song song = new Song();
        song.setSource(Song.Source.YOUTUBE);
        
        song.setTitle(snippet.get("title").getAsString());
        song.setArtist(snippet.get("channelTitle").getAsString());
        
        JsonObject thumbnails = snippet.getAsJsonObject("thumbnails");
        if (thumbnails != null) {
            JsonObject high = thumbnails.getAsJsonObject("high");
            if (high != null) {
                song.setArtworkUrl(high.get("url").getAsString());
            }
        }
        
        return song;
    }
}