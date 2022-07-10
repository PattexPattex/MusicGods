package com.pattexpattex.musicgods.interfaces.slash.objects;

import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.interfaces.slash.SlashDataBuilder;
import com.pattexpattex.musicgods.interfaces.slash.SlashInterfaceManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SlashCommand {

    public static final String DEFAULT_DESCRIPTION = "No description.";

    private final String name;
    private final SlashGroup group;
    private final SlashCommand.Data data;
    private final Map<String, SlashEndpoint> endpoints;
    private final Permission[] permissions;

    private SlashCommand(SlashCommand.Data data, SlashGroup group, Permission[] permissions) {
        this.name = data.getName();
        this.data = data;
        this.group = group;
        this.endpoints = new HashMap<>();
        this.permissions = permissions;

        group.addCommands(this);
    }

    public String getName() {
        return name;
    }

    public SlashCommand.Data getData() {
        return data;
    }

    public Map<String, SlashEndpoint> getEndpoints() {
        return Collections.unmodifiableMap(endpoints);
    }

    public Permission[] getPermissions() {
        return permissions;
    }

    public SlashGroup getGroup() {
        return group;
    }

    public SlashEndpoint getEndpointByPath(SlashPath path) {
        SlashEndpoint endpoint = endpoints.get(path.toString());

        if (endpoint == null)
            endpoint = endpoints.get(path.getBase());

        return endpoint;
    }

    @Contract(mutates = "this")
    public void addEndpoint(SlashEndpoint endpoint) {
        endpoints.put(endpoint.getPath().toString(), endpoint);
    }

    public static void createEndpoint(SlashInterfaceManager manager, Map<String, SlashCommand> commands,
                                      Class<? extends SlashInterface> controller, Method method) {
        SlashHandle handle = method.getAnnotation(SlashHandle.class);
        Permissions permissions = method.getAnnotation(Permissions.class);

        Grouped grouped = method.getAnnotation(Grouped.class);

        if (grouped == null)
            grouped = method.getDeclaringClass().getAnnotation(Grouped.class);

        SlashPath path = new SlashPath(handle.path());
        SlashCommand command = commands.get(path.getBase());

        if (command == null) {
            command = create(path, manager, grouped, permissions);
            commands.put(path.getBase(), command);
        }

        command.addEndpoint(SlashEndpoint.of(controller, handle, permissions, method));
        SlashDataBuilder.addEndpoint(method, handle, command);
    }

    private static SlashCommand create(SlashPath path, SlashInterfaceManager manager,
                                       Grouped grouped, Permissions permissions) {
        return new SlashCommand(
                SlashDataBuilder.buildEmpty(path),
                manager.getGroupManager().getGroup(grouped),
                (permissions == null ? Permission.EMPTY_PERMISSIONS : permissions.command()));
    }

    /*
    * JDA didn't implement a way to remove subcommand groups...
    * */
    public static class Data extends CommandDataImpl {

        public Data(@NotNull String name, @NotNull String description) {
            super(name, description);
            setGuildOnly(true);
        }

        public void mergeSubcommandGroup(SubcommandGroupData data) {
            for (SubcommandGroupData existing : getSubcommandGroups()) {
                if (existing.getName().equals(data.getName())) {
                    options.remove(existing.toData().toMap());
                    data.addSubcommands(existing.getSubcommands());
                    break;
                }
            }

            addSubcommandGroups(data);
        }
    }
}
