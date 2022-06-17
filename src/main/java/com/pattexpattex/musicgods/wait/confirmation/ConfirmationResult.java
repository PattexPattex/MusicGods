package com.pattexpattex.musicgods.wait.confirmation;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ConfirmationResult {
    
    private final ButtonInteractionEvent event;
    private final ConfirmationStatus status;
    private final String prompt;
    private final User requester;
    
    ConfirmationResult(ButtonInteractionEvent event, ConfirmationStatus status, Confirmation confirmation) {
        this.event = event;
        this.status = status;
        this.prompt = confirmation.getPrompt();
        this.requester = confirmation.getRequester();
    }
    
    public ButtonInteractionEvent getEvent() {
        return event;
    }
    
    public ConfirmationStatus getStatus() {
        return status;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public User getRequester() {
        return requester;
    }
}
