package com.pattexpattex.musicgods.interfaces.slash.objects;

import com.pattexpattex.musicgods.exceptions.WrongArgumentException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface SlashResponseHandler {
    void notFound(SlashCommandInteractionEvent event, SlashPath path);
    void wrongParameterType(SlashCommandInteractionEvent event, SlashPath path, int index, WrongArgumentException e, SlashParameter expected);
    void restricted(SlashCommandInteractionEvent event, SlashPath path, Permission[] required, Permission[] found);
    void selfRestricted(SlashCommandInteractionEvent event, SlashPath path, Permission[] required, Permission[] found);
    void exception(SlashCommandInteractionEvent event, SlashPath path, Throwable throwable);
}
