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
import com.pattexpattex.musicgods.interfaces.BaseInterface;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.audio.AudioPlayerSendHandler;
import com.pattexpattex.musicgods.music.audio.LoopMode;
import com.pattexpattex.musicgods.music.audio.MusicScheduler;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.music.helpers.CheckManager;
import com.pattexpattex.musicgods.music.helpers.EqualizerManager;
import com.pattexpattex.musicgods.music.helpers.LyricsHelper;
import com.pattexpattex.musicgods.music.helpers.QueueManager;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioSourceManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import com.pattexpattex.musicgods.wait.confirmation.Confirmation;
import com.pattexpattex.musicgods.wait.confirmation.ConfirmationStatus;
import com.pattexpattex.musicgods.wait.confirmation.choice.ChoiceConfirmation;
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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static com.pattexpattex.musicgods.music.Kvintakord.GROUP_ID;

//@SuppressWarnings("unused")
@Grouped(value = GROUP_ID, name = "Music", description = "Commands for controlling music.", emoji = BotEmoji.NOTES)
public class Kvintakord implements ButtonInterface, SlashInterface {
    
    public static final String GROUP_ID = "kvintakord";
    public static final CheckManager.Check[] PLAY_CHECKS = { CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED, CheckManager.Check.SELF_MUTED, CheckManager.Check.SAME_CHANNEL_WHILE_PLAYING };

    private final ApplicationManager manager;
    private final Guild guild;
    private final GuildConfig config;
    private final GuildContext context;

    private final MessageDispatcher messageDispatcher;
    private final AtomicReference<TextChannel> outputChannel;
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

