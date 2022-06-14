package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.config.storage.GuildConfig;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.wait.prompt.Prompt;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @see Check
 */
public class CheckManager {

    private final Kvintakord kvintakord;

    public CheckManager(Kvintakord kvintakord) {
        this.kvintakord = kvintakord;
    }
    
    /**
     * If the {@linkplain Check checks} pass, the {@link Runnable action} will be executed.
     */
    public void check(Runnable action, IReplyCallback event, Check... checks) {
        if (baseCheck(event, checks))
            action.run();
    }
    
    /**
     * Works identically to {@link CheckManager#check(Runnable, IReplyCallback, Check...) check(Runnable, IReplyCallback, Check...)},
     * except that it also calls {@link IReplyCallback#deferReply() deferReply()}
     * or {@link ButtonInteractionEvent#deferEdit() deferEdit()} if the checks pass.
     *
     * @param deferEdit If set to {@code true} and the event is an instance of
     * {@link ButtonInteractionEvent ButtonInteractionEvent} it will call the
     * {@link ButtonInteractionEvent#deferEdit() deferEdit()} method instead of
     * {@link IReplyCallback#deferReply() deferReply()}.
     */
    public void deferredCheck(Runnable action, IReplyCallback event, boolean deferEdit, Check... checks) {
        if (!baseCheck(event, checks)) return;
    
        if (deferEdit && event instanceof ButtonInteractionEvent bie)
            bie.deferEdit().queue(s -> action.run());
        else
            event.deferReply().queue(s -> action.run());
    }
    
    /**
     * Additionally to checking only the given {@linkplain Check checks},
     * it will also check if the member is a {@linkplain GuildConfig#getDj() DJ}.
     * If not, it will automatically create a {@link Prompt Prompt}.
     *
     * @param message Message that will appear on the {@linkplain Prompt prompt}. Must not be null.
     */
    public void fairCheck(Runnable action, @NotNull String message, IReplyCallback event, Check... checks) {
        fairCheck(action, message, event, false, checks);
    }
    
    /**
     * Works exactly like {@link CheckManager#fairCheck(Runnable, String, IReplyCallback, Check...) fairCheck(Runnable, String, IReplyCallback, Check...)}.
     *
     * @param deferEdit If set to {@code true} and the event is an instance of
     * {@link ButtonInteractionEvent ButtonInteractionEvent} it will call the
     * {@link ButtonInteractionEvent#deferEdit() deferEdit()} method instead of
     * {@link IReplyCallback#deferReply() deferReply()}.
     */
    public void fairCheck(Runnable action, @NotNull String message, IReplyCallback event, boolean deferEdit, Check... checks) {
        if (!baseCheck(event, checks)) return;
    
        AudioChannel channel = kvintakord.getGuild().getAudioManager().getConnectedChannel();
        if (channel == null) {
            deferAnEvent(event, deferEdit).queue(s -> action.run());
            return;
        }
    
        Member member = event.getMember();
        Role role = kvintakord.getConfig().getDj();
    
        boolean isDj = member.getRoles()
                .stream()
                .anyMatch(r -> r.getIdLong() == role.getIdLong());
    
        if (isDj) {
            deferAnEvent(event, deferEdit).queue(s -> action.run());
            return;
        }
    
        List<Member> members = channel.getMembers()
                .stream()
                .filter(m -> !m.getUser().isBot()
                        && !m.getVoiceState().isDeafened())
                .toList();
    
        if (members.size() <= 1) {
            deferAnEvent(event, deferEdit).queue(s -> action.run());
            return;
        }
    
    
        int i = members.size() / 2;
        new Prompt.Builder(message, event, result -> action.run())
                .setOnReject(result -> result.getEvent().deferEdit().delay(10, TimeUnit.SECONDS).queue(s -> s.deleteOriginal().queue()))
                .setOnCancel(result -> result.getEvent().deferEdit().delay(10, TimeUnit.SECONDS).queue(s -> s.deleteOriginal().queue()))
                .setRequiredAccepts(i)
                .setRequiredRejects(i)
                .setTimeout(60)
                .build();
    }
    
