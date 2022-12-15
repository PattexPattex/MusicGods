package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.DjCommands;
import com.pattexpattex.musicgods.music.Kvintakord;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Grouped(value = "alias", name = "Command Aliases", description = "Aliases for frequently used commands", emoji = "\uD83D\uDCCB")
public class Aliases implements SlashInterface {
    
    private final ApplicationManager manager;
    private final GuildContext context;
    
    public Aliases(ApplicationManager manager, GuildContext context) {
        this.manager = manager;
        this.context = context;
    }
    
    @SlashHandle(path = "p", description = "Plays a track.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK })
    public void play(SlashCommandInteractionEvent event,
                     @Parameter(description = "URL/query.") String identifier,
                     @Parameter(description = "A search engine.", required = false) @Choice(choices = { "youtube", "spotify" }) String engine) {
        context.getController(Kvintakord.class).play(event, identifier, engine);
    }
    
    @SlashHandle(path = "q", description = "Gets the current queue along with a simple GUI to control music.")
    @Permissions(self = { Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS })
    public void queue(SlashCommandInteractionEvent event,
                      @Parameter(description = "Page of the printed queue.", required = false) @Range(min = 1, max = OptionData.MAX_POSITIVE_NUMBER) Integer page) {
        context.getController(Kvintakord.class).queue(event, page);
    }
    
    @SlashHandle(path = "s", description = "Skips the current track.")
    public void skip(SlashCommandInteractionEvent event,
                     @Parameter(description = "Position to skip to.", required = false) @Range(min = 1, max = OptionData.MAX_POSITIVE_NUMBER) Integer position) {
        context.getController(DjCommands.class).skip(event, position);
    }
    
    public static class Factory implements SlashInterfaceFactory<Aliases> {
    
        @Override
        public Class<Aliases> getControllerClass() {
            return Aliases.class;
        }
    
        @Override
        public Aliases create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new Aliases(manager, context);
        }
    }
}
