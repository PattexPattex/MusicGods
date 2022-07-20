package com.pattexpattex.musicgods.music.audio.filter;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.ResamplingPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NightcoreConfig implements AudioFilterConfig<ResamplingPcmAudioFilter> {
    
    private float amount = 0;
    
    @Override
    public @NotNull String name() {
        return "nightcore";
    }
    
    @Override
    public @Nullable ResamplingPcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new ResamplingPcmAudioFilter(configuration, format.channelCount, output, format.sampleRate, (int) (format.sampleRate / amount));
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(amount, 0);
    }
    
    @Override
    public void reset() {
        amount = 0;
    }
    
    public float getAmount() {
        return amount;
    }
    
    public NightcoreConfig setAmount(float amount) {
        this.amount = amount;
        return this;
    }
}
