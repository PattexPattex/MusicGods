package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class KaraokeConfig implements AudioFilterConfig<KaraokePcmAudioFilter> {
    
    private float level = 0;
    private float monoLevel = 1;
    private float filterBand = 220;
    private float filterWidth = 100;
    
    @Override
    public @NotNull String name() {
        return "karaoke";
    }
    
    @Override
    public @NotNull KaraokePcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new KaraokePcmAudioFilter(output, format.channelCount, format.sampleRate).setFilterBand(filterBand).setFilterWidth(filterWidth).setLevel(level).setMonoLevel(monoLevel);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(level, 0) || isSet(monoLevel, 1) || isSet(filterBand, 220) || isSet(filterWidth, 100);
    }
    
    @Override
    public void reset() {
        level = 0;
        monoLevel = 1;
        filterBand = 220;
        filterWidth = 100;
    }
    
    public float getLevel() {
        return level;
    }
    
    public KaraokeConfig setLevel(float level) {
        this.level = level;
        return this;
    }
    
    public float getMonoLevel() {
        return monoLevel;
    }
    
    public KaraokeConfig setMonoLevel(float monoLevel) {
        this.monoLevel = monoLevel;
        return this;
    }
    
    public float getFilterBand() {
        return filterBand;
    }
    
    public KaraokeConfig setFilterBand(float filterBand) {
        this.filterBand = filterBand;
        return this;
    }
    
    public float getFilterWidth() {
        return filterWidth;
    }
    
    public KaraokeConfig setFilterWidth(float filterWidth) {
        this.filterWidth = filterWidth;
        return this;
    }
}
