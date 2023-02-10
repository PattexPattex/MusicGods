package com.pattexpattex.musicgods.interfaces.slash;

import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.Autocomplete;
import com.pattexpattex.musicgods.annotations.slash.parameter.Choice;
import com.pattexpattex.musicgods.annotations.slash.parameter.Parameter;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import com.pattexpattex.musicgods.interfaces.slash.objects.ParameterType;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashPath;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import static com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand.DEFAULT_DESCRIPTION;

public class SlashDataBuilder {

    public static void addEndpoint(Method method, SlashHandle handle, SlashCommand command) {
        SlashPath path = new SlashPath(handle.path());
        SlashCommandData data = command.getData();

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
            
            SubcommandGroupData oldSubGroupData = data.getSubcommandGroups()
                    .stream()
                    .filter(d -> d.getName().equals(subGroupData.getName()))
                    .findFirst()
                    .orElse(null);
            
            if (oldSubGroupData != null) {
                subGroupData.addSubcommands(oldSubGroupData.getSubcommands());
                data.removeSubcommandGroupByName(oldSubGroupData.getName());
                data.addSubcommandGroups(subGroupData);
            }
        }
        else {
            buildCommand(data, method);
            data.setDescription(handle.description());
        }
        
        buildPermissions(data, method);
    }

    public static SlashCommandData buildEmpty(SlashPath path) {
        return Commands.slash(path.getBase(), DEFAULT_DESCRIPTION);
        //return new SlashCommand.Data(path.getBase(), DEFAULT_DESCRIPTION);
    }

    private static void buildOptions(OptionData[] arr, Method method) {
        var parameters = method.getParameters();

        if (arr.length > 25) {
            throw new IndexOutOfBoundsException("A command cannot have more than 25 parameters");
        }

        for (int i = 1; i < parameters.length; i++) {
            var par = parameters[i];

            @Nullable Parameter parameter = par.getAnnotation(Parameter.class);
            @Nullable Choice choice = par.getAnnotation(Choice.class);
            @Nullable Range range = par.getAnnotation(Range.class);
            @Nullable Autocomplete autocomplete = par.getAnnotation(Autocomplete.class);

            ParameterType type = ParameterType.ofClass(par.getType());
            String name = par.getName();
            String description = DEFAULT_DESCRIPTION;
            
            if (parameter != null && !parameter.name().isBlank()) {
                name = parameter.name();
            }
            if (parameter != null && !parameter.description().isBlank()) {
                description = parameter.description();
            }

            OptionData optionData = new OptionData(type.getOptionType(), name, description);
            
            if (parameter != null) {
                optionData.setRequired(parameter.required());
            }
            else {
                optionData.setRequired(true);
            }

            if (choice != null && choice.choices().length > 0) {
                setChoices(optionData, choice, name);
            }
            else if (autocomplete != null && optionData.getType().canSupportChoices()) {
                optionData.setAutoComplete(true);
            }
            
            setOptionType(optionData, range, name, par, type);

            arr[i - 1] = optionData;
        }
    }

    private static void setOptionType(OptionData optionData, Range range, String name, java.lang.reflect.Parameter par, ParameterType type) {
        if (range != null && optionData.getType() != OptionType.NUMBER && optionData.getType() != OptionType.INTEGER) {
            throw new IllegalArgumentException(String.format("Incompatible types, parameter %s (%s) is annotated with %s",
                    name, par.getType().getSimpleName(), range.getClass().getSimpleName()));
        }
        else if (range != null && (optionData.getType() == OptionType.NUMBER || optionData.getType() == OptionType.INTEGER)) {
            optionData.setRequiredRange((long) setInRange(range.min()), (long) setInRange(range.max()));
        }
        else if (range != null) {
            optionData.setRequiredRange(setInRange(range.min()), setInRange(range.max()));
        }
    
        if (optionData.getType() == OptionType.CHANNEL) {
            optionData.setChannelTypes(type.getChannelTypes());
        }
    }

    private static void setChoices(OptionData optionData, Choice choice, String name) {
        String[] keys = choice.choices();
        String[] values = choice.values();
    
        if (values.length > 0 && keys.length != values.length) {
            throw new IllegalArgumentException(String.format("Choices for parameter %s are not the same length", name));
        }
    
        if (values.length == 0) {
            for (String key : keys) {
                optionData.addChoice(key, key);
            }
        }
        else {
            for (int j = 0; j < keys.length; j++) {
                optionData.addChoice(keys[j], values[j]);
            }
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
    
    private static void buildPermissions(SlashCommandData data, Method method) {
        Permissions permissions = method.getAnnotation(Permissions.class);
        
        if (permissions == null)
            return;
        
        if (Permission.getRaw(permissions.command()) == 0)
            data.setDefaultPermissions(DefaultMemberPermissions.ENABLED);
        else if (Permission.getRaw(permissions.command()) == Permission.ADMINISTRATOR.getRawValue())
            data.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
        else
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions.command()));
    }
}
