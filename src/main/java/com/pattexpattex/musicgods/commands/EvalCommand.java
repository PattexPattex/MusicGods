package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.SlashParameter;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EvalCommand implements SlashInterface {

    private static final AtomicLong COUNTER = new AtomicLong();

    private final ApplicationManager manager;
    private final GuildContext context;
    private final Guild guild;
    private ScriptEngine engine;

    private EvalCommand(ApplicationManager manager, GuildContext context, Guild guild) {
        this.manager = manager;
        this.context = context;
        this.guild = guild;
    }

    @SlashHandle(path = "system/eval", description = "Run arbitrary Kotlin code on the bot.")
    public void evalCommand(SlashCommandInteractionEvent event0,
                            @SlashParameter(description = "The code to run.") String code) {
        if (!manager.getBot().getConfig().getEval()) {
            event0.reply("Eval is not enabled.").setEphemeral(true).queue();
            return;
        }

        if (event0.getUser().getIdLong() != Bot.DEVELOPER_ID) {
            event0.reply("You have insufficient permissions.").setEphemeral(true).queue();
            return;
        }

        long id = COUNTER.incrementAndGet();
        event0.reply("Are you sure you want to eval this script?").setEphemeral(true).addActionRow(
                Button.dummy("eval:yes." + id, "Yes, do it", null, ButtonStyle.DANGER, false),
                Button.dummy("eval:no." + id, "No, cancel", null, ButtonStyle.SECONDARY, false))
                .queue();

        engine = new ScriptEngineManager().getEngineByExtension("java");

        if (code.contains("SlashCommandInteractionEvent event"))
            engine.put("event", event0);

        if (code.contains("ApplicationManager manager"))
            engine.put("manager", manager);

        if (code.contains("GuildContext context"))
            engine.put("context", context);

        manager.getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        ev -> ev.getUser().getIdLong() == Bot.DEVELOPER_ID &&
                                ev.getComponentId().equals(Button.DUMMY_PREFIX + "eval:yes." + id),
                        1, TimeUnit.MINUTES)
                .thenAccept(ev -> {
                    ev.deferEdit().queue();
                    eval(event0, code);
                });

        manager.getWaiter().waitForEvent(ButtonInteractionEvent.class,
                        ev -> ev.getUser().getIdLong() == Bot.DEVELOPER_ID &&
                                ev.getComponentId().equals(Button.DUMMY_PREFIX + "eval:no." + id),
                        1, TimeUnit.MINUTES)
                .thenAccept(ev ->
                        ev.editMessage("Okay then.").setActionRows().queue());
    }

    private void eval(SlashCommandInteractionEvent event, String script) {
        long start = System.currentTimeMillis();

        Object out;
        try {
            out = engine.eval(script);
        }
        catch (Throwable t) {
            out = t;
        }

        long elapsed = System.currentTimeMillis() - start;
        parseEvalResponse(event, out, elapsed);

        OtherUtils.getLog().info("Took {}ms for evaluating script by user {} ({})",
                elapsed, event.getUser().getAsTag(), event.getUser().getIdLong());
    }

    private void parseEvalResponse(SlashCommandInteractionEvent event, Object response, long elapsed) {
        if (response == null) {
            event.getHook().editOriginal(String.format("Eval success! **|** Took %sms", elapsed)).setActionRows().queue();
        }
        else if (response instanceof Throwable thr) {
            event.getHook().editOriginal(String.format("Eval failed. **|** Took %sms **|** What went wrong: `%s`",
                    elapsed, thr)).setActionRows().queue();

            OtherUtils.getLog().warn("Eval failed", thr);
        }
        else if (response instanceof RestAction<?> action) {
            action.queue(s -> {
                        event.getHook().editOriginal(
                                String.format("Eval success! **|** Took %sms **|** Rest action returned `%s`",
                                        elapsed, s)).setActionRows().queue();
                    },
                    thr -> {
                        event.getHook().editOriginal(String.format("Eval failed. **|** Took %sms **|** What went wrong: `%s`",
                                elapsed, thr)).setActionRows().queue();

                        OtherUtils.getLog().warn("Eval failed", thr);
                    });
        }
        else {
            event.getHook().editOriginal(
                    String.format("Eval success! **|** Took %sms **|** Script returned `%s`",
                            elapsed, response)).queue();
        }
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
