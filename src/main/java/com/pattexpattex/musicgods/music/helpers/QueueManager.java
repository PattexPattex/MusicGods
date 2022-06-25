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
import com.pattexpattex.musicgods.util.builders.QueueBoxBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Grouped(Kvintakord.GROUP_ID)
public class QueueManager implements ButtonInterface, Runnable {

    public static final int TRACK_LINE_SIZE = 32;
    public static final long TRACK_MOVE_MILLIS = 10000L;

    private final ApplicationManager manager;
    private final Kvintakord kvintakord;
    private final CheckManager checkManager;

    private final AtomicReference<InteractionHook> queueBox;
    private final AtomicInteger queueBoxPage;
    private final AtomicBoolean creatingQueueBox;

    private QueueManager(ApplicationManager manager, Kvintakord kvintakord) {
        this.manager = manager;
        this.kvintakord = kvintakord;
        this.checkManager = kvintakord.getCheckManager();

        this.queueBox = new AtomicReference<>();
        this.queueBoxPage = new AtomicInteger();
        this.creatingQueueBox = new AtomicBoolean();

        manager.getExecutorService().scheduleAtFixedRate(this, 3, 15, TimeUnit.SECONDS);
        kvintakord.subInterfaceLoaded(this);
    }

    @ButtonHandle(identifier = "kv:current.back", emoji = BotEmoji.REWIND)
    public void backButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().forCurrentTrack(track ->
                    track.setPosition(Math.max(0, track.getPosition()) - TRACK_MOVE_MILLIS));
    
            updateQueueMessage();
        }, String.format("Rewind the current track for %s seconds?", TRACK_MOVE_MILLIS), event, true);
    }

    @ButtonHandle(identifier = "kv:pause", emoji = BotEmoji.PLAY_PAUSE)
    public void pauseButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().pause();
            updateQueueMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:current.forward", emoji = BotEmoji.FAST_FORWARD)
    public void forwardButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().forCurrentTrack(track ->
                    track.setPosition(track.getPosition() + TRACK_MOVE_MILLIS));
    
            updateQueueMessage();
        }, String.format("Fast forward the current track for %s seconds?", TRACK_MOVE_MILLIS), event, true);
    }

    @ButtonHandle(identifier = "kv:skip", emoji = BotEmoji.NEXT_TRACK)
    public void skipButton(ButtonInteractionEvent event) {
        kvintakord.getCheckManager().fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().skipTrack();
            updateQueueMessage();
        }, "Skip the current track?", event, true);
    }

    @ButtonHandle(identifier = "kv:stop", emoji = BotEmoji.STOP_TRACK)
    public void stopButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().stop(true);
        }, "Stop playback?", event, true);
    }

    @ButtonHandle(identifier = "kv:loop", emoji = BotEmoji.REPEAT)
    public void loopButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().incrementLoop();
            updateQueueMessage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:shuffle", emoji = BotEmoji.ARROWS_TWISTED)
    public void shuffleButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().toggleShuffle();
            updateQueueMessage();
        }, "Toggle the shuffle mode?", event, true);
    }

    @ButtonHandle(identifier = "kv:clear", emoji = BotEmoji.WASTEBASKET)
    public void clearButton(ButtonInteractionEvent event) {
        checkManager.fairCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().emptyQueue();
            updateQueueMessage();
        }, "Clear the current queue?", event, true);
    }

    @ButtonHandle(identifier = "kv:lyrics", emoji = BotEmoji.SCROLL)
    public void lyricsButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setQueueBox(event.getHook());
            kvintakord.getScheduler().forCurrentTrack(track -> kvintakord.retrieveLyrics(event.getHook(), kvintakord.getLyricsHelper().buildSearchQuery(track)));
        }, event, false);
    }

    @ButtonHandle(identifier = "kv:page.prev", label = "Previous page",
            emoji = BotEmoji.ARROW_BACKWARD, style = ButtonStyle.PRIMARY)
    public void prevPageButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setQueueBox(event.getHook());
            previousQueueBoxPage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:page.next", label = "Next page",
            emoji = BotEmoji.ARROW_FORWARD, style = ButtonStyle.PRIMARY)
    public void nextPageButton(ButtonInteractionEvent event) {
        checkManager.deferredCheck(() -> {
            setQueueBox(event.getHook());
            nextQueueBoxPage();
        }, event, true);
    }

    @ButtonHandle(identifier = "kv:destroy", label = "End interaction", style = ButtonStyle.DANGER)
    public void destroyButton(ButtonInteractionEvent event) {
        setQueueBox(event.getHook());
        event.deferEdit().queue();
        destroyQueueMessage();
    }

    public void nextQueueBoxPage() {
        queueBoxPage.incrementAndGet();
        updateQueueMessage();
    }

    public void previousQueueBoxPage() {
        queueBoxPage.decrementAndGet();
        updateQueueMessage();
    }

    public void setQueueBoxPage(int newPage) {
        queueBoxPage.set(newPage);
        //updateQueueMessage();
    }

    public void setQueueBox(InteractionHook hook) {
        queueBox.compareAndSet(null, hook);
    }

    public void destroyQueueMessage() {
        InteractionHook hook = queueBox.getAndSet(null);
        queueBoxPage.set(0);

        if (hook != null)
            hook.retrieveOriginal().flatMap(Message::delete).queue();
    }

    public void updateQueueMessage() {
        updateQueueMessage(null);
    }

    public void updateQueueMessage(@Nullable InteractionHook hook) {
        AudioTrack track = kvintakord.getPlayer().getPlayingTrack();

        if (track == null)
            destroyQueueMessage();
        else {
            MusicScheduler scheduler = kvintakord.getScheduler();
            InteractionHook oldHook = queueBox.get();
            Message box = QueueBoxBuilder.build(track, scheduler.getQueue(), scheduler.getLoop(),
                    scheduler.getShuffle(), scheduler.isPaused(), scheduler.getVolume(), queueBoxPage.get(), manager);

            if (hook != null) {
                if (creatingQueueBox.compareAndSet(false, true)) {
                    hook.editOriginal(box).queue(created -> {
                        if (oldHook != null)
                            oldHook.deleteOriginal().queue();

                        queueBox.set(hook);
                        creatingQueueBox.set(false);
                    }, error ->
                            creatingQueueBox.set(false));
                }
            }
            else if (oldHook != null) {
                oldHook.editOriginal(box).queue(null, new ErrorHandler().ignore(ErrorResponse.INVALID_WEBHOOK_TOKEN));
            }
        }
    }

    public void cleanup() {
        destroyQueueMessage();
    }

    @Override
    public void run() {
        updateQueueMessage();
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
