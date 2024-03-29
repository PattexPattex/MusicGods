package com.pattexpattex.musicgods.music;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
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
import com.pattexpattex.musicgods.music.helpers.LyricsManager;
import com.pattexpattex.musicgods.music.helpers.QueueManager;
import com.pattexpattex.musicgods.music.spotify.SpotifyAudioSourceManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.OtherUtils;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static com.pattexpattex.musicgods.music.Kvintakord.GROUP_ID;

//@SuppressWarnings("unused")
@Grouped(value = GROUP_ID, name = "Music", description = "Commands for controlling music.", emoji = BotEmoji.NOTES)
public class Kvintakord implements ButtonInterface, SlashInterface {
    
    public static final String GROUP_ID = "kvintakord";
    public static final CheckManager.Check[] PLAY_CHECKS = { CheckManager.Check.USER_CONNECTED, CheckManager.Check.USER_DEAFENED,
            CheckManager.Check.SELF_MUTED, CheckManager.Check.SAME_CHANNEL_WHILE_PLAYING };

    private final ApplicationManager manager;
    private final Guild guild;
    private final GuildConfig config;
    private final GuildContext context;

    private final MessageDispatcher messageDispatcher;
    private final AtomicReference<MessageChannel> outputChannel;
    private final CheckManager checkManager;
    private final LyricsManager lyricsHelper;

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
        lyricsHelper = new LyricsManager();
        new AloneInVoiceHandler(this);
        manager.addListener(new DeafenedListener());

        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
    }


    /* ---- Base commands ---- */

    @SlashHandle(path = "play", description = "Plays a track.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void play(SlashCommandInteractionEvent event,
                     @Parameter(description = "URL/query.") String identifier,
                     @Parameter(description = "A search engine.", required = false) @Choice(choices = { "youtube", "spotify" }) String engine) {
        checkManager.check(() -> {
            event.deferReply().queue();
            addTrack(event, identifier, engine, false);
        }, event, PLAY_CHECKS);
    }

    @SlashHandle(path = "pause", description = "Pauses/resumes playback.")
    public void pause(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            if (scheduler.pause())
                event.reply("Paused playback.").queue();
            else
                event.reply("Resumed playback.").queue();
    
            updateQueueMessage();
        }, event);
    }

    @SlashHandle(path = "queue", description = "Gets the current queue along with a simple GUI to control music.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS })
    public void queue(SlashCommandInteractionEvent event,
                      @Parameter(description = "Page of the printed queue.", required = false) @Range(min = 1, max = OptionData.MAX_POSITIVE_NUMBER) Integer page) {
        checkManager.deferredCheck(() -> {
            outputChannel.set(event.getChannel().asGuildMessageChannel());
    
            if (page != null)
                getSubInterface(QueueManager.class).setPage(page);
            getSubInterface(QueueManager.class).updateMessage(event.getHook());
        }, event, false);
    }

    @SlashHandle(path = "loop", description = "Sets/gets the loop mode.")
    public void loop(SlashCommandInteractionEvent event,
                     @Parameter(description = "New loop mode.", required = false) @Choice(choices = { "off", "all", "single" }) String mode) {
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
                    updateQueueMessage();
                }
                catch (IllegalArgumentException e) {
                    event.reply("Invalid loop mode " + mode).queue();
                }
            }
        }, event);
    }

    @SlashHandle(path = "volume", description = "Sets/gets the current volume.")
    public void volume(SlashCommandInteractionEvent event,
                       @Parameter(description = "New volume.", required = false) @Range(min = 1, max = 1000) Integer volume) {
        checkManager.check(() -> {
            if (volume == null) {
                event.reply(String.format("Current volume is %d.", scheduler.getVolume())).queue();
            }
            else {
                scheduler.setVolume(volume);
                event.reply(String.format("Set volume to %d.", volume)).queue();
                updateQueueMessage();
            }
        }, event);
    }
    
    @SlashHandle(path = "search", description = "Searches sources with a query.")
    public void search(SlashCommandInteractionEvent event,
                       @Parameter(description = "Search query.") String identifier,
                       @Parameter(description = "A search engine.", required = false) @Choice(choices = { "youtube", "spotify" }) String engine) {
        checkManager.deferredCheck(() -> playerManager.loadItemOrdered(this, cleanIdentifier(identifier, engine), new AudioLoadResultHandler() {
    
            @Override
            public void trackLoaded(AudioTrack track) {
                TrackMetadata.buildMetadata(track);
                String prompt = String.format("Loaded %s from search. Play it now?", TrackMetadata.getBasicInfo(track));
    
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
                String prompt = String.format("Select a track from **%s**:", playlist.getName());
    
                new ChoiceConfirmation.Builder(prompt, event, (result, i) -> addTrack(event, playlist.getTracks().get(i), false))
                        .setTimeout(60)
                        .setChoices(playlist.getTracks()
                                .stream()
                                .limit(4)
                                .peek(TrackMetadata::buildMetadata)
                                .map(TrackMetadata::getBasicInfo)
                                .toList())
                        .setOnCancel(result -> {})
                        .build();
            }
    
            @Override
            public void noMatches() {
                event.getHook().editOriginal(String.format("No results for **%s**.", identifier)).queue();
            }
    
            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal(String.format("%s.", exception.getMessage())).queue();
            }
        }), event, false, PLAY_CHECKS);
    }

    @SlashHandle(path = "marker/set", description = "Sets a marker on the current track.", baseDescription = "Commands for custom track markers.")
    public void mark(SlashCommandInteractionEvent event,
                     @Parameter(description = "Timestamp to set the marker at. Use the pattern HH:mm:ss.") String timestamp,
                     @Parameter(description = "Display text on the marker.") String text) {
        checkManager.check(() -> {
            long position;
            try {
                position = FormatUtils.parseTime(timestamp) * 1000;
            }
            catch (NumberFormatException e) {
                event.reply("Invalid timestamp. Use the pattern `HH:mm:ss`.").queue();
                return;
            }
    
            scheduler.forCurrentTrack(track -> {
                if (track.getDuration() < position) {
                    event.reply("Timestamp is longer than the current track's duration.").queue();
                }
                else {
                    track.setMarker(new TrackMarker(position, state ->
                            event.getChannel().sendMessage(String.format("Hit marker [%s] **|** Cause: `%s`", text, state.name())).queue()));
                    event.reply(String.format("Added a marker [%s] at %s.", text, FormatUtils.formatTimestamp(position))).queue();
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
    public void lyrics(SlashCommandInteractionEvent event,
                       @Parameter(description = "Track to search the lyrics for.", required = false) String identifier) {
        if (identifier == null) {
            checkManager.deferredCheck(() -> scheduler.forCurrentTrack(track -> retrieveLyrics(event.getHook(), lyricsHelper.buildSearchQuery(track))),
                    event, false, CheckManager.Check.PLAYING);
        }
        else {
            event.deferReply().queue();
            retrieveLyrics(event.getHook(), identifier);
        }
    }
    

    /* ---- Other ---- */
    
    public void retrieveLyrics(InteractionHook hook, String query) {
        lyricsHelper.getLyricsAsync(query)
                .thenAccept(lyrics -> {
                    List<MessageEditData> messages = lyricsHelper.buildLyricsMessage(lyrics);
    
                    hook.editOriginal(messages.remove(0)).queue(message -> messages.stream()
                                .map(MessageCreateData::fromEditData)
                                .forEach(msg -> message.getChannel().sendMessage(msg).queue())
                    );
                })
                .exceptionally(th -> {
                    if (th instanceof TimeoutException te)
                        hook.editOriginal("Lyrics search timed out.").queue();
                    else {
                        hook.editOriginal("Something went wrong.").queue();
                        OtherUtils.getLog().warn("Something broke while retrieving lyrics for '{}'", query, th);
                    }
                    return null;
                });
    }

    public void stop(boolean withMessage) {
        QueueManager queueManager = getSubInterface(QueueManager.class);
        AudioFilterManager filterManager = getSubInterface(AudioFilterManager.class);

        queueManager.cleanup();
        filterManager.cleanup();
        scheduler.stop(withMessage);
    }

    public void updateQueueMessage() {
        getSubInterface(QueueManager.class).updateMessage();
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

    public void setOutputChannel(MessageChannel channel) {
        outputChannel.set(channel);
    }
    
    public String trackStartMessage(AudioTrack track) {
        return String.format("Started playing %s.", TrackMetadata.getBasicInfo(track));
    }
    
    public String trackLoadMessage(AudioTrack track) {
        return String.format("Loaded %s.", TrackMetadata.getBasicInfo(track));
    }
    
    public String cleanIdentifier(String identifier, String engine) {
        Matcher matcher = FormatUtils.HTTP_PATTERN.matcher(identifier);
        
        if (matcher.matches())
            return identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
        
        if ("spotify".equals(engine))
            return "spsearch: " + identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
        else
            return "ytsearch: " + identifier.replaceAll("(^[<|*_`]{0,3})|([>|*_`]{0,3}$)", "");
    }
    
    public void addTrack(IReplyCallback callback, String identifier, String engine, boolean first) {
        setOutputChannel(callback.getMessageChannel());
        AudioChannel channel = callback.getMember().getVoiceState().getChannel();
        
        playerManager.loadItemOrdered(this, cleanIdentifier(identifier, engine), new AudioLoadResultHandler() {
            
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
                callback.getHook().editOriginal(String.format("%s.", exception.getMessage())).queue();
            }
        });
    }
    
    public void addTrack(IReplyCallback callback, AudioTrack track, boolean first) {
        setOutputChannel(callback.getMessageChannel());
        AudioChannel channel = callback.getMember().getVoiceState().getChannel();
        
        TrackMetadata.buildMetadata(track);
        
        callback.getHook().editOriginal(new MessageEditBuilder().setContent(trackLoadMessage(track)).build()).queue();
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

    public LyricsManager getLyricsHelper() {
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
            MessageChannel channel = outputChannel.get();
            
            if (channel == null) return;
            channel.sendMessage(message).queue(success, failure);
        }
        
        @Override
        public void sendMessage(String message) {
            MessageChannel channel = outputChannel.get();
            
            if (channel == null) return;
            channel.sendMessage(message).queue();
        }
        
        @Override
        public void sendMessage(MessageCreateData message, Consumer<Message> success, Consumer<Throwable> failure) {
            MessageChannel channel = outputChannel.get();
            
            if (channel == null) return;
            channel.sendMessage(message).queue(success, failure);
        }
        
        @Override
        public void sendSuccess() {
            MessageChannel channel = outputChannel.get();
            
            if (channel == null) return;
            channel.sendMessage(BotEmoji.YES).queue();
        }
        
        @Override
        public void sendFailure() {
            MessageChannel channel = outputChannel.get();
            
            if (channel == null) return;
            channel.sendMessage(BotEmoji.NO).queue();
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
        public List<BaseInterfaceFactory<? extends BaseInterface>> getSubInterfaces() {
            return List.of(new QueueManager.Factory(), new DjCommands.Factory(), new AudioFilterManager.Factory());
        }
    }
}
