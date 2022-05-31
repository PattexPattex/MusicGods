package com.pattexpattex.musicgods.interfaces.button.objects;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonResponseHandler {
    void notFound(ButtonInteractionEvent event, String identifier);
    void buttonException(ButtonInteractionEvent event, String identifier, Throwable throwable);
}
