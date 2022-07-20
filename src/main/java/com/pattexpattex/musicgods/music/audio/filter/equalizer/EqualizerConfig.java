package com.pattexpattex.musicgods.music.audio.filter.equalizer;

import com.pattexpattex.musicgods.music.audio.filter.AudioFilterConfig;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class EqualizerConfig implements AudioFilterConfig<Equalizer> {
    
    private final float[] gains = new float[Equalizer.BAND_COUNT];
    
    @Override
    public @NotNull String name() {
        return "equalizer";
    }
    
    @Override
    public @Nullable Equalizer create(AudioConfiguration configuration, AudioDataFormat format, FloatPcmAudioFilter output) {
        return Equalizer.isCompatible(format) ? new Equalizer(format.channelCount, output, gains) : null;
    }
    
    @Override
    public boolean isEnabled() {
        for (float gain : gains)
            if (isSet(gain, 0))
                return true;
        
        return false;
    }
    
    @Override
    public void reset() {
        Arrays.fill(gains, 0);
    }
    
    public float[] getGains() {
        return Arrays.copyOf(gains, gains.length);
    }
    
    public void useGains(float[] gains) {
        if (gains.length != this.gains.length)
            throw new IllegalArgumentException("Input array length is invalid");
        
        for (int i = 0; i < this.gains.length; i++)
            this.gains[i] = inRange(gains[i]);
    }
    
    public void setGain(int band, float gain) {
        if (isValidBand(band))
            gains[band] = inRange(gain);
    }
    
    public float getGain(int band) {
        if (isValidBand(band))
            return gains[band];
        else
            return 0.0f;
    }
    
    private float inRange(float value) {
        return Math.max(Math.min(value, 1.0f), -0.25f);
    }
    
    private boolean isValidBand(int band) {
        return band >= 0 && band < gains.length;
    }
    
    public enum Preset {
        BASS_BOOST("bass-boost", new float[]{ 0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f }),
        DEFAULT("home-stereo", new float[]{ 0.0f, 0.0f, -0.041666668f, -0.083333336f, -0.125f, -0.16666667f, -0.20833334f, -0.25f, -0.20833334f, -0.16666667f, -0.125f, -0.041666664f, 0.04166667f, 0.125f, 0.125f }),
        OFF("none", new float[]{ 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f })
        ;
    
        private final String name;
        private final float[] arr;
    
        Preset(String name, float[] arr) {
            this.name = name;
            this.arr = arr;
        }
    
        public float[] getArray() {
            return Arrays.copyOf(arr, arr.length);
        }
    
        public String getName() {
            return name;
        }
    
        public static Preset of(String name) {
            for (Preset preset : values())
                if (preset.getName().equals(name))
                    return preset;
        
            return OFF;
        }
    }
}
