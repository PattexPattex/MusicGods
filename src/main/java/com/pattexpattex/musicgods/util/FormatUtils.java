package com.pattexpattex.musicgods.util;

import com.pattexpattex.musicgods.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.apache.http.client.utils.DateUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Pattern;

public class FormatUtils {

    public static final String LINE = "▬";
    public static final String CIRCLE = "🔘";
    public static final String BLOCK = "█";
    public static final String CODEBLOCK = "```";
    public static final String CODE = "`";
    
    public static final Pattern HTTP_PATTERN = Pattern.compile("^([<|*_`]{0,3})(http|https)://[a-zA-Z\\d\\-.]+\\.[a-zA-Z]{2,6}(/\\S*)?([>|*_`]{0,3})$");
    
    private static final String[] DATE_PARSER_PATTERNS = { "HH:mm:ss", "H:mm:ss", "mm:ss", "m:ss" };
    private static final String AVATAR = "https://imgur.com/fMEiH2k.png";
    private static final Color COLOR = new Color(0xDFE393);

    private FormatUtils() {}

    public static EmbedBuilder embed() {
        return new EmbedBuilder()
                .setColor(COLOR)
                .setFooter(String.format("Powered by MusicGods %s.", Bot.VERSION), AVATAR)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder kvintakordEmbed() {
        return new EmbedBuilder()
                .setColor(COLOR)
                .setFooter("Powered by Kvintakord.", AVATAR)
                .setTimestamp(Instant.now());
    }

    public static String permissionsToString(Permission[] permissions) {
        StringBuilder builder = new StringBuilder();

        for (Permission permission : permissions) {
            builder.append(String.format(" %s,", permission.getName()));
        }

        return builder.substring(0, builder.length() - 1);
    }

    public static String formatTimestamp(long millis) {
        if (millis == Long.MAX_VALUE) {
            return "LIVE";
        }

        long seconds = Math.round(millis / 1000.0);
        long hours = seconds / (60 * 60);

        seconds %= 60 * 60;

        long minutes = seconds / 60;

        seconds %= 60;

        return (hours > 0 ? hours + ":" : "") +
                (minutes < 10 ? "0" + minutes : minutes) + ":" +
                (seconds < 10 ? "0" + seconds : seconds);
    }

    public static String buildLine(@Range(from = 0, to = 1) double percent, int size) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < size; i++) {
            if (i == (int) (percent * size))
                builder.append(CIRCLE);
            else
                builder.append(LINE);
        }

        return builder.toString();
    }

    public static String buildFullLine(@Range(from = 0, to = 1) double percent, int size) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < size; i++) {
            if (i < (int) (percent * size))
                builder.append(BLOCK);
            else
                builder.append(LINE);
        }

        return builder.toString();
    }

    /**
     * @param time Formatted input: {@code HH:mm:ss / mm:ss / H:mm:ss / m:ss}
     * @throws NumberFormatException if the input is formatted illegally
     */
    @Contract(value = "null -> fail", pure = true)
    public static long parseTime(String time) {
        Date date = DateUtils.parseDate(time, DATE_PARSER_PATTERNS);
        return date.getTime() / 1000;
    }
}
