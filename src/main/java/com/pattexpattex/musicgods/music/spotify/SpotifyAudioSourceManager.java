package com.pattexpattex.musicgods.music.spotify;

import com.pattexpattex.musicgods.exceptions.SpotifyException;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

public class SpotifyAudioSourceManager implements AudioSourceManager {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

    private final SpotifyManager spotifyManager;
    private final SearchUtils searchUtils;

    public SpotifyAudioSourceManager(SpotifyManager spotifyManager, YoutubeAudioSourceManager youtubeAudioSourceManager) {
        this.spotifyManager = spotifyManager;
        this.searchUtils = new SearchUtils(youtubeAudioSourceManager);
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            return loadItemOnce(manager, reference);
        }
        catch (FriendlyException exception) {
            if (HttpClientTools.isRetriableNetworkException(exception)) {
                return loadItemOnce(manager, reference);
            }
            else {
                throw exception;
            }
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public void shutdown() {}

    private AudioItem loadItemOnce(AudioPlayerManager manager, AudioReference reference) {
        if (spotifyManager.isNotEnabled()) return AudioReference.NO_TRACK;

        String url = reference.identifier;
        Matcher matcher = SpotifyManager.URL_PATTERN.matcher(url);
        if (!matcher.find()) return null;

        switch (matcher.group(2)) {
            case "track" -> { return searchTrack(manager, url); }
            case "playlist" -> { return searchPlaylist(manager, url); }
            case "album" -> { return searchAlbum(manager, url); }
            case "artist" -> { return searchArtistTracks(manager, url); }
            default -> //Should never be thrown, URL validity is already checked with matcher.matches()
                    throw new SpotifyException("Unknown Spotify URL.");
        }
    }

    private SpotifyAudioTrack searchTrack(AudioPlayerManager manager, String url) {
        Track track = spotifyManager.getTrack(url);
        YoutubeAudioTrack youtubeAudioTrack = searchUtils.youtubeTrackSearch(manager, searchUtils.buildAudioReference(track));

        if (youtubeAudioTrack == null) return null;
        return new SpotifyAudioTrack(youtubeAudioTrack, track);
    }

    private AudioPlaylist searchPlaylist(AudioPlayerManager manager, String url) {
        Playlist playlist = spotifyManager.getPlaylist(url);
        List<AudioTrack> tracks = fetchPlaylistTracks(manager, playlist);
        String name = playlist.getName();
        String finalName = name == null || name.isEmpty() ? "Untitled Playlist" : name;

        return new BasicAudioPlaylist(finalName, tracks, null, false);
    }

    private AudioPlaylist searchAlbum(AudioPlayerManager manager, String url) {
        Album album = spotifyManager.getAlbum(url);
        List<AudioTrack> tracks = fetchAlbumTracks(manager, album);
        String name = album.getName();
        String finalName = name == null || name.isEmpty() ? "Untitled Album" : name;

        return new BasicAudioPlaylist(finalName, tracks, null, false);
    }

    private AudioPlaylist searchArtistTracks(AudioPlayerManager manager, String url) {
        Artist artist = spotifyManager.getArtist(url);
        Track[] artistTracks = spotifyManager.getArtistTracks(url);
        List<AudioTrack> tracks = fetchArtistTracks(manager, artistTracks);
        String name = "Top Tracks of " + artist.getName();

        return new BasicAudioPlaylist(name, tracks, null, false);
    }

    private List<AudioTrack> fetchPlaylistTracks(AudioPlayerManager manager, Playlist playlist) {
        List<CompletableFuture<AudioTrack>> tasks = new ArrayList<>();

        for (PlaylistTrack pt : playlist.getTracks().getItems()) {
            IPlaylistItem ipi = pt.getTrack();

            if (!(ipi instanceof Track track)) continue;

            CompletableFuture<AudioTrack> task = searchUtils.youtubeTrackSearchAsync(manager, searchUtils.buildAudioReference(track))
                    .thenApply(item -> new SpotifyAudioTrack(item, track));

            tasks.add(task);
        }

        return completeFetchingTasks(tasks);
    }

    private List<AudioTrack> fetchAlbumTracks(AudioPlayerManager manager, Album album) {
        List<CompletableFuture<AudioTrack>> tasks = new ArrayList<>();

        for (TrackSimplified track : album.getTracks().getItems()) {
            CompletableFuture<AudioTrack> task = searchUtils.youtubeTrackSearchAsync(manager, searchUtils.buildAudioReference(track))
                    .thenApply(item -> new SpotifyAudioTrack(item, track, album));

            tasks.add(task);
        }

        return completeFetchingTasks(tasks);
    }

    private List<AudioTrack> fetchArtistTracks(AudioPlayerManager manager, Track[] tracks) {
        List<CompletableFuture<AudioTrack>> tasks = new ArrayList<>();

        for (Track track : tracks) {
            CompletableFuture<AudioTrack> task = searchUtils.youtubeTrackSearchAsync(manager, searchUtils.buildAudioReference(track))
                    .thenApply(item -> new SpotifyAudioTrack(item, track));

            tasks.add(task);
        }

        return completeFetchingTasks(tasks);
    }

    private List<AudioTrack> completeFetchingTasks(List<CompletableFuture<AudioTrack>> tasks) {
        try {
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get();
        }
        catch (InterruptedException | ExecutionException ignored) {}

        return tasks.stream()
                .dropWhile(CompletableFuture::isCompletedExceptionally)
                .map(task ->
                {
                    try {
                        return task.get();
                    }
                    catch (InterruptedException | ExecutionException e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
