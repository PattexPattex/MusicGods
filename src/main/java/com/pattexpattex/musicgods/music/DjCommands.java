package com.pattexpattex.musicgods.music;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.SlashParameter;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.audio.ShuffleMode;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.music.download.TrackDownloader;
import com.pattexpattex.musicgods.music.helpers.CheckManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.FormatUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import static com.pattexpattex.musicgods.music.DjCommands.GROUP_ID;

@Grouped(value = GROUP_ID, name = "DJ", description = "Privileged music commands.", emoji = BotEmoji.CD)
public class DjCommands implements SlashInterface {
    
    public static final String GROUP_ID = Kvintakord.GROUP_ID + "/dj";
    
    private final ApplicationManager manager;
    private final Kvintakord kvintakord;
    private final CheckManager checkManager;
    
    private DjCommands(ApplicationManager manager, Kvintakord kvintakord) {
        this.manager = manager;
        this.kvintakord = kvintakord;
        this.checkManager = kvintakord.getCheckManager();
        
        kvintakord.subInterfaceLoaded(this);
    }
    
    @SlashHandle(path = "djrole", description = "Commands related to the DJ role.")
    @Permissions(Permission.MANAGE_SERVER)
    public void djRole(SlashCommandInteractionEvent event,
                        @SlashParameter(description = "An operation.") @Choice(choices = { "get", "set", "clear" }) String operation,
                        @SlashParameter(description = "A new DJ role.", required = false) Role role) {
        switch (operation) {
            case "get" -> {
                Role oldRole = kvintakord.getConfig().getDj();
                
                if (role == null)
                    event.reply("No DJ role is set.").queue();
                else
                    event.reply(String.format("Current DJ role is **%s**.", role.getName())).queue();
            }
            case "set" -> {
                if (role == null) {
                    event.reply("Please pass a new role.").queue();
                    return;
                }
                
                kvintakord.getConfig().setDj(role);
                event.reply(String.format("Set the DJ role to **%s**.", role.getName())).queue();
            }
            case "clear" -> {
                kvintakord.getConfig().setDj(null);
                event.reply("Cleared the DJ role.").queue();
            }
            default -> throw new IllegalArgumentException("Invalid option " + operation);
        }
    }
    
    @SlashHandle(path = "playfirst", description = "Plays a track next.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void playfirst(SlashCommandInteractionEvent event,
                          @SlashParameter(description = "URL/query.") String identifier,
                          @SlashParameter(description = "A search engine.", required = false) @Choice(choices = { "youtube", "spotify" }) String engine) {
        checkManager.fairCheck(() -> kvintakord.addTrack(event, identifier, engine, true),
                String.format("Play **%s** next?", identifier), event, Kvintakord.PLAY_CHECKS);
    }
    
    @SlashHandle(path = "skip", description = "Skips the current track.")
    public void skip(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Position to skip to.", required = false) Integer position) {
        checkManager.fairCheck(() -> {
            if (position != null) {
                if (!kvintakord.getScheduler().skipTrack(position - 1))
                    event.getHook().editOriginal(String.format("Position (%d) is invalid.", position)).queue();
            } else
                kvintakord.getScheduler().skipTrack();
            
            event.getHook().editOriginal("Skipped the current track.").queue();
        }, "Skip the current track?", event);
    }
    
    @SlashHandle(path = "move", description = "Moves a track.")
    public void move(SlashCommandInteractionEvent event,
                     @SlashParameter(description = "Position of the track to move.") int from,
                     @SlashParameter(description = "Position to move the track to.") int to) {
        
        checkManager.fairCheck(() -> {
            String info = TrackMetadata.getBasicInfo(kvintakord.getScheduler().getCurrentTrack());
            
            switch (kvintakord.getScheduler().moveTrack(from - 1, to - 1)) {
                case 1 -> event.getHook().editOriginal(String.format("Parameter from (%d) is invalid.", from)).queue();
                case 2 -> event.getHook().editOriginal(String.format("Parameter to (%d) is invalid.", to)).queue();
                default -> event.getHook().editOriginal(String.format("Moved **%s** from %d to %d.", info, from, to)).queue();
            }
    
            kvintakord.updateQueueMessage();
        }, String.format("Move a track from %d to %d?", from, to), event);
    }
    
    @SlashHandle(path = "remove", description = "Removes a track.")
    public void remove(SlashCommandInteractionEvent event, @SlashParameter(description = "Position of the track to remove.") int position) {
        checkManager.fairCheck(() -> {
            String info = TrackMetadata.getBasicInfo(kvintakord.getScheduler().getCurrentTrack());
            
            if (kvintakord.getScheduler().removeTrack(position - 1))
                event.getHook().editOriginal(String.format("Removed **%s** at position %d from queue.", info, position)).queue();
            else
                event.getHook().editOriginal(String.format("Position (%d) is invalid.", position)).queue();
            
            kvintakord.updateQueueMessage();
        }, String.format("Remove a track at position %d?", position), event);
    }
    
    @SlashHandle(path = "stop", description = "Stops playback, clears the queue and leaves the voice channel.")
    public void stop(SlashCommandInteractionEvent event) {
        checkManager.fairCheck(() -> {
            kvintakord.stop(false);
            event.getHook().editOriginal("Stopped playback.").queue();
        }, "Stop playback?", event);
    }
    
