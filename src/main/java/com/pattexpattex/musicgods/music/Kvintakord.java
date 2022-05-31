package com.pattexpattex.musicgods.music;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import com.pattexpattex.musicgods.annotations.slash.parameter.SlashParameter;
import com.pattexpattex.musicgods.config.storage.GuildConfig;
import com.pattexpattex.musicgods.exceptions.SpotifyException;
import com.pattexpattex.musicgods.interfaces.BaseInterface;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.download.TrackDownloader;
import com.pattexpattex.musicgods.music.helpers.*;
import com.pattexpattex.musicgods.music.audio.*;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioSourceManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.pattexpattex.musicgods.music.Kvintakord.GROUP_ID;

//@SuppressWarnings("unused")
@Grouped(value = GROUP_ID, name = "Music", description = "Commands for controlling music.", emoji = BotEmoji.NOTES)
public class Kvintakord implements ButtonInterface, SlashInterface {

    public static final String GROUP_ID = "kvintakord";

    private final ApplicationManager manager;
    private final Guild guild;
    private final GuildConfig config;
    private final GuildContext context;

    private final MessageDispatcher messageDispatcher;
    private final AtomicReference<TextChannel> outputChannel;
    private final KvintakordHelper helper;
    private final CheckManager checkManager;
    private final LyricsHelper lyricsHelper;

    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final MusicScheduler scheduler;

    public Kvintakord(ApplicationManager manager, GuildContext context, Guild guild) {
        this.manager = manager;
        this.guild = guild;
        config = context.config;
        this.context = context;
        messageDispatcher = new GlobalDispatcher();
        outputChannel = new AtomicReference<>();

        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFilterHotSwapEnabled(true);
        playerManager.registerSourceManager(new SpotifyAudioSourceManager(manager.getSpotifyManager(), new YoutubeAudioSourceManager()));
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        player = playerManager.createPlayer();
        player.setFrameBufferDuration(500);

        scheduler = new MusicScheduler(player, messageDispatcher, config, this);

        helper = new KvintakordHelper(this);
        checkManager = new CheckManager(this);
        lyricsHelper = new LyricsHelper(this);
        new AloneInVoiceHandler(this);
        manager.addListener(new DeafenedListener());

        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
    }


    /* ---- Commands ---- */

    @SlashHandle(path = "play",
            description = "Starts playing a track from a Spotify/Youtube URL or a Youtube search query.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void play(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "URL/query.") String identifier) {
        if (checkManager.check(event, CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED, CheckManager.Check.SELF_MUTED)) return;

        event.deferReply().queue();
        helper.addTrack(event, identifier, false);
    }

    @SlashHandle(path = "playfirst",
            description = "Puts a track from a Spotify/Youtube URL or a Youtube search query to the start of the queue.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void playfirst(SlashCommandInteractionEvent event,
                          @SlashParameter(description = "URL/query.") String identifier) {
        if (checkManager.check(event, CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED, CheckManager.Check.SELF_MUTED)) return;

        event.deferReply().queue();
        helper.addTrack(event, identifier, true);
    }

    @SlashHandle(path = "pause", description = "Pauses/resumes playback.")
    public void pause(SlashCommandInteractionEvent event) {
        if (checkManager.check(event)) return;

        if (scheduler.pause())
            event.reply("Paused playback.").queue();
        else
            event.reply("Resumed playback.").queue();
    }

    @SlashHandle(path = "skip", description = "Skips the current track.")
    public void skip(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Position to skip to.", required = false) Integer position) {
        if (checkManager.check(event)) return;

        if (position != null) {
            if (! scheduler.skipTrack(position - 1))
                event.reply(String.format("Position (%d) is invalid.", position)).queue();
        }
        else
            scheduler.skipTrack();

        event.reply("Skipped the current track.").queue();
    }

    @SlashHandle(path = "move", description = "Moves a track.")
    public void move(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Position of the track to move.") int from,
                     @SlashParameter(description = "Position to move the track to.") int to) {
        if (checkManager.check(event)) return;

        switch (scheduler.moveTrack(from - 1, to - 1)) {
            case 1 -> event.reply(String.format("Parameter from (%d) is invalid.", from)).queue();
            case 2 -> event.reply(String.format("Parameter to (%d) is invalid.", to)).queue();
            default -> event.reply(String.format("Moved track from %d to %d.", from, to)).queue();
        }
    }

    @SlashHandle(path = "remove", description = "Removes a track.")
    public void remove(SlashCommandInteractionEvent event,
                       @SlashParameter(description = "Position of the track to remove.") int position) {
        if (checkManager.check(event)) return;

        if (scheduler.removeTrack(position - 1))
            event.reply(String.format("Removed track at position %d from queue.", position)).queue();
        else
            event.reply(String.format("Position (%d) is invalid.", position)).queue();
    }

