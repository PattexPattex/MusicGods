package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.MusicScheduler;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.OtherUtils;
import com.pattexpattex.musicgods.util.TimeoutTimer;
import com.pattexpattex.musicgods.util.builders.QueueBoxBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Grouped(Kvintakord.GROUP_ID)
public class QueueManager implements ButtonInterface, Runnable {

    public static final int TRACK_LINE_SIZE = 32;
    public static final long TRACK_SEEK_MILLIS = 10000L;

    private final ApplicationManager manager;
    private final Kvintakord kvintakord;
    private final CheckManager checkManager;

    private final AtomicReference<InteractionHook> message = new AtomicReference<>();
    private final AtomicInteger messagePage = new AtomicInteger();
    private final AtomicBoolean creatingMessage = new AtomicBoolean();
    private final TimeoutTimer timer = new TimeoutTimer(5, TimeUnit.MINUTES, () -> OtherUtils.deleteHook(message.getAndSet(null)));

    private QueueManager(ApplicationManager manager, Kvintakord kvintakord) {
        this.manager = manager;
        this.kvintakord = kvintakord;
        this.checkManager = kvintakord.getCheckManager();

        manager.getExecutorService().scheduleAtFixedRate(this, 3, 15, TimeUnit.SECONDS);
        kvintakord.subInterfaceLoaded(this);
    }

    @ButtonHandle(identifier = "kv:current.back", emoji = BotEmoji.REWIND)
    public void backButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            kvintakord.getScheduler().forCurrentTrack(track ->
                    track.setPosition(Math.max(0, track.getPosition()) - TRACK_SEEK_MILLIS));
    
            setAndUpdate(event.getHook());
        }, String.format("Rewind the current track for %s seconds?", TRACK_SEEK_MILLIS), event, true);
    }

    @ButtonHandle(identifier = "kv:pause", emoji = BotEmoji.PLAY_PAUSE)
    public void pauseButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            kvintakord.getScheduler().pause();
            setAndUpdate(event.getHook());
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:current.forward", emoji = BotEmoji.FAST_FORWARD)
    public void forwardButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            kvintakord.getScheduler().forCurrentTrack(track ->
                    track.setPosition(track.getPosition() + TRACK_SEEK_MILLIS));
            
            setAndUpdate(event.getHook());
        }, String.format("Fast forward the current track for %s seconds?", TRACK_SEEK_MILLIS), event, true);
    }

    @ButtonHandle(identifier = "kv:skip", emoji = BotEmoji.NEXT_TRACK)
    public void skipButton(ButtonInteractionEvent event) {
        kvintakord.getCheckManager().fairCheck(() -> {
            kvintakord.getScheduler().skipTrack();
            setAndUpdate(event.getHook());
        }, "Skip the current track?", event, true);
    }

    @ButtonHandle(identifier = "kv:stop", emoji = BotEmoji.STOP_TRACK)
    public void stopButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setMessage(event.getHook());
            kvintakord.getScheduler().stop(true);
        }, "Stop playback?", event, true);
    }

    @ButtonHandle(identifier = "kv:loop", emoji = BotEmoji.REPEAT)
    public void loopButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            kvintakord.getScheduler().incrementLoop();
            setAndUpdate(event.getHook());
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:shuffle", emoji = BotEmoji.ARROWS_TWISTED)
    public void shuffleButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            kvintakord.getScheduler().toggleShuffle();
            setAndUpdate(event.getHook());
        }, "Toggle the shuffle mode?", event, true);
    }

    @ButtonHandle(identifier = "kv:clear", emoji = BotEmoji.WASTEBASKET)
    public void clearButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            kvintakord.getScheduler().emptyQueue();
            setAndUpdate(event.getHook());
        }, "Clear the current queue?", event, true);
    }

    @ButtonHandle(identifier = "kv:lyrics", emoji = BotEmoji.SCROLL)
    public void lyricsButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setMessage(event.getHook());
            kvintakord.getScheduler().forCurrentTrack(track -> kvintakord.retrieveLyrics(event.getHook(), kvintakord.getLyricsHelper().buildSearchQuery(track)));
        }, event, false);
    }

    @ButtonHandle(identifier = "kv:page.prev", label = "Previous page",
            emoji = BotEmoji.ARROW_BACKWARD, style = ButtonStyle.PRIMARY)
    public void prevPageButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setMessage(event.getHook());
            previousPage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:page.next", label = "Next page",
            emoji = BotEmoji.ARROW_FORWARD, style = ButtonStyle.PRIMARY)
    public void nextPageButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setMessage(event.getHook());
            nextPage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:destroy", label = "End interaction", style = ButtonStyle.DANGER)
    public void destroyButton(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        setMessage(event.getHook());
        destroyMessage();
    }

    public void nextPage() {
        messagePage.incrementAndGet();
        updateMessage();
    }

    public void previousPage() {
        messagePage.decrementAndGet();
        updateMessage();
    }

    public void setPage(int newPage) {
        messagePage.set(newPage);
        //updateQueueMessage();
    }

    public void setMessage(InteractionHook hook) {
        message.set(hook);
    }

    public void destroyMessage() {
        messagePage.set(0);
        OtherUtils.deleteHook(message.getAndSet(null));
    }
    
    public void setAndUpdate(InteractionHook hook) {
        setMessage(hook);
        updateMessage();
        timer.reset();
    }

    public void updateMessage() {
        updateMessage(null);
    }

    public void updateMessage(@Nullable InteractionHook hook) {
        AudioTrack track = kvintakord.getPlayer().getPlayingTrack();

        if (track == null) {
            destroyMessage();
        }
        else {
            MusicScheduler scheduler = kvintakord.getScheduler();
            InteractionHook old = message.get();
            Message box = QueueBoxBuilder.build(track, scheduler.getQueue(), scheduler.getLoop(),
                    scheduler.getShuffle(), scheduler.isPaused(), scheduler.getVolume(), messagePage.get(), manager);

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

    public void cleanup() {
        destroyMessage();
    }

    @Override
    public void run() {
        updateMessage();
    }

    public static class Factory implements ButtonInterfaceFactory<QueueManager> {

        @Override
        public Class<QueueManager> getControllerClass() {
            return QueueManager.class;
        }

        @Override
        public QueueManager create(ApplicationManager manager, GuildContext context, Guild guild) {
            return new QueueManager(manager, context.getController(Kvintakord.class));
        }
    }
}
