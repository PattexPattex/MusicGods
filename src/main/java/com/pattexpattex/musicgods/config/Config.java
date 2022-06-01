package com.pattexpattex.musicgods.config;

import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.regex.Pattern;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final Pattern DISCORD_PATTERN = Pattern.compile("\\d{17,20}");
    private static final Pattern SPOTIFY_PATTERN = Pattern.compile("[a-z\\d]{32}");
    private static final String PATH = "config.json";

    private final JSONObject refConfig;
    private final JSONObject config;

    public Config() {
        this.refConfig = new JSONObject(Objects.requireNonNull(OtherUtils.loadResource(Config.class, "/assets/ref-config.json")));
        this.config = readConfig();
    }


    /* ---- Basic ---- */

    public String getToken() {
        return config.getJSONObject("basic").getString("token");
    }

    public long getOwner() {
        long id = config.getJSONObject("basic").getLong("owner");

        if (!DISCORD_PATTERN.matcher(Long.toString(id)).matches()) {
            brokenValue("basic/owner");
            return -1;
        }

        return id;
    }

    public boolean getEval() {
        return config.getJSONObject("basic").getBoolean("eval");
    }

    public boolean getUpdateAlerts() {
        return config.getJSONObject("basic").getBoolean("update_alerts");
    }


    /* ---- Presence ---- */

    public OnlineStatus getStatus() {
        String status = config.getJSONObject("presence").getString("status");

        switch (status) {
            case "online", "idle", "dnd", "invisible", "offline":
                return OnlineStatus.fromKey(status);

            default: {
                brokenValue("presence/status");
                return OnlineStatus.ONLINE;
            }
        }
    }

    public Activity getActivity() {
        JSONObject obj = config.getJSONObject("presence").getJSONObject("activity");

        String type = obj.getString("type");
        String text = obj.getString("text");

        if (text.isBlank()) {
            brokenValue("presence/activity/text");
            text = "/help";
        }

        switch (type) {
            case "playing", "watching", "listening", "competing":
                return Activity.of(Activity.ActivityType.valueOf(type.toUpperCase()), text);

            case "streaming":
                return Activity.streaming(text, "https://www.youtube.com/watch?v=dQw4w9WgXcQ");

            default: {
                brokenValue("presence/activity/type");
                return Activity.playing(text);
            }
        }
    }


    /* ---- Music ---- */

    public String getSpotifyId() {
        String id = config.getJSONObject("music").getJSONObject("spotify").optString("id");

        if (!SPOTIFY_PATTERN.matcher(id).matches()) {
            brokenValue("music/spotify/id");
            return null;
        }

        return id;
    }

    public String getSpotifySecret() {
        String secret = config.getJSONObject("music").getJSONObject("spotify").optString("secret");

        if (!SPOTIFY_PATTERN.matcher(secret).matches()) {
            brokenValue("music/spotify/secret");
            return null;
        }

        return secret;
    }

    public String getLyricsProvider() {
        String provider = config.getJSONObject("music").getString("lyrics");

        switch (provider) {
            case "A-Z Lyrics", "MusixMatch", "Genius", "LyricsFreak":
                return provider;

            default: {
                brokenValue("music/lyrics");
                return "A-Z Lyrics";
            }
        }
    }

    public int getAloneTimeout() {
        return config.getJSONObject("music").getInt("alone");
    }


    /* ---- Private methods ---- */

    private JSONObject readConfig() {
        JSONObject obj = null;

        try {
            File file = new File(PATH);

            if (file.createNewFile()) {
                try {
                    Files.write(OtherUtils.getPath(PATH), refConfig.toString(4).getBytes());
                    log.info("Created new config file in '{}', please fill it out.", PATH);
                }
                catch (IOException e) {
                    log.error("Something broke while writing config to '{}'", PATH, e);
                    System.exit(1);
                }

                System.exit(2);
            }

            obj = new JSONObject(Files.readString(OtherUtils.getPath(PATH)));
        }
        catch (IOException e) {
            log.error("Something broke while reading config from '{}'", PATH, e);
            System.exit(1);
        }

        log.info("Successfully read config from '{}'...", PATH);
        return obj;
    }

    private static void brokenValue(String path) {
        log.warn("Broken config value '{}', please fix", path);
    }
}
