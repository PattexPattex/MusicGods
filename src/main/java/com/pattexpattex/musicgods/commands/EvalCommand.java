package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.SlashParameter;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

// TODO: 26. 05. 2022 Eval
public class EvalCommand implements SlashInterface {

    private final ApplicationManager manager;
    private final GuildContext context;
    private final Guild guild;

    private EvalCommand(ApplicationManager manager, GuildContext context, Guild guild) {
        this.manager = manager;
        this.context = context;
        this.guild = guild;
    }

    @SlashHandle(path = "system/eval", description = "Run arbitrary java code on the bot.")
    public void eval(SlashCommandInteractionEvent event, @SlashParameter(description = "The code to run.") String code) {

    }

    public static class Factory implements SlashInterfaceFactory<EvalCommand> {

        @Override
        public Class<EvalCommand> getControllerClass() {
            return EvalCommand.class;
        }

        @Override
        public EvalCommand create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new EvalCommand(manager, context, guild);
        }
    }
}
