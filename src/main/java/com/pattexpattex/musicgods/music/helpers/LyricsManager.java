package com.pattexpattex.musicgods.music.helpers;

import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;
import com.pattexpattex.musicgods.Launcher;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LyricsManager {
    
    private static final Logger log = LoggerFactory.getLogger(LyricsManager.class);
    
    private static final long TIMEOUT = 10000L;
    
    private final String provider;
    private final LyricsClient client;

    public LyricsManager() {
        this(Launcher.getInstance().getConfig().getLyricsProvider(), Launcher.getInstance().getApplicationManager().getExecutorService());
    }
    
    public LyricsManager(String provider) {
        this(provider, Launcher.getInstance().getApplicationManager().getExecutorService());
    }
    
    public LyricsManager(String provider, Executor executor) {
        this.provider = provider;
        this.client = new LyricsClient(provider, executor);
    }

    public Lyrics getLyrics(String identifier) {
        try {
            return client.getLyrics(identifier).get(TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Failed retrieving lyrics for: {} via {}", identifier, provider, e);
            return null;
        }
    }

    public CompletableFuture<Lyrics> getLyricsAsync(String identifier) {
        return client.getLyrics(identifier).orTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public List<MessageEditData> buildLyricsMessage(Lyrics lyrics) {
        MessageEditBuilder mb = new MessageEditBuilder();

        if (lyrics == null) {
            return new ArrayList<>(List.of(new MessageEditBuilder()
                    .setContent("No lyrics found.").build()));
        }

        String content = lyrics.getContent().trim();
        
        if (content.length() > 15000) {
            return new ArrayList<>(List.of(new MessageEditBuilder()
                    .setContent(String.format("Lyrics found but likely not correct: <%s>", lyrics.getURL())).build()));
        }
    
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%s** by **%s** - <%s>\n\n", lyrics.getTitle(), lyrics.getAuthor(), lyrics.getURL()));

        for (String st : content.split("\\n")) {
            sb.append(">").append(st).append("\n");
        }
        
        sb.append(String.format("\n_Provided by %s._", provider));
        
        return SplitUtil.split(sb.toString(), 2000, true, SplitUtil.Strategy.NEWLINE, SplitUtil.Strategy.WHITESPACE)
                .stream()
                .map(str -> new MessageEditBuilder().setContent(str).build())
                .toList();
    }

    public String buildSearchQuery(AudioTrack track) {
        TrackMetadata metadata = track.getUserData(TrackMetadata.class);
    
        if (metadata.isSpotify)
            return metadata.name + " " + metadata.author;
    
        return metadata.name;
    }
}
