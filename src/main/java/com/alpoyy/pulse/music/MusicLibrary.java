package com.alpoyy.pulse.music;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MusicLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LIBRARY_FILE = "pulse_library.json";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".mp3", ".ogg", ".wav", ".flac", ".m4a", ".aac"
    );
    
    private final List<Song> songs = new CopyOnWriteArrayList<>();
    private final List<Playlist> playlists = new CopyOnWriteArrayList<>();
    private final File libraryFile;
    private final Path musicDirectory;
    
    public MusicLibrary() {
        File configDir = ConfigManager.getInstance().getConfigFile().getParentFile();
        this.libraryFile = new File(configDir, LIBRARY_FILE);
        this.musicDirectory = getMusicDirectory();
        load();
    }
    
    private Path getMusicDirectory() {
        String dir = ConfigManager.getInstance().getConfig().localMusicDirectory;
        if (dir != null && !dir.isEmpty()) {
            return Paths.get(dir);
        }
        // Default to Music folder
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Music");
    }
    
    public void load() {
        // Load library index
        if (libraryFile.exists()) {
            try (FileReader reader = new FileReader(libraryFile)) {
                Type songListType = new TypeToken<ArrayList<Song>>(){}.getType();
                List<Song> loadedSongs = GSON.fromJson(reader, songListType);
                if (loadedSongs != null) {
                    songs.clear();
                    songs.addAll(loadedSongs);
                }
            } catch (IOException e) {
                Pulse.LOGGER.error("Failed to load music library", e);
            }
        }
        
        // Load playlists
        File playlistFile = new File(libraryFile.getParentFile(), "pulse_playlists.json");
        if (playlistFile.exists()) {
            try (FileReader reader = new FileReader(playlistFile)) {
                Type playlistListType = new TypeToken<ArrayList<Playlist>>(){}.getType();
                List<Playlist> loadedPlaylists = GSON.fromJson(reader, playlistListType);
                if (loadedPlaylists != null) {
                    playlists.clear();
                    playlists.addAll(loadedPlaylists);
                }
            } catch (IOException e) {
                Pulse.LOGGER.error("Failed to load playlists", e);
            }
        }
        
        Pulse.LOGGER.info("Music library loaded: {} songs, {} playlists", songs.size(), playlists.size());
    }
    
    public void save() {
        try {
            File parentDir = libraryFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(libraryFile)) {
                GSON.toJson(new ArrayList<>(songs), writer);
            }
            
            File playlistFile = new File(libraryFile.getParentFile(), "pulse_playlists.json");
            try (FileWriter writer = new FileWriter(playlistFile)) {
                GSON.toJson(new ArrayList<>(playlists), writer);
            }
            
            Pulse.LOGGER.debug("Music library saved");
        } catch (IOException e) {
            Pulse.LOGGER.error("Failed to save music library", e);
        }
    }
    
    public void saveAsync() {
        CompletableFuture.runAsync(this::save);
    }
    
    public CompletableFuture<Integer> scanMusicDirectory() {
        return CompletableFuture.supplyAsync(() -> {
            int added = 0;
            Path scanDir = getMusicDirectory();
            
            if (!Files.exists(scanDir) || !Files.isDirectory(scanDir)) {
                Pulse.LOGGER.warn("Music directory does not exist: {}", scanDir);
                return 0;
            }
            
            try {
                Files.walkFileTree(scanDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isSupportedFile(file)) {
                            String filePath = file.toAbsolutePath().toString();
                            if (!songs.stream().anyMatch(s -> filePath.equals(s.getFilePath()))) {
                                Song song = new Song(null, null, filePath);
                                songs.add(song);
                                added++;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        Pulse.LOGGER.warn("Failed to access file: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                Pulse.LOGGER.error("Error scanning music directory", e);
            }
            
            if (added > 0) {
                saveAsync();
                Pulse.LOGGER.info("Added {} new songs to library", added);
            }
            
            return added;
        });
    }
    
    private boolean isSupportedFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
    
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
            saveAsync();
        }
    }
    
    public void removeSong(Song song) {
        songs.remove(song);
        // Remove from playlists too
        playlists.forEach(p -> p.removeSong(song));
        saveAsync();
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    public List<Song> searchSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getSongs();
        }
        
        String lowerQuery = query.toLowerCase();
        return songs.stream()
            .filter(s -> s.getTitle().toLowerCase().contains(lowerQuery) ||
                        s.getArtist().toLowerCase().contains(lowerQuery) ||
                        s.getAlbum() != null && s.getAlbum().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    public List<Song> getSongsByArtist(String artist) {
        return songs.stream()
            .filter(s -> s.getArtist().equalsIgnoreCase(artist))
            .collect(Collectors.toList());
    }
    
    public List<Song> getSongsByAlbum(String album) {
        return songs.stream()
            .filter(s -> album != null && album.equalsIgnoreCase(s.getAlbum()))
            .collect(Collectors.toList());
    }
    
    public List<String> getAllArtists() {
        return songs.stream()
            .map(Song::getArtist)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    public List<String> getAllAlbums() {
        return songs.stream()
            .map(Song::getAlbum)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    // Playlist management
    public Playlist createPlaylist(String name) {
        Playlist playlist = new Playlist(name);
        playlists.add(playlist);
        saveAsync();
        return playlist;
    }
    
    public Playlist createPlaylist(String name, String description) {
        Playlist playlist = new Playlist(name, description);
        playlists.add(playlist);
        saveAsync();
        return playlist;
    }
    
    public void deletePlaylist(Playlist playlist) {
        playlists.remove(playlist);
        saveAsync();
    }
    
    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }
    
    public Optional<Playlist> getPlaylistById(String id) {
        return playlists.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst();
    }
    
    public int getSongCount() {
        return songs.size();
    }
    
    public int getPlaylistCount() {
        return playlists.size();
    }
    
    public Path getMusicDirectoryPath() {
        return musicDirectory;
    }
    
    public void setMusicDirectory(Path directory) {
        ConfigManager.getInstance().getConfig().localMusicDirectory = directory.toString();
        ConfigManager.getInstance().saveAsync();
    }
}