package com.pattexpattex.musicgods.util.dispatchers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.function.Consumer;

public interface InteractionMessageDispatcher {
    void sendMessage(String message, Consumer<InteractionHook> success, Consumer<Throwable> failure);
    void sendMessage(String message);
    void sendMessage(Message message, Consumer<InteractionHook> success, Consumer<Throwable> failure);
    void sendSuccess();
    void sendFailure();
}
