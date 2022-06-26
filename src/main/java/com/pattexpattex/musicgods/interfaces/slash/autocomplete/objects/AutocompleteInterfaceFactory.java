package com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;

public interface AutocompleteInterfaceFactory<T extends AutocompleteInterface> extends BaseInterfaceFactory<T> {
    Class<T> getControllerClass();
    
    T create(ApplicationManager manager, GuildContext context, Guild guild);
}
