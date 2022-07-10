package com.pattexpattex.musicgods.music.audio;

import com.pattexpattex.musicgods.music.spotify.SpotifyAudioTrack;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import java.util.Arrays;
import java.util.Comparator;

public class TrackMetadata {

    private static final Class<TrackMetadata> KLASS = TrackMetadata.class;

    @Nullable
    public final String image, album, authorUrl;
    public final String name, author, uri;
    public final boolean isSpotify;

    private TrackMetadata(Track track) {
        this.name = track.getName();
        this.author = track.getArtists()[0].getName();
        this.uri = track.getExternalUrls().get("spotify");
        this.image = Arrays.stream(track.getAlbum().getImages())
                .max(Comparator.comparingInt((Image i) -> i.getWidth() * i.getHeight()))
                .map(Image::getUrl)
                .orElse(null);

        this.album = track.getAlbum().getName();
        this.authorUrl = track.getArtists()[0].getExternalUrls().get("spotify");
        this.isSpotify = true;
    }

    private TrackMetadata(TrackSimplified track, Album album) {
        this.name = track.getName();
        this.author = track.getArtists()[0].getName();
        this.uri = track.getExternalUrls().get("spotify");
        this.image = Arrays.stream(album.getImages())
                .max(Comparator.comparingInt((Image i) -> i.getWidth() * i.getHeight()))
                .map(Image::getUrl)
                .orElse(null);

        this.album = album.getName();
        this.authorUrl = track.getArtists()[0].getExternalUrls().get("spotify");
        this.isSpotify = true;
    }

    private TrackMetadata(AudioTrack track) {
        this.name = track.getInfo().title;
        this.author = track.getInfo().author;
        this.uri = track.getInfo().uri;

        if (track instanceof YoutubeAudioTrack)
            this.image = String.format("https://img.youtube.com/vi/%s/mqdefault.jpg", track.getInfo().identifier);
        else
            this.image = null;

        this.album = null;
        this.authorUrl = null;
        this.isSpotify = false;
    }

    @Contract(mutates = "param1")
    public static void buildMetadata(AudioTrack track) {
        if (track instanceof SpotifyAudioTrack) return;
        if (track.getUserData(KLASS) != null) return;

        track.setUserData(new TrackMetadata(track));
    }

    @Contract(mutates = "param1")
    public static void buildMetadata(@NotNull SpotifyAudioTrack track, Track backingTrack) {
        track.setUserData(new TrackMetadata(backingTrack));
    }

    @Contract(mutates = "param1")
    public static void buildMetadata(SpotifyAudioTrack track, TrackSimplified backingTrack, Album backingAlbum) {
        track.setUserData(new TrackMetadata(backingTrack, backingAlbum));
    }

    public static String getImage(AudioTrack track) {
        if (track == null) return null;

        TrackMetadata metadata = track.getUserData(KLASS);

        if (metadata == null) return null;
        return metadata.image;
    }

    public static String getAuthor(AudioTrack track) {
        if (track == null) return null;

        TrackMetadata metadata = track.getUserData(KLASS);

        if (metadata == null) return null;
        return metadata.author;
    }

    public static String getAuthorUrl(AudioTrack track) {
        if (track == null) return null;

        TrackMetadata metadata = track.getUserData(KLASS);

        if (metadata == null) return null;
        return metadata.authorUrl;
    }

    public static String getName(AudioTrack track) {
        if (track == null) return null;

        TrackMetadata metadata = track.getUserData(KLASS);

        if (metadata == null) return track.getInfo().title;
        return metadata.name;
    }

    public static String getUri(AudioTrack track) {
        if (track == null) return null;

        TrackMetadata metadata = track.getUserData(KLASS);

        if (metadata == null) return track.getInfo().uri;
        return metadata.uri;
    }
    
    public static String getBasicInfo(AudioTrack track) {
        if (track == null) return null;
        
        return String.format("**%s** by %s (`%s`)", getName(track), getAuthor(track), FormatUtils.formatTimestamp(track.getDuration()));
    }
    
    public static String getBasicInfoWithUrls(AudioTrack track) {
        if (track == null) return null;
        
        String url = getUri(track);
        String authorUrl = getAuthorUrl(track);
        
        StringBuilder builder = new StringBuilder();
        
        if (url == null)
            builder.append(getName(track));
        else
            builder.append(String.format("[%s](%s)", getName(track), url));
        
        builder.append(" by ");
    
        if (authorUrl == null)
            builder.append(getAuthor(track));
        else
            builder.append(String.format("[%s](%s)", getAuthor(track), authorUrl));
        
        builder.append(String.format(" (`%s`)", FormatUtils.formatTimestamp(track.getDuration())));
        
        return builder.toString();
    }
}
