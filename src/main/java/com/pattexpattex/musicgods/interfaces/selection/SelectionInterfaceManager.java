package com.pattexpattex.musicgods.interfaces.selection;

import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.selection.SelectionHandle;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.selection.objects.Selection;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionInterface;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionResponseHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class SelectionInterfaceManager {

    private static final Logger log = LoggerFactory.getLogger(SelectionInterfaceManager.class);

    private final Map<String, Selection> selections;
    private final InterfaceManagerConnector connector;

    public SelectionInterfaceManager(InterfaceManagerConnector connector) {
        this.connector = connector;
        this.selections = new HashMap<>();
    }

    public InterfaceManagerConnector getConnector() {
        return connector;
    }

    public Map<String, Selection> getSelections() {
        return Collections.unmodifiableMap(selections);
    }

    public Selection getSelection(String identifier) {
        return selections.get(identifier);
    }

    public void dispatch(Map<Class<? extends SelectionInterface>, SelectionInterface> controllers,
                         SelectMenuInteractionEvent event, SelectionResponseHandler handler) {
        String identifier = event.getComponentId();
        Selection selection = selections.get(identifier);

        if (identifier.startsWith(Selection.DUMMY_PREFIX))
            return;

        if (selection == null) {
            handler.notFound(event, identifier);
            log.warn("Received an unknown SelectMenuInteractionEvent: {}", identifier, new NoSuchElementException());
            return;
        }

        if (checkPermissions(event, selection, handler)) return;

        Object[] args = new Object[selection.getMethod().getParameterCount()];

        args[0] = event;

        if (args.length == 2)
            args[1] = event.getSelectedOptions();

        try {
            selection.getMethod().invoke(controllers.get(selection.getController()), args);
        }
        catch (InvocationTargetException e) {
            handler.exception(event, identifier, e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerMethod(Class<? extends SelectionInterface> controller, Method method,
                               SelectionHandle handle, Permissions permissions) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0 || !parameters[0].getType().isAssignableFrom(SelectMenuInteractionEvent.class)) {
            log.warn("Method ({}) does not have a SelectMenuInteractionEvent as the 1st parameter", method);
            return;
        }

        if (parameters.length == 2 && !parameters[1].getType().isAssignableFrom(List.class)) {
            log.warn("Method ({}) must have a List<SelectOption> as the 2nd parameter", method);
            return;
        }

        method.setAccessible(true);
        Selection selection = Selection.of(controller, handle, permissions, method);
        selections.put(selection.getIdentifier(), selection);
    }

    public SelectMenu buildSelection(String identifier, boolean disabled) {
        if (!disabled)
            return selections.get(identifier).build();
        else
            return selections.get(identifier).build().asDisabled();
    }

    private boolean checkPermissions(SelectMenuInteractionEvent event,
                                     Selection selection,
                                     SelectionResponseHandler handler) {
        Selection.Permissions permissions = selection.getPermissions();
        GuildChannel channel = event.getGuildChannel();

        Member member = event.getMember();
        EnumSet<Permission> mp = member.getPermissions(channel);
        Permission[] rp = permissions.getMain();

        if (rp.length > 0 && !member.hasPermission(rp)) {
            handler.restricted(event, selection.getIdentifier(),
                    permissions.getMain(), mp.toArray(Permission[]::new));
            return true;
        }

        Member self = event.getGuild().getSelfMember();
        EnumSet<Permission> sp = self.getPermissions(channel);
        Permission[] rsp = permissions.getSelf();

        if (rsp.length > 0 && !self.hasPermission(rsp)) {
            handler.selfRestricted(event, selection.getIdentifier(),
                    permissions.getSelf(), sp.toArray(Permission[]::new));
            return true;
        }

        return false;
    }
}
