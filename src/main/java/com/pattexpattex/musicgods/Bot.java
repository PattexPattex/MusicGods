package com.pattexpattex.musicgods;

import com.pattexpattex.musicgods.config.Config;
import com.pattexpattex.musicgods.config.storage.GuildConfigManager;
import com.pattexpattex.musicgods.util.BundledLibs;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class Bot {

    public static final long DEVELOPER_ID = 714406547161350155L;
    public static final String VERSION = OtherUtils.getCurrentVersion();
    public static final String GITHUB = "https://github.com/PattexPattex/MusicGods";
    public static final String DONATION = "https://ko-fi.com/pattexpattex";

    private static final String UPDATE_MSG = "There is a new version of MusicGods available!\nCurrent: `%s` **|** Latest: `%s`\nGrab it here: %s/releases/tag/%s";

    public static final Permission[] PERMISSIONS = {
            Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_DEAF_OTHERS,
            Permission.VOICE_MOVE_OTHERS, Permission.VOICE_MUTE_OTHERS, Permission.VIEW_CHANNEL
    };

    private static final String STARTUP = """
 __  __           _       _____           _
|  \\/  |         (_)     / ____|         | |
| \\  / |_   _ ___ _  ___| |  __  ___   __| |___
| |\\/| | | | / __| |/ __| | |_ |/ _ \\ / _` / __|
| |  | | |_| \\__ \\ | (__| |__| | (_) | (_| \\__ \\\040\040""" + VERSION + """
 
|_|  |_|\\__,_|___/_|\\___|\\_____|\\___/ \\__,_|___/
        
  (https://github.com/PattexPattex/MusicGods)
            """;

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private static final AtomicBoolean isShuttingDown = new AtomicBoolean();
    private static boolean lazy = false;
    private static BundledLibs.FFMPEG ffmpeg;
    private static BundledLibs.YTDL ytdl;

    private static Bot bot;

    public static void main(String[] args) {
        if (bot != null) throw new IllegalStateException();

        if (args.length > 0 && (args[0].equals("-l") || args[0].equals("--lazy")))
            lazy = true;

        bot = new Bot();
    }

    public static Bot getInstance() {
        return bot;
    }
    private JDA jda;
    private final Config config;
    private final GuildConfigManager guildConfig;
    private final ApplicationManager applicationManager;

    private Bot() {
        System.out.println(STARTUP);

        log.info("Starting MusicGods...");

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ignore) {}

        if (lazy)
            log.info("Booting in lazy mode...");

        ffmpeg = setupFFMPEG();
        ytdl = setupYTDL();

        config = new Config();
        applicationManager = new ApplicationManager(this);

        try {
            jda = JDABuilder.createDefault(config.getToken(), GUILD_MESSAGES, GUILD_VOICE_STATES)
                    .disableCache(CacheFlag.EMOTE)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setActivity(Activity.watching("me load"))
                    .setStatus((config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB))
                    .addEventListeners(applicationManager)
                    //.setAudioSendFactory(new NativeAudioSendFactory())
                    .build()
                    .awaitReady();
        }
        catch (LoginException e) {
            log.error("Failed logging in! Are you sure you entered the right token?", e);
            System.exit(3);
        }
        catch (IllegalArgumentException e) {
            log.error("Invalid JDA configuration", e);
            System.exit(4);
        }
        catch (InterruptedException e) {
            log.error("Interrupted while waiting for a ReadyEvent", e);
        }

        guildConfig = new GuildConfigManager(this);
        jda.getPresence().setPresence(config.getStatus(), config.getActivity());

        String latest = OtherUtils.getLatestVersion();
        if (latest != null && !Bot.VERSION.equalsIgnoreCase(latest))
            log.info("There is a new update available: {} (current {}) - {}/releases/tag/{}", latest, VERSION, GITHUB, latest);
    }

    public void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) return;

        log.info("Shutting down...");
        applicationManager.shutdown();
        jda.shutdown();
        log.info("Goodbye!");
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

    public void checkForUpdates() {
        if (!config.getUpdateAlerts())
            return;

        applicationManager.getExecutorService().scheduleWithFixedDelay(() -> {
            try {
                String current = Bot.VERSION;
                String latest = OtherUtils.getLatestVersion();
                User owner = jda.retrieveUserById(config.getOwner()).complete();

                if (latest == null)
                    return;

                if (current.equalsIgnoreCase(latest))
                    return;

                owner.openPrivateChannel().queue(channel -> channel.sendMessage(String.format(UPDATE_MSG, current, latest, GITHUB, latest)).queue());
            }
            catch (Exception e) {
                log.warn("Something broke when sending an update notification", e);
            }

        }, 0, 24, TimeUnit.HOURS);
    }


    /* ---- Static methods ---- */

    public static long getApplicationId() {
        return bot.getJDA().getSelfUser().getApplicationIdLong();
    }

    public static boolean isLazy() {
        return lazy;
    }


    /* ---- Bundled libraries / executables ---- */

    public static BundledLibs.FFMPEG getFFMpegStatus() {
        return ffmpeg;
    }

    public static BundledLibs.YTDL getYTDlStatus() {
        return ytdl;
    }

    private static BundledLibs.FFMPEG setupFFMPEG() {
        try {
            Runtime.getRuntime().exec("ffmpeg -h");
            log.info("Found ffmpeg in filesystem...");
            return BundledLibs.FFMPEG.SYSTEM;
        }
        catch (IOException ignored) {
        }

        InputStream stream = Bot.class.getResourceAsStream("/bin/ffmpeg.exe");

        if (stream == null) {
            log.warn("Cannot find ffmpeg.exe in resources, please replace the .jar file");
            return BundledLibs.FFMPEG.NOT_FOUND;
        }

        File ffmpeg = new File("bin/ffmpeg.exe");

        try {
            log.warn("Ffmpeg was not found, falling back to the bundled distribution...");

            if (!ffmpeg.exists()) {
                File dir = new File("bin");
                if (dir.mkdir())
                    log.info("Created '/bin'...");

                Files.copy(stream, ffmpeg.toPath());
            }
        }
        catch (IOException e) {
            log.error("Something went wrong while extracting ffmpeg.exe", e);
            return BundledLibs.FFMPEG.NOT_FOUND;
        }

        return BundledLibs.FFMPEG.BUNDLED;
    }

    private static BundledLibs.YTDL setupYTDL() {
        try {
            Runtime.getRuntime().exec("youtube-dl -h");
            log.info("Found youtube-dl in filesystem...");
            return BundledLibs.YTDL.SYSTEM;
        }
        catch (IOException ignored) {
        }

        InputStream stream = Bot.class.getResourceAsStream("/bin/youtube-dl.exe");

        if (stream == null) {
            log.warn("Cannot find youtube-dl.exe in resources, please replace the .jar file");
            return BundledLibs.YTDL.NOT_FOUND;
        }

        File ytdl = new File("bin/youtube-dl.exe");

        try {
            log.warn("Youtube-dl was not found, falling back to the bundled distribution...");

            if (!ytdl.exists()) {
                File dir = new File("bin");
                if (dir.mkdir())
                    log.info("Created '/bin'...");

                Files.copy(stream, ytdl.toPath());
            }
        }
        catch (IOException e) {
            log.error("Something went wrong while extracting youtube-dl.exe", e);
            return BundledLibs.YTDL.NOT_FOUND;
        }

        return BundledLibs.YTDL.BUNDLED;
    }
}
