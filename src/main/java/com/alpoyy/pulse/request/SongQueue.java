package com.alpoyy.pulse.request;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SongQueue {
    private final ConfigManager configManager;
    private final CopyOnWriteArrayList<SongRequest> queue = new CopyOnWriteArrayList<>();
    private final Map<String, Instant> userLastRequest = new ConcurrentHashMap<>();
    private final Map<String, Integer> userRequestCount = new ConcurrentHashMap<>();
    
    private int currentIndex = -1;
    private SongRequest currentRequest;
    
    public SongQueue() {
        this.configManager = ConfigManager.getInstance();
    }
    
    public synchronized boolean addRequest(SongRequest request) {
        PulseConfig config = configManager.getConfig();
        
        // Check max queue size
        if (queue.size() >= config.maxQueueSize) {
            request.setStatus(SongRequest.RequestStatus.REJECTED);
            request.setValidationResult("Queue is full (max " + config.maxQueueSize + ")");
            return false;
        }
        
        // Check duplicate prevention
        if (config.duplicatePrevention) {
            for (SongRequest existing : queue) {
                if (existing.getRequester().equalsIgnoreCase(request.getRequester()) &&
                    existing.getSongName().equalsIgnoreCase(request.getSongName()) &&
                    existing.getStatus() != SongRequest.RequestStatus.COMPLETED &&
                    existing.getStatus() != SongRequest.RequestStatus.REJECTED) {
                    request.setStatus(SongRequest.RequestStatus.REJECTED);
                    request.setValidationResult("Duplicate request");
                    return false;
                }
            }
        }
        
        // Check per-user request limit
        int userCount = userRequestCount.getOrDefault(request.getRequester().toLowerCase(), 0);
        if (userCount >= config.maxRequestsPerUser) {
            request.setStatus(SongRequest.RequestStatus.REJECTED);
            request.setValidationResult("Max requests per user reached (" + config.maxRequestsPerUser + ")");
            return false;
        }
        
        // Check user cooldown
        Instant lastRequest = userLastRequest.get(request.getRequester().toLowerCase());
        if (lastRequest != null) {
            long secondsSinceLastRequest = java.time.Duration.between(lastRequest, Instant.now()).getSeconds();
            if (secondsSinceLastRequest < config.userCooldownSeconds) {
                request.setStatus(SongRequest.RequestStatus.REJECTED);
                request.setValidationResult("Cooldown active (" + config.userCooldownSeconds + "s)");
                return false;
            }
        }
        
        // Add to queue
        request.setStatus(SongRequest.RequestStatus.QUEUED);
        request.setQueuePosition(queue.size() + 1);
        queue.add(request);
        
        // Update user tracking
        userLastRequest.put(request.getRequester().toLowerCase(), Instant.now());
        userRequestCount.merge(request.getRequester().toLowerCase(), 1, Integer::sum);
        
        updateQueuePositions();
        return true;
    }
    
    public synchronized SongRequest getNext() {
        if (queue.isEmpty()) {
            currentRequest = null;
            currentIndex = -1;
            return null;
        }
        
        // Find next playable request
        for (int i = 0; i < queue.size(); i++) {
            SongRequest request = queue.get(i);
            if (request.getStatus() == SongRequest.RequestStatus.QUEUED ||
                request.getStatus() == SongRequest.RequestStatus.AVAILABLE) {
                
                currentIndex = i;
                currentRequest = request;
                request.setStatus(SongRequest.RequestStatus.PLAYING);
                updateQueuePositions();
                return request;
            }
        }
        
        currentRequest = null;
        currentIndex = -1;
        return null;
    }
    
    public synchronized SongRequest getCurrent() {
        return currentRequest;
    }
    
    public synchronized void markCurrentCompleted() {
        if (currentRequest != null) {
            currentRequest.setStatus(SongRequest.RequestStatus.COMPLETED);
            
            PulseConfig config = configManager.getConfig();
            if (config.autoClearCompleted) {
                queue.remove(currentRequest);
            }
            
            currentRequest = null;
            currentIndex = -1;
            updateQueuePositions();
        }
    }
    
    public synchronized void skipCurrent() {
        if (currentRequest != null) {
            currentRequest.setStatus(SongRequest.RequestStatus.COMPLETED);
            currentRequest = null;
            currentIndex = -1;
            updateQueuePositions();
        }
    }
    
    public synchronized boolean removeRequest(String requestId) {
        SongRequest toRemove = queue.stream()
            .filter(r -> r.getId().equals(requestId))
            .findFirst()
            .orElse(null);
        
        if (toRemove != null) {
            queue.remove(toRemove);
            
            // Update user request count
            String requester = toRemove.getRequester().toLowerCase();
            userRequestCount.merge(requester, -1, (old, delta) -> Math.max(0, old + delta));
            
            if (toRemove == currentRequest) {
                currentRequest = null;
                currentIndex = -1;
            }
            
            updateQueuePositions();
            return true;
        }
        return false;
    }
    
    public synchronized void clearQueue() {
        queue.clear();
        currentRequest = null;
        currentIndex = -1;
    }
    
    public synchronized void clearCompleted() {
        queue.removeIf(r -> r.getStatus() == SongRequest.RequestStatus.COMPLETED);
        updateQueuePositions();
    }
    
    public List<SongRequest> getQueue() {
        return new ArrayList<>(queue);
    }
    
    public List<SongRequest> getPendingRequests() {
        return queue.stream()
            .filter(r -> r.getStatus() == SongRequest.RequestStatus.QUEUED ||
                        r.getStatus() == SongRequest.RequestStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
    
    public List<SongRequest> getUserRequests(String username) {
        return queue.stream()
            .filter(r -> r.getRequester().equalsIgnoreCase(username))
            .collect(Collectors.toList());
    }
    
    public int getQueueSize() {
        return queue.size();
    }
    
    public int getPendingCount() {
        return (int) queue.stream()
            .filter(r -> r.getStatus() == SongRequest.RequestStatus.QUEUED ||
                        r.getStatus() == SongRequest.RequestStatus.AVAILABLE)
            .count();
    }
    
    private void updateQueuePositions() {
        int position = 1;
        for (SongRequest request : queue) {
            if (request.getStatus() == SongRequest.RequestStatus.QUEUED ||
                request.getStatus() == SongRequest.RequestStatus.AVAILABLE) {
                request.setQueuePosition(position++);
            }
        }
    }
    
    public void cleanupOldRequests() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour
        queue.removeIf(r -> r.getStatus() == SongRequest.RequestStatus.COMPLETED &&
                           r.getTimestamp().isBefore(cutoff));
    }
}