    /**
     * The action will only be executed, if the member has the DJ role and the checks pass.
     */
    public void djCheck(Runnable action, IReplyCallback event, boolean deferEdit, Check... checks) {
        if (!baseCheck(event, checks)) return;
        
        Member member = event.getMember();
        Role role = kvintakord.getConfig().getDj();
        
        boolean isDj = member.getRoles()
                .stream()
                .anyMatch(r -> r.getIdLong() == role.getIdLong());
        
        if (isDj)
            deferAnEvent(event, deferEdit).queue(s -> action.run());
        else
            event.reply("You have insufficient permissions.").queue();
    }
    
    private RestAction<InteractionHook> deferAnEvent(IReplyCallback event, boolean deferEdit) {
        if (deferEdit && event instanceof ButtonInteractionEvent bie)
            return bie.deferEdit();
        else
            return event.deferReply();
    }
    
    /**
     * @return {@code True} if the checks pass. {@code False} otherwise. Additionally,
     * if any of the checks fail, it will automatically reply to the {@link IReplyCallback IReplyCallback}
     * with the most significant check fail message.
     *
     * @throws NullPointerException if {@link IReplyCallback#getMember() event.getMember()} returns {@code null}.
     */
    private boolean baseCheck(IReplyCallback event, Check... checks) {
        Check check = memberCheck(event.getMember(), checks);
        
        if (check == Check.NULL_MEMBER)
            throw new NullPointerException("Member is null");
        
        if (check != Check.OK) {
            event.reply(check.message).queue();
            return false;
        }
        
        return true;
    }
    
    /**
     * @deprecated - Use {@link CheckManager#check(Runnable, IReplyCallback, Check...) check(Runnable, IReplyCallback, Check...).
     */
    @Deprecated(forRemoval = true)
    public boolean check(IReplyCallback event, Check... checks) {
        Check check = memberCheck(event.getMember(), checks);
        if (check != Check.OK) {
            event.reply(check.message).queue();
            return true;
        }
        
        if (event instanceof ButtonInteractionEvent bie)
            bie.deferEdit().queue();
        
        return false;
    }
    
    /**
     * @deprecated - Use {@link CheckManager#deferredCheck(Runnable, IReplyCallback, boolean, Check...) deferredCheck(Runnable, IReplyCallback, boolean, Check...)}.
     */
    @Deprecated(forRemoval = true)
    public boolean check(ButtonInteractionEvent event, Check... checks) {
        Check check = memberCheck(event.getMember(), checks);
        if (check != Check.OK) {
            event.editMessage(check.message).setActionRows().setEmbeds().queue();
            return true;
        }

        event.deferEdit().queue();
        return false;
    }
    
    /**
     * @deprecated - Use {@link CheckManager#deferredCheck(Runnable, IReplyCallback, boolean, Check...) deferredCheck(Runnable, IReplyCallback, boolean, Check...)}.
     */
    @Deprecated(forRemoval = true)
    public boolean checkAndReply(IReplyCallback event, Check... checks) {
        Check check = memberCheck(event.getMember(), checks);
        if (check != Check.OK) {
            event.reply(check.message).queue();
            return true;
        }

        event.deferReply().queue();
        return false;
    }
    
    private Check memberCheck(Member member, Check... checks) {
        if (member == null) return Check.NULL_MEMBER;
        if (checks.length == 0) checks = Check.values();
        
        List<Check> failed = new ArrayList<>();
        GuildVoiceState userState = member.getVoiceState();
        GuildVoiceState selfState = member.getGuild().getSelfMember().getVoiceState();
        
        for (Check check : checks) {
            switch (check) {
                case USER_CONNECTED -> {
                    if (!userState.inAudioChannel()) failed.add(check);
                }
                case USER_DEAFENED -> {
                    if (userState.isDeafened()) failed.add(check);
                }
                case SAME_CHANNEL -> {
                    if (!Objects.equals(userState.getChannel(), selfState.getChannel())) failed.add(check);
                }
                case SELF_CONNECTED -> {
                    if (!selfState.inAudioChannel()) failed.add(check);
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
        
        NULL_MEMBER(Integer.MAX_VALUE, "Message author is null"), //How would this ever happen?
        
        OK(Integer.MIN_VALUE, null); //A placeholder for "Yes, all good here."

        /**
         * Higher number means higher severity.
         */
        public final int severity;
        public final String message;

        Check(int severity, String message) {
            this.severity = severity;
            this.message = message;
        }

    }
}