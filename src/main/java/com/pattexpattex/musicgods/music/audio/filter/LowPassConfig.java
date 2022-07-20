package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class LowPassConfig implements AudioFilterConfig<LowPassPcmAudioFilter> {
    
    private float smoothing = 0;
    
    @Override
    public @NotNull String name() {
        return "low-pass";
    }
    
    @Override
    public @NotNull LowPassPcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new LowPassPcmAudioFilter(output, format.channelCount).setSmoothing(smoothing);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(smoothing, 0);
    }
    
    @Override
    public void reset() {
        smoothing = 0;
    }
    
    public float getSmoothing() {
        return smoothing;
    }
    
    public LowPassConfig setSmoothing(float smoothing) {
        this.smoothing = smoothing;
        return this;
    }
}
