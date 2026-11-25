package com.alpoyy.pulse.request;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.live.LiveChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestParser {
    private final ConfigManager configManager;
    private final Pattern requestPattern;
    private final Pattern artistTitlePattern;
    
    public RequestParser() {
        this.configManager = ConfigManager.getInstance();
        this.requestPattern = buildRequestPattern();
        this.artistTitlePattern = Pattern.compile("^\\s*(.+?)\\s*[-–—]\\s*(.+?)\\s*$");
    }
    
    private Pattern buildRequestPattern() {
        PulseConfig config = configManager.getConfig();
        String prefix = config.requestCommandPrefix != null ? config.requestCommandPrefix : "!";
        String command = config.requestCommand != null ? config.requestCommand : "req";
        
        // Escape special regex characters
        prefix = Pattern.quote(prefix);
        command = Pattern.quote(command);
        
        // Pattern: !req <song name> or !req <artist> - <title>
        return Pattern.compile("^" + prefix + command + "\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    }
    
    public void refreshPattern() {
        // Rebuild pattern if config changed
        // Pattern is immutable, so we just note that it should be rebuilt
    }
    
    public ParsedRequest parse(LiveChatEvent event) {
        if (event == null || !event.isChatMessage()) {
            return null;
        }
        
        String message = event.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = requestPattern.matcher(message.trim());
        if (!matcher.matches()) {
            return null;
        }
        
        String content = matcher.group(1).trim();
        if (content.isEmpty()) {
            return null;
        }
        
        // Try to parse artist - title format
        String songName;
        String artist = null;
        
        Matcher artistMatcher = artistTitlePattern.matcher(content);
        if (artistMatcher.matches()) {
            artist = artistMatcher.group(1).trim();
            songName = artistMatcher.group(2).trim();
        } else {
            songName = content;
        }
        
        // Validate length
        if (songName.length() > 200) {
            return new ParsedRequest(null, null, "Song name too long", false);
        }
        
        return new ParsedRequest(songName, artist, null, true);
    }
    
    public boolean isCommand(LiveChatEvent event, String command) {
        if (event == null || !event.isChatMessage()) return false;
        
        PulseConfig config = configManager.getConfig();
        String prefix = config.requestCommandPrefix != null ? config.requestCommandPrefix : "!";
        String cmd = prefix + command;
        
        return event.getMessage() != null && 
               event.getMessage().trim().toLowerCase().startsWith(cmd.toLowerCase());
    }
    
    public String extractCommandArgs(LiveChatEvent event, String command) {
        if (event == null || !event.isChatMessage()) return null;
        
        PulseConfig config = configManager.getConfig();
        String prefix = config.requestCommandPrefix != null ? config.requestCommandPrefix : "!";
        String cmd = prefix + command;
        
        String message = event.getMessage().trim();
        if (message.toLowerCase().startsWith(cmd.toLowerCase())) {
            return message.substring(cmd.length()).trim();
        }
        return null;
    }
    
    public static class ParsedRequest {
        private final String songName;
        private final String artist;
        private final String error;
        private final boolean valid;
        
        public ParsedRequest(String songName, String artist, String error, boolean valid) {
            this.songName = songName;
            this.artist = artist;
            this.error = error;
            this.valid = valid;
        }
        
        public String getSongName() {
            return songName;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public String getError() {
            return error;
        }
        
        public boolean isValid() {
            return valid;
        }
    }
}