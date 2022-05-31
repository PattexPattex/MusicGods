package com.pattexpattex.musicgods.interfaces.slash;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashPath;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.List;
import java.util.Map;

public class SlashCommandUpdater {

    private SlashCommandUpdater() {}

    /**
     * @throws RuntimeException if a {@link net.dv8tion.jda.api.requests.RestAction RestAction} fails.
     */
    public static void updateCommands(Guild guild, Map<String, SlashCommand> commands) {
        if (commands.isEmpty()) {
            guild.updateCommands().queue();
            return;
        }

        List<Command> list = guild.updateCommands()
                .addCommands(commands.values()
                .stream()
                .map(SlashCommand::getData)
                .toArray(SlashCommandData[]::new))
                .complete();

        for (Command cmd : list) {
            SlashCommand command = commands.get(cmd.getName());
            boolean check = command.getPermissions().length > 0;

            cmd.editCommand()
                    .setDefaultEnabled(false)
                    .addCheck(() -> check)
                    .submit()
                    .thenCompose(c ->
                            c.updatePrivileges(guild, getEnabled(guild, command.getPermissions()))
                            .addCheck(() -> check)
                            .submit());
        }
    }

    public static Command retrieveCommandFromPath(Guild guild, SlashPath path) {
        return guild.retrieveCommands()
                .complete()
                .stream()
                .filter(command -> command.getApplicationIdLong() == Bot.getApplicationId() &&
                                command.getName().equals(path.getBase()))
                .findAny()
                .orElse(null);
    }

    private static CommandPrivilege[] getEnabled(Guild guild, Permission[] permissions) {
        return guild.getRoleCache()
                .stream()
                .filter(role -> role.hasPermission(permissions))
                .map(CommandPrivilege::enable)
                .toArray(CommandPrivilege[]::new);
    }
}
