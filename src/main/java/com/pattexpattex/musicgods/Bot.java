package com.pattexpattex.musicgods;

import com.pattexpattex.musicgods.config.Config;
import com.pattexpattex.musicgods.config.storage.GuildConfigManager;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static final AtomicBoolean isShuttingDown = new AtomicBoolean();
    private static final long start = System.currentTimeMillis();
    
    private JDA jda;
    private final Random random;
    private final Config config;
    private final GuildConfigManager guildConfig;
    private final ApplicationManager applicationManager;

    Bot() {
        System.out.println(String.format(Launcher.startup, Launcher.version, Launcher.github));

        log.info("Starting MusicGods...");

        random = new Random();
        config = new Config();
        guildConfig = new GuildConfigManager(this);
        applicationManager = new ApplicationManager(this);
        applicationManager.cleanTemp();

        try {
            jda = JDABuilder.createDefault(config.getToken(), GUILD_MESSAGES, GUILD_VOICE_STATES)
                    .disableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setActivity(Activity.watching("me load"))
                    .setStatus((config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB))
                    .addEventListeners(applicationManager)
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .build()
                    .awaitReady();
        }
        catch (IllegalArgumentException e) {
            log.error("Invalid JDA configuration", e);
            System.exit(4);
        }
        catch (InterruptedException e) {
            log.error("Interrupted while waiting for a ReadyEvent", e);
        }
        
        jda.getPresence().setPresence(config.getStatus(), config.getActivity());

        String latest = OtherUtils.getLatestVersion();
        if (latest != null && !Launcher.version.equalsIgnoreCase(latest)) {
            log.info("There is a new update available: {} (current {}) - {}/releases/latest", latest, Launcher.version, Launcher.github);
        }
    }

    public void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) return;

        log.info("Shutting down...");
        applicationManager.shutdown();
        jda.shutdown();
        log.info("Goodbye! - Total uptime: {}", FormatUtils.formatTimestamp(System.currentTimeMillis() - start));
        System.exit(0);
    }

    public JDA getJDA() {
        return jda;
    }

    public Config getConfig() {
        return config;
    }

    public GuildConfigManager getGuildConfig() {
        return guildConfig;
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }
    
    public Random getRandom() {
        return random;
    }
    
    public void checkForUpdates() {
        if (!config.getUpdateAlerts()) {
            return;
        }

        applicationManager.getExecutorService().scheduleWithFixedDelay(() -> {
            try {
                String current = Launcher.version;
                String latest = OtherUtils.getLatestVersion();
                User owner = jda.retrieveUserById(config.getOwner()).complete();

                if (latest == null) {
                    return;
                }

                if (current.equalsIgnoreCase(latest)) {
                    return;
                }

                owner.openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage(String.format("There is a new version of MusicGods available!\nCurrent: `%s` **|** Latest: `%s`\nGrab it here: %s/releases/tag/%s",
                                current, latest, Launcher.github, latest)))
                        .queue();
            }
            catch (Exception e) {
                log.warn("Something broke when sending an update notification", e);
            }

        }, 0, 24, TimeUnit.HOURS);
    }
}
