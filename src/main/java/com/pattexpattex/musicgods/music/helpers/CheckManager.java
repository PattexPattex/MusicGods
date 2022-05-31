package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.music.Kvintakord;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CheckManager {

    private final Kvintakord kvintakord;

    public CheckManager(Kvintakord kvintakord) {
        this.kvintakord = kvintakord;
    }

    /**
     * Checks the given conditions. If no checks failed, a {@link Check#OK Check.OK} is returned.
     * Otherwise, a {@link Check Check} with the highest severity is returned. If no {@code Checks}
     * were passed, the method will check all conditions.
     *
     * @return The most severe failed {@link Check Check}, or
     * {@link Check#OK Check.OK}.
     */
    public Check check(Member member, Check... checks) {
        if (member == null) return Check.NULL_MEMBER;
        if (checks.length == 0) checks = Check.values();

        List<Check> failed = new ArrayList<>();
        GuildVoiceState userState = member.getVoiceState();
        GuildVoiceState selfState = member.getGuild().getSelfMember().getVoiceState();

        for (Check check : checks) {
            switch (check) {
                case USER_CONNECTED -> {
                    if (! userState.inAudioChannel()) failed.add(check);
                }
                case USER_DEAFENED -> {
                    if (userState.isDeafened()) failed.add(check);
                }
                case SAME_CHANNEL -> {
                    if (! Objects.equals(userState.getChannel(), selfState.getChannel())) failed.add(check);
                }
                case SELF_CONNECTED -> {
                    if (! selfState.inAudioChannel()) failed.add(check);
                }
                case SELF_MUTED -> {
                    if (selfState.isMuted()) failed.add(check);
                }
                case PLAYING -> kvintakord.getScheduler().forCurrentTrackNullable(track -> {
                    if (track == null) failed.add(check);
                });
                default -> {
                }
            }
        }

        return failed.stream().max(Comparator.comparingInt(check -> check.severity)).orElse(Check.OK);
    }

    public boolean check(SlashCommandInteractionEvent event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.reply(check.description).queue();
            return true;
        }

        return false;
    }

    public boolean check(ButtonInteractionEvent event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.editMessage(check.description).setActionRows().setEmbeds().queue();
            return true;
        }

        event.deferEdit().queue();
        return false;
    }

    public boolean checkAndReply(ButtonInteractionEvent event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.editMessage(check.description).setActionRows().setEmbeds().queue();
            return true;
        }

        event.deferReply().queue();
        return false;
    }

    public enum Check {
        USER_CONNECTED(5, "You are not connected to a voice channel."),
        SELF_CONNECTED(4, "I am not connected to a voice channel."),
        SAME_CHANNEL(3, "We are not in the same voice channel."),
        USER_DEAFENED(2, "You are deafened."),
        SELF_MUTED(1, "I am muted."),
        PLAYING(0, "I am not playing anything."),
        NULL_MEMBER(Integer.MAX_VALUE, "Message author is null"), //Lol IDK how would this happen.
        OK(Integer.MIN_VALUE, null);

        /**
         * Higher number means higher severity.
         */
        public final int severity;

        public final String description;

        Check(int severity, String description) {
            this.severity = severity;
            this.description = description;
        }

    }
}