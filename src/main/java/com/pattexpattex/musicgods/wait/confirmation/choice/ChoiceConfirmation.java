package com.pattexpattex.musicgods.wait.confirmation.choice;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.wait.Waiter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.Checks;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ChoiceConfirmation {
    
    private static final Random RANDOM = Bot.getInstance().getRandom();
    private static final Waiter WAITER = Bot.getInstance().getApplicationManager().getWaiter();
    
    private final long id;
    private final long timeout;
    private final long submittedAt;
    
    private final String prompt;
    private final InteractionHook hook;
    private final User requester;
    
    private final String[] choices;
    
    private final BiConsumer<ChoiceConfirmationResult, Integer> onConfirm;
    private final Consumer<ChoiceConfirmationResult> onCancel;
    private final Consumer<InteractionHook> onTimeout;
    
    private final Predicate<ButtonInteractionEvent> predicate;
    
    private ChoiceConfirmation(String[] choices, BiConsumer<ChoiceConfirmationResult, Integer> onConfirm,
                               Consumer<ChoiceConfirmationResult> onCancel, Consumer<InteractionHook> onTimeout,
                               String prompt, IReplyCallback event, long timeout) {
        
        if (choices.length > 4)
            throw new IllegalStateException("Cannot have more than 4 choices");
        
        this.id = RANDOM.nextLong(Long.MAX_VALUE);
        this.timeout = timeout;
        
        this.choices = choices;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.onTimeout = onTimeout;
        
        this.prompt = prompt;
        this.requester = event.getUser();
        
        this.predicate = ev -> ev.getComponentId().contains(Button.DUMMY_PREFIX + "confirmation.choice:")
                && ev.getComponentId().contains(String.valueOf(id))
                && ev.getUser().getIdLong() == requester.getIdLong();
        
        event.reply(buildMessage()).queue(null, f -> event.getHook().editOriginal(MessageEditData.fromCreateData(buildMessage())).queue());
        this.hook = event.getHook();
        this.submittedAt = OtherUtils.epoch();
    }
    
    private void waitForResponse() {
        WAITER.waitForEvent(ButtonInteractionEvent.class, predicate, calculateTimeout(), TimeUnit.SECONDS)
                .thenAccept(event -> {
                    ChoiceConfirmationStatus status = ChoiceConfirmationStatus.fromComponentId(event.getComponentId());
                    ChoiceConfirmationResult result = new ChoiceConfirmationResult(event, status, this);
                    
                    if (status == ChoiceConfirmationStatus.CANCELLED)
                        hook.editOriginal(buildFinishedMessage(status)).queue(s -> {
                            if (onCancel != null)
                                onCancel.accept(result);
                        });
                    else
                        hook.editOriginal(buildFinishedMessage(status)).queue(s -> onConfirm.accept(result, status.getId()));
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
    
    private long calculateTimeout() {
        long diff = OtherUtils.epoch() - submittedAt;
        long time = timeout - diff;
        return (time < 0 ? -1 : time);
    }
    
    private MessageCreateData buildMessage() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(prompt);
        
        for (int i = 0; i < choices.length; i++) {
            sb.append("\n> ").append(getEmojiFromInt(i)).append(" ").append(choices[i]);
        }
        
        return new MessageCreateBuilder().addComponents(buildRow()).setContent(sb.toString()).build();
    }
    
    private MessageEditData buildFinishedMessage(ChoiceConfirmationStatus status) {
        StringBuilder builder = new StringBuilder();
        
        if (status == ChoiceConfirmationStatus.CANCELLED) {
            builder.append("Cancelled")
                    .append(" **|** ")
                    .append(prompt);
        } else {
            builder.append(getEmojiFromInt(status.getId()))
                    .append(" ")
                    .append(choices[status.getId()])
                    .append(" **|** ")
                    .append(prompt);
        }
        
        return new MessageEditBuilder().setContent(builder.toString()).setComponents().build();
    }
    
    private ActionRow buildRow() {
        int size = choices.length;
        
        List<ItemComponent> list = new ArrayList<>();
        
        for (int i = 0; i < size; i++)
            list.add(Button.dummy(String.format("confirmation.choice:%d.%d", i, id), null, getEmojiFromInt(i), ButtonStyle.PRIMARY, false));
        
        if (onCancel != null)
            list.add(Button.dummy("confirmation.choice:cancel." + id, "Cancel", null, ButtonStyle.DANGER, false));
        
        return ActionRow.of(list);
    }
    
    private static final String EMOJI_1 = "1️⃣";
    private static final String EMOJI_2 = "2️⃣";
    private static final String EMOJI_3 = "3️⃣";
    private static final String EMOJI_4 = "4️⃣";
    private static final String EMOJI_5 = "5️⃣";
    
    private static String getEmojiFromInt(int x) {
        return switch (x) {
            case 0 -> EMOJI_1;
            case 1 -> EMOJI_2;
            case 2 -> EMOJI_3;
            case 3 -> EMOJI_4;
            case 4 -> EMOJI_5;
            default -> throw new IndexOutOfBoundsException();
        };
    }
    
    public long getId() {
        return id;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public User getRequester() {
        return requester;
    }
    
    public String[] getChoices() {
        return Arrays.copyOf(choices, choices.length);
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public long getSubmissionTime() {
        return submittedAt;
    }
    
    @SuppressWarnings("unused")
    public static class Builder {
        
        private final IReplyCallback event;
        private final List<String> choices;
    
        private String prompt;
        private BiConsumer<ChoiceConfirmationResult, Integer> onConfirm;
        private Consumer<ChoiceConfirmationResult> onCancel;
        private Consumer<InteractionHook> onTimeout;
        private long timeout;
        
        public Builder(String prompt, IReplyCallback event, BiConsumer<ChoiceConfirmationResult, Integer> onConfirm) {
            Checks.notBlank(prompt, "Prompt");
            Objects.requireNonNull(event, "Event cannot be null");
            Objects.requireNonNull(onConfirm, "OnConfirm cannot be null");
            
            this.prompt = prompt;
            this.event = event;
            this.onConfirm = onConfirm;
            this.choices = new LinkedList<>();
        }
    
        public String getPrompt() {
            return prompt;
        }
    
        public Builder setPrompt(String prompt) {
            Checks.notBlank(prompt, "Prompt");
            this.prompt = prompt;
            return this;
        }
    
        public long getTimeout() {
            return timeout;
        }
    
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder setOnConfirm(BiConsumer<ChoiceConfirmationResult, Integer> action) {
            Objects.requireNonNull(onTimeout, "OnConfirm cannot be null");
            this.onConfirm = action;
            return this;
        }
    
        public Builder setOnCancel(Consumer<ChoiceConfirmationResult> onCancel) {
            this.onCancel = onCancel;
            return this;
        }
    
        public Builder setOnTimeout(Consumer<InteractionHook> onTimeout) {
            this.onTimeout = onTimeout;
            return this;
        }
        
        public Builder addChoices(String choice, String... other) {
            choices.add(choice);
            choices.addAll(Arrays.asList(other));
            return this;
        }
    
        public Builder addChoices(Collection<String> choices) {
            this.choices.addAll(choices);
            return this;
        }
    
        public Builder setChoices(String choice, String... other) {
            choices.clear();
            choices.add(choice);
            choices.addAll(Arrays.asList(other));
            return this;
        }
    
        public Builder setChoices(Collection<String> choices) {
            this.choices.clear();
            this.choices.addAll(choices);
            return this;
        }
        
        public Builder clearChoices() {
            choices.clear();
            return this;
        }
        
        public void build() {
            if (choices.size() < 1 || choices.size() > 4)
                throw new IllegalStateException(String.format("Too much/not enough choices (%d)", choices.size()));
            
            new ChoiceConfirmation(choices.toArray(new String[0]),
                    onConfirm,
                    onCancel,
                    onTimeout,
                    prompt,
                    event,
                    Objects.requireNonNullElse(timeout, -1L))
                    .waitForResponse();
        }
    }
}