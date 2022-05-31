package com.pattexpattex.musicgods.music.helpers;

import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;
import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.dv8tion.jda.api.MessageBuilder.SplitPolicy.NEWLINE;

public class LyricsHelper {

    public static final String PROVIDER = Bot.getInstance().getConfig().getLyricsProvider();
    public static final String PROVIDER_URL = getProviderUrl();

    private static final Logger log = LoggerFactory.getLogger(LyricsHelper.class);

    private static final long TIMEOUT = 10000L;

    private final Kvintakord kvintakord;
    private final LyricsClient client;

    public LyricsHelper(Kvintakord kvintakord) {
        this.kvintakord = kvintakord;
        this.client = new LyricsClient(PROVIDER, kvintakord.getApplicationManager().getExecutorService());
    }

    public Lyrics getLyrics(String identifier) {
        try {
            return client.getLyrics(identifier).get(TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Failed retrieving lyrics for: {} via {}", identifier, PROVIDER, e);
            return null;
        }
    }

    public CompletableFuture<Lyrics> getLyricsAsync(String identifier) {
        return client.getLyrics(identifier).orTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public Queue<Message> buildLyricsMessage(Lyrics lyrics) {
        MessageBuilder mb = new MessageBuilder();

        if (lyrics == null)
            return mb.append("No lyrics found.").buildAll(NEWLINE);

        String content = lyrics.getContent().trim();

        if (content.length() > 15000)
            return mb.setContent(String.format("Lyrics found but likely not correct: %s", lyrics.getURL())).buildAll();

        String[] split = content.split("\\n");

        mb.appendFormat("**%s** by **%s** - %s\n\n", lyrics.getTitle(), lyrics.getAuthor(), lyrics.getURL());

        for (String st : split)
            mb.append("> ").append(st).append("\n");

        mb.appendFormat("\n_Provided by %s._", PROVIDER);

        return mb.buildAll(NEWLINE);
    }

    public String buildSearchQuery(AudioTrack track) {
        TrackMetadata metadata = track.getUserData(TrackMetadata.class);

        if (metadata.isSpotify)
            return metadata.name + " " + metadata.author;

        return metadata.name;
    }

    private static String getProviderUrl() {
        return switch (PROVIDER) {
            case "MusixMatch" -> "https://www.musixmatch.com/";
            case "Genius" -> "https://genius.com/";
            case "LyricsFreak" -> "https://www.lyricsfreak.com/";
            default -> "https://www.azlyrics.com";
        };
    }
}
