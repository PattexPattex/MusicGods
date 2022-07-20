package com.pattexpattex.musicgods.interfaces.slash.objects;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public enum ParameterType {

    STRING(OptionType.STRING, String.class),
    LONG(OptionType.INTEGER, Long.class),
    INTEGER(OptionType.INTEGER, Integer.class),
    DOUBLE(OptionType.NUMBER, Double.class),
    BOOLEAN(OptionType.BOOLEAN, Boolean.class),
    USER(OptionType.USER, User.class),
    ROLE(OptionType.ROLE, Role.class),
    MEMBER(OptionType.USER, Member.class),
    MENTIONABLE(OptionType.MENTIONABLE, IMentionable.class),
    ATTACHMENT(OptionType.ATTACHMENT, Message.Attachment.class),
    MENTIONS(OptionType.STRING, Mentions.class),
    
    TEXT_CHANNEL(OptionType.CHANNEL, TextChannel.class),
    NEWS_CHANNEL(OptionType.CHANNEL, NewsChannel.class),
    AUDIO_CHANNEL(OptionType.CHANNEL, AudioChannel.class),
    GUILD_CHANNEL(OptionType.CHANNEL, GuildChannel.class),
    STAGE_CHANNEL(OptionType.CHANNEL, StageChannel.class),
    VOICE_CHANNEL(OptionType.CHANNEL, VoiceChannel.class),
    THREAD_CHANNEL(OptionType.CHANNEL, ThreadChannel.class),
    MESSAGE_CHANNEL(OptionType.CHANNEL, GuildMessageChannel.class),
    STANDARD_MESSAGE_CHANNEL(OptionType.CHANNEL, StandardGuildMessageChannel.class),
    STANDARD_GUILD_CHANNEL(OptionType.CHANNEL, StandardGuildChannel.class),
    CATEGORY(OptionType.CHANNEL, Category.class),
    UNION(OptionType.CHANNEL, GuildChannelUnion.class),
    ;
    
    private GuildChannel getAsChannel(OptionMapping mapping) {
        GuildChannelUnion c = mapping.getAsChannel();
        
        if (clazz == TextChannel.class) return c.asTextChannel();
        if (clazz == NewsChannel.class) return c.asNewsChannel();
        if (clazz == AudioChannel.class) return c.asAudioChannel();
        if (clazz == GuildChannel.class) return c.asStandardGuildChannel();
        if (clazz == StageChannel.class) return c.asStageChannel();
        if (clazz == VoiceChannel.class) return c.asVoiceChannel();
        if (clazz == ThreadChannel.class) return c.asThreadChannel();
        if (clazz == GuildMessageChannel.class) return c.asGuildMessageChannel();
        if (clazz == StandardGuildMessageChannel.class) return c.asStandardGuildMessageChannel();
        if (clazz == StandardGuildChannel.class) return c.asStandardGuildChannel();
        if (clazz == Category.class) return c.asCategory();
        
        return c;
    }
    
    private final Function<OptionMapping, Object> function;
    private final OptionType optionType;
    private final Class<?> clazz;
    
    ParameterType(OptionType optionType, Class<?> clazz) {
        this.optionType = optionType;
        this.clazz = clazz;
        this.function = getFunction();
    }
    
    public static ParameterType ofClass(Class<?> clazz) {
        if (clazz == long.class) return ParameterType.LONG;
        else if (clazz == int.class) return ParameterType.INTEGER;
        else if (clazz == double.class) return ParameterType.DOUBLE;
        else if (clazz == boolean.class) return ParameterType.BOOLEAN;
    
        for (ParameterType type : values())
            if (type.clazz == clazz)
                return type;
        
        throw new IllegalArgumentException("Bad type " + clazz.getSimpleName());
    }
    
    public Object apply(OptionMapping mapping) {
        return function.apply(mapping);
    }
    
    public OptionType getOptionType() {
        return optionType;
    }
    
    public Class<?> getClazz() {
        return clazz;
    }
    
    public Collection<ChannelType> getChannelTypes() {
        if (optionType != OptionType.CHANNEL)
            return Collections.emptyList();
        
        if (clazz == TextChannel.class) return List.of(ChannelType.TEXT);
        if (clazz == NewsChannel.class) return List.of(ChannelType.NEWS);
        if (clazz == AudioChannel.class) return List.of(ChannelType.VOICE);
        if (clazz == GuildChannel.class) return getGuildTypes().toList();
        if (clazz == StageChannel.class) return List.of(ChannelType.STAGE);
        if (clazz == VoiceChannel.class) return List.of(ChannelType.VOICE, ChannelType.STAGE);
        if (clazz == ThreadChannel.class) return getGuildTypes().filter(ChannelType::isThread).toList();
        if (clazz == GuildMessageChannel.class) return getGuildTypes().filter(ChannelType::isMessage).toList();
        if (clazz == StandardGuildMessageChannel.class) return getGuildTypes().filter(ChannelType::isMessage).toList();
        if (clazz == StandardGuildChannel.class) return getGuildTypes().toList();
        if (clazz == Category.class) return List.of(ChannelType.CATEGORY);
        if (clazz == GuildChannelUnion.class) return getGuildTypes().toList();
        
        return Collections.emptyList();
    }
    
    private static Stream<ChannelType> getGuildTypes() {
        return Stream.of(ChannelType.values()).filter(ChannelType::isGuild);
    }
    
    private Function<OptionMapping, Object> getFunction() {
        if (optionType == OptionType.CHANNEL)
            return this::getAsChannel;
        
        if (clazz == String.class) return OptionMapping::getAsString;
        if (clazz == Long.class) return OptionMapping::getAsLong;
        if (clazz == Integer.class) return OptionMapping::getAsInt;
        if (clazz == Double.class) return OptionMapping::getAsDouble;
        if (clazz == Boolean.class) return OptionMapping::getAsBoolean;
        if (clazz == User.class) return OptionMapping::getAsUser;
        if (clazz == Role.class) return OptionMapping::getAsRole;
        if (clazz == Member.class) return OptionMapping::getAsMember;
        if (clazz == IMentionable.class) return OptionMapping::getAsMentionable;
        if (clazz == Message.Attachment.class) return OptionMapping::getAsAttachment;
        if (clazz == Mentions.class) return OptionMapping::getMentions;
        
        throw new IllegalStateException();
    }
}
