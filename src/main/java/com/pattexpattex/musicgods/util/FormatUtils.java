package com.pattexpattex.musicgods.util;

import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.time.Instant;
import java.util.regex.Pattern;

public class FormatUtils {

    public static final String LINE = "▬";
    public static final String CIRCLE = "🔘";
    public static final String BLOCK = "█";
    public static final String CODEBLOCK = "```";
    public static final String CODE = "`";

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

    public static String permissionsArrayToString(Permission[] permissions) {
        StringBuilder builder = new StringBuilder();

        for (Permission permission : permissions) {
            builder.append(String.format(" %s,", permission.getName()));
        }

        return builder.substring(0, builder.length() - 1);
    }

    public static String formatTimeFromMillis(long millis) {
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

    public static String formatTrackUrl(AudioTrack track) {
        return String.format("[%s](%s)", TrackMetadata.getName(track), TrackMetadata.getUri(track));
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

    public static String buildFilledLine(@Range(from = 0, to = 1) double percent, int size) {
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
     * @param time Formatted input: {@code HH:MM:SS / MM:SS / H:MM:SS / M:SS}
     * @throws NumberFormatException if the input is formatted illegally
     */
    @Contract(value = "null -> fail", pure = true)
    public static long decodeTimeToSeconds(String time) {
        Checks.notNull(time, "input");

        Pattern one = Pattern.compile("([01]?\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?");

        if (!one.matcher(time).matches()) {
            throw new NumberFormatException("Invalid input");
        }

        long hours;
        long minutes;
        long seconds;

        time = time.replaceAll(":", "");

        if (time.length() == 3) {
            hours = 0;
            minutes = Long.parseLong(time, 0, 1, 10);
            seconds = Long.parseLong(time, 1, 3, 10);
        }
        else if (time.length() == 4) {
            hours = 0;
            minutes = Long.parseLong(time, 0, 2, 10);
            seconds = Long.parseLong(time, 2, 4, 10);
        }
        else if (time.length() == 5) {
            hours = Long.parseLong(time, 0, 1, 10);
            minutes = Long.parseLong(time, 1, 3, 10);
            seconds = Long.parseLong(time, 3, 5, 10);
        }
        else {
            hours = Long.parseLong(time, 0, 2, 10);
            minutes = Long.parseLong(time, 2, 4, 10);
            seconds = Long.parseLong(time, 4, 6, 10);
        }

        return seconds + (minutes * 60) + (hours * 60 * 60);
    }
}