    @SlashHandle(path = "deafen", description = "Deafens/un-deafens this bot.")
    public void deafen(SlashCommandInteractionEvent event) {
        checkManager.djCheck(() -> {
            boolean isDeaf = kvintakord.getGuild().getSelfMember().getVoiceState().isSelfDeafened();
            kvintakord.getGuild().getAudioManager().setSelfDeafened(!isDeaf);
    
            if (isDeaf)
                event.reply("Un-deafened myself.").queue();
            else
                event.reply("Deafened myself.").queue();
        }, event, false, CheckManager.Check.OK);
    }
    
    @SlashHandle(path = "download", description = "Downloads a track.")
    public void download(SlashCommandInteractionEvent event,
                         @SlashParameter(description = "Track to download.", required = false) String identifier,
                         @SlashParameter(description = "A search engine.", required = false) @Choice(choices = { "youtube", "spotify" }) String engine) {
        if (identifier == null)
            checkManager.djCheck(() ->
                            kvintakord.getScheduler().forCurrentTrack(track -> TrackDownloader.newProcess(track, event.getHook()).start()),
                    event, false, CheckManager.Check.PLAYING);
        else
            checkManager.djCheck(() -> TrackDownloader.newProcess(identifier, engine, event.getHook(), kvintakord).start(),
                    event, false, CheckManager.Check.OK);
        
    }
    
    @SlashHandle(path = "shuffle", description = "Sets/gets the shuffle mode.")
    public void shuffle(SlashCommandInteractionEvent event, @SlashParameter(description = "New shuffle mode", required = false) @Choice(choices = { "shuffled", "normal" }) String mode) {
        if (mode == null) {
            checkManager.check(() -> {
                ShuffleMode shuffle = kvintakord.getScheduler().getShuffle();
                event.reply(String.format("Shuffle is %s %s.",
                        (shuffle.isEnabled() ? "enabled" : "disabled"), shuffle.getEmoji())).queue();
            }, event);
        }
        else {
            ShuffleMode shuffle = ShuffleMode.fromString(mode);
            checkManager.fairCheck(() -> {
                kvintakord.getScheduler().setShuffle(shuffle);
                event.getHook().editOriginal(String.format("Set shuffle mode to %s %s.", (shuffle.isEnabled() ? "enabled" : "disabled"), shuffle.getEmoji())).queue();
                kvintakord.updateQueueMessage();
                }, (shuffle == ShuffleMode.ON ? "Shuffle the current queue?" : "Disable queue shuffling?"), event);
        }
    }
    
    @SlashHandle(path = "restart", description = "Plays the current track from the beginning.")
    public void restart(SlashCommandInteractionEvent event) {
        checkManager.fairCheck(() -> kvintakord.getScheduler().forCurrentTrack(track -> {
            track.setPosition(0);
            event.getHook().editOriginal(kvintakord.trackStartMessage(track)).queue();
            kvintakord.updateQueueMessage();
        }), "Restart playback of the current track?", event);
    }
    
    @SlashHandle(path = "forward", description = "Fast forwards the current track for some seconds.")
    public void forward(SlashCommandInteractionEvent event, @SlashParameter(description = "Seconds to fast forward.") int duration) {
        checkManager.fairCheck(() -> kvintakord.getScheduler().forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.reply("Current track is not seekable.").queue();
            }
            else {
                track.setPosition(track.getPosition() + (duration * 1000L));
                event.reply(String.format("Started playing from %s.", FormatUtils.formatTimestamp(track.getPosition()))).queue();
                kvintakord.updateQueueMessage();
            }
        }), String.format("Fast forward the current track for %d seconds?", duration), event);
    }
    
    @SlashHandle(path = "rewind", description = "Rewinds the current track for some seconds.")
    public void backward(SlashCommandInteractionEvent event,
                         @SlashParameter(description = "Seconds to rewind.") int duration) {
        checkManager.fairCheck(() -> kvintakord.getScheduler().forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.reply("Current track is not seekable.").queue();
            }
            else {
                track.setPosition(Math.max(0, track.getPosition() - (duration * 1000L)));
                event.reply(String.format("Started playing from %s.", FormatUtils.formatTimestamp(track.getPosition()))).queue();
                kvintakord.updateQueueMessage();
            }
        }), String.format("Rewind the current track for %d seconds?", duration), event);
    }
    
    @SlashHandle(path = "seek", description = "Starts playing the current track from the given position.")
    public void seek(SlashCommandInteractionEvent event, @SlashParameter(description = "Timestamp to play from, use the pattern HH:mm:ss.") String timestamp) {
        long position;
        try {
            position = FormatUtils.parseTime(timestamp) * 1000;
        }
        catch (NumberFormatException e) {
            event.reply("Invalid timestamp. Use the pattern `HH:mm:ss`.").queue();
            return;
        }
        
        checkManager.fairCheck(() -> kvintakord.getScheduler().forCurrentTrack(track -> {
            if (!track.isSeekable()) {
                event.getHook().editOriginal("Current track is not seekable.").queue();
            }
            else if (track.getDuration() < position) {
                event.getHook().editOriginal("Timestamp is longer than the current track's duration.").queue();
            }
            else {
                track.setPosition(position);
                event.getHook().editOriginal(String.format("Set position to %s.", FormatUtils.formatTimestamp(position))).queue();
                kvintakord.updateQueueMessage();
            }
        }), String.format("Play the current track from %s?", FormatUtils.formatTimestamp(position)), event);
    }
    
    public Kvintakord getKvintakord() {
        return kvintakord;
    }
    
    public static class Factory implements SlashInterfaceFactory<DjCommands> {
        
        @Override
        public Class<DjCommands> getControllerClass() {
            return DjCommands.class;
        }
        
        @Override
        public DjCommands create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new DjCommands(manager, context.getController(Kvintakord.class));
        }
    }
}
