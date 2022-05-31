package com.pattexpattex.musicgods.interfaces.button.objects;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;

public interface ButtonInterfaceFactory<T extends ButtonInterface> extends BaseInterfaceFactory<T> {
    Class<T> getControllerClass();

    T create(ApplicationManager manager, GuildContext context, Guild guild);
}
