package com.pattexpattex.musicgods.util;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.Launcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

        return result;
    }

    public static String getCurrentVersion() {
        String v = Bot.class.getPackage().getImplementationVersion();
        return (v == null ? "DEV" : v);
    }

    public static String getLatestVersion() {
        Request request = new Request.Builder()
                .get().url("https://api.github.com/repos/PattexPattex/MusicGods/releases/latest").build();

        try (Response response = new OkHttpClient.Builder().build().newCall(request).execute()) {
            ResponseBody body = response.body();

            if (body != null) {
                JSONObject obj = new JSONObject(body.string());
                return obj.optString("tag_name", null);
            }
            else
                return null;
        }
        catch (IOException e) {
            log.warn("Failed retrieving latest version info", e);
            return null;
        }
    }

    public static String getInviteUrl() {
        JDA jda = Launcher.getInstance().getJDA();
        jda.setRequiredScopes("bot", "applications.commands");
        return jda.getInviteUrl(Launcher.permissions);
    }

    public static boolean isBotPublic() {
        return Launcher.getInstance().getJDA().retrieveApplicationInfo().complete().isBotPublic();
    }

    public static Logger getLog() {
        return LoggerFactory.getLogger(Util.getCallingClass());
    }

    public static @Nullable String loadResource(Class<?> clazz, String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(clazz.getResourceAsStream(name))))) {
            StringBuilder sb = new StringBuilder();

            reader.lines().forEach(line -> sb.append(System.lineSeparator()).append(line));

            return sb.toString().trim();
        }
        catch (IOException | NullPointerException e) {
            log.error("Failed loading resource '{}'", name, e);
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
    
    public static List<String> sortByQuery(List<String> list, String query) {
        List<String> copy = new ArrayList<>(list);
        
        copy.sort(Comparator.comparingInt(o -> levenshteinDistance(o, query)));
        
        return copy;
    }
    
    public static void deleteHook(InteractionHook hook) {
        if (hook != null && !hook.isExpired()) hook.deleteOriginal().queue();
    }
    
    private static final Map<Pair<String, String>, Integer> levenshteinCache = new HashMap<>();
    
    public static int levenshteinDistance(String s1, String s2) {
        if (s1.isEmpty())
            return s2.length();
        
        if (s2.isEmpty())
            return s1.length();
        
        Integer cached = levenshteinCache.get(Pair.of(s1, s2));
        
        if (cached != null)
            return cached;
        
        int sub = levenshteinDistance(s1.substring(1), s2.substring(1)) + costOfSubstitute(s1.charAt(0), s2.charAt(0));
        int ins = levenshteinDistance(s1, s2.substring(1)) + 1;
        int del = levenshteinDistance(s1.substring(1), s2) + 1;
        
        int res = Math.min(sub, Math.min(ins, del));
        
        levenshteinCache.put(Pair.of(s1, s2), res);
        
        return res;
    }
    
    private static int costOfSubstitute(char c1, char c2) {
        return c1 == c2 ? 0 : 1;
    }
}
