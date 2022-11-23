package com.pattexpattex.musicgods.util.dispatchers.impl;

import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class AutocompleteMessageDispatcherImpl implements MessageDispatcher {
    
    private final CommandAutoCompleteInteractionEvent event;
    
    public AutocompleteMessageDispatcherImpl(CommandAutoCompleteInteractionEvent event) {
        this.event = event;
    }
    
    @Override
    public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
        event.getMessageChannel().sendMessage(message).queue(success, failure);
    }
    
    @Override
    public void sendMessage(String message) {
        event.getMessageChannel().sendMessage(message).queue();
    }
    
    @Override
    public void sendMessage(MessageCreateData message, Consumer<Message> success, Consumer<Throwable> failure) {
        event.getMessageChannel().sendMessage(message).queue(success, failure);
    }
    
    @Override
    public void sendSuccess() {
        event.getMessageChannel().sendMessage(BotEmoji.YES).queue();
    }
    
    @Override
    public void sendFailure() {
        event.getMessageChannel().sendMessage(BotEmoji.NO).queue();
    }
}
