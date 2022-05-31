package com.pattexpattex.musicgods.util.builders;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.interfaces.button.ButtonInterfaceManager;
import com.pattexpattex.musicgods.music.helpers.QueueManager;
import com.pattexpattex.musicgods.music.audio.LoopMode;
import com.pattexpattex.musicgods.music.audio.ShuffleMode;
import com.pattexpattex.musicgods.music.audio.TrackMetadata;
import com.pattexpattex.musicgods.util.BotEmoji;
import com.pattexpattex.musicgods.util.FormatUtils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;

import java.util.List;

import static com.pattexpattex.musicgods.util.FormatUtils.formatTimeFromMillis;

public class QueueBoxBuilder {

    private QueueBoxBuilder() {}

    public static Message build(AudioTrack track, List<AudioTrack> queue,
                                LoopMode loopMode, ShuffleMode shuffleMode, boolean isPaused,
                                int volume, int page, ApplicationManager manager) {
        MessageBuilder messageBuilder = new MessageBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        EmbedBuilder embedBuilder = embed(track);

        while (queue.size() < page * 20) page--;

        stringBuilder.append(buildFirstLine(track));
        stringBuilder.append(buildSecondLine(isPaused, loopMode, shuffleMode, volume));

        for (int i = (page * 20); i < queue.size(); i++) {
            if (stringBuilder.length() > 4000 || i > 20 * (page + 1)) {
                stringBuilder.append(String.format("\n**And %d more...**", queue.size() - i));
                break;
            }

            AudioTrack tr = queue.get(i);
            stringBuilder.append(String.format("**%d.** `%s` %s\n", i + 1, formatTimeFromMillis(tr.getDuration()),
                    FormatUtils.formatTrackUrl(tr)));
        }

        return messageBuilder.setActionRows(buildActionRows(track, queue, page, manager))
                .setEmbeds(embedBuilder.setDescription(stringBuilder).build()).build();
    }

    private static ActionRow[] buildActionRows(AudioTrack track, List<AudioTrack> queue, int page,
                                               ApplicationManager applicationManager) {
        ButtonInterfaceManager manager = applicationManager.getInterfaceManager().getButtonManager();

        List<ItemComponent> firstRow = List.of(
                manager.buildButton("kv:current.back", !track.isSeekable()),
                manager.buildButton("kv:pause", !track.isSeekable()),
                manager.buildButton("kv:current.forward", ! track.isSeekable()),
                manager.buildButton("kv:skip", false),
                manager.buildButton("kv:stop", false));

        List<ItemComponent> secondRow = List.of(
                manager.buildButton("kv:loop", false),
                manager.buildButton("kv:shuffle", false),
                manager.buildButton("kv:clear", queue.isEmpty()),
                manager.buildButton("kv.equalizer:gui", false),
                manager.buildButton("kv:lyrics", false));

        List<ItemComponent> thirdRow = (queue.size() > 20 ?
                List.of(manager.buildButton("kv:page.prev", page <= 0),
                        manager.buildButton("kv:page.next", queue.size() <= 20 * (page + 1)),
                        manager.buildButton("kv:destroy", false)) :
                List.of(manager.buildButton("kv:destroy", false)));

        return new ActionRow[]{ ActionRow.of(firstRow), ActionRow.of(secondRow), ActionRow.of(thirdRow) };
    }

    private static String buildFirstLine(AudioTrack track) {
        if (!track.isSeekable())
            return BotEmoji.RED_CIRCLE + " LIVE";

        long position = track.getPosition();
        long duration = track.getDuration();

        double percent = (double) (position) / (double) (duration);
        String line = "**" + FormatUtils.CODE + FormatUtils.buildLine(percent, QueueManager.TRACK_LINE_SIZE);

        line += FormatUtils.CODE + " " + formatTimeFromMillis(position) +
                " / " + formatTimeFromMillis(duration) + "**\n";

        return line;
    }

    private static String buildSecondLine(boolean paused, LoopMode loop,
                                          ShuffleMode shuffle, int volume) {
        return "**" +
                "Playback: " + (paused ? BotEmoji.PAUSE : BotEmoji.ARROW_FORWARD) + " | " +
                "Loop: " + loop.getEmoji() + " | " +
                "Shuffle: " + shuffle.getEmoji() + " | " +
                "Volume: " + volumeIcon(volume) + " `" + volume +
                "`**" + "\n\n";
    }

    private static EmbedBuilder embed(AudioTrack track) {
        return FormatUtils.kvintakordEmbed()
                .setTitle(TrackMetadata.getName(track),
                        (!track.getIdentifier().startsWith("C:\\") ? TrackMetadata.getUri(track) : null))
                .setAuthor(TrackMetadata.getAuthor(track), TrackMetadata.getAuthorUrl(track))
                .setThumbnail(TrackMetadata.getImage(track));
    }

    private static String volumeIcon(int vol) {
        if (vol == 0)
            return BotEmoji.SOUND_MUTE;
        if (vol < 30)
            return BotEmoji.SOUND_QUIET;
        if (vol < 70)
            return BotEmoji.SOUND;
        return BotEmoji.SOUND_LOUD;
    }
}
