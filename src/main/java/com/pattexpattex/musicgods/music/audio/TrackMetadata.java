package com.pattexpattex.musicgods.music.audio;

import com.pattexpattex.musicgods.music.spotify.SpotifyAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.Contract;
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

    /*
    private TrackMetadata(String name, String author, String uri,
                          @Nullable String image, @Nullable String album,
                          @Nullable String authorUrl) {
        this.name = name;
        this.author = author;
        this.uri = uri;
        this.image = image;
        this.album = album;
        this.authorUrl = authorUrl;
    }
    */

    @Contract(mutates = "param1")
    public static void buildMetadata(AudioTrack track) {
        if (track instanceof SpotifyAudioTrack) return;
        if (track.getUserData(KLASS) != null) return;

        track.setUserData(new TrackMetadata(track));
    }

    @Contract(mutates = "param1")
    public static void buildSpotifyMetadata(SpotifyAudioTrack track, Track backingTrack) {
        track.setUserData(new TrackMetadata(backingTrack));
    }

    @Contract(mutates = "param1")
    public static void buildSpotifyMetadata(SpotifyAudioTrack track, TrackSimplified backingTrack, Album backingAlbum) {
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

    /*
    @Override
    public String toString() {
        return "TrackMetadata{" +
                "name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", uri='" + uri + '\'' +
                ", image='" + image + '\'' +
                ", album='" + album + '\'' +
                ", authorUrl='" + authorUrl + '\'' +
                '}';
    }

    private static final Pattern TO_STRING_PATTERN = Pattern.compile("TrackMetadata\\{(name='.+', )(author='.+', )(uri='.+', )(image=(?:'.+'|null), )(album=(?:'.+'|null), )(authorUrl=(?:'.+'|null))}");


    public static TrackMetadata fromString(String input) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(input);

        Matcher matcher = TO_STRING_PATTERN.matcher(input);
        if (!matcher.matches()) throw new IllegalArgumentException();

        return new TrackMetadata(extractValue(matcher.group(1)),
                extractValue(matcher.group(2)),
                extractValue(matcher.group(3)),
                extractValue(matcher.group(4)),
                extractValue(matcher.group(5)),
                extractValue(matcher.group(6)));
    }

    private static String extractValue(String entry) {
        String value = entry.substring(entry.indexOf('\'') + 1, entry.lastIndexOf('\''));

        return "null".equals(value) ? null : value;
    }
    */
}
