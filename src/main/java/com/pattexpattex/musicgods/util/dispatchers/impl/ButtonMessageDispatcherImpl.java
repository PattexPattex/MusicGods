package com.pattexpattex.musicgods.util.dispatchers.impl;

import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.dispatchers.InteractionMessageDispatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.function.Consumer;

public class ButtonMessageDispatcherImpl implements InteractionMessageDispatcher {

    private final ButtonInteractionEvent event;

    public ButtonMessageDispatcherImpl(ButtonInteractionEvent event) {
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
    public void sendMessage(Message message, Consumer<InteractionHook> success, Consumer<Throwable> failure) {
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
