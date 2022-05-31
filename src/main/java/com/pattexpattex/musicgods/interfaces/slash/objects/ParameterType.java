package com.pattexpattex.musicgods.interfaces.slash.objects;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.AttachedFile;

import java.util.function.Function;

public enum ParameterType {

    STRING(OptionMapping::getAsString, OptionType.STRING, String.class),
    LONG(OptionMapping::getAsLong, OptionType.INTEGER, Long.class),
    INTEGER(OptionMapping::getAsInt, OptionType.INTEGER, Integer.class),
    DOUBLE(OptionMapping::getAsDouble, OptionType.NUMBER, Double.class),
    BOOLEAN(OptionMapping::getAsBoolean, OptionType.BOOLEAN, Boolean.class),
    USER(OptionMapping::getAsUser, OptionType.USER, User.class),
    ROLE(OptionMapping::getAsRole, OptionType.ROLE, Role.class),
    MEMBER(OptionMapping::getAsMember, OptionType.USER, Member.class),
    MENTIONABLE(OptionMapping::getAsMentionable, OptionType.MENTIONABLE, IMentionable.class),
    ATTACHMENT(OptionMapping::getAsAttachment, OptionType.ATTACHMENT, Message.Attachment.class),
    TEXT_CHANNEL(OptionMapping::getAsTextChannel, OptionType.CHANNEL, TextChannel.class),
    NEWS_CHANNEL(OptionMapping::getAsNewsChannel, OptionType.CHANNEL, NewsChannel.class),
    AUDIO_CHANNEL(OptionMapping::getAsAudioChannel, OptionType.CHANNEL, AudioChannel.class),
    GUILD_CHANNEL(OptionMapping::getAsGuildChannel, OptionType.CHANNEL, GuildChannel.class),
    STAGE_CHANNEL(OptionMapping::getAsStageChannel, OptionType.CHANNEL, StageChannel.class),
    VOICE_CHANNEL(OptionMapping::getAsVoiceChannel, OptionType.CHANNEL, VoiceChannel.class),
    THREAD_CHANNEL(OptionMapping::getAsThreadChannel, OptionType.CHANNEL, ThreadChannel.class),
    MESSAGE_CHANNEL(OptionMapping::getAsMessageChannel, OptionType.CHANNEL, GuildMessageChannel.class);

    private final Function<OptionMapping, Object> function;
    private final OptionType optionType;
    private final Class<?> klass;

    ParameterType(Function<OptionMapping, Object> function, OptionType optionType, Class<?> klass) {
        this.function = function;
        this.optionType = optionType;
        this.klass = klass;
    }

    public static ParameterType ofClass(Class<?> klass) {
        if (klass == String.class) return ParameterType.STRING;
        else if (klass == long.class || klass == Long.class) return ParameterType.LONG;
        else if (klass == int.class || klass == Integer.class) return ParameterType.INTEGER;
        else if (klass == double.class || klass == Double.class) return ParameterType.DOUBLE;
        else if (klass == boolean.class || klass == Boolean.class) return ParameterType.BOOLEAN;
        else if (klass == User.class) return ParameterType.USER;
        else if (klass == Role.class) return ParameterType.ROLE;
        else if (klass == Member.class) return ParameterType.MEMBER;
        else if (klass == IMentionable.class) return ParameterType.MENTIONABLE;
        else if (klass == Message.Attachment.class) return ParameterType.ATTACHMENT;
        else if (klass == TextChannel.class) return ParameterType.TEXT_CHANNEL;
        else if (klass == NewsChannel.class) return ParameterType.NEWS_CHANNEL;
        else if (klass == AudioChannel.class) return ParameterType.AUDIO_CHANNEL;
        else if (klass == GuildChannel.class) return ParameterType.GUILD_CHANNEL;
        else if (klass == StageChannel.class) return ParameterType.STAGE_CHANNEL;
        else if (klass == VoiceChannel.class) return ParameterType.VOICE_CHANNEL;
        else if (klass == ThreadChannel.class) return ParameterType.THREAD_CHANNEL;
        else if (klass == GuildMessageChannel.class) return ParameterType.MESSAGE_CHANNEL;
        else throw new IllegalArgumentException("Bad type " + klass.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <T> T apply(OptionMapping mapping) {
        return ((Class<T>) klass).cast(function.apply(mapping));
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public Class<?> getKlass() {
        return klass;
    }
}
