package com.pattexpattex.musicgods.music.audio.filter;

import com.pattexpattex.musicgods.music.audio.filter.equalizer.EqualizerConfig;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class FilterChainBuilder {
    
    private final ChannelMixConfig channelMix = new ChannelMixConfig();
    private final EqualizerConfig equalizer = new EqualizerConfig();
    private final KaraokeConfig karaoke = new KaraokeConfig();
    private final LowPassConfig lowPass = new LowPassConfig();
    private final NightcoreConfig nightcore = new NightcoreConfig();
    private final RotationConfig rotation = new RotationConfig();
    private final TimescaleConfig timescale = new TimescaleConfig();
    private final TremoloConfig tremolo = new TremoloConfig();
    private final VaporwaveConfig vaporwave = new VaporwaveConfig();
    private final VibratoConfig vibrato = new VibratoConfig();
    
    
    private final Map<Class<? extends AudioFilterConfig<?>>, AudioFilterConfig<?>> filters = new HashMap<>();
    
    private final AudioConfiguration configuration;
    
    public FilterChainBuilder(AudioConfiguration configuration) {
        this.configuration = configuration;
        filters.put(channelMix.getClass(), channelMix);
        filters.put(equalizer.getClass(), equalizer);
        filters.put(karaoke.getClass(), karaoke);
        filters.put(lowPass.getClass(), lowPass);
        filters.put(nightcore.getClass(), nightcore);
        filters.put(rotation.getClass(), rotation);
        filters.put(timescale.getClass(), timescale);
        filters.put(tremolo.getClass(), tremolo);
        filters.put(vaporwave.getClass(), vaporwave);
        filters.put(vibrato.getClass(), vibrato);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AudioFilterConfig<?>> T getConfiguration(Class<T> clazz) {
        return (T) filters.get(clazz);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AudioFilterConfig<?>> T getConfiguration(Class<T> clazz, Supplier<T> supplier) {
        return (T) filters.computeIfAbsent(clazz, __ -> {
            T config = Objects.requireNonNull(supplier.get(), "Supplier may not return null");
            
            if (!clazz.isInstance(config))
                throw new IllegalArgumentException("Config is not instance of provided class");
            
            for (AudioFilterConfig<?> c : filters.values())
                if (c.name().equals(config.name()))
                    throw new IllegalArgumentException(String.format("Duplicate name '%s'", c.name()));
            
            return config;
        });
    }
    
    public boolean isEnabled() {
        for (AudioFilterConfig<?> c : filters.values())
            if (c.isEnabled())
                return true;
        
        return false;
    }
    
    public Map<Class<? extends AudioFilterConfig<?>>, AudioFilterConfig<?>> getFilters() {
        return Collections.unmodifiableMap(filters);
    }
    
    public void reset() {
        for (AudioFilterConfig<?> config : filters.values())
            config.reset();
    }
    
    @Nullable
    public PcmFilterFactory build() {
        return isEnabled() ? new Factory(this, configuration) : null;
    }
    
    public ChannelMixConfig getChannelMix() {
        return channelMix;
    }
    
    public EqualizerConfig getEqualizer() {
        return equalizer;
    }
    
    public KaraokeConfig getKaraoke() {
        return karaoke;
    }
    
    public LowPassConfig getLowPass() {
        return lowPass;
    }
    
    public NightcoreConfig getNightcore() {
        return nightcore;
    }
    
    public RotationConfig getRotation() {
        return rotation;
    }
    
    public TimescaleConfig getTimescale() {
        return timescale;
    }
    
    public TremoloConfig getTremolo() {
        return tremolo;
    }
    
    public VaporwaveConfig getVaporwave() {
        return vaporwave;
    }
    
    public VibratoConfig getVibrato() {
        return vibrato;
    }
    
    private record Factory(FilterChainBuilder builder, AudioConfiguration configuration) implements PcmFilterFactory {
    
        @Override
        public @NotNull List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output) {
            List<AudioFilter> list = new ArrayList<>();
            list.add(output);
            
            for (AudioFilterConfig<?> config : builder.filters.values()) {
                AudioFilter filter = config.isEnabled() ? config.create(configuration, format, (FloatPcmAudioFilter) list.get(0)) : null;
                
                if (filter != null)
                    list.add(0, filter);
            }
            
            return list.subList(0, list.size() - 1);
        }
    }
}
