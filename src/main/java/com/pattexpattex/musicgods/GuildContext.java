package com.pattexpattex.musicgods;

import com.pattexpattex.musicgods.config.storage.GuildConfig;
import com.pattexpattex.musicgods.interfaces.BaseInterface;

import java.util.HashMap;
import java.util.Map;

public class GuildContext {

    public final Bot bot;
    public final long guildId;
    public final GuildConfig config;
    public final Map<Class<? extends BaseInterface>, BaseInterface> controllers;

    public GuildContext(long guildId, Bot bot) {
        this.bot = bot;
        this.guildId = guildId;
        this.config = bot.getGuildConfig().getConfig(guildId);
        this.controllers = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseInterface> T getController(Class<T> klass) {
        return (T) controllers.get(klass);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseInterface> Map<Class<? extends T>, T> filter(Class<T> klass) {
        Map<Class<? extends T>, T> map = new HashMap<>();

        controllers.forEach((k, v) -> {
            if (klass.isAssignableFrom(k))
                map.put((Class<? extends T>) k, (T) v);
        });

        return map;
    }

    public void destroy() {
        controllers.forEach((k, v) -> v.destroy());
        bot.getGuildConfig().removeConfig(guildId);
    }

    public void shutdown() {
        controllers.forEach((k, v) -> v.shutdown());
    }
}
