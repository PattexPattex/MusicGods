package com.pattexpattex.musicgods.music.helpers;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.ButtonHandle;
import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterfaceFactory;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterfaceFactory;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.music.audio.MusicScheduler;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.builders.QueueBoxBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;

import javax.annotation.Nullable;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Grouped(Kvintakord.GROUP_ID)
public class QueueManager implements SlashInterface, ButtonInterface, Runnable {

    public static final int TRACK_LINE_SIZE = 32;
    public static final long TRACK_MOVE_MILLIS = 10000L;

    private final ApplicationManager manager;
    private final Kvintakord kvintakord;

    private final AtomicReference<InteractionHook> queueBox;
    private final AtomicInteger queueBoxPage;
    private final AtomicBoolean creatingQueueBox;

    private QueueManager(ApplicationManager manager, Kvintakord kvintakord) {
        this.manager = manager;
        this.kvintakord = kvintakord;

        this.queueBox = new AtomicReference<>();
        this.queueBoxPage = new AtomicInteger();
        this.creatingQueueBox = new AtomicBoolean();

        manager.getExecutorService().scheduleAtFixedRate(this, 3, 15, TimeUnit.SECONDS);
        kvintakord.subInterfaceLoaded(this);
    }

    @ButtonHandle(identifier = "kv:current.back", emoji = BotEmoji.REWIND)
    public void backButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;

        kvintakord.getScheduler().forCurrentTrack(track ->
                track.setPosition(Math.max(0, track.getPosition()) - TRACK_MOVE_MILLIS));

        setQueueBox(event.getHook());
        updateQueueMessage();
    }

    @ButtonHandle(identifier = "kv:pause", emoji = BotEmoji.PLAY_PAUSE)
    public void pauseButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().pause();
    }

    @ButtonHandle(identifier = "kv:current.forward", emoji = BotEmoji.FAST_FORWARD)
    public void forwardButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;

        kvintakord.getScheduler().forCurrentTrack(track ->
                track.setPosition(track.getPosition() + TRACK_MOVE_MILLIS));

        setQueueBox(event.getHook());
        updateQueueMessage();
    }

    @ButtonHandle(identifier = "kv:skip", emoji = BotEmoji.NEXT_TRACK)
    public void skipButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().skipTrack();
    }

    @ButtonHandle(identifier = "kv:stop", emoji = BotEmoji.STOP_TRACK)
    public void stopButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().stop(true);
    }

    @ButtonHandle(identifier = "kv:loop", emoji = BotEmoji.REPEAT)
    public void loopButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().incrementLoop();
    }

    @ButtonHandle(identifier = "kv:shuffle", emoji = BotEmoji.ARROWS_TWISTED)
    public void shuffleButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().toggleShuffle();
    }

    @ButtonHandle(identifier = "kv:clear", emoji = BotEmoji.WASTEBASKET)
    public void clearButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        kvintakord.getScheduler().emptyQueue();
        updateQueueMessage();
    }

    @ButtonHandle(identifier = "kv:lyrics", emoji = BotEmoji.SCROLL)
    public void lyricsButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().checkAndReply(event, CheckManager.Check.PLAYING)) return;

        setQueueBox(event.getHook());

        LyricsHelper helper = kvintakord.getLyricsHelper();

        kvintakord.getScheduler().forCurrentTrack(track ->
                helper.getLyricsAsync(helper.buildSearchQuery(track))
                        .thenAccept(lyrics -> {
                            Queue<Message> messages = helper.buildLyricsMessage(lyrics);

                            event.getHook().editOriginal(messages.remove()).queue(message -> {
                                for (Message msg : messages)
                                    message.getChannel().sendMessage(msg).queue();
                            });
                        })
                        .exceptionally(th -> {
                            if (th instanceof TimeoutException te)
                                event.getHook().editOriginal("Lyrics search timed out.").queue();
                            else
                                event.getHook().editOriginal("Something went wrong.").queue();
                            return null;
                        })
        );
    }

    @ButtonHandle(identifier = "kv:page.prev", label = "Previous page",
            emoji = BotEmoji.ARROW_BACKWARD, style = ButtonStyle.PRIMARY)
    public void prevPageButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        previousQueueBoxPage();
    }

    @ButtonHandle(identifier = "kv:page.next", label = "Next page",
            emoji = BotEmoji.ARROW_FORWARD, style = ButtonStyle.PRIMARY)
    public void nextPageButton(ButtonInteractionEvent event) {
        if (kvintakord.getCheckManager().check(event)) return;
        setQueueBox(event.getHook());
        nextQueueBoxPage();
    }

    @ButtonHandle(identifier = "kv:destroy", label = "End interaction", style = ButtonStyle.DANGER)
    public void destroyButton(ButtonInteractionEvent event) {
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
        updateQueueMessage();
    }

    public void setQueueBox(InteractionHook hook) {
        queueBox.compareAndSet(null, hook);
    }

    public void destroyQueueMessage() {
        InteractionHook hook = queueBox.getAndSet(null);
        queueBoxPage.set(0);

        if (hook != null)
            hook.deleteOriginal().queue();
    }

    public void updateQueueMessage() {
        updateQueueMessage(null);
    }

    public void updateQueueMessage(@Nullable SlashCommandInteractionEvent event) {
        AudioTrack track = kvintakord.getPlayer().getPlayingTrack();

        if (track == null)
            destroyQueueMessage();
        else {
            MusicScheduler scheduler = kvintakord.getScheduler();
            InteractionHook hook = queueBox.get();
            Message box = QueueBoxBuilder.build(track, scheduler.getQueue(), scheduler.getLoop(),
                    scheduler.getShuffle(), scheduler.isPaused(), scheduler.getVolume(), queueBoxPage.get(), manager);

            if (event != null) {
                if (creatingQueueBox.compareAndSet(false, true)) {
                    event.reply(box).queue(created -> {
                        if (hook != null)
                            hook.deleteOriginal().queue();

                        queueBox.set(created);
                        creatingQueueBox.set(false);
                    }, error ->
                            creatingQueueBox.set(false));
                }
            }
            else if (hook != null) {
                hook.editOriginal(box).queue(null, new ErrorHandler().ignore(ErrorResponse.INVALID_WEBHOOK_TOKEN));
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

    public static class Factory implements SlashInterfaceFactory<QueueManager>,
            ButtonInterfaceFactory<QueueManager> {

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
