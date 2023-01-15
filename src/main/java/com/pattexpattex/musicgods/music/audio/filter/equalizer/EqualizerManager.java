package com.pattexpattex.musicgods.music.audio.filter.equalizer;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.Launcher;
import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.music.AudioFilterManager;
import com.pattexpattex.musicgods.music.helpers.CheckManager;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.util.TimeoutTimer;
import com.pattexpattex.musicgods.util.builders.EqualizerGuiBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class EqualizerManager implements ButtonInterface {
    
    private static final String PRESET_SUBMIT_URL = Launcher.github + "/issues/new?template=new-equalizer-preset.md";
    
    public static final String[] GAIN_FREQUENCIES = { "25", "40", "63", "100", "160", "250", "400", "630", "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k" };
    public static final int[] GUI_GAINS = { 1, 4, 7, 10, 13 };
    
    public static final float GAIN_STEP = 0.125f;
    public static final float GAIN_MIN = -0.25f;
    public static final float GAIN_MAX = 1.0f;
    public static final float[] GAIN_STEPS;
    
    static {
        float diff = GAIN_MAX - GAIN_MIN;
        GAIN_STEPS = new float[(int) (diff / GAIN_STEP) + 1];
    
        for (int i = 0; i < GAIN_STEPS.length; i++)
            GAIN_STEPS[i] = GAIN_MIN + GAIN_STEP * i;
    }
    
    private final CheckManager checkManager;
    private final AudioFilterManager manager;
    private final EqualizerConfig equalizer;
    private final AtomicReference<InteractionHook> message = new AtomicReference<>();
    private final AtomicBoolean creatingMessage = new AtomicBoolean();
    private final TimeoutTimer timer = new TimeoutTimer(5, TimeUnit.MINUTES, () -> OtherUtils.deleteHook(message.getAndSet(null)));
    
    public EqualizerManager(AudioFilterManager manager) {
        this.manager = manager;
        this.checkManager = manager.getKvintakord().getCheckManager();
        this.equalizer = manager.getFilters().getEqualizer();
        
        manager.subInterfaceLoaded(this);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:gui", emoji = BotEmoji.SLIDER)
    public void eqGuiButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> updateMessage(event.getHook()), event, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:up.0", emoji = BotEmoji.ARROW_UP_S)
    public void eqButton00(ButtonInteractionEvent event) {
        equalizerModification(event, 0, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:up.1", emoji = BotEmoji.ARROW_UP_S)
    public void eqButton01(ButtonInteractionEvent event) {
        equalizerModification(event, 1, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:up.2", emoji = BotEmoji.ARROW_UP_S)
    public void eqButton02(ButtonInteractionEvent event) {
        equalizerModification(event, 2, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:up.3", emoji = BotEmoji.ARROW_UP_S)
    public void eqButton03(ButtonInteractionEvent event) {
        equalizerModification(event, 3, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:up.4", emoji = BotEmoji.ARROW_UP_S)
    public void eqButton04(ButtonInteractionEvent event) {
        equalizerModification(event, 4, false);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:down.0", emoji = BotEmoji.ARROW_DOWN_S)
    public void eqButton10(ButtonInteractionEvent event) {
        equalizerModification(event, 0, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:down.1", emoji = BotEmoji.ARROW_DOWN_S)
    public void eqButton11(ButtonInteractionEvent event) {
        equalizerModification(event, 1, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:down.2", emoji = BotEmoji.ARROW_DOWN_S)
    public void eqButton12(ButtonInteractionEvent event) {
        equalizerModification(event, 2, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:down.3", emoji = BotEmoji.ARROW_DOWN_S)
    public void eqButton13(ButtonInteractionEvent event) {
        equalizerModification(event, 3, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:down.4", emoji = BotEmoji.ARROW_DOWN_S)
    public void eqButton14(ButtonInteractionEvent event) {
        equalizerModification(event, 4, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:reset", emoji = BotEmoji.ARROWS_ANTICLOCKWISE)
    public void eqResetButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            equalizer.reset();
            setAndUpdate(event.getHook());
            manager.updateFilters();
        }, event, true);
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:destroy", label = "End interaction", style = ButtonStyle.DANGER)
    public void eqDestroyButton(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        setMessage(event.getHook());
        destroyMessage();
    }
    
    @ButtonHandle(identifier = "kv:filters.equalizer:export", label = "Export configuration", style = ButtonStyle.SUCCESS)
    public void eqExportButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            String arr = Arrays.toString(equalizer.getGains());
            event.getHook().editOriginal(String.format("**Exported equalizer configuration:**\n`%s`", arr))
                    .setActionRow(Button.url("Submit to developer", PRESET_SUBMIT_URL)).queue();
        }, event, false);
    }
    
    private void equalizerModification(ButtonInteractionEvent event, int band, boolean decrement) {
        checkManager.deferredCheck(( ) -> {
            if (decrement) decrement(GUI_GAINS[band], true);
            else increment(GUI_GAINS[band], true);
            setAndUpdate(event.getHook());
            manager.updateFilters();
        }, event, true);
    }
    
    public void destroyMessage() {
        OtherUtils.deleteHook(message.getAndSet(null));
    }
    
    public void setMessage(InteractionHook hook) {
        message.set(hook);
    }
    
    public void setAndUpdate(InteractionHook hook) {
        setMessage(hook);
        updateMessage();
        timer.reset();
    }
    
    public void updateMessage() {
        updateMessage(null);
    }
    
    public void updateMessage(InteractionHook hook) {
        AudioTrack track = manager.getKvintakord().getPlayer().getPlayingTrack();
        
        if (track == null) {
            destroyMessage();
        }
        else {
            InteractionHook old = message.get();
            MessageEditData box = EqualizerGuiBuilder.build(equalizer, manager.getManager());
            
            if (hook != null) {
                if (creatingMessage.compareAndSet(false, true)) {
                    hook.editOriginal(box).queue(msg -> {
                        OtherUtils.deleteHook(old);
                        setMessage(hook);
                    });
                    
                    creatingMessage.set(false);
                }
            }
            else if (old != null) {
                if (!old.isExpired()) old.editOriginal(box).queue();
            }
        }
    }
    
    public void increment(int band, boolean smooth) {
        float gain = round(equalizer.getGain(band)) + GAIN_STEP;
        
        if (smooth) useAdjacentGains(band, adjacentGains(band, gain));
        else equalizer.setGain(band, gain);
    }
    
    public void decrement(int band, boolean smooth) {
        float gain = round(equalizer.getGain(band)) - GAIN_STEP;
        
        if (smooth) useAdjacentGains(band, adjacentGains(band, gain));
        else equalizer.setGain(band, gain);
    }
    
    public AudioFilterManager getManager() {
        return manager;
    }
    
    public EqualizerConfig getEqualizer() {
        return equalizer;
    }
    
    private void useAdjacentGains(int center, float[] gains) {
        for (int i = 0; i < 5; i++)
            equalizer.setGain(center + i - 2, gains[i]);
    }
    
    private float[] adjacentGains(int center, float gain) {
        float[] arr = new float[5];
    
        float prev = (gain - (center < 3 ? gain : equalizer.getGain(center - 3))) / 3;
        float next = (gain - (center > 12 ? gain : equalizer.getGain(center + 3))) / 3;
    
        arr[0] = gain - (prev * 2);
        arr[1] = gain - prev;
        arr[2] = gain;
        arr[3] = gain - next;
        arr[4] = gain - (next * 2);
        
        return arr;
    }
    
    private float round(float gain) {
        return GAIN_STEP * Math.round(gain / GAIN_STEP);
    }
    
    public static class Factory implements ButtonInterfaceFactory<EqualizerManager> {
    
        @Override
        public Class<EqualizerManager> getControllerClass() {
            return EqualizerManager.class;
        }
    
        @Override
        public EqualizerManager create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new EqualizerManager(context.getController(AudioFilterManager.class));
        }
    }
}
