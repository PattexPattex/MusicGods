package com.pattexpattex.musicgods.music.download;

import com.pattexpattex.musicgods.Launcher;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioSourceManager;
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
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackDownloader {

    private static final Logger log = LoggerFactory.getLogger(TrackDownloader.class);
    private static final String FILE_NAME = "temp/%s.%%(ext)s";
    private static final Random RANDOM = Launcher.getInstance().getRandom();

    private static final AtomicBoolean DISABLED = new AtomicBoolean();
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();

    static {
        if (!Launcher.isFfmpeg() || !Launcher.isYTDL()) {
            DISABLED.set(true);
        }
    }

    private final long id;
    private final String url;
    private final AudioTrack track;
    private final InteractionHook hook;

    private TrackDownloader(InteractionHook hook, String url, AudioTrack track) {
        TrackMetadata.buildMetadata(track);
        this.id = RANDOM.nextLong(Long.MAX_VALUE);
        this.url = url;
        this.track = track;
        this.hook = hook;
    }

    public CompletableFuture<Void> start() {
        return CompletableFuture.supplyAsync(() -> {
            if (url == null) {
                return null;
            }

            hook.editOriginal("Starting download, this may take a while...").queue();

            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.setOption("max-filesize", "25m");
            request.setOption("output", buildOutputLocation(id));
            request.setOption("audio-format", "mp3");
            request.setOption("extract-audio");
            request.setOption("no-playlist");

            try {
                if (!ACTIVE.compareAndSet(false, true)) {
                    hook.editOriginal("A download is already active, please try again later.").queue();
                    return null;
                }

                YoutubeDLResponse response = YoutubeDL.execute(request, (progress, eta) -> hook.editOriginal(
                        String.format("Download progress: `%s` (`%s%%`) **|** Estimated time left: `%s`",
                                FormatUtils.buildFullLine(progress / 100, 10), progress,
                                FormatUtils.formatTimestamp(eta * 1000L))).queue());

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

                hook.editOriginal(String.format("%s **|** URL: <%s> **|** File size: `%s MB` **|** Elapsed time: `%s`",
                        TrackMetadata.getBasicInfo(track), TrackMetadata.getUri(track), bytesToMegaBytes(file.length()),
                        FormatUtils.formatTimestamp(response.getElapsedTime())))
                        .setFiles(FileUpload.fromData(file, TrackMetadata.getName(track) + ".mp3")).queue(
                                s -> {
                                    if (!file.delete())
                                        log.warn("Failed deleting '{}'", file.getName());
                                }, f -> {
                                    if (!file.delete())
                                        log.warn("Failed deleting '{}'", file.getName());
                                });

                file.deleteOnExit();
            }
            catch (YoutubeDLException e) {
                log.error("Something broke while downloading", e);
                hook.editOriginal("Something went wrong while downloading.").queue();
            }
            finally {
                ACTIVE.set(false);
            }

            return null;
        });
    }

    public static TrackDownloader newProcess(String identifier, String engine, InteractionHook hook, Kvintakord kvintakord) {
        if (DISABLED.get()) {
            hook.editOriginal("Track downloading is currently disabled.").queue();
            return new TrackDownloader(hook, null, null);
        }

        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new SpotifyAudioSourceManager(kvintakord.getApplicationManager().getSpotifyManager(), new YoutubeAudioSourceManager()));
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        
        CompletableFuture<TrackDownloader> future = new CompletableFuture<>();
        
        manager.loadItemOrdered(hook, kvintakord.cleanIdentifier(identifier, engine), new FunctionalResultHandler(
                track -> future.complete(new TrackDownloader(hook, parseYoutubeUrl(track), track)),
                playlist -> {
                    AudioTrack track1 = playlist.getTracks().get(0);
                    future.complete(new TrackDownloader(hook, parseYoutubeUrl(track1), track1));
                },
                () -> hook.editOriginal(String.format("No results for **%s**.", identifier)).queue(),
                e -> hook.editOriginal(String.format("Failed with an exception: `%s`.", e.getMessage())).queue()
        ));
    
        try {
            return future.get(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException e) {
            hook.editOriginal("Something went wrong.").queue();
            log.error("Something went wrong while looking up a track", e);
        }
        catch (TimeoutException e) {
            hook.editOriginal("Search timed out.").queue();
        }
    
        return new TrackDownloader(hook, null, null);
    }

    public static TrackDownloader newProcess(AudioTrack track, InteractionHook hook) {
        if (DISABLED.get()) {
            hook.editOriginal("Track downloading is currently disabled.").queue();
            return new TrackDownloader(hook, null, null);
        }

        return new TrackDownloader(hook, parseYoutubeUrl(track), track);
    }

    private static String parseYoutubeUrl(AudioTrack track) {
        return track.getInfo().uri;
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
        return String.format(OtherUtils.getPath(FILE_NAME).toAbsolutePath().toString(), id);
    }
}
