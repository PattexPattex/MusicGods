package com.pattexpattex.musicgods.music.download;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioSourceManager;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioTrack;
import com.pattexpattex.musicgods.util.BundledLibs;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.COMMON_PCM_S16_BE;

public class TrackDownloader {

    private static final Logger log = LoggerFactory.getLogger(TrackDownloader.class);
    private static final String FILE_NAME = "temp/%s.%%(ext)s";
    private static final Random RANDOM = new Random();

    private static final AtomicBoolean DISABLED = new AtomicBoolean();

    static {
        BundledLibs.YTDL ytdl = Bot.getYTDlStatus();

        switch (ytdl) {
            case NOT_FOUND -> DISABLED.set(true);
            case BUNDLED -> {
                DISABLED.set(false);
                YoutubeDL.setExecutablePath("bin/youtube-dl.exe");
            }
            default -> DISABLED.set(false);
        }

        if (!DISABLED.get()) {
            BundledLibs.FFMPEG ffmpeg = Bot.getFFMpegStatus();
            DISABLED.set(ffmpeg == BundledLibs.FFMPEG.NOT_FOUND);
        }
    }

    private final long id;
    private final String url;
    private final String clientUrl;
    private final InteractionHook hook;

    private TrackDownloader(InteractionHook hook, String url, String clientUrl) {
        this.id = RANDOM.nextLong(Long.MAX_VALUE);
        this.url = url;
        this.clientUrl = clientUrl;
        this.hook = hook;
    }

    public CompletableFuture<Void> start() {
        return CompletableFuture.supplyAsync(() -> {
            if (url == null)
                return null;

            hook.editOriginal("Starting download, this may take a while...").queue();

            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.setOption("max-filesize", "8m");
            request.setOption("output", buildOutputLocation(id));
            request.setOption("audio-format", "mp3");
            request.setOption("extract-audio");
            request.setOption("no-playlist");

            try {
                YoutubeDLResponse response = YoutubeDL.execute(request, (progress, eta) -> hook.editOriginal(
                        String.format("Download progress: `%s` (`%s%%`) **|** Estimated time left: `%s`",
                                FormatUtils.buildFilledLine(((double) progress) / 100, 12), progress,
                                FormatUtils.formatTimeFromMillis(eta * 1000L))).queue());

                log.info("Completed download: {}", response.getCommand());
                File file = new File(String.format("temp/%s.mp3", id));

                if (!file.exists()) {
                    if (response.getOut().contains("File is larger than max-filesize")) {
                        hook.editOriginal("Video is too large (`>8 MB`).").queue();
                        return null;
                    }

                    log.warn("Something broke while downloading" , wrapOut(response.getOut(), response.getErr()));
                    hook.editOriginal("Something went wrong while downloading.").queue();
                    return null;
                }

                hook.sendFile(file).setContent(String.format("URL: <%s> **|** File size: `%s MB` **|** Elapsed time: `%s`",
                        clientUrl, bytesToMegaBytes(file.length()),
                        FormatUtils.formatTimeFromMillis(response.getElapsedTime()))).queue(
                                s -> {
                                    if (!file.delete())
                                        log.warn("Failed deleting '{}'", file.getName());
                                }, f -> {
                                    if (! file.delete())
                                        log.warn("Failed deleting '{}'", file.getName());
                                });

                file.deleteOnExit();
            }
            catch (YoutubeDLException e) {
                log.error("Something broke while downloading", e);
                hook.editOriginal("Something went wrong while downloading.").queue();
            }

            return null;
        });
    }

    public static TrackDownloader newProcess(String identifier, InteractionHook hook, Kvintakord kvintakord) {
        if (DISABLED.get()) {
            hook.editOriginal("Track downloading is currently disabled.").queue();
            return new TrackDownloader(hook, null, null);
        }

        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        manager.getConfiguration().setOutputFormat(COMMON_PCM_S16_BE);
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        manager.registerSourceManager(new SpotifyAudioSourceManager(kvintakord.getApplicationManager().getSpotifyManager(), new YoutubeAudioSourceManager()));

        String[] urls = new String[2];
        try {
            manager.loadItemOrdered(hook, kvintakord.getHelper().cleanIdentifier(identifier), new FunctionalResultHandler(
                    track -> parseYoutubeUrl(track, urls),
                    playlist -> parseYoutubeUrl(playlist.getTracks().get(0), urls),
                    () -> hook.editOriginal(String.format("No results for **%s**.", identifier)).queue(),
                    e -> hook.editOriginal(String.format("Failed with an exception: `%s`.", e.getMessage())).queue()
            )).get(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof TimeoutException)
                hook.editOriginal("Search timed out.").queue();
            else {
                hook.editOriginal("Something went wrong.").queue();
                log.error("Something went wrong while looking up a track", e);
            }

            return new TrackDownloader(hook, null, null);
        }

        return new TrackDownloader(hook, urls[0], urls[1]);
    }

    public static TrackDownloader newProcess(AudioTrack track, InteractionHook hook) {
        if (DISABLED.get()) {
            hook.editOriginal("Track downloading is currently disabled.").queue();
            return new TrackDownloader(hook, null, null);
        }

        String[] urls = new String[2]; //I am too lazy to implement a better system
        parseYoutubeUrl(track, urls);
        return new TrackDownloader(hook, urls[0], urls[1]);
    }

    private static void parseYoutubeUrl(AudioTrack track, String[] urls) {
        if (track instanceof YoutubeAudioTrack)
            urls[0] = track.getInfo().uri;
        else
            throw new IllegalArgumentException("Track is not a YoutubeAudioTrack");

        if (track instanceof SpotifyAudioTrack)
            urls[1] = TrackMetadata.getUri(track);
    }

    private static RuntimeException wrapOut(String out, String err) {
        RuntimeException ex = new RuntimeException(
                "ytdl OUT: " + out +
                "\nytdl ERR: " + err);

        ex.setStackTrace(new StackTraceElement[0]);
        return ex;
    }

    private static float bytesToMegaBytes(long bytes) {
        return ((float) bytes) / (1024 * 1024);
    }

    private static String buildOutputLocation(long id) {
        return String.format(OtherUtils.getPath(FILE_NAME).toString(), id);
    }
}
