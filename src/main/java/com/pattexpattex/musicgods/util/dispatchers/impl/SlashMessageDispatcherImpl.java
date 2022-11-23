package com.pattexpattex.musicgods.util.dispatchers.impl;

import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.dispatchers.InteractionMessageDispatcher;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class SlashMessageDispatcherImpl implements InteractionMessageDispatcher {

    private final SlashCommandInteractionEvent event;

    public SlashMessageDispatcherImpl(SlashCommandInteractionEvent event) {
        this.event = event;
    }

    @Override
    public void sendMessage(String message, Consumer<InteractionHook> success, Consumer<Throwable> failure) {
        event.reply(message).queue(success, failure);
    }

    @Override
    public void sendMessage(String message) {
        event.reply(message).queue();
    }

    @Override
    public void sendMessage(MessageCreateData message, Consumer<InteractionHook> success, Consumer<Throwable> failure) {
        event.reply(message).queue(success, failure);
    }

    @Override
    public void sendSuccess() {
        event.reply(BotEmoji.YES).queue();
    }

    @Override
    public void sendFailure() {
        event.reply(BotEmoji.NO).queue();
    }
}