    @SlashHandle(path = "stop", description = "Stops playback, clears the queue and leaves the voice channel.")
    public void stop(SlashCommandInteractionEvent event) {
        if (checkManager.check(event)) return;
        stop(false);
        event.reply("Stopped playback.").queue();
    }

    @SlashHandle(path = "queue", description = "Gets the current queue along with a simple GUI to control music.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS })
    public void queue(SlashCommandInteractionEvent event,
                      @SlashParameter(description = "Page of the printed queue.", required = false) Integer page) {
        if (checkManager.check(event)) return;

        outputChannel.set(event.getTextChannel());

        if (page != null)
            getSubInterface(QueueManager.class).setQueueBoxPage(page);
        getSubInterface(QueueManager.class).updateQueueMessage(event);
    }

    @SlashHandle(path = "loop", description = "Sets/gets the loop mode.")
    public void loop(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "New loop mode.", required = false)
                        @Choice(choices = { "off", "all", "single" }) String mode) {
        if (checkManager.check(event)) return;

        if (mode == null) {
            event.reply(String.format("Current loop mode is %s %s.",
                    scheduler.getLoop().getEmoji(), scheduler.getLoop().getFormatted())).queue();
        }
        else {
            try {
                LoopMode lm = LoopMode.ofString(mode);
                scheduler.setLoop(lm);
                event.reply(String.format("Set loop to %s %s.", lm.getEmoji(), lm.getFormatted())).queue();
            }
            catch (IllegalArgumentException e) {
                event.reply("Invalid loop mode " + mode).queue();
            }
        }
    }

    @SlashHandle(path = "volume", description = "Sets/gets the current volume.")
    public void volume(SlashCommandInteractionEvent event,
                       @SlashParameter(description = "New volume.", required = false)
                        @Range(from = 0, to = 1000) Integer volume) {
        if (checkManager.check(event)) return;

        if (volume == null) {
            event.reply(String.format("Current volume is %d.", scheduler.getVolume())).queue();
        }
        else {
            scheduler.setVolume(volume);
            updateQueueMessage();
            event.reply(String.format("Set volume to %d.", volume)).queue();
        }
    }

