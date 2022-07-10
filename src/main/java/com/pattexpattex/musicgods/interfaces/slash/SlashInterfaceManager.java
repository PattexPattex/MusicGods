package com.pattexpattex.musicgods.interfaces.slash;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.exceptions.WrongArgumentException;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.slash.objects.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class SlashInterfaceManager {

    private static final Logger log = LoggerFactory.getLogger(SlashInterfaceManager.class);

    private final Map<String, SlashCommand> commands;
    private final SlashGroupManager slashGroupManager;
    private final InterfaceManagerConnector connector;

    public SlashInterfaceManager(InterfaceManagerConnector connector) {
        this.connector = connector;
        this.commands = new HashMap<>();
        this.slashGroupManager = new SlashGroupManager(this);
    }

    public Map<String, SlashCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public InterfaceManagerConnector getConnector() {
        return connector;
    }

    public void dispatch(Map<Class<? extends SlashInterface>, SlashInterface> controllers,
                         SlashCommandInteractionEvent event, SlashResponseHandler handler) {
        SlashPath path = new SlashPath(event.getCommandPath());
        SlashEndpoint endpoint = getEndpointFromPath(path);

        if (endpoint == null) {
            handler.notFound(event, path);
            log.warn("Received an unknown SlashCommandInteraction: {}", path, new NoSuchElementException());
            return;
        }

        Object[] args = new Object[endpoint.getParametersSize() + 1];
        if (parseArgs(event, args, endpoint, handler)) return;
        if (checkPermissions(event, endpoint, handler)) return;

        try {
            endpoint.getMethod().invoke(controllers.get(endpoint.getController()), args);
        }
        catch (InvocationTargetException e) {
            handler.exception(event, path, e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerMethod(Class<? extends SlashInterface> controllerClass, Method method) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0 || !parameters[0].getType().isAssignableFrom(SlashCommandInteractionEvent.class))
            return;

        method.setAccessible(true);
        SlashCommand.createEndpoint(this, commands, controllerClass, method);
    }

    public void updateCommands(Guild guild) {
        //guild.updateCommands().queue();
        guild.updateCommands()
                .addCommands(commands.values()
                        .stream()
                        .map(SlashCommand::getData)
                        .toArray(SlashCommandData[]::new))
                .queue();
    }
    
    @Nullable
    public Command retrieveCommandFromPath(Guild guild, SlashPath path) {
        return guild.retrieveCommands()
                .complete()
                .stream()
                .filter(command -> command.getApplicationIdLong() == Bot.getApplicationId() &&
                        command.getName().equals(path.getBase()))
                .findAny()
                .orElse(null);
    }

    public SlashGroupManager getGroupManager() {
        return slashGroupManager;
    }

    private boolean parseArgs(SlashCommandInteractionEvent event, Object[] args,
                              SlashEndpoint endpoint, SlashResponseHandler handler) {
        args[0] = event;

        for (int i = 0; i < endpoint.getParametersSize(); i++) {
            SlashParameter parameter = endpoint.getParameters().get(i);

            try {
                args[i + 1] = parseSingleArg(parameter.type(), event.getOption(parameter.name()), parameter.required());
            }
            catch (WrongArgumentException e) {
                handler.wrongParameterType(event, new SlashPath(event.getCommandPath()), i, e, parameter);
                return true;
            }
        }

        return false;
    }

    private Object parseSingleArg(ParameterType type, OptionMapping m, boolean required) {
        if (!required && m == null)
            return null;

        Object o = type.apply(m);
        if (required && o == null)
            throw new WrongArgumentException("Argument is an invalid type", type, m.getAsString());

        return o;
    }

    private boolean checkPermissions(SlashCommandInteractionEvent event,
                                     SlashEndpoint endpoint,
                                     SlashResponseHandler handler) {
        SlashEndpoint.Permissions metadata = endpoint.getPermissions();
        GuildMessageChannel channel = event.getGuildChannel();

        Member member = event.getMember();
        EnumSet<Permission> mp = member.getPermissions(channel);
        Permission[] rp = metadata.getMain();

        if (rp.length > 0 && !member.hasPermission(rp)) {
            handler.restricted(event, new SlashPath(event.getCommandPath()),
                    metadata.getMain(), mp.toArray(Permission[]::new));
            return true;
        }

        Member self = event.getGuild().getSelfMember();
        EnumSet<Permission> sp = self.getPermissions(channel);
        Permission[] rsp = metadata.getSelf();

        if (rsp.length > 0 && !self.hasPermission(rsp)) {
            handler.selfRestricted(event, new SlashPath(event.getCommandPath()),
                    metadata.getSelf(), sp.toArray(Permission[]::new));
            return true;
        }

        return false;
    }

    private SlashEndpoint getEndpointFromPath(SlashPath path) {
        SlashCommand command = commands.get(path.getBase());

        if (command == null)
            return null;

        return command.getEndpointByPath(path);
    }
}
