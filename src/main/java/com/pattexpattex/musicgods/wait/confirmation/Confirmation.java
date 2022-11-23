package com.pattexpattex.musicgods.wait.confirmation;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.wait.Waiter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Confirmation {
    
    private static final Random RANDOM = Bot.getInstance().getRandom();
    private static final Waiter WAITER = Bot.getInstance().getApplicationManager().getWaiter();
    
    private final long id;
    private final long timeoutSeconds;
    private final long submittedAt;
    
    private final String prompt;
    private final InteractionHook hook;
    private final User requester;
    
    private final Consumer<ConfirmationResult> onConfirm;
    private final Consumer<ConfirmationResult> onDeny;
    private final Consumer<ConfirmationResult> onCancel;
    private final Consumer<InteractionHook> onTimeout;
    
    private final Predicate<ButtonInteractionEvent> predicate;
    
    private Confirmation(@NotNull Consumer<ConfirmationResult> onConfirm, @Nullable Consumer<ConfirmationResult> onDeny,
                         @Nullable Consumer<ConfirmationResult> onCancel, @Nullable Consumer<InteractionHook> onTimeout,
                         String prompt, IReplyCallback event, long timeoutSeconds) {
        
        this.id = RANDOM.nextLong(Long.MAX_VALUE);
        this.timeoutSeconds = timeoutSeconds;
    
        this.onConfirm = onConfirm;
        this.onDeny = onDeny;
        this.onCancel = onCancel;
        this.onTimeout = onTimeout;
    
        this.prompt = prompt;
        this.requester = event.getUser();
    
        this.predicate = ev -> ev.getComponentId().contains(Button.DUMMY_PREFIX + "confirmation:")
                && ev.getComponentId().contains(String.valueOf(id))
                && ev.getUser().getIdLong() == requester.getIdLong();
    
        event.reply(buildMessage()).queue(null, f -> event.getHook().editOriginal(MessageEditData.fromCreateData(buildMessage())).queue());
        this.hook = event.getHook();
        
        this.submittedAt = OtherUtils.epoch();
    }
    
    private void waitForResponse() {
        WAITER.waitForEvent(ButtonInteractionEvent.class, predicate, calculateTimeout(), TimeUnit.SECONDS)
                .thenAccept(event -> {
                    ConfirmationStatus status = ConfirmationStatus.fromComponentId(event.getComponentId());
                    ConfirmationResult result = new ConfirmationResult(event, status, this);
                    
                    switch (status) {
                        case CONFIRMED -> hook.editOriginal(buildFinishedMessage(status)).queue(s -> onConfirm.accept(result));
                        case DENIED -> hook.editOriginal(buildFinishedMessage(status)).queue(s -> {
                            if (onDeny != null)
                                onDeny.accept(result);
                        });
                        case CANCELLED -> hook.editOriginal(buildFinishedMessage(status)).queue(s -> {
                            if (onCancel != null)
                                onCancel.accept(result);
                        });
                        default -> OtherUtils.getLog().error("uhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh... what?");
                    }
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof TimeoutException)
                        hook.editOriginal(new MessageEditBuilder().setContent("Timed out. **|** " + prompt).build()).queue(s -> {
                            if (onTimeout != null)
                                onTimeout.accept(hook);
                        });
                    else
                        OtherUtils.getLog().error("Something broke in a confirmation", throwable);
                    
                    return null;
                });
    }
    
    private MessageCreateData buildMessage() {
        List<ItemComponent> list = new ArrayList<>();
        
        list.add(Button.dummy("confirmation:yes." + id, null, BotEmoji.YES, ButtonStyle.SUCCESS, false));
        
        if (onDeny != null) {
            list.add(Button.dummy("confirmation:no." + id, null, BotEmoji.NO, ButtonStyle.SECONDARY, false));
        }
        
        if (onCancel != null) {
            list.add(Button.dummy("confirmation:cancel." + id, "Cancel", null, ButtonStyle.DANGER, false));
        }
        
        return new MessageCreateBuilder().setContent(prompt).addActionRow(list).build();
    }
    
    private MessageEditData buildFinishedMessage(ConfirmationStatus status) {
        StringBuilder builder = new StringBuilder();
        
        switch (status) {
            case CONFIRMED -> builder.append(BotEmoji.YES);
            case DENIED -> builder.append(BotEmoji.NO);
            case CANCELLED -> builder.append("Cancelled");
            default -> throw new NoSuchElementException();
        }
        
        builder.append(" **|** ").append(prompt);
        
        return new MessageEditBuilder().setContent(builder.toString()).setComponents().build();
    }
    
    private long calculateTimeout() {
        long diff = OtherUtils.epoch() - submittedAt;
        long time = timeoutSeconds - diff;
        return (time < 0 ? -1 : time);
    }
    
    public long getId() {
        return id;
    }
    
    public long getTimeout() {
        return timeoutSeconds;
    }
    
    public long getSubmissionTime() {
        return submittedAt;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public User getRequester() {
        return requester;
    }
    
    @SuppressWarnings("unused")
    public static class Builder {
        
        private final IReplyCallback event;
        
        private String prompt;
        private Consumer<ConfirmationResult> onConfirm, onDeny, onCancel;
        private Consumer<InteractionHook> onTimeout;
        private long timeout;
        
        public Builder(String prompt, IReplyCallback event, Consumer<ConfirmationResult> onConfirm) {
            Checks.notBlank(prompt, "Prompt");
            Objects.requireNonNull(event, "Event cannot be null");
            Objects.requireNonNull(onConfirm, "OnConfirm cannot be null");
            
            this.prompt = prompt;
            this.event = event;
            this.onConfirm = onConfirm;
        }
    
        public String getPrompt() {
            return prompt;
        }
    
        public Builder setPrompt(String prompt) {
            Checks.notBlank(prompt, "Prompt");
            this.prompt = prompt;
            return this;
        }
    
        public Builder setOnConfirm(Consumer<ConfirmationResult> onConfirm) {
            Objects.requireNonNull(onTimeout, "OnConfirm cannot be null");
            this.onConfirm = onConfirm;
            return this;
        }
    
        public Builder setOnDeny(Consumer<ConfirmationResult> onDeny) {
            this.onDeny = onDeny;
            return this;
        }
        
        public Builder setOnCancel(Consumer<ConfirmationResult> onCancel) {
            this.onCancel = onCancel;
            return this;
        }
    
        public Builder setOnTimeout(Consumer<InteractionHook> onTimeout) {
            this.onTimeout = onTimeout;
            return this;
        }
    
        public long getTimeout() {
            return timeout;
        }
    
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public void build() {
            new Confirmation(onConfirm,
                    onDeny,
                    onCancel,
                    onTimeout,
                    prompt,
                    event,
                    Objects.requireNonNullElse(timeout, -1L))
                    .waitForResponse();
        }
    }
}
