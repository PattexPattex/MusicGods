package com.pattexpattex.musicgods.commands;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.Launcher;
import com.pattexpattex.musicgods.annotations.selection.SelectionHandle;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.interfaces.selection.objects.Selection;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionInterface;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashGroup;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.util.TimeoutTimer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HelpCommand implements SlashInterface, SelectionInterface {

    private final ApplicationManager manager;
    private final AtomicReference<InteractionHook> message = new AtomicReference<>();
    private final TimeoutTimer timer = new TimeoutTimer(5, TimeUnit.MINUTES,
            () -> disableComponents(message.getAndSet(null)));

    private HelpCommand(ApplicationManager manager) {
        this.manager = manager;
    }

    @SelectionHandle("help:help")
    public void helpSelection(GenericSelectMenuInteractionEvent<?, ?> event, List<String> selected) {
        timer.reset();
        message.set(event.getHook());
        
        SlashGroup group = manager.getInterfaceManager()
                .getSlashManager()
                .getGroupManager()
                .getGroup(selected.get(0));

        Emoji emoji = group.getEmoji();

        EmbedBuilder eb = FormatUtils.embed()
                .setTitle(String.format("%s %s", group.getName(), (emoji != null ? emoji.getFormatted() : "")));

        String desc = group.getDescription();

        if (desc != null && !desc.isBlank())
            eb.appendDescription(desc);

        eb.appendDescription("\n\n")
                .appendDescription(group.getString())
                .appendDescription("\n\n")
                .appendDescription(helpBottomLine());

        event.editMessageEmbeds(eb.build()).queue();
    }

    @SlashHandle(path = "help", description = "Help & info about this bot.")
    public void help(SlashCommandInteractionEvent event) {
        timer.reset();
        message.set(event.getHook());
    
        EmbedBuilder eb = FormatUtils.embed().setTitle("Help");
        MessageCreateBuilder mb = new MessageCreateBuilder().setComponents(ActionRow.of(
                manager.getInterfaceManager().getSelectionManager().buildSelection("help:help", false)));

        eb.appendDescription(helpBottomLine());

        event.reply(mb.setEmbeds(eb.build()).build()).queue();
    }

    private static String helpBottomLine() {
        String a = String.format(
                "[Github](%s) **|** [Submit an issue](%s) **|** [Support me](%s)",
                Launcher.github, Launcher.github + "/issues", Launcher.donation);

        if (OtherUtils.isBotPublic())
            a += String.format(" **|** [Invite me](%s)", OtherUtils.getInviteUrl());

        return a;
    }
    
    private void disableComponents(InteractionHook hook) {
        if (hook == null || hook.isExpired()) {
            return;
        }
        
        hook.retrieveOriginal().queue(msg -> hook.editOriginalComponents(msg
                .getComponents()
                .stream()
                .map(LayoutComponent::asDisabled)
                .toList()).queue());
    }

    public static class Factory implements SlashInterfaceFactory<HelpCommand>, SelectionInterfaceFactory<HelpCommand> {

        @Override
        public Class<HelpCommand> getControllerClass() {
            return HelpCommand.class;
        }

        @Override
        public HelpCommand create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new HelpCommand(manager);
        }

        @Override
        public void finishSetup(ApplicationManager manager) {
            Selection selection = manager.getInterfaceManager()
                    .getSelectionManager()
                    .getSelection("help:help")
                    .setPlaceholder("Select a command group...")
                    .setRange(1, 1);

            manager.getInterfaceManager()
                    .getSlashManager()
                    .getGroupManager()
                    .getGroups()
                    .forEach((identifier, group) ->
                            selection.addOptions(SelectOption.of(group.getName(), identifier)
                                    .withDescription(group.getDescription())
                                    .withEmoji(group.getEmoji())));
        }
    }
}
