package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.wait.prompt.Prompt;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

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
     * @deprecated - change visibility to {@code private}.
     * Use {@link CheckManager#check(Runnable, IReplyCallback, Check...) check(Runnable, IReplyCallback, Check...)}
     */
    @Deprecated(forRemoval = true)
    public boolean check(IReplyCallback event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.reply(check.description).queue();
            return true;
        }
        
        if (event instanceof ButtonInteractionEvent bie)
            bie.deferEdit().queue();

        return false;
    }
    
    public void fairCheck(Runnable action, String message, IReplyCallback event, Check... checks) {
        if (check(event, checks)) return;
    
        AudioChannel channel = kvintakord.getGuild().getAudioManager().getConnectedChannel();
        if (channel == null) {
            event.deferReply().queue(s -> action.run());
            return;
        }
        
        Member member = event.getMember();
        Role role = kvintakord.getConfig().getDj();
        
        boolean isDj = member.getRoles()
                .stream()
                .anyMatch(r -> r.getIdLong() == role.getIdLong());
        
        if (isDj) {
            event.deferReply().queue(s -> action.run());
            return;
        }
        
        List<Member> members = channel.getMembers()
                .stream()
                .filter(m -> !m.getUser().isBot()
                        && !m.getVoiceState().isDeafened())
                .toList();
        
        if (members.size() <= 1) {
            event.deferReply().queue(s -> action.run());
            return;
        }
        
    
        int i = members.size() / 2;
        new Prompt.Builder(message, event, result -> action.run())
                .setOnReject(result -> {})
                .setRequiredAccepts(i)
                .setRequiredRejects(i)
                .setTimeout(60)
                .build();
    }
    
    public void check(Runnable action, IReplyCallback event, Check... checks) {
        if (!check(event, checks))
            action.run();
    }
    
    public void checkAndReply(Runnable action, ButtonInteractionEvent event, Check... checks) {
        if (!checkAndReply(event, checks))
            action.run();
    }
    
    @Deprecated(forRemoval = true)
    public boolean check(ButtonInteractionEvent event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.editMessage(check.description).setActionRows().setEmbeds().queue();
            return true;
        }

        event.deferEdit().queue();
        return false;
    }
    
    @Deprecated(forRemoval = true)
    public boolean checkAndReply(IReplyCallback event, Check... checks) {
        Check check = check(event.getMember(), checks);
        if (check != Check.OK) {
            event.reply(check.description).queue();
            return true;
        }

        event.deferReply().queue();
        return false;
    }
    
    private Check check(Member member, Check... checks) {
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