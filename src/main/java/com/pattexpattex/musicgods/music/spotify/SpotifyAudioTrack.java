package com.pattexpattex.musicgods.music.spotify;

import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

public class SpotifyAudioTrack extends YoutubeAudioTrack {

    public SpotifyAudioTrack(YoutubeAudioTrack baseAudioTrack, Track backingTrack) {
        super(baseAudioTrack.getInfo(), (YoutubeAudioSourceManager) baseAudioTrack.getSourceManager());
        TrackMetadata.buildSpotifyMetadata(this, backingTrack);
    }

    public SpotifyAudioTrack(YoutubeAudioTrack baseAudioTrack, TrackSimplified backingTrack, Album backingAlbum) {
        super(baseAudioTrack.getInfo(), (YoutubeAudioSourceManager) baseAudioTrack.getSourceManager());
        TrackMetadata.buildSpotifyMetadata(this, backingTrack, backingAlbum);
    }
}
