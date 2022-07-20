package com.pattexpattex.musicgods.music.audio.filter;

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;

public class RotationConfig implements AudioFilterConfig<RotationPcmAudioFilter> {
    
    private float rotation = 0;
    
    @Override
    public @NotNull String name() {
        return "rotation";
    }
    
    @Override
    public @NotNull RotationPcmAudioFilter create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return new RotationPcmAudioFilter(output, format.sampleRate).setRotationSpeed(rotation);
    }
    
    @Override
    public boolean isEnabled() {
        return isSet(rotation, 0);
    }
    
    @Override
    public void reset() {
        rotation = 0;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
