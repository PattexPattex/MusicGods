package com.pattexpattex.musicgods.interfaces.selection.objects;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;

public interface SelectionResponseHandler {
    void notFound(GenericSelectMenuInteractionEvent<?, ?> event, String identifier);
    void restricted(GenericSelectMenuInteractionEvent<?, ?> event, String identifier, Permission[] required, Permission[] found);
    void selfRestricted(GenericSelectMenuInteractionEvent<?, ?> event, String identifier, Permission[] required, Permission[] found);
    void exception(GenericSelectMenuInteractionEvent<?, ?> event, String identifier, Throwable throwable);
}
