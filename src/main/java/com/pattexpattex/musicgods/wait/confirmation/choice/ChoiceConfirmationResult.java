package com.pattexpattex.musicgods.wait.confirmation.choice;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ChoiceConfirmationResult {
    
    private final ButtonInteractionEvent event;
    private final ChoiceConfirmationStatus status;
    private final String prompt;
    private final User user;
    private final String[] choices;
    private final String selected;
    
    ChoiceConfirmationResult(ButtonInteractionEvent event, ChoiceConfirmationStatus status,
                             ChoiceConfirmation choiceConfirmation) {
        this.event = event;
        this.status = status;
        this.prompt = choiceConfirmation.getPrompt();
        this.user = choiceConfirmation.getRequester();
        this.choices = choiceConfirmation.getChoices();
        this.selected = (status == ChoiceConfirmationStatus.CANCELLED ? "Cancelled" : choices[status.getId()]);
    }
    
    public ButtonInteractionEvent getEvent() {
        return event;
    }
    
    public ChoiceConfirmationStatus getStatus() {
        return status;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public User getUser() {
        return user;
    }
    
    public String[] getChoices() {
        return choices;
    }
    
    public String getChoice(int i) {
        return choices[i];
    }
    
    public String getSelectedChoice() {
        return selected;
    }
}