        checkManager = new CheckManager(this);
        lyricsHelper = new LyricsHelper();
        new AloneInVoiceHandler(this);
        manager.addListener(new DeafenedListener());

        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
    }


    /* ---- Base commands ---- */

    @SlashHandle(path = "play", description = "Starts playing a track from a Spotify/Youtube URL or a Youtube search query.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void play(SlashCommandInteractionEvent event, @SlashParameter(description = "URL/query.") String identifier) {
        checkManager.check(() -> {
            event.deferReply().queue();
            addTrack(event, identifier, false);
        }, event, PLAY_CHECKS);
    }

    @SlashHandle(path = "pause", description = "Pauses/resumes playback.")
    public void pause(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            if (scheduler.pause())
                event.reply("Paused playback.").queue();
            else
                event.reply("Resumed playback.").queue();
        }, event);
    }

    @SlashHandle(path = "queue", description = "Gets the current queue along with a simple GUI to control music.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS })
    public void queue(SlashCommandInteractionEvent event, @SlashParameter(description = "Page of the printed queue.", required = false) Integer page) {
        checkManager.check(() -> {
            outputChannel.set(event.getTextChannel());
    
            if (page != null)
                getSubInterface(QueueManager.class).setQueueBoxPage(page);
            getSubInterface(QueueManager.class).updateQueueMessage(event);
        }, event);
    }

    @SlashHandle(path = "loop", description = "Sets/gets the loop mode.")
    public void loop(SlashCommandInteractionEvent event, @SlashParameter(description = "New loop mode.", required = false) @Choice(choices = { "off", "all", "single" }) String mode) {
        checkManager.check(() -> {
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
        }, event);
    }

    @SlashHandle(path = "volume", description = "Sets/gets the current volume.")
    public void volume(SlashCommandInteractionEvent event, @SlashParameter(description = "New volume.", required = false) @Range(from = 0, to = 1000) Integer volume) {
        checkManager.check(() -> {
            if (volume == null) {
                event.reply(String.format("Current volume is %d.", scheduler.getVolume())).queue();
            }
            else {
                scheduler.setVolume(volume);
                updateQueueMessage();
                event.reply(String.format("Set volume to %d.", volume)).queue();
            }
        }, event);
    }
    
    @SlashHandle(path = "search", description = "Searches all sources with a query.")
    public void search(SlashCommandInteractionEvent event, @SlashParameter(description = "Search query.") String identifier) {
        checkManager.deferredCheck(() -> playerManager.loadItemOrdered(this, cleanIdentifier(identifier), new AudioLoadResultHandler() {
    
            @Override
            public void trackLoaded(AudioTrack track) {
                String prompt = String.format("Loaded **%s** (`%s`) from search. Play it now?",
                        TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration()));
    
                new Confirmation.Builder(prompt, event, result -> {
                    if (result.getStatus() == ConfirmationStatus.CONFIRMED) {
                        result.getEvent().deferEdit().queue();
                        addTrack(result.getEvent(), track, false);
                    }
                }).setTimeout(60)
                        .setOnDeny(result -> {})
                        .build();
            }
    
            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                String prompt = String.format("Select a track from **%s**", playlist.getName());
    
                new ChoiceConfirmation.Builder(prompt, event, (result, i) -> addTrack(event, playlist.getTracks().get(i), false))
                        .setTimeout(60)
                        .setChoices(playlist.getTracks()
                                .stream()
                                .limit(4)
                                .map(track -> String.format(
                                        "(`%s`) **%s**",
                                        FormatUtils.formatTimeFromMillis(track.getDuration()),
                                        TrackMetadata.getName(track)))
                                .toList())
                        .setOnCancel(result -> {})
                        .build();
            }
    
            @Override
            public void noMatches() {
                event.getHook().editOriginal(String.format("No results for **%s**", identifier)).queue();
            }
    
            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal(String.format("Failed with an exception: `%s`.", exception.getMessage())).queue();
            }
        }), event, false, PLAY_CHECKS);
    }

    @SlashHandle(path = "marker/set", description = "Sets a marker on the current track.", baseDescription = "Commands for custom track markers.")
    public void mark(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Timestamp to set the marker at, formatted like this - (hh):mm:ss.") String timestamp,
                     @SlashParameter(description = "Display text on the marker.") String text) {
        checkManager.check(() -> {
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
        }, event);
    }
    
    @SlashHandle(path = "marker/remove", description = "Removes the marker on the current track.")
    public void unMark(SlashCommandInteractionEvent event) {
        checkManager.check(() -> scheduler.forCurrentTrack(track -> {
            track.setMarker(null);
            event.reply("Removed the marker.").queue();
        }), event);
    }
    
    @SlashHandle(path = "lyrics", description = "Gets the lyrics of the current/given track.")
    public void lyrics(SlashCommandInteractionEvent event, @SlashParameter(description = "Track to search the lyrics for.", required = false) String identifier) {
        if (identifier == null) {
            checkManager.deferredCheck(() -> scheduler.forCurrentTrack(track -> {
                Queue<Message> messages = lyricsHelper.buildLyricsMessage(lyricsHelper.getLyrics(lyricsHelper.buildSearchQuery(track)));
    
                event.getHook().editOriginal(messages.remove()).queue(message -> {
                    for (Message msg : messages)
                        message.getChannel().sendMessage(msg).queue();
                });
            }), event, false, CheckManager.Check.PLAYING);
        }
        else {
            event.deferReply().queue();
            Queue<Message> messages = lyricsHelper.buildLyricsMessage(lyricsHelper.getLyrics(identifier));

            event.getHook().editOriginal(messages.remove()).queue(message -> {
                for (Message msg : messages)
                    message.getChannel().sendMessage(msg).queue();
            });
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
    
    public String trackStartMessage(AudioTrack track) {
        return String.format("Started playing **%s** (`%s`).", TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration()));
    }
    
    public String trackLoadMessage(AudioTrack track) {
        return String.format("Loaded **%s** (`%s`).", TrackMetadata.getName(track), FormatUtils.formatTimeFromMillis(track.getDuration()));
    }
    
    public String cleanIdentifier(String identifier) {
        Matcher matcher = FormatUtils.HTTP_PATTERN.matcher(identifier);
        
        if (matcher.matches()) {
            return identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
        }
        
        return "ytsearch: " + identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
    }
    
    public void addTrack(IReplyCallback callback, String identifier, boolean first) {
        setOutputChannel(callback.getTextChannel());
        AudioChannel channel = callback.getMember().getVoiceState().getChannel();
        
        playerManager.loadItemOrdered(this, cleanIdentifier(identifier), new AudioLoadResultHandler() {
            
            @Override
            public void trackLoaded(AudioTrack track) {
                addTrack(callback, track, first);
            }
            
            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                connectToVoiceChannel(channel);
                
                if (playlist.isSearchResult()) {
                    AudioTrack track = playlist.getSelectedTrack();
                    
                    if (track == null)
                        track = tracks.get(0);
                    
                    addTrack(callback, track, first);
                }
                else {
                    callback.getHook().editOriginal(String.format("Loaded playlist **%s** (%d tracks).",
                            playlist.getName(), tracks.size())).queue();
                    
                    for (AudioTrack track : tracks) {
                        TrackMetadata.buildMetadata(track);
                    }
                    
                    scheduler.addToQueue(tracks);
                }
            }
            
            @Override
            public void noMatches() {
                callback.getHook().editOriginal(String.format("No results for **%s**.", identifier)).queue();
            }
            
            @Override
            public void loadFailed(FriendlyException exception) {
                callback.getHook().editOriginal(String.format("Failed with an exception: `%s`.", exception.getMessage())).queue();
            }
        });
    }
    
    public void addTrack(IReplyCallback callback, AudioTrack track, boolean first) {
        setOutputChannel(callback.getTextChannel());
        AudioChannel channel = callback.getMember().getVoiceState().getChannel();
        
        TrackMetadata.buildMetadata(track);
        
        callback.getHook().editOriginal(new MessageBuilder(trackLoadMessage(track)).build()).queue();
        connectToVoiceChannel(channel);
        
        if (first)
            scheduler.addToQueueFirst(track);
        else
            scheduler.addToQueue(track);
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
    
    public GuildConfig getConfig() {
        return config;
    }
    
    @Override
    public void shutdown() {
        destroy();
    }
    
    @Override
    public void destroy() {
        stop(false);
        player.destroy();
        //guild.getAudioManager().setSendingHandler(null);
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
            return List.of(new EqualizerManager.Factory(), new QueueManager.Factory(), new DjCommands.Factory());
        }
    }

    private class DeafenedListener extends ListenerAdapter {

        private DeafenedListener() {}

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
