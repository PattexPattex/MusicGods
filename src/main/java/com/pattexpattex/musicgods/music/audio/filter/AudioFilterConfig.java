package com.pattexpattex.musicgods.music.audio.filter;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AudioFilterConfig<T extends AudioFilter> {
    @NotNull String name();
    @Nullable T create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output);
    boolean isEnabled();
    void reset();
    
    float MIN_DIFF = 0.1f;
    
    default boolean isSet(float val, float defaultVal) {
        return Math.abs(val - defaultVal) >= MIN_DIFF;
    }
}
