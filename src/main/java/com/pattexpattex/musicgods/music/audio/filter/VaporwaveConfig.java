package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaporwaveConfig implements AudioFilterConfig<TimescalePcmAudioFilter> {
    
    private float value = 0;
    
    @Override
    public @NotNull String name() {
        return "vaporwave";
    }
    
    @Override
    public @Nullable TimescalePcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate).setSpeed(value).setPitchSemiTones(-7.0);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(value, 0);
    }
    
    @Override
    public void reset() {
        value = 0;
    }
    
    public float getValue() {
        return value;
    }
    
    public VaporwaveConfig setValue(float value) {
        this.value = value;
        return this;
    }
}
