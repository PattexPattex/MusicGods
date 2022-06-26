package com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public interface AutocompleteResponseHandler {
    void notFound(CommandAutoCompleteInteractionEvent event, String identifier);
    void autocompleteException(CommandAutoCompleteInteractionEvent event, String identifier, Throwable throwable);
}
