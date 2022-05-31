package com.pattexpattex.musicgods.music;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AloneInVoiceHandler extends ListenerAdapter {

    private final Kvintakord kvintakord;
    private final long timeout;
    private final AtomicLong aloneSince;

    public AloneInVoiceHandler(Kvintakord kvintakord) {
        ApplicationManager manager = kvintakord.getApplicationManager();

        this.kvintakord = kvintakord;
        this.aloneSince = new AtomicLong(-1);
        this.timeout = manager.getBot()
                .getConfig()
                .getAloneTimeout();

        if (timeout > 0) {
            manager.getExecutorService().scheduleWithFixedDelay(this::check, 5, 5, TimeUnit.SECONDS);
            manager.addListener(this);
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        if (guild.getAudioManager().getSendingHandler() == null) return;

        boolean isAlone = isAlone();

        if (isAlone && aloneSince.get() == -1)
            aloneSince.set(OtherUtils.epoch());
        else if (!isAlone)
            aloneSince.set(-1);
    }

    private void check() {
        if (aloneSince.get() == -1)
            return;

        if (aloneSince.get() + timeout < OtherUtils.epoch()) {
            kvintakord.stop(true);
            aloneSince.set(-1);
        }
    }

    private boolean isAlone() {
        Guild guild = kvintakord.getGuild();

        if (guild.getAudioManager().getConnectedChannel() == null)
            return false;

        return guild.getAudioManager()
                .getConnectedChannel()
                .getMembers()
                .stream()
                .noneMatch(member ->
                        !member.getVoiceState().isDeafened() &&
                        !member.getUser().isBot());
    }
}
