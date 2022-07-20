package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.vibrato.VibratoPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class VibratoConfig implements AudioFilterConfig<VibratoPcmAudioFilter> {
    
    private float frequency = 0;
    private float depth = 0.5f;
    
    @Override
    public @NotNull String name() {
        return "vibrato";
    }
    
    @Override
    public @NotNull VibratoPcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new VibratoPcmAudioFilter(output, format.channelCount, format.sampleRate).setFrequency(frequency).setDepth(depth);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(frequency, 0) || isSet(depth, 0.5f);
    }
    
    @Override
    public void reset() {
        frequency = 0;
        depth = 0.5f;
    }
    
    public float getFrequency() {
        return frequency;
    }
    
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }
    
    public float getDepth() {
        return depth;
    }
    
    public void setDepth(float depth) {
        this.depth = depth;
    }
}
