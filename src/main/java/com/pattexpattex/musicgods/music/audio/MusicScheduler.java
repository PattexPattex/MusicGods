package com.pattexpattex.musicgods.music.audio;

import com.pattexpattex.musicgods.config.storage.GuildConfig;
import com.pattexpattex.musicgods.music.Kvintakord;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.pattexpattex.musicgods.util.dispatchers.MessageDispatcher;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MusicScheduler extends AudioEventAdapter {

    private static final Logger log = LoggerFactory.getLogger(MusicScheduler.class);

    private final AudioPlayer player;
    private final MessageDispatcher messageDispatcher;
    private final GuildConfig config;
    private final Kvintakord kvintakord;
    private final Object lock;
    private final BlockingDeque<AudioTrack> queue;
    private final AtomicReference<LoopMode> loopMode;
    private final AtomicReference<ShuffleMode> shuffleMode;
    private final AtomicBoolean retried;

    public MusicScheduler(AudioPlayer player, MessageDispatcher messageDispatcher,
                          GuildConfig config, Kvintakord kvintakord) {
        this.player = player;
        this.messageDispatcher = messageDispatcher;
        this.config = config;
        this.kvintakord = kvintakord;
        lock = new Object();
        queue = new LinkedBlockingDeque<>();
        loopMode = new AtomicReference<>(config.getLoop());
        shuffleMode = new AtomicReference<>(config.getShuffle());
        retried = new AtomicBoolean();

        this.player.addListener(this);
        this.player.setVolume(config.getVol());
        this.player.setPaused(false);
    }

    public void addToQueue(AudioTrack track) {
        queue.addLast(track);
        startNext(true);
    }

    public void addToQueueFirst(AudioTrack track) {
        queue.addFirst(track);
        startNext(true);
    }

    public void addToQueue(Collection<AudioTrack> tracks) {
        queue.addAll(tracks);
        startNext(true);
    }

    public void stop(boolean withMessage) {
        player.stopTrack();
        player.setPaused(false);
        retried.set(false);
        emptyQueue();
        kvintakord.disconnectFromVoiceChannel();
        kvintakord.updateQueueMessage();

        if (withMessage)
            messageDispatcher.sendMessage("Playback finished.");
    }

    public void setVolume(int volume) {
        player.setVolume(volume);
        config.setVol(volume);
    }

    public int getVolume() {
        return player.getVolume();
    }

    public void setLoop(LoopMode loop) {
        loopMode.set(loop);
        config.setLoop(loop);
        kvintakord.updateQueueMessage();
    }

    public LoopMode getLoop() {
        return loopMode.get();
    }

    public void incrementLoop() {
        switch (getLoop()) {
            case OFF -> setLoop(LoopMode.ALL);
            case ALL -> setLoop(LoopMode.SINGLE);
            case SINGLE -> setLoop(LoopMode.OFF);
            default -> throw new IllegalArgumentException("Unknown LoopMode " + loopMode);
        }
    }

    public void setShuffle(ShuffleMode shuffle) {
        shuffleMode.set(shuffle);
        config.setShuffle(shuffle);
        kvintakord.updateQueueMessage();
    }

    public ShuffleMode getShuffle() {
        return shuffleMode.get();
    }

    public void toggleShuffle() {
        setShuffle(ShuffleMode.fromBoolean(!shuffleMode.get().isEnabled()));
    }

    public boolean pause() {
        player.setPaused(!isPaused());
        return isPaused();
    }

    public boolean isPaused() {
        return player.isPaused();
    }

    public void skipTrack() {
        if (loopMode.get() == LoopMode.ALL)
            forCurrentTrack(track -> queue.addLast(track.makeClone()));

        startNext(false);
    }

    /**
     * @return {@code true} if the position is valid
     */
    public boolean skipTrack(int position) {
        if (position < 1) {
            skipTrack();
            return true;
        }

        if (position > queue.size()) return false;

        synchronized (lock) {
            if (getLoop() == LoopMode.ALL) {
                forCurrentTrack(track -> queue.addLast(track.makeClone()));
                queue.addAll(drainQueue(position));
            }
            if (getLoop() != LoopMode.ALL) {
                drainQueue(position);
            }
        }

        startNext(false);
        return true;
    }

    /**
     * @return 0 if parameters are valid,
     * 1 if the original position is invalid,
     * 2 if the new position is invalid
     */
    public int moveTrack(int position, int newPosition) {
        if (position < 0 || position >= queue.size()) return 1;
        if (newPosition < 0 || newPosition > queue.size()) return 2;

        List<AudioTrack> tracks = new ArrayList<>(queue);
        AudioTrack track = tracks.remove(position);
        tracks.add(newPosition, track);

        synchronized (lock) {
            queue.clear();
            queue.addAll(tracks);
        }

        kvintakord.updateQueueMessage();
        return 0;
    }

    public boolean removeTrack(int position) {
        if (position < 0 || position >= queue.size()) return false;
        boolean done = queue.remove(getQueue().get(position));

        if (done) kvintakord.updateQueueMessage();
        return done;
    }

    /**
     * @return An immutable list of {@link AudioTrack AudioTracks}.
     */
    public List<AudioTrack> getQueue() {
        return queue.stream().toList();
    }

    public void emptyQueue() {
        queue.clear();
    }

    public List<AudioTrack> drainQueue() {
        List<AudioTrack> list = new ArrayList<>();
        queue.drainTo(list);
        return list;
    }

    public List<AudioTrack> drainQueue(int amount) {
        amount = Math.min(amount, queue.size());
        List<AudioTrack> list = new ArrayList<>();
        queue.drainTo(list, amount);
        return list;
    }

    public void forCurrentTrack(TrackOperation operation) {
        AudioTrack track = player.getPlayingTrack();

        if (track != null)
            operation.execute(track);
    }

    public void forCurrentTrackNullable(TrackOperation operation) {
        operation.execute(player.getPlayingTrack());
    }

    public void forTrackAt(TrackOperation operation, int position) {
        if (position < 0 || position >= queue.size()) return;
        AudioTrack track = getQueue().get(position);

        if (track != null)
            operation.execute(track);
    }

    private void startNext(boolean noInterrupt) {
        if (shuffleMode.get().isEnabled() && queue.size() > 1) {
            Random rand = new Random();
            List<AudioTrack> list = new ArrayList<>(queue);
            AudioTrack temp = list.remove(list.size() - 1);
            AudioTrack next = list.get(rand.nextInt(list.size()));

            list.add(list.size(), temp);
            queue.remove(next);

            if (!player.startTrack(next, noInterrupt)) {
                synchronized (lock) {
                    queue.clear();
                    queue.addAll(list);
                }

                kvintakord.updateQueueMessage();
            }
        }
        else {
            AudioTrack next = queue.pollFirst();

            if (next != null) {
                if (!player.startTrack(next, noInterrupt)) {
                    queue.addFirst(next);
                    kvintakord.updateQueueMessage();
                }
            }
            else
                stop(true);
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        kvintakord.updateQueueMessage();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        kvintakord.updateQueueMessage();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (!retried.get())
            messageDispatcher.sendMessage(String.format("Started playing **%s** (`%s`).", TrackMetadata.getName(track),
                    FormatUtils.formatTimeFromMillis(track.getDuration())));

        kvintakord.updateQueueMessage();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.LOAD_FAILED && retried.compareAndSet(false, true)) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignored) {}

            addToQueueFirst(track.makeClone());
            return;
        }
        else
            retried.set(false);

        if (endReason.mayStartNext) {
            if (loopMode.get() == LoopMode.SINGLE)
                addToQueueFirst(track.makeClone());
            else if (loopMode.get() == LoopMode.ALL)
                addToQueue(track.makeClone());
            else
                startNext(true);
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        log.warn("Track {} got stuck for >{}ms", TrackMetadata.getUri(track), thresholdMs);
        messageDispatcher.sendMessage(String.format("Track **%s** got stuck, skipping.", TrackMetadata.getName(track)));
        startNext(false);
    }

    public interface TrackOperation {
        void execute(AudioTrack track);
    }
}
