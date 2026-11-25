package com.alpoyy.pulse.request;

import com.alpoyy.pulse.music.Song;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.Objects;

public class SongRequest {
    @SerializedName("id")
    private String id;
    
    @SerializedName("song_name")
    private String songName;
    
    @SerializedName("artist")
    private String artist;
    
    @SerializedName("requester")
    private String requester;
    
    @SerializedName("original_message")
    private String originalMessage;
    
    @SerializedName("timestamp")
    private Instant timestamp;
    
    @SerializedName("queue_position")
    private int queuePosition;
    
    @SerializedName("source")
    private Song.Source source;
    
    @SerializedName("source_id")
    private String sourceId;
    
    @SerializedName("status")
    private RequestStatus status;
    
    @SerializedName("validation_result")
    private String validationResult;
    
    @SerializedName("resolved_song")
    private Song resolvedSong;
    
    public SongRequest() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.status = RequestStatus.RECEIVED;
        this.queuePosition = -1;
    }
    
    public SongRequest(String songName, String requester, String originalMessage) {
        this();
        this.songName = songName;
        this.requester = requester;
        this.originalMessage = originalMessage;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSongName() {
        return songName;
    }
    
    public void setSongName(String songName) {
        this.songName = songName;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getRequester() {
        return requester;
    }
    
    public void setRequester(String requester) {
        this.requester = requester;
    }
    
    public String getOriginalMessage() {
        return originalMessage;
    }
    
    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getQueuePosition() {
        return queuePosition;
    }
    
    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
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
    
    public RequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(RequestStatus status) {
        this.status = status;
    }
    
    public String getValidationResult() {
        return validationResult;
    }
    
    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }
    
    public Song getResolvedSong() {
        return resolvedSong;
    }
    
    public void setResolvedSong(Song resolvedSong) {
        this.resolvedSong = resolvedSong;
        if (resolvedSong != null) {
            this.source = resolvedSong.getSource();
            this.sourceId = resolvedSong.getSourceId();
        }
    }
    
    public boolean isResolved() {
        return resolvedSong != null && status == RequestStatus.AVAILABLE;
    }
    
    public String getDisplayName() {
        if (artist != null && !artist.isEmpty()) {
            return artist + " - " + songName;
        }
        return songName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SongRequest that = (SongRequest) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "SongRequest{" +
                "id='" + id + '\'' +
                ", songName='" + songName + '\'' +
                ", artist='" + artist + '\'' +
                ", requester='" + requester + '\'' +
                ", status=" + status +
                '}';
    }
    
    public enum RequestStatus {
        RECEIVED,
        SEARCHING,
        AVAILABLE,
        QUEUED,
        PLAYING,
        COMPLETED,
        UNAVAILABLE,
        REJECTED,
        FAILED
    }
}