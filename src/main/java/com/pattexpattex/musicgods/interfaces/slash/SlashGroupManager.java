package com.pattexpattex.musicgods.interfaces.slash;

import com.pattexpattex.musicgods.annotations.slash.Grouped;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashGroup;
import net.dv8tion.jda.api.entities.Emoji;

import java.util.*;

public class SlashGroupManager {

    private final Map<String, SlashGroup> groups;
    private final SlashInterfaceManager manager;

    public SlashGroupManager(SlashInterfaceManager manager) {
        this.manager = manager;
        this.groups = new HashMap<>();

        groups.put(SlashGroup.UNGROUPED.getIdentifier(), SlashGroup.UNGROUPED);
    }

    public SlashInterfaceManager getManager() {
        return manager;
    }

    public SlashGroup getGroup(Grouped grouped) {
        if (grouped == null)
            return SlashGroup.UNGROUPED;

        if (grouped.hidden())
            return SlashGroup.HIDDEN;

        if (!groups.containsKey(grouped.value())) {
            SlashGroup group = SlashGroup.empty(grouped);
            groups.put(group.getIdentifier(), group);
            return group;
        }

        SlashGroup group = groups.get(grouped.value());

        String desc = group.getDescription();
        if (desc == null || SlashGroup.DEFAULT_DESCRIPTION.equals(desc))
            group.setDescription(grouped.description());

        if (group.getName().equals(group.getIdentifier()))
            group.setName(grouped.name());

        Emoji emoji = group.getEmoji();
        if (emoji == null)
            group.setEmoji(Emoji.fromUnicode(grouped.emoji()));

        return group;
    }

    public SlashGroup getGroup(String identifier) {
        return groups.get(identifier);
    }

    public Map<String, SlashGroup> getGroups() {
        return Collections.unmodifiableMap(groups);
    }
}
