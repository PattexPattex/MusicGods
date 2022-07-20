package com.pattexpattex.musicgods.music;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.Autocomplete;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.AutocompleteHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import com.pattexpattex.musicgods.interfaces.BaseInterface;
import com.pattexpattex.musicgods.interfaces.BaseInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterface;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.audio.filter.*;
import com.pattexpattex.musicgods.music.audio.filter.equalizer.EqualizerConfig;
import com.pattexpattex.musicgods.music.audio.filter.equalizer.EqualizerManager;
import com.pattexpattex.musicgods.music.helpers.CheckManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import static com.pattexpattex.musicgods.music.AudioFilterManager.GROUP_ID;

@Grouped(value = GROUP_ID, name = "Audio Filters", description = "Commands related to audio filters.", emoji = BotEmoji.CONTROL_KNOBS)
public class AudioFilterManager implements SlashInterface, AutocompleteInterface, ButtonInterface {
    
    public static final String GROUP_ID = Kvintakord.GROUP_ID + "/filters";
    
    private final ApplicationManager manager;
    private final Kvintakord kvintakord;
    private final CheckManager checkManager;
    private final FilterChainBuilder filters;
    private final EqualizerManager equalizerManager;
    
    private AudioFilterManager(ApplicationManager manager, Kvintakord kvintakord) {
        this.manager = manager;
        this.kvintakord = kvintakord;
        this.filters = new FilterChainBuilder(kvintakord.getPlayerManager().getConfiguration());
        this.equalizerManager = new EqualizerManager(this);
        this.checkManager = kvintakord.getCheckManager();
        kvintakord.subInterfaceLoaded(this);
    }
    
    @SlashHandle(path = "filters/active", description = "Gets a list of active audio filters.", baseDescription = "Performs basic filter operations.")
    public void active(SlashCommandInteractionEvent event) {
        checkManager.check(() -> event.reply(buildActiveFiltersMessage()).queue(), event, CheckManager.Check.PLAYING);
    }
    
