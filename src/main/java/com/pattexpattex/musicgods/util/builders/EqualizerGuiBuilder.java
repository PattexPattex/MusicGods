package com.pattexpattex.musicgods.util.builders;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.music.audio.filter.equalizer.EqualizerConfig;
import com.pattexpattex.musicgods.util.FormatUtils;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Arrays;
import java.util.List;

import static com.pattexpattex.musicgods.music.audio.filter.equalizer.EqualizerManager.*;

public class EqualizerGuiBuilder {

    private static final String LINE = "║";
    private static final String LINE_THIN = "│";
    private static final String BLOCK = "█";
    private static final String MSG_CODEBLOCK = "```";
    private static final String SPACER = "        ";
    private static final String SPACER_THIN = "  ";

    private static final String EQ_BUTTON_ID = "kv:filters.equalizer:%s.%d"; // %s = up / down | %d = 0 - 4

    public static MessageEditData build(EqualizerConfig equalizer, ApplicationManager applicationManager) {
        StringBuilder stringBuilder = new StringBuilder(MSG_CODEBLOCK);

        stringBuilder.append(buildFirstLine()).append("\n");

        for (int i = GAIN_STEPS.length - 1; i >= 0; i--)
            stringBuilder.append(buildLine(equalizer, i));

        stringBuilder.append(MSG_CODEBLOCK);
        return new MessageEditBuilder().setEmbeds(FormatUtils.kvintakordEmbed().setDescription(stringBuilder).build())
                .setComponents(buildActionRows(equalizer, applicationManager.getInterfaceManager())).build();
    }

    private static String buildFirstLine() {
        StringBuilder builder = new StringBuilder(SPACER_THIN);

        for (int i : GUI_GAINS) {
            String assign = GAIN_FREQUENCIES[i];
            builder.append(assign).append(SPACER.substring(assign.length() - 1));
        }

        return builder.toString();
    }

    private static String buildLine(EqualizerConfig equalizer, int line) {
        StringBuilder builder = new StringBuilder();

        float step = GAIN_STEPS[line];

        for (int i = 0; i < 15; i++) {
            if (Arrays.stream(GUI_GAINS).anyMatch(((Integer) i)::equals)) {
                if (isCloseEnough(equalizer.getGain(i), step)) builder.append(BLOCK);
                else builder.append(LINE);
            }
            else {
                if (isCloseEnough(equalizer.getGain(i), step)) builder.append(BLOCK);
                else builder.append(LINE_THIN);
            }

            builder.append(SPACER_THIN);
        }

        String st = String.valueOf(step);
        builder.append(SPACER.substring(st.length())).append(st).append("\n");

        return builder.toString();
    }

    private static boolean isCloseEnough(float gain, float step) {
        float round = GAIN_STEP * Math.round(gain / GAIN_STEP);
        return round == step;
    }

    private static ActionRow[] buildActionRows(EqualizerConfig equalizer, InterfaceManagerConnector manager) {
        ButtonBuilder builder = new ButtonBuilder(equalizer, manager);

        List<ItemComponent> firstRow = List.of(builder.build(true, 0), builder.build(true, 1),
                builder.build(true, 2), builder.build(true, 3), builder.build(true, 4));

        List<ItemComponent> secondRow = List.of(builder.build(false, 0), builder.build(false, 1),
                builder.build(false, 2), builder.build(false, 3), builder.build(false, 4));

        List<ItemComponent> thirdRow = List.of(
                manager.getButtonManager().buildButton("kv:filters.equalizer:reset", false),
                manager.getButtonManager().buildButton("kv:filters.equalizer:destroy", false),
                manager.getButtonManager().buildButton("kv:filters.equalizer:export", false));

        return new ActionRow[]{ ActionRow.of(firstRow), ActionRow.of(secondRow), ActionRow.of(thirdRow) };
    }
    
    private record ButtonBuilder(EqualizerConfig eq, InterfaceManagerConnector manager) {
        Button build(boolean top, int pos) {
                float gain = eq.getGain(GUI_GAINS[pos]);
                boolean disabled = (top ? (gain >= 1.0f) : (gain <= -0.25f));
                return manager.getButtonManager().buildButton(String.format(EQ_BUTTON_ID, (top ? "up" : "down"), pos), disabled);
            }
        }
}
