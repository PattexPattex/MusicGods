package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class ChannelMixConfig implements AudioFilterConfig<ChannelMixPcmAudioFilter> {
    
    private float ltr = 0f;
    private float rtl = 0f;
    private float ltl = 1f;
    private float rtr = 1f;
    
    @Override
    public @NotNull String name() {
        return "channel-mix";
    }
    
    @Override
    public @NotNull ChannelMixPcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new ChannelMixPcmAudioFilter(output).setLeftToRight(ltr).setRightToLeft(rtl).setLeftToLeft(ltl).setRightToRight(rtr);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(ltr, 0) || isSet(rtl, 0) || isSet(ltl, 1) || isSet(rtr, 1);
    }
    
    @Override
    public void reset() {
        ltr = 0;
        rtl = 0;
        ltl = 1;
        rtr = 1;
    }
    
    public float getRtl() {
        return rtl;
    }
    
    public ChannelMixConfig setRtl(float rtl) {
        this.rtl = rtl;
        return this;
    }
    
    public float getLtr() {
        return ltr;
    }
    
    public ChannelMixConfig setLtr(float ltr) {
        this.ltr = ltr;
        return this;
    }
    
    public float getRtr() {
        return rtr;
    }
    
    public ChannelMixConfig setRtr(float rtr) {
        this.rtr = rtr;
        return this;
    }
    
    public float getLtl() {
        return ltl;
    }
    
    public ChannelMixConfig setLtl(float ltl) {
        this.ltl = ltl;
        return this;
    }
}
