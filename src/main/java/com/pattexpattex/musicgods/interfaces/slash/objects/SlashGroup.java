package com.pattexpattex.musicgods.interfaces.slash.objects;

import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.util.BotEmoji;
import net.dv8tion.jda.api.entities.Emoji;

import javax.annotation.Nullable;
import java.util.*;

public class SlashGroup {

    public static final String DEFAULT_DESCRIPTION = "No description.";

    public static final SlashGroup UNGROUPED = new SlashGroup("ungrouped", "Ungrouped", false, "Ungrouped commands.", BotEmoji.DIVIDERS);
    public static final SlashGroup HIDDEN = new SlashGroup("hidden", null, true, null, null);

    private static final String CMD_CURVE = "└";

    private String name;
    private final String identifier;
    @Nullable private String description;
    @Nullable private Emoji emoji;
    private final boolean hidden;
    private final List<SlashCommand> commands;

    private SlashGroup(String identifier, @Nullable String name, boolean hidden,
                       @Nullable String description, @Nullable String emoji) {
        this.identifier = identifier;
        this.hidden = hidden;
        this.commands = new ArrayList<>();
        this.description = description;

        if (name == null || name.isBlank())
            this.name = identifier;
        else
            this.name = name;

        if (emoji == null || emoji.isBlank())
            this.emoji = null;
        else
            this.emoji = Emoji.fromUnicode(emoji);
    }

    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Emoji getEmoji() {
        return emoji;
    }

    public List<SlashCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setEmoji(@Nullable Emoji emoji) {
        this.emoji = emoji;
    }

    public SlashGroup addCommands(SlashCommand command, SlashCommand... other) {
        List<SlashCommand> list = new ArrayList<>();
        list.add(command);
        list.addAll(Arrays.asList(other));

        return addCommands(list);
    }

    public SlashGroup addCommands(Collection<SlashCommand> commands) {
        this.commands.addAll(commands);
        return this;
    }

    public String getString() {
        StringBuilder builder = new StringBuilder();

        if (commands.isEmpty())
            builder.append("_Nothing here._");

        for (SlashCommand command : commands) {
            builder.append("**/")
                    .append(command.getName())
                    .append("**\n")
                    .append("  ").append(CMD_CURVE).append(" ")
                    .append(command.getData().getDescription())
                    .append("\n");
        }

        return builder.toString();
    }

    /**
     * This returns a string used for debugging. To get a formatted string, use {@link SlashGroup#getString() getString()}.
     */
    @Override
    public String toString() {
        return "SlashGroup{" +
                "identifier='" + identifier + '\'' +
                '}';
    }

    public static SlashGroup empty(Grouped grouped) {
        if (grouped == null)
            return UNGROUPED;

        if (grouped.hidden())
            return HIDDEN;

        return new SlashGroup(grouped.value(), grouped.name(), false, grouped.description(), grouped.emoji());
    }

    public static SlashGroup empty(String identifier, String name) {
        return new SlashGroup(identifier, name, false, null, null);
    }
}