    @SlashHandle(path = "filters/reset", description = "Resets all audio filters.")
    public void reset(SlashCommandInteractionEvent event) {
        checkManager.djCheck(() -> {
            reset();
            equalizerManager.updateMessage();
            event.getHook().editOriginal("Reset all audio filters.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "channelmix", description = "Channel mix audio filter.")
    public void channelMix(SlashCommandInteractionEvent event,
                           @Parameter(name = "strength", description = "Strength of the filter (in percent). Set to 0 to disable.", required = false) @Range(min = 0, max = 100) Double s) {
        checkManager.djCheck(() -> {
            ChannelMixConfig filter = filters.getChannelMix();
            float strength = nonNullOr(s, 75).floatValue() / 100;
            
            if (filter.isEnabled() && s == null) filter.reset();
            else filter.setLtr(strength).setRtl(strength).setLtl(1.0f - strength).setRtr(1.0f - strength);
            
            updateFilters();
            event.getHook().editOriginal("Toggled channel mix.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "karaoke", description = "Karaoke audio filter.")
    public void karaoke(SlashCommandInteractionEvent event,
                        @Parameter(description = "Strength of the filter (in percent). Set to 0 to disable.", required = false) @Range(min = 0, max = 100) Double strength) {
        checkManager.djCheck(() -> {
            KaraokeConfig filter = filters.getKaraoke();
            
            if (filter.isEnabled() && strength == null) filter.reset();
            else filter.setLevel(nonNullOr(strength, 100).floatValue() / 100);
            updateFilters();
            event.getHook().editOriginal("Toggled karaoke.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "lowpass", description = "Low pass audio filter.")
    public void lowPass(SlashCommandInteractionEvent event) {
        checkManager.djCheck(() -> {
            LowPassConfig filter = filters.getLowPass();
            
            if (filter.isEnabled()) filter.reset();
            else filter.setSmoothing(20);
            
            updateFilters();
            event.getHook().editOriginal("Toggled low pass.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "rotation", description = "Rotation audio filter.")
    public void rotation(SlashCommandInteractionEvent event,
                         @Parameter(description = "Rotation frequency (in Hz). Set to 0 to disable.", required = false) @Range(min = 0, max = OptionData.MAX_POSITIVE_NUMBER) Double rotation) {
        checkManager.djCheck(() -> {
            RotationConfig filter = filters.getRotation();
            
            if (filter.isEnabled() && rotation == null) filter.reset();
            else filter.setRotation(nonNullOr(rotation, 5).floatValue());
            
            updateFilters();
            event.getHook().editOriginal("Toggled rotation.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "speed", description = "Playback speed.")
    public void speed(SlashCommandInteractionEvent event,
                      @Parameter(description = "The speed (in percent). Set to 100 to disable.", required = false) @Range(min = 25, max = 200) Double speed) {
        checkManager.djCheck(() -> {
            TimescaleConfig filter = filters.getTimescale();
    
            float s = nonNullOr(speed, 100).floatValue() / 100;
            
            if (filter.isEnabled() && speed == null) filter.reset();
            else filter.setSpeed(s).setPitch(s);
            
            updateFilters();
            event.getHook().editOriginal(String.format("Set speed to %s%%.", nonNullOr(speed, 100))).queue();
        }, event, false);
    }
    
    @SlashHandle(path = "tremolo", description = "Tremolo audio filter.")
    public void tremolo(SlashCommandInteractionEvent event,
                        @Parameter(description = "Tremolo frequency (in Hz). Set to 0 to disable.") @Range(min = 0, max = OptionData.MAX_POSITIVE_NUMBER) Double frequency) {
        checkManager.djCheck(() -> {
            TremoloConfig filter = filters.getTremolo();
            
            if (filter.isEnabled() && frequency == null) filter.reset();
            else filter.setFrequency(nonNullOr(frequency, 4).floatValue());
            
            updateFilters();
            event.getHook().editOriginal("Toggled tremolo.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "vibrato", description = "Vibrato audio filter.")
    public void vibrato(SlashCommandInteractionEvent event,
                        @Parameter(description = "Vibrato frequency (in Hz). Set to 0 to disable.") @Range(min = 0, max = 14) Double frequency) {
        checkManager.djCheck(() -> {
            VibratoConfig filter = filters.getVibrato();
            
            if (filter.isEnabled() && frequency == null) filter.reset();
            else filter.setFrequency(nonNullOr(frequency, 4).floatValue());
    
            updateFilters();
            event.getHook().editOriginal("Toggled vibrato.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "nightcore", description = "Nightcore audio filter.")
    public void nightcore(SlashCommandInteractionEvent event) {
        checkManager.djCheck(() -> {
            NightcoreConfig filter = filters.getNightcore();
            
            if (filter.isEnabled()) filter.reset();
            else filter.setAmount(1.25f);
            
            updateFilters();
            event.getHook().editOriginal("Toggled nightcore.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "vaporwave", description = "Vaporwave audio filter.")
    public void vaporwave(SlashCommandInteractionEvent event) {
        checkManager.djCheck(() -> {
            VaporwaveConfig filter = filters.getVaporwave();
            
            if (filter.isEnabled()) filter.reset();
            else filter.setValue(0.9f);
            
            updateFilters();
            event.getHook().editOriginal("Toggled vaporwave.").queue();
        }, event, false);
    }
    
    @SlashHandle(path = "equalizer/reset", description = "Resets the equalizer.", baseDescription = "Equalizer related commands.")
    public void eqReset(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            EqualizerConfig filter = filters.getEqualizer();
            filter.useGains(EqualizerConfig.Preset.OFF.getArray());
            
            updateFilters();
            event.reply("Reset the equalizer.").queue();
        }, event);
    }
    
    @SlashHandle(path = "equalizer/gui", description = "Control the equalizer with a GUI.")
    public void eqGui(SlashCommandInteractionEvent event) {
        checkManager.deferredCheck(() -> equalizerManager.updateMessage(event.getHook()), event, false);
    }
    
    @SlashHandle(path = "equalizer/preset", description = "Use an equalizer preset.")
    public void eqPreset(SlashCommandInteractionEvent event,
                         @Parameter(name = "preset", description = "A preset.") @Autocomplete String id,
                         @Parameter(description = "A gain offset.", required = false) @Range(min = -0.25, max = 1.0) Double offset) {
        checkManager.check(() -> {
            EqualizerConfig.Preset preset = EqualizerConfig.Preset.of(id);
            
            if (preset == EqualizerConfig.Preset.OFF) {
                equalizerManager.getEqualizer().useGains(preset.getArray());
                event.reply("Disabled the equalizer.").queue();
                equalizerManager.updateMessage();
                return;
            }
    
            float[] array = preset.getArray();
            if (offset != null)
                for (int i = 0; i < array.length; i++)
                    array[i] += offset;
            
            equalizerManager.getEqualizer().useGains(array);
            updateFilters();
            event.reply(String.format("Using equalizer preset **%s**.", preset.getName())).queue();
            equalizerManager.updateMessage();
        }, event);
    }
    
    @AutocompleteHandle("equalizer/preset/preset")
    public void eqPresetAutocomplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery query) {
        event.replyChoices(
                Arrays.stream(EqualizerConfig.Preset.values())
                        .limit(25)
                        .sorted(Comparator.comparingInt(it -> OtherUtils.levenshteinDistance(it.getName(), query.getValue())))
                        .map(it -> new Command.Choice(it.getName(), it.getName()))
                        .toList())
                .queue();
    }
    
    private String buildActiveFiltersMessage() {
        StringJoiner sj = new StringJoiner(", ", "**Active audio filters:**\n", "")
                .setEmptyValue("**Active audio filters:**\n_No filters._");
    
        for (AudioFilterConfig<?> config : filters.getFilters().values())
            if (config.isEnabled())
                sj.add(config.name());
        
        return sj.toString();
    }
    
    public void updateFilters() {
        kvintakord.getPlayer().setFilterFactory(filters.build());
    }
    
    private <T> T nonNullOr(T in, T other) {
        return in == null ? other : in;
    }
    
    public void reset() {
        filters.reset();
        updateFilters();
    }
    
    public void cleanup() {
        equalizerManager.destroyMessage();
        reset();
    }
    
    public ApplicationManager getManager() {
        return manager;
    }
    
    public Kvintakord getKvintakord() {
        return kvintakord;
    }
    
    public FilterChainBuilder getFilters() {
        return filters;
    }
    
    public static class Factory implements SlashInterfaceFactory<AudioFilterManager>,
            AutocompleteInterfaceFactory<AudioFilterManager>, ButtonInterfaceFactory<AudioFilterManager> {
    
        @Override
        public Class<AudioFilterManager> getControllerClass() {
            return AudioFilterManager.class;
        }
    
        @Override
        public AudioFilterManager create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new AudioFilterManager(manager, context.getController(Kvintakord.class));
        }
    
        @Override
        public List<BaseInterfaceFactory<? extends BaseInterface>> getSubInterfaces() {
            return List.of(new EqualizerManager.Factory());
        }
    }
}
