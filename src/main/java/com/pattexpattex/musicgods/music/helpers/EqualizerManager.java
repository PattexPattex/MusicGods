package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.Bot;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.Autocomplete;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.AutocompleteHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterface;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.util.builders.EqualizerGuiBuilder;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Grouped(Kvintakord.GROUP_ID)
public class EqualizerManager implements SlashInterface, ButtonInterface, AutocompleteInterface {

    public static final String[] GAIN_ASSIGNMENTS_FREQ = { "25", "40", "63", "100", "160",
            "250", "400", "630", "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k" };

    public static final float[] GAIN_STEPS = getGainSteps();
    public static final int[] GAIN_ASSIGNMENTS = { 1, 4, 7, 10, 13 };
    public static final float GAIN_MIN = -0.25f;
    public static final float GAIN_MAX = 1.0f;
    public static final float GAIN_STEP = 0.125f;

    private static final String SUBMIT_URL = Bot.GITHUB + "/issues/new?template=new-equalizer-preset.md";

    private final Kvintakord kvintakord;
    private final CheckManager checkManager;
    private final AudioPlayer player;
    private final EqualizerFactory equalizer;
    private final AtomicReference<InteractionHook> eqBox;
    private final AtomicBoolean eqEnabled;
    private final AtomicBoolean creatingEqBox;

    public EqualizerManager(EqualizerFactory equalizer, Kvintakord kvintakord) {
        this.player = kvintakord.getPlayer();
        this.checkManager = kvintakord.getCheckManager();
        this.kvintakord = kvintakord;
        this.equalizer = equalizer;
        this.eqBox = new AtomicReference<>();
        this.eqEnabled = new AtomicBoolean();
        this.creatingEqBox = new AtomicBoolean();

        kvintakord.subInterfaceLoaded(this);
    }


    /* ---- Commands ---- */
    
