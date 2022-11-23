package com.pattexpattex.musicgods.util.dispatchers.impl;

import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class MessageDispatcherImpl implements MessageDispatcher {

    private final Message message;

    public MessageDispatcherImpl(Message message) {
        this.message = message;
    }

    @Override
    public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
        this.message.getChannel().sendMessage(message).queue(success, failure);
    }

    @Override
    public void sendMessage(String message) {
        this.message.getChannel().sendMessage(message).queue();
    }

    @Override
    public void sendMessage(MessageCreateData message, Consumer<Message> success, Consumer<Throwable> failure) {
        this.message.getChannel().sendMessage(message).queue(success, failure);
    }

    @Override
    public void sendSuccess() {
        this.message.addReaction(Emoji.fromUnicode(BotEmoji.YES)).queue();
    }

    @Override
    public void sendFailure() {
        this.message.addReaction(Emoji.fromUnicode(BotEmoji.NO)).queue();
    }
}
