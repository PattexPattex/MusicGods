package com.pattexpattex.musicgods.interfaces.selection.objects;

import com.pattexpattex.musicgods.annotations.selection.SelectionHandle;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.internal.interactions.component.StringSelectMenuImpl;

import java.lang.reflect.Method;
import java.util.List;

public class Selection {

    public static final String DUMMY_PREFIX = "dummy:";

    private final String identifier;
    private final Class<? extends SelectionInterface> controller;
    private final Method method;
    private final StringSelectMenu.Builder menu;
    private final Permissions permissions;

    private Selection(String identifier, Class<? extends SelectionInterface> controller, Method method, Permissions permissions) {
        this.identifier = identifier;
        this.controller = controller;
        this.method = method;
        this.menu = StringSelectMenu.create(identifier);
        this.permissions = permissions;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Class<? extends SelectionInterface> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public StringSelectMenu.Builder getMenu() {
        return menu;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public Selection addOptions(List<SelectOption> options) {
        menu.addOptions(options.toArray(SelectOption[]::new));
        return this;
    }

    public Selection addOptions(SelectOption... options) {
        menu.addOptions(options);
        return this;
    }

    public Selection setRange(int min, int max) {
        menu.setRequiredRange(min, max);
        return this;
    }

    public Selection setPlaceholder(String placeholder) {
        menu.setPlaceholder(placeholder);
        return this;
    }

    public Selection setMax(int value) {
        menu.setMaxValues(value);
        return this;
    }

    public Selection setMin(int value) {
        menu.setMinValues(value);
        return this;
    }

    public SelectMenu build() {
        return menu.build();
    }

    public static Selection of(Class<? extends SelectionInterface> controller,
                               SelectionHandle handle, com.pattexpattex.musicgods.annotations.Permissions permissions, Method method) {
        Permission[] main = (permissions == null ? Permission.EMPTY_PERMISSIONS : permissions.value());
        Permission[] self = (permissions == null ? Permission.EMPTY_PERMISSIONS : permissions.self());
        String identifier = handle.value();

        return new Selection(identifier, controller, method, new Permissions(main, self));
    }

    public static SelectMenu dummy(String identifier, String placeholder, int min,
                                   int max, boolean disabled, List<SelectOption> options) {
        return new StringSelectMenuImpl(DUMMY_PREFIX + identifier, placeholder, min, max, disabled, options);
    }

    public static class Permissions {

        private final Permission[] main;
        private final Permission[] self;

        private Permissions(Permission[] main, Permission[] self) {
            this.main = main;
            this.self = self;
        }

        public Permission[] getMain() {
            return main;
        }

        public Permission[] getSelf() {
            return self;
        }
    }
}
