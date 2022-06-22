package com.pattexpattex.musicgods.interfaces.modal.objects;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import net.dv8tion.jda.api.entities.Guild;

public interface ModalInterfaceFactory<T extends ModalInterface> extends BaseInterfaceFactory<T> {
    Class<T> getControllerClass();
    
    T create(ApplicationManager manager, GuildContext context, Guild guild);
}
