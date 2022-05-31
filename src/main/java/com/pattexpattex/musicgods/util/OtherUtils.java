package com.pattexpattex.musicgods.util;

import com.pattexpattex.musicgods.Bot;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class OtherUtils {

    private static final Logger log = LoggerFactory.getLogger(OtherUtils.class);

    private OtherUtils() {}

    public static Path getPath(String path) {
        Path result = Paths.get(path);

        if (result.toAbsolutePath().toString().toLowerCase().startsWith("c:\\windows\\system32\\")) {
            try {
                result = Paths.get(new File(Bot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getParentFile().getPath() + File.separator + path);
            }
            catch (URISyntaxException ignored) {}
        }

        return result.toAbsolutePath();
    }

    public static String getInviteUrl() {
        JDA jda = Bot.getInstance().getJDA();
        jda.setRequiredScopes("bot", "application.commands");
        return jda.getInviteUrl(Bot.PERMISSIONS);
    }

    public static boolean isBotPublic() {
        return Bot.getInstance().getJDA().retrieveApplicationInfo().complete().isBotPublic();
    }

    public static Logger getLog() {
        return LoggerFactory.getLogger(Util.getCallingClass());
    }

    public static @Nullable String loadResource(Class<?> clazz, String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(clazz.getResourceAsStream(name))))) {
            StringBuilder sb = new StringBuilder();

            reader.lines().forEach(line -> sb.append("\r\n").append(line));

            return sb.toString().trim();
        }
        catch (IOException | NullPointerException e) {
            log.error("Failed loading resource {}", name, e);
            return null;
        }
    }

    public static byte[] loadResourceBytes(Class<?> clazz, String name) {
        try (InputStream is = Objects.requireNonNull(clazz.getResourceAsStream(name))) {
            return is.readAllBytes();
        }
        catch (IOException | NullPointerException e) {
            log.error("Failed loading resource {}", name, e);
            return null;
        }
    }

    /**
     * @return Elements present in {@code base} and not present in {@code other}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] differenceInArray(T[] base, T[] other) {
        return (T[]) Arrays.stream(base)
                .dropWhile(element -> Arrays.asList(other).contains(element))
                .toList()
                .toArray();
    }

    public static long epoch() {
        return System.currentTimeMillis() / 1000L;
    }
}
