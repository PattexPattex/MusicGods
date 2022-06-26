package com.pattexpattex.musicgods.config.storage;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.music.audio.LoopMode;
import com.pattexpattex.musicgods.music.audio.ShuffleMode;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.JDA;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class GuildConfigManager {

    private static final Logger log = LoggerFactory.getLogger(GuildConfigManager.class);

    private static final String FILE = "storage/servers.json";
    private final Map<Long, GuildConfig> configMap;
    private final Bot bot;

    public GuildConfigManager(Bot bot) {
        this.bot = bot;
        this.configMap = new HashMap<>();

        try {
            createFile();
            JSONObject loaded = new JSONObject(Files.readString(OtherUtils.getPath(FILE)));

            loaded.keySet().forEach((id) -> {
                long longId = Long.parseLong(id);

                configMap.put(longId, new GuildConfig(loaded.getJSONObject(id), longId, this));
            });

            write();
        }
        catch (IOException | JSONException e) {
            log.warn("Something broke while reading from '{}'", FILE, e);
        }
    }

    public GuildConfig getConfig(long id) {
        return configMap.computeIfAbsent(id, this::createDefault);
    }

    public void removeConfig(long id) {
        configMap.remove(id);
        write();
    }
    
    public void cleanupGuilds(JDA jda) {
        for (long id : configMap.keySet())
            if (jda.getGuildById(id) == null)
                configMap.remove(id);
        
        write();
    }

    private GuildConfig createDefault(long id) {
        return new GuildConfig(0L, 100, LoopMode.OFF, ShuffleMode.OFF, id, this);
    }

    private static void createFile() throws IOException {
        File dir = new File(FILE.split("/")[0]);
        File file = new File(FILE);

        if (!dir.mkdir()) return;
        if (!file.createNewFile()) return;

        log.info("Created '{}'", file.getName());
        Files.write(OtherUtils.getPath(FILE), "{}".getBytes());
    }

    protected void write() {
        JSONObject toWrite = new JSONObject();

        configMap.forEach((id, config) ->
                toWrite.put(Long.toString(id), config.toJSON()));

        try {
            createFile();
            Files.write(OtherUtils.getPath(FILE), toWrite.toString(4).getBytes());
        }
        catch (IOException e) {
            log.error("Something broke while writing to '{}'", FILE, e);
        }
    }

    protected Bot getBot() {
        return bot;
    }
}
