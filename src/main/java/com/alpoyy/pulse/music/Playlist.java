package com.alpoyy.pulse.music;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Playlist {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("artwork_url")
    private String artworkUrl;
    
    @SerializedName("songs")
    private List<Song> songs;
    
    @SerializedName("created_at")
    private Instant createdAt;
    
    @SerializedName("updated_at")
    private Instant updatedAt;
    
    @SerializedName("source")
    private Song.Source source;
    
    @SerializedName("source_id")
    private String sourceId;
    
    public Playlist() {
        this.id = UUID.randomUUID().toString();
        this.songs = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.source = Song.Source.LOCAL;
    }
    
    public Playlist(String name) {
        this();
        this.name = name;
    }
    
    public Playlist(String name, String description) {
        this(name);
        this.description = description;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }
    
    public String getArtworkUrl() {
        return artworkUrl;
    }
    
    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
        this.updatedAt = Instant.now();
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }
    
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
            this.updatedAt = Instant.now();
        }
    }
    
    public void removeSong(Song song) {
        songs.remove(song);
        this.updatedAt = Instant.now();
    }
    
    public void removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            songs.remove(index);
            this.updatedAt = Instant.now();
        }
    }
    
    public int getSongCount() {
        return songs.size();
    }
    
    public long getTotalDurationMs() {
        return songs.stream().mapToLong(Song::getDurationMs).sum();
    }
    
    public String getFormattedTotalDuration() {
        long seconds = getTotalDurationMs() / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds %= 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Song.Source getSource() {
        return source;
    }
    
    public void setSource(Song.Source source) {
        this.source = source;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return Objects.equals(id, playlist.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Playlist{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", songCount=" + songs.size() +
                ", source=" + source +
                '}';
    }
}