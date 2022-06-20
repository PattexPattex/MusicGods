package com.pattexpattex.musicgods.wait.prompt;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.wait.Waiter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.utils.Checks;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Prompt {

    private static final Random RANDOM = Bot.getInstance().getRandom();
    private static final Waiter WAITER = Bot.getInstance().getApplicationManager().getWaiter();
    
    private final long id;
    private final long timeoutSeconds;
    private final long submittedAt;
    
    private final String prompt;
    private final InteractionHook hook;
    private final User requester;
    
    private final List<User> accepts;
    private final List<User> rejects;
    
    private final Consumer<PromptResult> onAccept;
    private final Consumer<PromptResult> onReject;
    private final Consumer<PromptResult> onCancel;
    private final Consumer<InteractionHook> onTimeout;
    
    private final int reqAccepts;
    private final int reqRejects;
    
    private final Predicate<ButtonInteractionEvent> predicate;
    
    private Prompt(Consumer<PromptResult> onAccept, Consumer<PromptResult> onReject,
                   Consumer<PromptResult> onCancel, Consumer<InteractionHook> onTimeout,
                   String prompt, IReplyCallback event,
                   int reqAccepts, int reqRejects, long timeoutSeconds) {
        
        this.id = RANDOM.nextLong(Long.MAX_VALUE);
        this.timeoutSeconds = timeoutSeconds;
        
        this.onAccept = onAccept;
        this.onReject = onReject;
        this.onCancel = onCancel;
        this.onTimeout = onTimeout;
        
        this.reqAccepts = reqAccepts;
        this.reqRejects = reqRejects;
        
        this.prompt = prompt;
        this.requester = event.getUser();
        
        this.accepts = new ArrayList<>();
        this.rejects = new ArrayList<>();
        
        this.predicate = ev -> ev.getComponentId().contains(Button.DUMMY_PREFIX + "prompt:")
                && ev.getComponentId().contains(String.valueOf(id))
                && !accepts.contains(ev.getUser())
                && !rejects.contains(ev.getUser());
        
        this.hook = event.reply(buildMessage(onReject != null, onCancel != null)).complete();
        this.submittedAt = OtherUtils.epoch();
    }
    
    private void waitForResponse() {
        WAITER.waitForEvent(ButtonInteractionEvent.class, predicate, calculateTimeout(), TimeUnit.SECONDS)
                .thenAccept(event -> {
                    PromptStatus status = PromptStatus.fromComponentId(event.getComponentId());
                    User user = event.getUser();
                    PromptResult result = new PromptResult(event, status, this);
                
                    switch (status) {
                        case ACCEPT -> {
                            if (event.getUser().getIdLong() == requester.getIdLong()) {
                                event.reply("You cannot accept your own prompt.").setEphemeral(true).queue();
                                waitForResponse();
                                return;
                            }
                            
                            accepts.add(user);
                        
                            if (accepts.size() >= reqAccepts)
                                hook.editOriginal(buildFinishedMessage(status)).queue(s -> onAccept.accept(result));
                            else {
                                event.editMessage(buildMessage(onReject != null, onCancel != null)).queue();
                                waitForResponse();
                            }
                        }
                        case REJECT -> {
                            if (event.getUser().getIdLong() == requester.getIdLong()) {
                                event.reply("You cannot reject your own prompt.").setEphemeral(true).queue();
                                waitForResponse();
                                return;
                            }
                            
                            rejects.add(user);
                        
                            if (rejects.size() >= reqRejects)
                                hook.editOriginal(buildFinishedMessage(status)).queue(s -> onReject.accept(result));
                            else {
                                event.editMessage(buildMessage(onReject != null, onCancel != null)).queue();
                                waitForResponse();
                            }
                        }
                        case CANCEL -> {
                            if (event.getUser().getIdLong() != requester.getIdLong()) {
                                event.reply("You cannot cancel this prompt.").setEphemeral(true).queue();
                                waitForResponse();
                                return;
                            }
                            
                            hook.editOriginal(buildFinishedMessage(status)).queue(s -> onCancel.accept(result));
                        }
                        default -> OtherUtils.getLog().error("uhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh... what?");
                    }
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof TimeoutException)
                        hook.editOriginal(new MessageBuilder("Timed out. **|** " + prompt).build()).queue(s -> {
                            if (onTimeout != null)
                                onTimeout.accept(hook);
                        });
                    else
                        OtherUtils.getLog().error("Something broke in a confirmation", throwable);
                
                    return null;
                });
    }
    
    private long calculateTimeout() {
        long diff = OtherUtils.epoch() - submittedAt;
        long time = timeoutSeconds - diff;
        return (time < 0 ? -1 : time);
    }
    
    private Message buildMessage(boolean isRejectable, boolean isCancellable) {
        MessageBuilder builder = new MessageBuilder();
        builder.append(prompt)
                .append(" ")
                .append(buildMessageSuffix());
        
        if (isRejectable && isCancellable) {
            builder.setActionRows(
                    ActionRow.of(
                            Button.dummy("prompt:yes." + id, null, BotEmoji.YES, ButtonStyle.SUCCESS, false),
                            Button.dummy("prompt:no." + id, null, BotEmoji.NO, ButtonStyle.SECONDARY, false),
                            Button.dummy("prompt:cancel." + id, "Cancel", null, ButtonStyle.DANGER, false)));
        }
        else if (isRejectable) {
            builder.setActionRows(
                    ActionRow.of(
                            Button.dummy("prompt:yes." + id, null, BotEmoji.YES, ButtonStyle.SUCCESS, false),
                            Button.dummy("prompt:no." + id, null, BotEmoji.NO, ButtonStyle.SECONDARY, false)));
        }
        else if (isCancellable) {
            builder.setActionRows(
                    ActionRow.of(
                            Button.dummy("prompt:yes." + id, null, BotEmoji.YES, ButtonStyle.SUCCESS, false),
                            Button.dummy("prompt:cancel." + id, "Cancel", null, ButtonStyle.DANGER, false)));
        }
        else {
            builder.setActionRows(
                    ActionRow.of(
                            Button.dummy("prompt:yes." + id, null, BotEmoji.YES, ButtonStyle.SUCCESS, false)));
        }
        
        return builder.build();
    }
    
    private Message buildFinishedMessage(PromptStatus status) {
        MessageBuilder builder = new MessageBuilder();
        
        switch (status) {
            case ACCEPT -> builder.append(BotEmoji.YES);
            case REJECT -> builder.append(BotEmoji.NO);
            case CANCEL -> builder.append("Cancelled.");
            default -> throw new NoSuchElementException();
        }
        
        builder.append(" **|** ").append(prompt);
        
        return builder.build();
    }
    
    private String buildMessageSuffix() {
        return String.format("(`%d` / `%d`)", accepts.size(), reqAccepts);
    }
    
    
    /* ---- Getters ---- */
    
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
    
    public List<User> getAccepts() {
        return Collections.unmodifiableList(accepts);
    }
    
    public List<User> getRejects() {
        return Collections.unmodifiableList(rejects);
    }
    
    public int getRequiredAccepts() {
        return reqAccepts;
    }
    
    public int getRequiredRejects() {
        return reqRejects;
    }
    
    
    /* ---- Builder ---- */
    
    public static class Builder {
        
        private final IReplyCallback event;
        
        private String prompt;
        private Consumer<PromptResult> onAccept, onReject, onCancel;
        private Consumer<InteractionHook> onTimeout;
        private int reqAccepts, reqRejects;
        private long timeout;
        
        public Builder(String prompt, IReplyCallback event, Consumer<PromptResult> onAccept) {
            Checks.notBlank(prompt, "Prompt");
            Objects.requireNonNull(event, "Event cannot be null");
            Objects.requireNonNull(onAccept, "OnAccept cannot be null");
            
            this.prompt = prompt;
            this.event = event;
            this.onAccept = onAccept;
        }
    
        public String getPrompt() {
            return prompt;
        }
    
        public Builder setPrompt(String prompt) {
            Checks.notBlank(prompt, "Prompt");
            this.prompt = prompt;
            return this;
        }
    
        public Consumer<PromptResult> getOnAccept() {
            return onAccept;
        }
        
        public Builder setOnAccept(Consumer<PromptResult> onAccept) {
            Objects.requireNonNull(onAccept, "OnAccept cannot be null");
            this.onAccept = onAccept;
            return this;
        }
    
        public Consumer<PromptResult> getOnReject() {
            return onReject;
        }
    
        public Builder setOnReject(Consumer<PromptResult> onReject) {
            this.onReject = onReject;
            return this;
        }
    
        public Consumer<PromptResult> getOnCancel() {
            return onCancel;
        }
    
        public Builder setOnCancel(Consumer<PromptResult> onCancel) {
            this.onCancel = onCancel;
            return this;
        }
    
        public int getRequiredAccepts() {
            return reqAccepts;
        }
    
        public Builder setRequiredAccepts(int reqAccepts) {
            this.reqAccepts = reqAccepts;
            return this;
        }
    
        public int getRequiredRejects() {
            return reqRejects;
        }
    
        public Builder setRequiredRejects(int reqRejects) {
            this.reqRejects = reqRejects;
            return this;
        }
    
        public long getTimeout() {
            return timeout;
        }
    
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
    
        public Consumer<InteractionHook> getOnTimeout() {
            return onTimeout;
        }
    
        public Builder setOnTimeout(Consumer<InteractionHook> onTimeout) {
            this.onTimeout = onTimeout;
            return this;
        }
    
        public IReplyCallback getEvent() {
            return event;
        }
    
        public void build() {
            new Prompt(onAccept,
                    onReject,
                    onCancel,
                    onTimeout,
                    prompt,
                    event,
                    Objects.requireNonNullElse(reqAccepts, 1),
                    Objects.requireNonNullElse(reqRejects, 1),
                    Objects.requireNonNullElse(timeout, -1L))
                    .waitForResponse();
        }
    }
}
