package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class TimescaleConfig implements AudioFilterConfig<TimescalePcmAudioFilter> {
    
    private float speed = 1;
    private float pitch = 1;
    private float rate = 1;
    
    @Override
    public @NotNull String name() {
        return "timescale";
    }
    
    @Override
    public @NotNull TimescalePcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate).setSpeed(speed).setPitch(pitch).setRate(rate);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(speed, 1) || isSet(pitch, 1) || isSet(rate, 1);
    }
    
    @Override
    public void reset() {
        speed = 1;
        pitch = 1;
        rate = 1;
    }
    
    public double getRate() {
        return rate;
    }
    
    public TimescaleConfig setRate(float rate) {
        this.rate = rate;
        return this;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public TimescaleConfig setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public TimescaleConfig setSpeed(float speed) {
        this.speed = speed;
        return this;
    }
}
