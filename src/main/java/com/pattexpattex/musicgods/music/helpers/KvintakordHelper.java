package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KvintakordHelper {

    private static final Pattern HTTP_PATTERN = Pattern.compile("^([<|*_`]{0,3})(http|https)://[a-zA-Z\\d\\-.]+\\.[a-zA-Z]{2,6}(/\\S*)?([>|*_`]{0,3})$");

    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchTrackYesButton =
            Button.dummy("kv:search.track.yes", "Yes", null, ButtonStyle.SUCCESS, false);
    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchTrackNoButton =
            Button.dummy("kv:search.track.no", "No", null, ButtonStyle.DANGER, false);

    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchPlaylist1Button =
            Button.dummy("kv:search.playlist.1", "1");
    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchPlaylist2Button =
            Button.dummy("kv:search.playlist.2", "2");
    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchPlaylist3Button =
            Button.dummy("kv:search.playlist.3", "3");
    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchPlaylist4Button =
            Button.dummy("kv:search.playlist.4", "4");
    public static final net.dv8tion.jda.api.interactions.components.buttons.Button searchPlaylistCancelButton =
            Button.dummy("kv:search.playlist.cancel", "Cancel", null, ButtonStyle.DANGER, false);

    public static net.dv8tion.jda.api.interactions.components.buttons.Button[] getSearchPlaylistButtons(int amount) {
        net.dv8tion.jda.api.interactions.components.buttons.Button[] buttons =
                new net.dv8tion.jda.api.interactions.components.buttons.Button[amount + 1];

        for (int i = 0; i < amount; i++) {
            switch (i) {
                case 0 -> buttons[i] = searchPlaylist1Button;
                case 1 -> buttons[i] = searchPlaylist2Button;
                case 2 -> buttons[i] = searchPlaylist3Button;
                case 3 -> buttons[i] = searchPlaylist4Button;
                default -> throw new IndexOutOfBoundsException(i);
            }
        }

        buttons[amount] = searchPlaylistCancelButton;
        return buttons;
    }

    private final Kvintakord kvintakord;

    public KvintakordHelper(Kvintakord kvintakord) {
        this.kvintakord = kvintakord;
    }
    
    public String formatTrackStartMessage(AudioTrack track) {
        return String.format("Started playing **%s** (`%s`).", TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration()));
    }

    public String cleanIdentifier(String identifier) {
        Matcher matcher = HTTP_PATTERN.matcher(identifier);

        if (matcher.matches()) {
            return identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
        }

        return "ytsearch: " + identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
    }
    
    // TODO: 14. 06. 2022 Optimize and refactor this
    public void trackLoadedWaiter(AudioTrack track, SlashCommandInteractionEvent slashEvent) {

        kvintakord.getApplicationManager().getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        event -> event.getUser().getIdLong() == slashEvent.getUser().getIdLong() && event.getComponentId().startsWith(Button.DUMMY_PREFIX + "kv:search.track."),
                        1, TimeUnit.MINUTES)
                .thenAccept(event -> kvintakord.getCheckManager().check(() -> {
                    switch (event.getComponentId()) {
                        case Button.DUMMY_PREFIX + "kv:search.track.yes" -> {
                            event.getHook().editOriginal(String.format("Loaded **%s** (`%s`).", TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration())))
                                    .setActionRows(Collections.emptyList()).queue();
        
                            kvintakord.connectToVoiceChannel(event.getMember().getVoiceState().getChannel());
        
                            kvintakord.setOutputChannel(event.getTextChannel());
                            kvintakord.getScheduler().addToQueue(track);
                        }
                        case Button.DUMMY_PREFIX + "kv:search.track.no" -> event.getHook().editOriginal("Okay then.").setActionRows(Collections.emptyList()).queue();
                        default -> event.getHook().editOriginal("Unknown option.").setActionRows(Collections.emptyList()).queue();
                    }
                }, event, CheckManager.Check.SELF_MUTED, CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED))
                .exceptionally(throwable -> {
                    slashEvent.getHook().editOriginal("Timed out.").setActionRows(Collections.emptyList()).queue();

                    return null;
                });
    }

    public void playlistLoadedWaiter(AudioPlaylist playlist, SlashCommandInteractionEvent slashEvent) {

        kvintakord.getApplicationManager().getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        event -> event.getUser().getIdLong() == slashEvent.getUser().getIdLong() && event.getComponentId().startsWith(Button.DUMMY_PREFIX + "kv:search.playlist."),
                        1, TimeUnit.MINUTES)
                .thenAccept(event -> kvintakord.getCheckManager().check(() -> {
                    switch (event.getComponentId()) {
                        case Button.DUMMY_PREFIX + "kv:search.playlist.1" -> playTrackFromSearchPlaylist(event, playlist, 0);
                        case Button.DUMMY_PREFIX + "kv:search.playlist.2" -> playTrackFromSearchPlaylist(event, playlist, 1);
                        case Button.DUMMY_PREFIX + "kv:search.playlist.3" -> playTrackFromSearchPlaylist(event, playlist, 2);
                        case Button.DUMMY_PREFIX + "kv:search.playlist.4" -> playTrackFromSearchPlaylist(event, playlist, 3);
                        case Button.DUMMY_PREFIX + "kv:search.playlist.cancel" -> event.getHook().editOriginal("Okay then.").setActionRows(Collections.emptyList()).queue();
                        default -> event.getHook().editOriginal("Unknown option.").setActionRows(Collections.emptyList()).queue();
                    }
                }, event, CheckManager.Check.SELF_MUTED, CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED))
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException)
                        slashEvent.getHook().editOriginal("Timed out.").setActionRows(Collections.emptyList()).queue();


                    return null;
                });
    }

    public void playTrackFromSearchPlaylist(ButtonInteractionEvent event,
                                             AudioPlaylist playlist, int position) {
        AudioTrack track = playlist.getTracks().get(position);
        TrackMetadata.buildMetadata(track);

        event.getHook().editOriginal(String.format("Loaded **%s** (`%s`).", TrackMetadata.getName(track),
                FormatUtils.formatTimeFromMillis(track.getDuration()))).setActionRows(Collections.emptyList()).queue();

        kvintakord.connectToVoiceChannel(event.getMember().getVoiceState().getChannel());

        kvintakord.setOutputChannel(event.getTextChannel());
        kvintakord.getScheduler().addToQueue(track);
    }

    public void addTrack(final SlashCommandInteractionEvent event,
                          final String identifier, final boolean first) {
        kvintakord.setOutputChannel(event.getTextChannel());
        AudioChannel channel = event.getMember().getVoiceState().getChannel();

        kvintakord.getPlayerManager().loadItemOrdered(this, cleanIdentifier(identifier), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                TrackMetadata.buildMetadata(track);
                event.getHook().editOriginal(String.format("Loaded **%s** (`%s`).",
                        TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration()))).queue();
                kvintakord.connectToVoiceChannel(channel);

                if (first)
                    kvintakord.getScheduler().addToQueueFirst(track);
                else
                    kvintakord.getScheduler().addToQueue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                kvintakord.connectToVoiceChannel(channel);

                if (playlist.isSearchResult()) {
                    AudioTrack track = playlist.getSelectedTrack();

                    if (track == null)
                        track = tracks.get(0);

                    trackLoaded(track);
                }
                else {
                    event.getHook().editOriginal(String.format("Loaded playlist **%s** (%d tracks).",
                            playlist.getName(), tracks.size())).queue();

                    for (AudioTrack track : tracks) {
                        TrackMetadata.buildMetadata(track);
                    }

                    kvintakord.getScheduler().addToQueue(tracks);
                }
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal(String.format("No results for **%s**.", identifier)).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal(String.format("Failed with an exception: `%s`.", exception.getMessage())).queue();
            }
        });
    }
}
