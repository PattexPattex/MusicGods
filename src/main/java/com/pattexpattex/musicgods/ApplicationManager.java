package com.pattexpattex.musicgods;

import com.pattexpattex.musicgods.commands.EvalCommand;
import com.pattexpattex.musicgods.commands.HelpCommand;
import com.pattexpattex.musicgods.commands.SystemCommands;
import com.pattexpattex.musicgods.exceptions.WrongArgumentException;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonResponseHandler;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalInterface;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalResponseHandler;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionInterface;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionResponseHandler;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterface;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteResponseHandler;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashParameter;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashPath;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashResponseHandler;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.spotify.SpotifyManager;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.util.dispatchers.InteractionMessageDispatcher;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import com.pattexpattex.musicgods.util.dispatchers.impl.*;
import com.pattexpattex.musicgods.wait.Waiter;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ApplicationManager extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ApplicationManager.class);

    private final Bot bot;
    private final InterfaceManagerConnector interfaceManager;
    private final ScheduledExecutorService executorService;
    private final Waiter waiter;
    private final SpotifyManager spotifyManager;
    private final Map<Long, GuildContext> guildContexts;

    public ApplicationManager(Bot bot) {
        this.bot = bot;
        this.spotifyManager = new SpotifyManager(bot.getConfig());
        this.guildContexts = new HashMap<>();
        this.interfaceManager = new InterfaceManagerConnector(this);
        this.executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));
        this.waiter = new Waiter();

        interfaceManager.registerControllers(new SystemCommands.Factory(),
                new Kvintakord.Factory(), new HelpCommand.Factory());

        if (bot.getConfig().getEval()) {
            interfaceManager.registerControllers(new EvalCommand.Factory());
        }

        interfaceManager.finishSetup();
    }

    public SpotifyManager getSpotifyManager() {
        return spotifyManager;
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public InterfaceManagerConnector getInterfaceManager() {
        return interfaceManager;
    }

    public Bot getBot() {
        return bot;
    }

    public void addListener(Object listener) {
        bot.getJDA().addEventListener(listener);
    }

    public void shutdown() {
        guildContexts.values().forEach(GuildContext::shutdown);
        cleanTemp();
    }


    /* ---- Guild contexts ---- */

    private GuildContext newGuildContext(long guildId, Guild guild) {
        GuildContext context = new GuildContext(guildId, bot);
        interfaceManager.createControllers(this, context, guild);
        return context;
    }

    private synchronized GuildContext getGuildContext(Guild guild) {
        return guildContexts.computeIfAbsent(guild.getIdLong(), id -> newGuildContext(id, guild));
    }

    public List<GuildContext> getGuildContexts() {
        return guildContexts.values().stream().toList();
    }


    /* ---- Interactions ---- */

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        InteractionMessageDispatcher messageDispatcher = new SlashMessageDispatcherImpl(event);
        Member member = event.getMember();

        if (event.getGuild() == null || member == null || member.getUser().isBot()) return;

        GuildContext context = getGuildContext(event.getGuild());

        interfaceManager.getSlashManager().dispatch(context.filter(SlashInterface.class), event, new SlashResponseHandler() {

            @Override
            public void notFound(SlashCommandInteractionEvent event, SlashPath path) {
                messageDispatcher.sendMessage("This is an unknown / unregistered command.");
            }

            @Override
            public void wrongParameterType(SlashCommandInteractionEvent event, SlashPath path, int index,
                                           WrongArgumentException e, SlashParameter expected) {
                messageDispatcher.sendMessage(String.format("The argument %s is not a valid type - expected %s, got %s",
                        expected.name(), expected.type().getClazz().getSimpleName(), e.getType().getClazz().getSimpleName()));
            }

            @Override
            public void restricted(SlashCommandInteractionEvent event, SlashPath path,
                                   Permission[] required, Permission[] found) {
                messageDispatcher.sendMessage(String.format("You have insufficient permissions - Missing: %s",
                        FormatUtils.permissionsToString(OtherUtils.differenceInArray(required, found))));
            }

            @Override
            public void selfRestricted(SlashCommandInteractionEvent event, SlashPath path,
                                       Permission[] required, Permission[] found) {
                messageDispatcher.sendMessage(String.format("I have insufficient permissions - Missing: %s",
                        FormatUtils.permissionsToString(OtherUtils.differenceInArray(required, found))));
            }

            @Override
            public void exception(SlashCommandInteractionEvent event, SlashPath path, Throwable throwable) {
                messageDispatcher.sendMessage(String.format("Command threw an exception: %s", throwable));
                log.error("Command ({}) threw an exception", path, throwable);
            }
        });
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        MessageDispatcher messageDispatcher = new AutocompleteMessageDispatcherImpl(event);
        Member member = event.getMember();
        
        if (event.getGuild() == null || member == null || member.getUser().isBot()) return;
        
        GuildContext context = getGuildContext(event.getGuild());
        
        interfaceManager.getAutocompleteManager().dispatch(context.filter(AutocompleteInterface.class),
                event, new AutocompleteResponseHandler() {
            
            @Override
            public void notFound(CommandAutoCompleteInteractionEvent event, String identifier) {
                messageDispatcher.sendMessage("This is an unknown / unregistered autocomplete option.");
            }
    
            @Override
            public void autocompleteException(CommandAutoCompleteInteractionEvent event, String identifier, Throwable throwable) {
                messageDispatcher.sendMessage(String.format("Option threw an exception: %s", throwable));
                log.error("Autocomplete option ({}) threw an exception", identifier, throwable);
            }
        });
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        InteractionMessageDispatcher messageDispatcher = new ButtonMessageDispatcherImpl(event);
        Member member = event.getMember();

        if (event.getGuild() == null || member == null || member.getUser().isBot()) return;

        GuildContext context = getGuildContext(event.getGuild());

        interfaceManager.getButtonManager().dispatch(context.filter(ButtonInterface.class), event, new ButtonResponseHandler() {

            @Override
            public void notFound(ButtonInteractionEvent event, String identifier) {
                messageDispatcher.sendMessage("This is an unknown / unregistered button.");
            }

            @Override
            public void buttonException(ButtonInteractionEvent event, String identifier, Throwable throwable) {
                messageDispatcher.sendMessage(String.format("Button threw an exception: %s", throwable));
                log.error("Button ({}) threw an exception", identifier, throwable);
            }
        });
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        InteractionMessageDispatcher messageDispatcher = new SelectionMessageDispatcherImpl(event);
        Member member = event.getMember();

        if (event.getGuild() == null || member == null || member.getUser().isBot()) return;

        GuildContext context = getGuildContext(event.getGuild());

        interfaceManager.getSelectionManager().dispatch(context.filter(SelectionInterface.class),
                event, new SelectionResponseHandler() {

            @Override
            public void notFound(SelectMenuInteractionEvent event, String identifier) {
                messageDispatcher.sendMessage("This is an unknown / unregistered selection menu.");
            }

            @Override
            public void restricted(SelectMenuInteractionEvent event, String identifier,
                                   Permission[] required, Permission[] found) {
                messageDispatcher.sendMessage(String.format("You have insufficient permissions - Missing: %s",
                        FormatUtils.permissionsToString(OtherUtils.differenceInArray(required, found))));
            }

            @Override
            public void selfRestricted(SelectMenuInteractionEvent event, String identifier,
                                       Permission[] required, Permission[] found) {
                messageDispatcher.sendMessage(String.format("I have insufficient permissions - Missing: %s",
                        FormatUtils.permissionsToString(OtherUtils.differenceInArray(required, found))));
            }

            @Override
            public void exception(SelectMenuInteractionEvent event, String identifier, Throwable throwable) {
                messageDispatcher.sendMessage(String.format("Selection menu threw an exception: %s", throwable));
                log.error("Selection menu ({}) threw an exception", identifier, throwable);
            }
        });
    }
    
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        InteractionMessageDispatcher messageDispatcher = new ModalMessageDispatcherImpl(event);
        Member member = event.getMember();
        
        if (event.getGuild() == null || member == null || member.getUser().isBot()) return;
        
        GuildContext context = getGuildContext(event.getGuild());
        
        interfaceManager.getModalManager().dispatch(context.filter(ModalInterface.class), event, new ModalResponseHandler() {
            @Override
            public void notFound(ModalInteractionEvent event, String identifier) {
                messageDispatcher.sendMessage("This is an unknown / unregistered modal.");
            }
    
            @Override
            public void modalException(ModalInteractionEvent event, String identifier, Throwable throwable) {
                messageDispatcher.sendMessage(String.format("Modal threw an exception: %s", throwable));
                log.error("Modal ({}) threw an exception", identifier, throwable);
            }
        });
    }
    
    /* ---- Other listeners ---- */

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if (!Bot.isLazy())
            interfaceManager.getSlashManager().updateCommands(event.getGuild());
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        interfaceManager.getSlashManager().updateCommands(event.getGuild());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        log.info("Available guilds: {} | Unavailable guilds: {} | Total guilds: {}",
                event.getGuildAvailableCount(), event.getGuildUnavailableCount(), event.getGuildTotalCount());

        bot.getGuildConfig().cleanupGuilds(event.getJDA());
        bot.checkForUpdates();
    }

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        waiter.onGenericEvent(event);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        GuildContext context = guildContexts.remove(event.getGuild().getIdLong());

        if (context != null)
            context.destroy();
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        Member member = event.getMember();

        if (member.getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
            GuildContext context = getGuildContext(event.getGuild());

            context.getController(Kvintakord.class).stop(false);
        }
    }

    /* ---- Other methods ---- */

    public void cleanTemp() {
        File dir = new File("temp");

        if (!dir.exists()) return;

        File[] content = dir.listFiles();

        if (content == null) return;

        for (File file : content) {
            if (file.isDirectory()) continue;

            if (!file.delete())
                log.warn("Couldn't delete temp file '{}'", file.getName());
        }
    }
}
