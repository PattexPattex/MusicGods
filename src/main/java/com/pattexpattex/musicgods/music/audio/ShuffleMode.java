package com.pattexpattex.musicgods.music.audio;

import com.pattexpattex.musicgods.util.BotEmoji;

public enum ShuffleMode {
    ON(true, BotEmoji.ARROWS_TWISTED),
    OFF(false, BotEmoji.ARROW_FORWARD);

    private final String emoji;
    private final boolean enabled;

    ShuffleMode(boolean enabled, String emoji) {
        this.enabled = enabled;
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static ShuffleMode fromBoolean(boolean enabled) {
        if (enabled)
            return ON;

        return OFF;
    }
    
    public static ShuffleMode fromString(String s) {
        if ("shuffled".equals(s))
            return ON;
        
        return OFF;
    }
}
