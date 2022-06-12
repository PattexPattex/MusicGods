package com.pattexpattex.musicgods.wait.prompt;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class PromptResult {

    private final ButtonInteractionEvent event;
    private final PromptStatus status;
    private final String prompt;
    private final User requester;
    private final List<User> accepts, rejects;

    PromptResult(ButtonInteractionEvent event, PromptStatus status, Prompt prompt) {
        this.event = event;
        this.status = status;
        this.prompt = prompt.getPrompt();
        this.requester = prompt.getRequester();
        this.accepts = prompt.getAccepts();
        this.rejects = prompt.getRejects();
        
    }

    public ButtonInteractionEvent getEvent() {
        return event;
    }
    
    public PromptStatus getStatus() {
        return status;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public User getRequester() {
        return event.getJDA().getUserById(requester.getIdLong());
    }
    
    /**
     * @return An unmodifiable list.
     */
    public List<User> getAccepts() {
        return accepts;
    }
    
    /**
     * @return An unmodifiable list.
     */
    public List<User> getRejects() {
        return rejects;
    }
}
