package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.modal.Element;
import com.pattexpattex.musicgods.annotations.modal.ModalHandle;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalInterface;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.wait.confirmation.Confirmation;
import com.pattexpattex.musicgods.wait.confirmation.ConfirmationResult;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Objects;

public class EvalCommand implements SlashInterface, ModalInterface {

    public static final String IMPORTS = """
            import com.pattexpattex.musicgods.ApplicationManager
            import com.pattexpattex.musicgods.GuildContext
            import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
        
            val event: ButtonInteractionEvent = // ...
            val manager: ApplicationManager = // ...
            val ctx: GuildContext = // ...
            /* You can use these variables, they are automatically supplied */
            """;

    private final ApplicationManager manager;
    private final GuildContext context;
    private final Guild guild;
    private ScriptEngine engine;

    private EvalCommand(ApplicationManager manager, GuildContext context, Guild guild) {
        this.manager = manager;
        this.context = context;
        this.guild = guild;
    }

    @SlashHandle(path = "system/eval", description = "Run Kotlin code directly from the bot")
    public void eval(SlashCommandInteractionEvent event, @Parameter(description = "Code to run", required = false) String code) {
        if (event.getUser().getIdLong() != context.bot.getConfig().getOwner()) {
            event.reply("You have insufficient permissions.").queue();
            return;
        }
    
        if (code == null) {
            event.replyModal(manager.getInterfaceManager().getModalManager().buildModal("sys.eval")).queue();
            return;
        }
        
        createConfirm(event, code);
    }
    
    @ModalHandle(id = "sys.eval", title = "Code evaluation")
    public void evalModal(ModalInteractionEvent event,
                          @Element(
                                  label = "Your code",
                                  placeholder = "event.user.openPrivateChannel().queue {\n    it.sendMessage(\":)\").queue()\n}",
                                  style = TextInputStyle.PARAGRAPH) String code) {
        createConfirm(event, code);
    }
    
    private void createConfirm(IReplyCallback event, String code) {
        new Confirmation.Builder(
                String.format("Are you sure you want to run this code?\n```kt\n%s\n%s\n```", IMPORTS, FormatUtils.limitString(code, 500)),
                event, result -> runCode(result, code))
                .setOnCancel(result -> result.getEvent().editMessage("Okay then.").queue())
                .build();
    }
    
    private void runCode(ConfirmationResult result, String code) {
        try {
            engine = Objects.requireNonNull(new ScriptEngineManager().getEngineByExtension("kts"));
        } catch (NullPointerException e) {
            result.getEvent().getChannel().sendMessageFormat("Whoops, something went wrong. Here is the stacktrace:\n```\n%s```", e).queue();
        }
    
        engine.put("event", result.getEvent());
        engine.put("manager", manager);
        engine.put("ctx", context);
        
        Object out;
        
        try {
            out = engine.eval(code);
        } catch (ScriptException e) {
            result.getEvent().getChannel().sendMessageFormat("Whoops, your code doesn't work. Here is the exception:\n```\n%s```", e).queue();
            return;
        }
        
        result.getEvent().getChannel().sendMessageFormat("Success! Your code returned `%s`.", out).queue();
    }

    public static class Factory implements SlashInterfaceFactory<EvalCommand>, ModalInterfaceFactory<EvalCommand> {

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
