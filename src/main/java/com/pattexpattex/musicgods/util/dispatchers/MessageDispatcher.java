package com.pattexpattex.musicgods.util.dispatchers;

import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

public interface MessageDispatcher {
    void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);
    void sendMessage(String message);
    void sendMessage(Message message, Consumer<Message> success, Consumer<Throwable> failure);
    void sendSuccess();
    void sendFailure();
}
