package com.pattexpattex.musicgods.interfaces.slash.objects;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;

public interface SlashInterfaceFactory<T extends SlashInterface> extends BaseInterfaceFactory<T> {
    Class<T> getControllerClass();

    T create(ApplicationManager manager, GuildContext context, Guild guild);
}
