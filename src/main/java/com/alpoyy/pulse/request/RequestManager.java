package com.alpoyy.pulse.request;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.live.LiveChatEvent;
import com.alpoyy.pulse.music.PlaybackController;
import com.alpoyy.pulse.music.Song;
import com.alpoyy.pulse.widget.NotificationWidget;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class RequestManager {
    private static RequestManager instance;
    
    private final RequestParser parser;
    private final SongResolver resolver;
    private final SongQueue queue;
    private final ConfigManager configManager;
    private final PlaybackController playbackController;
    
    private final CopyOnWriteArrayList<Consumer<SongRequest>> requestListeners = new CopyOnWriteArrayList<>();
    private boolean enabled = true;
    private boolean autoPlay = true;
    
    private RequestManager() {
        this.parser = new RequestParser();
        this.resolver = new SongResolver();
        this.queue = new SongQueue();
        this.configManager = ConfigManager.getInstance();
        this.playbackController = PlaybackController.getInstance();
    }
    
    public static RequestManager getInstance() {
        if (instance == null) {
            instance = new RequestManager();
        }
        return instance;
    }
    
    public void initialize() {
        enabled = true;
        autoPlay = true;
        Pulse.LOGGER.info("RequestManager initialized");
    }
    
    public void processChatMessage(String username, String message) {
        if (!enabled) return;
        
        LiveChatEvent event = new LiveChatEvent();
        event.setEventType("comment");
        event.setUsername(username);
        event.setMessage(message);
        
        processChatEvent(event);
    }
    
    public void processChatEvent(LiveChatEvent event) {
        if (!enabled || event == null) return;
        
        RequestParser.ParsedRequest parsed = parser.parse(event);
        if (parsed == null || !parsed.isValid()) {
            return;
        }
        
        SongRequest request = new SongRequest(
            parsed.getSongName(),
            event.getUsername(),
            event.getMessage()
        );
        request.setArtist(parsed.getArtist());
        
        notifyRequestReceived(request);
        
        // Resolve asynchronously
        resolver.resolveAsync(request, new SongResolver.ResolutionCallback() {
            @Override
            public void onResolved(SongRequest resolvedRequest) {
                boolean added = queue.addRequest(resolvedRequest);
                if (added) {
                    resolvedRequest.setStatus(SongRequest.RequestStatus.QUEUED);
                    notifyRequestQueued(resolvedRequest);
                    
                    // Show notification
                    showNotification("Song Request", 
                        resolvedRequest.getRequester() + " requested: " + resolvedRequest.getDisplayName());
                    
                    // Auto-play if enabled and nothing is playing
                    if (autoPlay && !playbackController.isPlaying() && !playbackController.isPaused()) {
                        playNext();
                    }
                } else {
                    notifyRequestRejected(resolvedRequest);
                }
            }
            
            @Override
            public void onUnavailable(SongRequest unresolvedRequest) {
                queue.addRequest(unresolvedRequest); // Still add to queue but marked unavailable
                notifyRequestUnavailable(unresolvedRequest);
                showNotification("Request Unavailable", 
                    unresolvedRequest.getRequester() + " requested: " + unresolvedRequest.getDisplayName() + " (not found)");
            }
            
            @Override
            public void onError(SongRequest failedRequest, Throwable error) {
                failedRequest.setStatus(SongRequest.RequestStatus.FAILED);
                failedRequest.setValidationResult("Error: " + error.getMessage());
                notifyRequestError(failedRequest, error);
            }
        });
    }
    
    public void playNext() {
        SongRequest next = queue.getNext();
        if (next != null && next.getResolvedSong() != null) {
            playbackController.playLocal(next.getResolvedSong());
            next.setStatus(SongRequest.RequestStatus.PLAYING);
            notifyRequestPlaying(next);
        }
    }
    
    public void onTrackEnded() {
        queue.markCurrentCompleted();
        playNext();
    }
    
    public void skipCurrent() {
        SongRequest current = queue.getCurrent();
        if (current != null) {
            queue.skipCurrent();
            notifyRequestSkipped(current);
            playNext();
        }
    }
    
    public void clearQueue() {
        queue.clearQueue();
        notifyQueueCleared();
    }
    
    public void removeRequest(String requestId) {
        queue.removeRequest(requestId);
        notifyQueueChanged();
    }
    
    public List<SongRequest> getQueue() {
        return queue.getQueue();
    }
    
    public List<SongRequest> getPendingRequests() {
        return queue.getPendingRequests();
    }
    
    public SongRequest getCurrentRequest() {
        return queue.getCurrent();
    }
    
    public int getQueueSize() {
        return queue.getQueueSize();
    }
    
    public int getPendingCount() {
        return queue.getPendingCount();
    }
    
    public List<SongRequest> getUserRequests(String username) {
        return queue.getUserRequests(username);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }
    
    public boolean isAutoPlay() {
        return autoPlay;
    }
    
    public void addRequestListener(Consumer<SongRequest> listener) {
        if (listener != null) {
            requestListeners.add(listener);
        }
    }
    
    public void removeRequestListener(Consumer<SongRequest> listener) {
        requestListeners.remove(listener);
    }
    
    private void notifyRequestReceived(SongRequest request) {
        for (Consumer<SongRequest> listener : requestListeners) {
            try {
                listener.accept(request);
            } catch (Exception e) {
                Pulse.LOGGER.warn("Error in request listener", e);
            }
        }
    }
    
    private void notifyRequestQueued(SongRequest request) {
        // Additional notification for queued
    }
    
    private void notifyRequestPlaying(SongRequest request) {
        showNotification("Now Playing", request.getDisplayName() + " (requested by " + request.getRequester() + ")");
    }
    
    private void notifyRequestSkipped(SongRequest request) {
        showNotification("Skipped", request.getDisplayName());
    }
    
    private void notifyRequestRejected(SongRequest request) {
        showNotification("Request Rejected", request.getValidationResult());
    }
    
    private void notifyRequestUnavailable(SongRequest request) {
        // Already notified in callback
    }
    
    private void notifyRequestError(SongRequest request, Throwable error) {
        showNotification("Request Error", request.getValidationResult());
    }
    
    private void notifyQueueCleared() {
        showNotification("Queue Cleared", "All requests removed");
    }
    
    private void notifyQueueChanged() {
        // Notify listeners of queue change
    }
    
    private void showNotification(String title, String message) {
        NotificationWidget widget = (NotificationWidget) com.alpoyy.pulse.widget.WidgetManager.getInstance().getWidget("notifications");
        if (widget != null) {
            widget.addNotification(title, message);
        }
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public SongQueue getQueueInstance() {
        return queue;
    }
}