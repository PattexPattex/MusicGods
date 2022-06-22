package com.pattexpattex.musicgods.interfaces.modal.objects;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalResponseHandler {
    void notFound(ModalInteractionEvent event, String identifier);
    void modalException(ModalInteractionEvent event, String identifier, Throwable throwable);
}
