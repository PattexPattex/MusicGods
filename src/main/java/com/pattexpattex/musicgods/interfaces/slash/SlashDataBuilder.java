package com.pattexpattex.musicgods.interfaces.slash;

import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import com.pattexpattex.musicgods.annotations.slash.parameter.SlashParameter;
import com.pattexpattex.musicgods.interfaces.slash.objects.ParameterType;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashPath;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand.DEFAULT_DESCRIPTION;

public class SlashDataBuilder {

    public static void addEndpoint(Method method, SlashHandle handle, SlashCommand command) {
        SlashPath path = new SlashPath(handle.path());
        SlashCommand.Data data = command.getData();

        if (!handle.baseDescription().equals(DEFAULT_DESCRIPTION))
            data.setDescription(handle.baseDescription());

        if (path.isSubcommand()) {
            SubcommandData subData = new SubcommandData(path.getElement(1), handle.description());
            buildSubcommand(subData, method);
            data.addSubcommands(subData);
        }
        else if (path.isSubcommandGroup()) {
            SubcommandGroupData subGroupData = new SubcommandGroupData(path.getElement(1), DEFAULT_DESCRIPTION);
            SubcommandData subData = new SubcommandData(path.getElement(2), handle.description());

            buildSubcommand(subData, method);
            subGroupData.addSubcommands(subData);
            data.mergeSubcommandGroup(subGroupData);
        }
        else {
            buildCommand(data, method);
            data.setDescription(handle.description());
        }
    }

    public static SlashCommand.Data buildEmpty(SlashPath path) {
        return new SlashCommand.Data(path.getBase(), DEFAULT_DESCRIPTION);
    }

    private static void buildOptions(OptionData[] arr, Method method) {
        Parameter[] parameters = method.getParameters();

        if (arr.length > 25)
            throw new IndexOutOfBoundsException("A command cannot have more than 25 parameters");

        for (int i = 1; i < parameters.length; i++) {
            Parameter par = parameters[i];

            @Nullable SlashParameter parameter = par.getAnnotation(SlashParameter.class);
            @Nullable Choice choice = par.getAnnotation(Choice.class);
            @Nullable Range range = par.getAnnotation(Range.class);

            ParameterType type = ParameterType.ofClass(par.getType());
            String name = par.getName();
            String description = DEFAULT_DESCRIPTION;
            if (parameter != null && !parameter.name().isBlank()) name = parameter.name();
            if (parameter != null && !parameter.description().isBlank()) description = parameter.description();

            OptionData optionData = new OptionData(type.getOptionType(), name, description);

            if (parameter != null)
                optionData.setRequired(parameter.required());
            else
                optionData.setRequired(true);

            if (choice != null && choice.choices().length > 0) {
                String[] keys = choice.choices();
                String[] values = choice.values();

                if (values.length > 0 && keys.length != values.length)
                    throw new IllegalArgumentException(String.format("Choices for parameter %s are not the same length", name));

                if (values.length == 0)
                    for (String key : keys)
                        optionData.addChoice(key, key);
                else
                    for (int j = 0; j < keys.length; j++)
                        optionData.addChoice(keys[j], values[j]);
            }

            if (range != null && type != ParameterType.INTEGER && type != ParameterType.LONG && type != ParameterType.DOUBLE) {
                throw new IllegalArgumentException(String.format("Incompatible types, parameter %s (%s) is annotated with %s",
                        name, par.getType().getSimpleName(), range.getClass().getSimpleName()));
            }
            else if (range != null && (type == ParameterType.INTEGER || type == ParameterType.LONG)) {
                optionData.setRequiredRange((long) setInRange(range.min()), (long) setInRange(range.max()));
            }
            else if (range != null) {
                optionData.setRequiredRange(setInRange(range.min()), setInRange(range.max()));
            }

            arr[i - 1] = optionData;
        }
    }

    private static double setInRange(double d) {
        return Math.min(OptionData.MAX_POSITIVE_NUMBER, Math.max(d, OptionData.MIN_NEGATIVE_NUMBER));
    }

    private static void buildSubcommand(SubcommandData subData, Method method) {
        OptionData[] arr = new OptionData[method.getParameterCount() - 1];
        buildOptions(arr, method);
        subData.addOptions(arr);
    }

    private static void buildCommand(SlashCommandData data, Method method) {
        OptionData[] arr = new OptionData[method.getParameterCount() - 1];
        buildOptions(arr, method);
        data.addOptions(arr);
    }
}
