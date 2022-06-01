package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@SuppressWarnings("unused")
public class SystemCommands implements SlashInterface {

    private final Bot bot;

    public SystemCommands(GuildContext context) {
        this.bot = context.bot;
    }

    @SlashHandle(path = "system/ping", description = "Checks my ping.", baseDescription = "System commands.")
    public void ping(SlashCommandInteractionEvent event) {
        long ping = System.currentTimeMillis();
        event.deferReply().complete();
        event.getHook().editOriginal(String.format("My ping is: %dms | Gateway ping: %dms",
                System.currentTimeMillis() - ping, event.getJDA().getGatewayPing())).queue();
    }

    @SlashHandle(path = "system/shutdown", description = "Safely shuts down this bot.")
    public void shutdown(SlashCommandInteractionEvent event) {
        if (event.getUser().getIdLong() != bot.getConfig().getOwner())
            event.reply("You have insufficient permissions.").queue();

        event.reply("Shutting down.").queue(m -> bot.shutdown(), f -> bot.shutdown());
    }

    //@SlashHandle(path = "system/test", description = "Used for testing.")
    public void test(SlashCommandInteractionEvent event) {
        event.reply("Please stop.").setEphemeral(true).queue();
    }

    @SlashHandle(path = "system/version", description = "Get versions of used libraries.")
    public void version(SlashCommandInteractionEvent event) {
        event.reply(String.format("Lavaplayer version: `%s`\nJDA version: `%s`\nMusicGods version: `%s`",
                PlayerLibrary.VERSION, JDAInfo.VERSION, Bot.VERSION)).queue();
    }

    public static class Factory implements SlashInterfaceFactory<SystemCommands> {

        @Override
        public Class<SystemCommands> getControllerClass() {
            return SystemCommands.class;
        }

        @Override
        public SystemCommands create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new SystemCommands(context);
        }
    }
}
