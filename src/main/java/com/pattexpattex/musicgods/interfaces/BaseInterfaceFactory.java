package com.pattexpattex.musicgods.interfaces;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Collections;
import java.util.List;

public interface BaseInterfaceFactory<T extends BaseInterface> {
    Class<T> getControllerClass();

    T create(ApplicationManager manager, GuildContext context, Guild guild);

    default void finishSetup(ApplicationManager manager) {}

    default List<BaseInterfaceFactory<? extends BaseInterface>> subInterfaces() {
        return Collections.emptyList();
    }
}
