package com.pattexpattex.musicgods.config.storage;

import com.pattexpattex.musicgods.music.audio.LoopMode;
import com.pattexpattex.musicgods.music.audio.ShuffleMode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class GuildConfig {

    private final GuildConfigManager manager;
    private final long guild;
    private final AtomicLong dj;
    private final AtomicInteger vol;
    private final AtomicReference<LoopMode> loop;
    private final AtomicReference<ShuffleMode> shuffle;

    protected GuildConfig(JSONObject obj, long guild, GuildConfigManager manager) {
        this(obj.optLong("dj", 0),
                obj.optInt("vol", 100),
                obj.optEnum(LoopMode.class, "loop", LoopMode.OFF),
                obj.optEnum(ShuffleMode.class, "shuffle", ShuffleMode.OFF),
                guild, manager);
    }

    protected GuildConfig(long dj, int vol, LoopMode loop, ShuffleMode shuffle,
                          long guild, GuildConfigManager manager) {
        this.dj = new AtomicLong(dj);
        this.vol = new AtomicInteger(vol);
        this.loop = new AtomicReference<>(loop);
        this.shuffle = new AtomicReference<>(shuffle);
        this.guild = guild;
        this.manager = manager;
    }

    protected JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        if (dj.get() != 0) obj.put("dj", dj.get());
        if (vol.get() != 100) obj.put("vol", vol.get());
        if (loop.get() != LoopMode.OFF) obj.put("loop", loop.get());
        if (shuffle.get() != ShuffleMode.OFF) obj.put("shuffle", shuffle.get());

        if (obj.isEmpty())
            return null;

        return obj;
    }

    public Role getDj() {
        Guild guild = manager.getBot().getJDA().getGuildById(this.guild);
        if (guild == null) return null;
        return guild.getRoleById(dj.get());
    }

    public void setDj(Role role) {
        this.dj.set(role.getIdLong());
        manager.write();
    }

    public int getVol() {
        return vol.get();
    }

    public void setVol(int vol) {
        this.vol.set(vol);
        manager.write();
    }

    public LoopMode getLoop() {
        return loop.get();
    }

    public void setLoop(LoopMode loop) {
        this.loop.set(loop);
        manager.write();
    }

    public ShuffleMode getShuffle() {
        return shuffle.get();
    }

    public void setShuffle(ShuffleMode shuffle) {
        this.shuffle.set(shuffle);
        manager.write();
    }
}
