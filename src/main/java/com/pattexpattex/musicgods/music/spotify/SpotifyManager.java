package com.pattexpattex.musicgods.music.spotify;

import com.neovisionaries.i18n.CountryCode;
import com.pattexpattex.musicgods.config.Config;
import com.pattexpattex.musicgods.exceptions.SpotifyException;
import com.pattexpattex.musicgods.util.OtherUtils;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.special.SearchResult;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumRequest;
import se.michaelthelin.spotify.requests.data.artists.GetArtistRequest;
import se.michaelthelin.spotify.requests.data.artists.GetArtistsTopTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistRequest;
import se.michaelthelin.spotify.requests.data.search.SearchItemRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyManager {

    private static final Logger log = LoggerFactory.getLogger(SpotifyManager.class);

    public static final Pattern AUTH_PATTERN = Pattern.compile("[a-z\\d]{32}");
    public static final Pattern URL_PATTERN = Pattern.compile("^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])(track|artist|playlist|album)\\1([a-zA-Z\\d]+)");

    private final SpotifyApi api;
    private final AtomicBoolean useSpotify;
    private final AtomicLong authExpiresOn;

    public SpotifyManager(Config config) {
        this.useSpotify = new AtomicBoolean(true);
        this.authExpiresOn = new AtomicLong(0L);

        String id = config.getSpotifyId();
        String secret = config.getSpotifySecret();

        if (id == null || !AUTH_PATTERN.matcher(id).matches()) {
            log.warn("Invalid Spotify application ID");
            useSpotify.set(false);
        }

        if (secret == null || !AUTH_PATTERN.matcher(secret).matches()) {
            log.warn("Invalid Spotify application secret");
            useSpotify.set(false);
        }

        this.api = SpotifyApi.builder().setClientId(id).setClientSecret(secret).build();
    }

    private boolean getBearerAuthorization() {
        ClientCredentialsRequest request = api.clientCredentials().build();

        try {
            setAccessToken(request.execute());
            return true;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            log.info("Disabling Spotify support");
            return false;
        }
    }

    private void setAccessToken(ClientCredentials credentials) {
        api.setAccessToken(credentials.getAccessToken());
        authExpiresOn.set(OtherUtils.epoch() + credentials.getExpiresIn() - 5);
        useSpotify.set(true);

        log.info("Retrieved Spotify bearer token, expires in {} seconds", credentials.getExpiresIn());
    }

    private boolean checkNotAuthorized() {
        if (OtherUtils.epoch() >= authExpiresOn.get())
            return !getBearerAuthorization();

        return false;
    }

    private String getIdFromUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);

        if (!matcher.find()) throw new IllegalArgumentException(String.format("Url %s is not a valid Spotify URL", url));

        return matcher.group(3);
    }

    public boolean isNotEnabled() {
        return !useSpotify.get();
    }
    
    public SearchResult searchSpotify(@NotNull String identifier) {
        if (checkNotAuthorized()) return null;
        SearchItemRequest request = api.searchItem(identifier, "track").build();
        
        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }

    public Track getTrack(@NotNull String url) {
        if (checkNotAuthorized()) return null;
        GetTrackRequest request = api.getTrack(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }

    public Playlist getPlaylist(@NotNull String url) {
        if (checkNotAuthorized()) return null;
        GetPlaylistRequest request = api.getPlaylist(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }

    public Album getAlbum(@NotNull String url) {
        if (checkNotAuthorized()) return null;
        GetAlbumRequest request = api.getAlbum(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }

    public Track[] getArtistTracks(@NotNull String url) {
        if (checkNotAuthorized()) return null;
        GetArtistsTopTracksRequest request = api.getArtistsTopTracks(getIdFromUrl(url), CountryCode.getByLocale(Locale.getDefault())).build();

        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }

    public Artist getArtist(@NotNull String url) {
        if (checkNotAuthorized()) return null;
        GetArtistRequest request = api.getArtist(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            log.warn("Something broke while executing request on route {}", request.getUri(), e);
            throw new SpotifyException(e);
        }
    }
}
