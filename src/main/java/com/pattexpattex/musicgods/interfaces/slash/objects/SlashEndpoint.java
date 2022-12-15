package com.pattexpattex.musicgods.interfaces.slash.objects;

import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import net.dv8tion.jda.api.Permission;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SlashEndpoint {

    private final SlashPath path;
    private final List<SlashParameter> parameters;
    private final Permissions metadata;
    private final Class<? extends SlashInterface> controller;
    private final Method method;
    private final int flags;

    private SlashEndpoint(SlashPath path, List<SlashParameter> parameters, Permissions metadata,
                          Class<? extends SlashInterface> controller, Method method, int flags) {
        this.path = path;
        this.parameters = parameters;
        this.metadata = metadata;
        this.controller = controller;
        this.method = method;
        this.flags = flags;
    }

    public SlashPath getPath() {
        return path;
    }

    public List<SlashParameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public int getParametersSize() {
        return parameters.size();
    }

    public Permissions getPermissions() {
        return metadata;
    }
    
    public boolean isGuildOnly() {
        return flags % 2 == 1;
    }
    
    public boolean isPrivateOnly() {
        return  (flags >> 1) % 2 == 1;
    }
    
    public int getFlags() {
        return flags;
    }

    public Class<? extends SlashInterface> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public static SlashEndpoint of(Class<? extends SlashInterface> controller, SlashHandle handle, int flags,
                                   com.pattexpattex.musicgods.annotations.Permissions permissions, Method method) {
        SlashPath path = new SlashPath(handle.path());
        Parameter[] methodParameters = method.getParameters();
        List<SlashParameter> commandParameters = new LinkedList<>();

        if (methodParameters.length > 26)
            throw new IndexOutOfBoundsException("A command cannot have more than 25 parameters");

        for (int i = 1; i < methodParameters.length; i++) {
            Parameter par = methodParameters[i];
            com.pattexpattex.musicgods.annotations.slash.parameter.Parameter slashParameter =
                    par.getAnnotation(com.pattexpattex.musicgods.annotations.slash.parameter.Parameter.class);

            if (slashParameter == null) {
                commandParameters.add(new SlashParameter(par.getName(), ParameterType.ofClass(par.getType()), true));
                continue;
            }

            String name = slashParameter.name();
            if (name.isBlank()) {
                name = par.getName();
            }

            commandParameters.add(new SlashParameter(name, ParameterType.ofClass(par.getType()), slashParameter.required()));
        }

        if (permissions == null) {
            return new SlashEndpoint(path, commandParameters, Permissions.DEFAULT, controller, method, flags);
        }

        return new SlashEndpoint(path, commandParameters, new Permissions(permissions.value(), permissions.self()), controller, method, flags);
    }

    public static class Permissions {

        private final Permission[] permissions;
        private final Permission[] selfPermissions;

        private Permissions(Permission[] permissions, Permission[] selfPermissions) {
            this.permissions = permissions;
            this.selfPermissions = selfPermissions;
        }

        public Permission[] getMain() {
            return permissions;
        }

        public Permission[] getSelf() {
            return selfPermissions;
        }
        
        public static final Permissions DEFAULT = new Permissions(com.pattexpattex.musicgods.annotations.Permissions.DEFAULT, com.pattexpattex.musicgods.annotations.Permissions.SELF_DEFAULT);
    }
}