    @SlashHandle(path = "eq/enable", description = "Enables the equalizer.", baseDescription = "Equalizer related commands.")
    public void eqEnable(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            if (enableEqualizer())
                event.reply("Enabled the equalizer.").queue();
            else
                event.reply("Equalizer is already enabled.").queue();
    
            updateEqualizerMessage();
        }, event);
    }

    @SlashHandle(path = "eq/disable", description = "Disables the equalizer.")
    public void eqDisable(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            if (disableEqualizer())
                event.reply("Disabled the equalizer.").queue();
            else
                event.reply("Equalizer is already disabled.").queue();
    
            updateEqualizerMessage();
        }, event);
    }

    @SlashHandle(path = "eq/reset", description = "Resets the equalizer to its default.")
    public void eqReset(SlashCommandInteractionEvent event) {
        checkManager.check(() -> {
            resetEqualizer();
            event.reply("Reset the equalizer.").queue();
            updateEqualizerMessage();
        }, event);
    }

    @SlashHandle(path = "eq/gui", description = "Control the equalizer with a GUI.")
    public void eqGui(SlashCommandInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            enableEqualizer();
            updateEqualizerMessage(event.getHook());
        }, event, false);
    }
    
    @SlashHandle(path = "eq/preset", description = "Uses a equalizer preset.")
    public void eqPreset(SlashCommandInteractionEvent event,
                                @Parameter(name = "preset", description = "A equalizer preset.") @Autocomplete String id,
                                @Parameter(description = "A offset for the equalizer, positive or negative.", required = false) Double offset) {
        checkManager.check(() -> {
            GainPreset preset = GainPreset.getByName(id);
            
            if (preset == GainPreset.OFF) {
                resetEqualizer();
                event.reply("Reset the equalizer.").queue();
            }
            else if (offset == null) {
                useGains(preset.getArray(), 0.0f);
                event.reply(String.format("Using equalizer preset **%s**.", preset.getName())).queue();
            }
            else {
                useGains(preset.getArray(), offset.floatValue());
                event.reply(String.format("Using equalizer preset **%s** with an offset of **%s**.", preset.getName(), offset)).queue();
            }
            
            updateEqualizerMessage();
        }, event);
    }
    
    @AutocompleteHandle("eq/preset/preset")
    public void eqPresetAutocomplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery query) {
        Command.Choice[] choices = Arrays.stream(GainPreset.values())
                .limit(25)
                .sorted(Comparator.comparingInt(preset -> OtherUtils.levenshteinDistance(preset.getName(), query.getValue())))
                .map(preset -> new Command.Choice(preset.getName(), preset.getName()))
                .toArray(Command.Choice[]::new);
        
        event.replyChoices(choices).queue();
    }


    /* ---- Buttons ---- */

    @ButtonHandle(identifier = "kv.equalizer:gui", emoji = BotEmoji.SLIDER)
    public void equalizerButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            enableEqualizer();
            updateEqualizerMessage();
        }, event, false);
    }

    @ButtonHandle(identifier = "kv.equalizer:up.0", emoji = BotEmoji.ARROW_UP_S)
    public void button00(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            incrementGain(GAIN_ASSIGNMENTS[0]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:up.1", emoji = BotEmoji.ARROW_UP_S)
    public void button01(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            incrementGain(GAIN_ASSIGNMENTS[1]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:up.2", emoji = BotEmoji.ARROW_UP_S)
    public void button02(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            incrementGain(GAIN_ASSIGNMENTS[2]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:up.3", emoji = BotEmoji.ARROW_UP_S)
    public void button03(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            incrementGain(GAIN_ASSIGNMENTS[3]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:up.4", emoji = BotEmoji.ARROW_UP_S)
    public void button04(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            incrementGain(GAIN_ASSIGNMENTS[4]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:down.0", emoji = BotEmoji.ARROW_DOWN_S)
    public void button10(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            decrementGain(GAIN_ASSIGNMENTS[0]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:down.1", emoji = BotEmoji.ARROW_DOWN_S)
    public void button11(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            decrementGain(GAIN_ASSIGNMENTS[1]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:down.2", emoji = BotEmoji.ARROW_DOWN_S)
    public void button12(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            decrementGain(GAIN_ASSIGNMENTS[2]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:down.3", emoji = BotEmoji.ARROW_DOWN_S)
    public void button13(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            decrementGain(GAIN_ASSIGNMENTS[3]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:down.4", emoji = BotEmoji.ARROW_DOWN_S)
    public void button14(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            decrementGain(GAIN_ASSIGNMENTS[4]);
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:reset", emoji = BotEmoji.ARROWS_ANTICLOCKWISE)
    public void resetButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            eqBox.set(event.getHook());
            resetEqualizer();
            updateEqualizerMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv.equalizer:destroy", label = "End interaction", style = ButtonStyle.DANGER)
    public void destroyButton(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        destroyEqualizerMessage();
    }

    @ButtonHandle(identifier = "kv.equalizer:export", label = "Export this configuration", style = ButtonStyle.SUCCESS)
    public void getAccurateButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            String arr = Arrays.toString(getCurrentGains());
            event.getHook().editOriginal(String.format("Exported equalizer configuration: \n`%s`", arr))
                    .setActionRow(Button.url("Submit to developer", SUBMIT_URL)).queue();
        }, event, false);
    }

    public boolean enableEqualizer() {
        if (eqEnabled.compareAndSet(false, true)) {
            player.setFilterFactory(equalizer);
            return true;
        }

        return false;
    }

    public boolean disableEqualizer() {
        if (eqEnabled.compareAndSet(true, false)) {
            player.setFilterFactory(null);
            resetEqualizer();
            return true;
        }

        return false;
    }

    public void resetEqualizer() {
        for (int i = 0; i < GainPreset.OFF.getArray().length; i++)
            equalizer.setGain(i, 0.0f);
    }

    public boolean isEqualizerEnabled() {
        return eqEnabled.get();
    }

    public float[] getCurrentGains() {
        float[] arr = new float[15];

        for (int i = 0; i < arr.length; i++)
            arr[i] = equalizer.getGain(i);

        return arr;
    }

    public void setGain(int band, float value) {
        equalizer.setGain(band, value);
    }

    public void incrementGain(int band) {
        float prev = equalizer.getGain(band);

        float[] arr = calculateAdjacentGains(band, roundToStep(prev) + GAIN_STEP);
        setAdjacentGains(arr, band);
    }

    public void decrementGain(int band) {
        float prev = equalizer.getGain(band);

        float[] arr = calculateAdjacentGains(band, roundToStep(prev) - GAIN_STEP);
        setAdjacentGains(arr, band);
    }

    private void setAdjacentGains(float[] gains, int band) {
        equalizer.setGain(band - 2, gains[0]);
        equalizer.setGain(band - 1, gains[1]);
        equalizer.setGain(band, gains[2]);
        equalizer.setGain(band + 1, gains[3]);
        equalizer.setGain(band + 2, gains[4]);
    }

    private float[] calculateAdjacentGains(int band, float gain) {
        float[] arr = new float[5];

        float prev = (band < 3 ? gain : equalizer.getGain(band - 3));
        float prevDiff = gain - prev;
        float prevStep = prevDiff / 3;
        float next = (band > 12 ? gain : equalizer.getGain(band + 3));
        float nextDiff = gain - next;
        float nextStep = nextDiff / 3;

        arr[0] = gain - (prevStep * 2);
        arr[1] = gain - prevStep;
        arr[2] = gain;
        arr[3] = gain - nextStep;
        arr[4] = gain - (nextStep * 2);

        return arr;
    }

    public void useGains(float[] gains, float offset) {
        enableEqualizer();

        for (int i = 0; i < gains.length; i++) {
            setGain(i, gains[i] + offset);
        }
    }

    public void destroyEqualizerMessage() {
        InteractionHook hook = eqBox.getAndSet(null);

        if (hook != null)
            hook.deleteOriginal().queue();
    }

    public void updateEqualizerMessage() {
        updateEqualizerMessage(null);
    }

    public void updateEqualizerMessage(@Nullable InteractionHook hook) {
        AudioTrack track = player.getPlayingTrack();

        if (track == null)
            destroyEqualizerMessage();
        else {
            InteractionHook oldHook = eqBox.get();
            Message box = EqualizerGuiBuilder.build(equalizer, kvintakord.getApplicationManager());

            if (hook != null) {
                if (creatingEqBox.compareAndSet(false, true)) {
                    hook.editOriginal(box).queue(created -> {
                        if (oldHook != null)
                            oldHook.deleteOriginal().queue();

                        eqBox.set(hook);
                        creatingEqBox.set(false);
                    }, error ->
                            creatingEqBox.set(false));
                }
            }
            else if (oldHook != null)
                oldHook.editOriginal(box).queue();
        }
    }

    public void cleanup() {
        disableEqualizer();
        destroyEqualizerMessage();
    }


    /* ---- Static methods ---- */

    public static float[] getGainSteps() {
        float total = -GAIN_MIN + GAIN_MAX;
        float[] arr = new float[(int) (total / GAIN_STEP) + 1];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = GAIN_MIN + GAIN_STEP * i;
        }

        return arr;
    }

    public static float roundToStep(float value) {
        return GAIN_STEP * Math.round(value / GAIN_STEP);
    }
    
    public enum GainPreset {
        
        BASS_BOOST("bass-boost", new float[]{ 0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f }),
        DEFAULT("home-stereo", new float[]{ 0.12f, 0.1f, 0.06666667f, 0.033333335f, 0.0f, -0.041666668f, -0.083333336f, -0.125f, -0.06666666f, -0.008333333f, 0.05f, 0.11666666f, 0.18333334f, 0.25f, 0.25f }),
        OFF("none", new float[]{ 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f })
        ;
        
        private final String name;
        private final float[] arr;
        
        GainPreset(String name, float[] arr) {
            this.name = name;
            this.arr = arr;
        }
        
        public float[] getArray() {
            return arr;
        }
        
        public String getName() {
            return name;
        }
        
        public static GainPreset getByName(String name) {
            for (GainPreset preset : values())
                if (preset.getName().equals(name))
                    return preset;
            
            return OFF;
        }
    }
    
    public static class Factory implements SlashInterfaceFactory<EqualizerManager>,
            ButtonInterfaceFactory<EqualizerManager>, AutocompleteInterfaceFactory<EqualizerManager> {
        
        @Override
        public Class<EqualizerManager> getControllerClass() {
            return EqualizerManager.class;
        }
        
        @Override
        public EqualizerManager create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new EqualizerManager(new EqualizerFactory(), context.getController(Kvintakord.class));
        }
    }
}
