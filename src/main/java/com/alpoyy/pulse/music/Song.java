package com.alpoyy.pulse.music;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.time.Instant;
import java.util.Objects;

public class Song {
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("artist")
    private String artist;
    
    @SerializedName("album")
    private String album;
    
    @SerializedName("duration_ms")
    private long durationMs;
    
    @SerializedName("file_path")
    private String filePath;
    
    @SerializedName("artwork_url")
    private String artworkUrl;
    
    @SerializedName("source")
    private Source source;
    
    @SerializedName("source_id")
    private String sourceId;
    
    @SerializedName("added_at")
    private Instant addedAt;
    
    public enum Source {
        LOCAL,
        YOUTUBE,
        SPOTIFY,
        SOUNDCLOUD,
        UNKNOWN
    }
    
    public Song() {
        this.addedAt = Instant.now();
        this.id = java.util.UUID.randomUUID().toString();
        this.source = Source.UNKNOWN;
    }
    
    public Song(String title, String artist, String filePath) {
        this();
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.source = Source.LOCAL;
        
        // Try to extract metadata from file
        extractMetadata(filePath);
    }
    
    private void extractMetadata(String filePath) {
        if (filePath == null) return;
        
        File file = new File(filePath);
        if (!file.exists()) return;
        
        // Try to parse from filename if no ID3 tags
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        
        // Common patterns: "Artist - Title" or "Title"
        if (title == null || title.isEmpty()) {
            int dashIndex = name.indexOf(" - ");
            if (dashIndex > 0) {
                this.artist = name.substring(0, dashIndex).trim();
                this.title = name.substring(dashIndex + 3).trim();
            } else {
                this.title = name.trim();
            }
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title != null ? title : "Unknown Title";
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist != null ? artist : "Unknown Artist";
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getArtworkUrl() {
        return artworkUrl;
    }
    
    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }
    
    public Source getSource() {
        return source;
    }
    
    public void setSource(Source source) {
        this.source = source;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
    
    public Instant getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
    
    public String getFormattedDuration() {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    public String getDisplayName() {
        return getArtist() + " - " + getTitle();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(id, song.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Song{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", source=" + source +
                '}';
    }
}