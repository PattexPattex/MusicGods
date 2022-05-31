package com.pattexpattex.musicgods.music.spotify;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import java.util.concurrent.CompletableFuture;

public class SearchUtils {

    private final YoutubeAudioSourceManager youtubeAudioSourceManager;

    public SearchUtils(YoutubeAudioSourceManager youtubeAudioSourceManager) {
        this.youtubeAudioSourceManager = youtubeAudioSourceManager;
    }

    public AudioItem youtubeSearch(AudioPlayerManager manager, AudioReference reference) {
        return youtubeAudioSourceManager.loadItem(manager, reference);
    }

    public YoutubeAudioTrack youtubeTrackSearch(AudioPlayerManager manager, AudioReference reference) {
        AudioItem item = youtubeSearch(manager, reference);

        if (item instanceof AudioPlaylist playlist) {
            AudioTrack track = playlist.getSelectedTrack();
            if (track == null) track = playlist.getTracks().get(0);
            return (YoutubeAudioTrack) track;
        }

        return (YoutubeAudioTrack) item;
    }

    public CompletableFuture<YoutubeAudioTrack> youtubeTrackSearchAsync(AudioPlayerManager manager, AudioReference reference) {
        return CompletableFuture.supplyAsync(() -> youtubeTrackSearch(manager, reference));
    }

    public AudioReference buildAudioReference(Track track) {
        return new AudioReference(String.format("ytsearch: %s %s", track.getName(), track.getArtists()[0].getName()), null);
    }

    public AudioReference buildAudioReference(TrackSimplified track) {
        return new AudioReference(String.format("ytsearch: %s %s", track.getName(), track.getArtists()[0].getName()), null);
    }
}
