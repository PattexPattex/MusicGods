package com.pattexpattex.musicgods.interfaces.selection.objects;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;

public interface SelectionInterfaceFactory<T extends SelectionInterface> extends BaseInterfaceFactory<T> {
    Class<T> getControllerClass();

    T create(ApplicationManager manager, GuildContext context, Guild guild);
}
