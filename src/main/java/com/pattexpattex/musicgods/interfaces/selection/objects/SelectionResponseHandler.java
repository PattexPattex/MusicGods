package com.pattexpattex.musicgods.interfaces.selection.objects;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

public interface SelectionResponseHandler {
    void notFound(SelectMenuInteractionEvent event, String identifier);
    void restricted(SelectMenuInteractionEvent event, String identifier, Permission[] required, Permission[] found);
    void selfRestricted(SelectMenuInteractionEvent event, String identifier, Permission[] required, Permission[] found);
    void exception(SelectMenuInteractionEvent event, String identifier, Throwable throwable);
}
