package com.pattexpattex.musicgods.util.dispatchers;

import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public interface InteractionMessageDispatcher {
    void sendMessage(String message, Consumer<InteractionHook> success, Consumer<Throwable> failure);
    void sendMessage(String message);
    void sendMessage(MessageCreateData message, Consumer<InteractionHook> success, Consumer<Throwable> failure);
    void sendSuccess();
    void sendFailure();
}
