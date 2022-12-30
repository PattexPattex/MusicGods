package com.pattexpattex.musicgods;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.ConsoleAppender;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Launcher {
	
	public static final String version = OtherUtils.getCurrentVersion();
	public static final String github = "https://github.com/PattexPattex/MusicGods";
	public static final String donation = "https://ko-fi.com/pattexpattex";
	private static final List<RuntimeFlags.Flags> flags = new ArrayList<>();
	private static final Logger log = OtherUtils.getLog();
	static final String updateMsg = "There is a new version of MusicGods available!\nCurrent: `%s` **|** Latest: `%s`\nGrab it here: %s/releases/latest";
	static final String startup = """
 
 __  __           _       _____           _
|  \\/  |         (_)     / ____|         | |
| \\  / |_   _ ___ _  ___| |  __  ___   __| |___
| |\\/| | | | / __| |/ __| | |_ |/ _ \\ / _` / __|
| |  | | |_| \\__ \\ | (__| |__| | (_) | (_| \\__ \\  %s
|_|  |_|\\__,_|___/_|\\___|\\_____|\\___/ \\__,_|___/
  
  (%s)
            """;
	public static final Permission[] permissions = {
			Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES,
			Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_DEAF_OTHERS,
			Permission.VOICE_MOVE_OTHERS, Permission.VOICE_MUTE_OTHERS, Permission.VIEW_CHANNEL
	};
	/**
	 * This {@link Consumer} is executed when the {@code -up} flag is applied. To disable, set to {@code null}.
	 */
	@Nullable
	public static final Consumer<ReadyEvent> migrationConsumer = event -> {
		log.info("Migrating commands in {} out of {} guilds ({} unavailable)", event.getGuildAvailableCount(), event.getGuildTotalCount(), event.getGuildUnavailableCount());
		event.getJDA().getGuilds().stream().map(Guild::updateCommands).forEach(RestAction::queue);
	};
	
	private static int libs = 0;
	private static Bot instance;
	
	public static void main(String[] args) {
		if (instance != null) {
			throw new IllegalStateException();
		}
		
		flags.addAll(new RuntimeFlags(args).getFlags());
		log.info("Using flags '{}'", flags.stream().map(flag -> flag.longFlag).collect(Collectors.joining(", ")));
		
		if (RuntimeFlags.Flags.VERBOSE.isActive()) {
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.ALL);
		}
		
		checkFfmpeg();
		checkYTDL();
		
		if (!(isFfmpeg() && isYTDL())) {
			log.info("""
                    To enable track downloads, download
                      - ffmpeg (https://ffmpeg.org/download.html) and
                      - youtube-dl (http://ytdl-org.github.io/youtube-dl/download.html)
                    and place the executables in the bot's working directory.""");
		}
		
		instance = new Bot();
	}
	
	public static Bot getInstance() {
		return instance;
	}
	
	public static long getApplicationId() {
		return instance.getJDA().getSelfUser().getApplicationIdLong();
	}
	
	public static List<RuntimeFlags.Flags> getFlags() {
		return flags;
	}
	
	public static boolean isFfmpeg() {
		return libs % 2 == 1;
	}
	
	public static boolean isYTDL() {
		return (libs >> 1) % 2 == 1;
	}
	
	private static void checkFfmpeg() {
		boolean result;
		
		try {
			Runtime.getRuntime().exec("ffmpeg -h");
			log.info("Found ffmpeg...");
			result = true;
		}
		catch (IOException e) {
			log.warn("Couldn't detect ffmpeg, track downloading will be disabled", e);
			result = false;
		}
		
		libs += (result ? 1 : 0);
	}
	
	private static void checkYTDL() {
		boolean result;
		
		try {
			Runtime.getRuntime().exec("youtube-dl -h");
			log.info("Found youtube-dl...");
			result = true;
		}
		catch (IOException e) {
			log.warn("Couldn't detect youtube-dl, track downloading will be disabled", e);
			result = false;
		}
		
		libs += (result ? 1 : 0) << 1;
	}
}