    @SlashHandle(path = "search", description = "Searches all sources with a query.")
    public void search(SlashCommandInteractionEvent event,
                       @SlashParameter(description = "Search query.") String identifier) {
        if (checkManager.check(event, CheckManager.Check.SELF_MUTED, CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED)) return;

        event.deferReply().queue();

        playerManager.loadItemOrdered(this, helper.cleanIdentifier(identifier), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                TrackMetadata.buildMetadata(track);
                event.getHook().editOriginal(String.format("Loaded **%s** (`%s`) from search. Play it now?",
                                TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration())))
                        .setActionRow(KvintakordHelper.searchTrackYesButton, KvintakordHelper.searchTrackNoButton).queue();

                helper.trackLoadedWaiter(track, event);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                StringBuilder builder = new StringBuilder(
                        String.format("Search results for **%s**: (select/cancel by clicking a button)", identifier));

                int i = 0;
                for (AudioTrack track : playlist.getTracks()) {
                    if (i >= 4) break;
                    i++;
                    TrackMetadata.buildMetadata(track);
                    builder.append(String.format("\n> %d (`%s`) %s", i,
                            FormatUtils.formatTimeFromMillis(track.getDuration()), TrackMetadata.getName(track)));
                }

                event.getHook().editOriginal(builder.toString()).setActionRow(KvintakordHelper.getSearchPlaylistButtons(i)).queue();

                helper.playlistLoadedWaiter(playlist, event);
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal(String.format("No results for **%s**", identifier)).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (exception.getCause() instanceof SpotifyException se && se.isFromLoader)
                    event.getHook().editOriginal(se.getMessage()).queue();
                else
                    event.getHook().editOriginal(String.format("Failed with an exception: `%s`",
                            exception.getMessage())).queue();
            }
        });
    }

    @SlashHandle(path = "shuffle", description = "Sets/gets the shuffle mode.")
    public void shuffle(SlashCommandInteractionEvent event,
                        @SlashParameter(description = "New shuffle mode", required = false) Boolean mode) {
        if (checkManager.check(event)) return;

        if (mode == null) {
            ShuffleMode shuffle = scheduler.getShuffle();
            event.reply(String.format("Shuffle is %s %s.",
                    (shuffle.isEnabled() ? "enabled" : "disabled"), shuffle.getEmoji())).queue();
        }
        else {
            ShuffleMode shuffle = ShuffleMode.fromBoolean(mode);
            scheduler.setShuffle(shuffle);
            event.reply(String.format("Set shuffle mode to %s %s.",
                    (shuffle.isEnabled() ? "enabled" : "disabled"), shuffle.getEmoji())).queue();
        }
    }

    @SlashHandle(path = "forward", description = "Fast forwards the current track for some seconds.")
    public void forward(SlashCommandInteractionEvent event,
                        @SlashParameter(description = "Seconds to fast forward.") int duration) {
        if (checkManager.check(event)) return;

        scheduler.forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.reply("Current track is not seekable.").queue();
            }
            else {
                track.setPosition(track.getPosition() + (duration * 1000L));
                event.reply(String.format("Started playing from %s.", FormatUtils.formatTimeFromMillis(track.getPosition()))).queue();
            }
        });

        updateQueueMessage();
    }

    @SlashHandle(path = "rewind", description = "Rewinds the current track for some seconds.")
    public void backward(SlashCommandInteractionEvent event,
                         @SlashParameter(description = "Seconds to rewind.") int duration) {
        if (checkManager.check(event)) return;

        scheduler.forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.reply("Current track is not seekable.").queue();
            }
            else {
                track.setPosition(Math.max(0, track.getPosition() - (duration * 1000L)));
                event.reply(String.format("Started playing from %s.", FormatUtils.formatTimeFromMillis(track.getPosition()))).queue();
            }
        });

        updateQueueMessage();
    }

    @SlashHandle(path = "seek", description = "Starts playing the current track from the given position.")
    public void seek(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Timestamp to play from, formatted like this - (hh):mm:ss.") String timestamp) {
        long position;
        try {
            position = FormatUtils.decodeTimeToSeconds(timestamp) * 1000;
        }
        catch (NumberFormatException e) {
            event.reply("Invalid timestamp. Please format it like this - `hh:mm:ss / h:mm:ss / mm:ss / m:ss`.").queue();
            return;
        }

        scheduler.forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.reply("Current track is not seekable.").queue();
            }
            else if (track.getDuration() < position) {
                event.reply("Timestamp is longer than the current track's duration.").queue();
            }
            else {
                track.setPosition(position);
                event.reply(String.format("Set position to %s.", FormatUtils.formatTimeFromMillis(position))).queue();
            }
        });

        updateQueueMessage();
    }

    @SlashHandle(path = "marker/set", description = "Sets a marker on the current track.", baseDescription = "Commands for custom track markers.")
    public void mark(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Timestamp to set the marker at, formatted like this - (hh):mm:ss.") String timestamp,
                     @SlashParameter(description = "Display text on the marker.") String text) {
        if (checkManager.check(event)) return;

        long position;
        try {
            position = FormatUtils.decodeTimeToSeconds(timestamp) * 1000;
        }
        catch (NumberFormatException e) {
            event.reply("Invalid timestamp. Please format it like this - `hh:mm:ss / h:mm:ss / mm:ss / m:ss`.").queue();
            return;
        }

        scheduler.forCurrentTrack(track -> {
            if (track.getDuration() < position) {
                event.reply("Timestamp is longer than the current track's duration.").queue();
            }
            else {
                track.setMarker(new TrackMarker(position, state ->
                    event.getChannel().sendMessage(String.format("Hit marker [%s], cause [%s].", text, state.name())).queue()));
            }
        });
    }

    @SlashHandle(path = "marker/remove", description = "Removes the marker on the current track.")
    public void unMark(SlashCommandInteractionEvent event) {
        if (checkManager.check(event)) return;

        scheduler.forCurrentTrack(track -> {
            track.setMarker(null);
            event.reply("Removed the marker.").queue();
        });
    }

    @SlashHandle(path = "lyrics", description = "Gets the lyrics of the current/given track.")
    public void lyrics(SlashCommandInteractionEvent event,
                       @SlashParameter(description = "Track to search the lyrics for.", required = false) String identifier) {
        event.deferReply().queue();

        if (identifier == null) {
            if (checkManager.check(event, CheckManager.Check.PLAYING)) return;

            scheduler.forCurrentTrack(track -> {
                Queue<Message> messages = lyricsHelper.buildLyricsMessage(lyricsHelper.getLyrics(lyricsHelper.buildSearchQuery(track)));

                event.getHook().editOriginal(messages.remove()).queue(message -> {
                    for (Message msg : messages)
                        message.getChannel().sendMessage(msg).queue();
                });
            });
        }
        else {
            Queue<Message> messages = lyricsHelper.buildLyricsMessage(lyricsHelper.getLyrics(identifier));

            event.getHook().editOriginal(messages.remove()).queue(message -> {
                for (Message msg : messages)
                    message.getChannel().sendMessage(msg).queue();
            });
        }
    }

    @SlashHandle(path = "deafen", description = "Deafens/un-deafens this bot.")
    public void deafen(SlashCommandInteractionEvent event) {
        if (checkManager.check(event)) return;

        boolean isDeaf = guild.getSelfMember().getVoiceState().isSelfDeafened();
        guild.getAudioManager().setSelfDeafened(!isDeaf);

        if (isDeaf)
            event.reply("Un-deafened myself.").queue();
        else
            event.reply("Deafened myself.").queue();
    }

    @SlashHandle(path = "download", description = "Downloads and sends you a track.")
    public void download(SlashCommandInteractionEvent event,
                         @SlashParameter(description = "Track to download.", required = false) String identifier) {
        if (identifier == null) {
            if (checkManager.check(event, CheckManager.Check.PLAYING)) return;

            event.deferReply().queue();
            scheduler.forCurrentTrack(track -> TrackDownloader.newProcess(track, event.getHook()).start());
        }
        else {
            event.deferReply().queue();
            TrackDownloader.newProcess(identifier, event.getHook(), this).start();
        }
    }


    /* ---- Other ---- */

    public void stop(boolean withMessage) {
        EqualizerManager eqManager = getSubInterface(EqualizerManager.class);
        QueueManager queueManager = getSubInterface(QueueManager.class);

        eqManager.cleanup();
        queueManager.cleanup();
        scheduler.stop(withMessage);
    }

    public void updateQueueMessage() {
        getSubInterface(QueueManager.class).updateQueueMessage();
    }

    public void connectToVoiceChannel(AudioChannel channel) {
        AudioManager manager = guild.getAudioManager();

        if (!manager.isConnected() && channel != null) {
            manager.openAudioConnection(channel);
        }
    }

    public void disconnectFromVoiceChannel() {
        AudioManager manager = guild.getAudioManager();

        if (manager.isConnected()) {
            manager.closeAudioConnection();
        }
    }

    public void setOutputChannel(TextChannel channel) {
        outputChannel.set(channel);
    }

    public Guild getGuild() {
        return guild;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public MusicScheduler getScheduler() {
        return scheduler;
    }

    public ApplicationManager getApplicationManager() {
        return manager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public LyricsHelper getLyricsHelper() {
        return lyricsHelper;
    }

    public KvintakordHelper getHelper() {
        return helper;
    }

    private class GlobalDispatcher implements MessageDispatcher {

        @Override
        public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
            TextChannel channel = outputChannel.get();

            if (channel == null) return;
            channel.sendMessage(message).queue(success, failure);
        }

        @Override
        public void sendMessage(String message) {
            TextChannel channel = outputChannel.get();

            if (channel == null) return;
            channel.sendMessage(message).queue();
        }

        @Override
        public void sendMessage(Message message, Consumer<Message> success, Consumer<Throwable> failure) {
            TextChannel channel = outputChannel.get();

            if (channel == null) return;
            channel.sendMessage(message).queue(success, failure);
        }

        @Override
        public void sendSuccess() {
            TextChannel channel = outputChannel.get();

            if (channel == null) return;
            channel.sendMessage(BotEmoji.YES).queue();
        }

        @Override
        public void sendFailure() {
            TextChannel channel = outputChannel.get();

            if (channel == null) return;
            channel.sendMessage(BotEmoji.NO).queue();
        }

    }

    @Override
    public void shutdown() {
        destroy();
    }

    @Override
    public void destroy() {
        stop(false);
        player.destroy();
        guild.getAudioManager().setSendingHandler(null);
    }

    public static class Factory implements ButtonInterfaceFactory<Kvintakord>, SlashInterfaceFactory<Kvintakord> {

        @Override
        public Class<Kvintakord> getControllerClass() {
            return Kvintakord.class;
        }

        @Override
        public Kvintakord create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new Kvintakord(manager, context, guild);
        }

        @Override
        public List<BaseInterfaceFactory<? extends BaseInterface>> subInterfaces() {
            return List.of(new EqualizerManager.Factory(), new QueueManager.Factory());
        }
    }

    private class DeafenedListener extends ListenerAdapter {

        private DeafenedListener() {
        }

        @Override
        public void onGuildVoiceGuildDeafen(@NotNull GuildVoiceGuildDeafenEvent event) {
            Member member = event.getMember();
            GuildVoiceState state = member.getVoiceState();

            if (member.getIdLong() == guild.getSelfMember().getIdLong()) {
                if (!state.inAudioChannel() || !event.isGuildDeafened()) return;

                member.deafen(false).queue();
                guild.getAudioManager().setSelfDeafened(true);
                messageDispatcher.sendMessage("Please don't deafen me, use `/deafen` instead.");
            }
        }
    }
}
