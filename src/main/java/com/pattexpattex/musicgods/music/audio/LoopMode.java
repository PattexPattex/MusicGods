package com.pattexpattex.musicgods.music.audio;

import com.pattexpattex.musicgods.util.BotEmoji;

public enum LoopMode {

    OFF("off", BotEmoji.ARROW_RIGHT),
    ALL("all", BotEmoji.REPEAT),
    SINGLE("single", BotEmoji.REPEAT_ONE);

    private final String formatted;
    private final String emoji;

    LoopMode(String formatted, String emoji) {
        this.formatted = formatted;
        this.emoji = emoji;
    }

    public String getFormatted() {
        return formatted;
    }

    public String getEmoji() {
        return emoji;
    }

    public static LoopMode ofString(String val) {

        for (LoopMode mode : values()) {
            if (mode.toString().equalsIgnoreCase(val))
                return mode;
        }

        throw new IllegalArgumentException();
    }
}
