package com.pattexpattex.musicgods.interfaces;

import com.pattexpattex.musicgods.ApplicationManager;
import com.pattexpattex.musicgods.GuildContext;
import com.pattexpattex.musicgods.annotations.Permissions;
import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.annotations.modal.ModalHandle;
import com.pattexpattex.musicgods.annotations.selection.SelectionHandle;
import com.pattexpattex.musicgods.annotations.slash.SlashHandle;
import com.pattexpattex.musicgods.annotations.slash.autocomplete.AutocompleteHandle;
import com.pattexpattex.musicgods.interfaces.button.ButtonInterfaceManager;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.modal.ModalInterfaceManager;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalInterface;
import com.pattexpattex.musicgods.interfaces.selection.SelectionInterfaceManager;
import com.pattexpattex.musicgods.interfaces.selection.objects.SelectionInterface;
import com.pattexpattex.musicgods.interfaces.slash.SlashInterfaceManager;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.AutocompleteInterfaceManager;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterface;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashInterface;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class InterfaceManagerConnector {

    private final List<BaseInterfaceFactory<? extends BaseInterface>> factories;
    private final SlashInterfaceManager slashManager;
    private final AutocompleteInterfaceManager autocompleteManager;
    private final ButtonInterfaceManager buttonManager;
    private final SelectionInterfaceManager selectionManager;
    private final ModalInterfaceManager modalManager;
    private final ApplicationManager manager;

    public InterfaceManagerConnector(ApplicationManager manager) {
        this.manager = manager;
        this.factories = new LinkedList<>();
        this.slashManager = new SlashInterfaceManager(this);
        this.autocompleteManager = new AutocompleteInterfaceManager(this);
        this.buttonManager = new ButtonInterfaceManager(this);
        this.selectionManager = new SelectionInterfaceManager(this);
        this.modalManager = new ModalInterfaceManager(this);
    }


    /* ---- Base ---- */

    @SafeVarargs
    public final void registerControllers(@NotNull BaseInterfaceFactory<? extends BaseInterface> factory,
                                          BaseInterfaceFactory<? extends BaseInterface>... other) {
        registerController(factory);

        for (BaseInterfaceFactory<? extends BaseInterface> otherFactory : other) {
            registerController(otherFactory);
        }
    }

    @SuppressWarnings("unchecked")
    public void registerController(BaseInterfaceFactory<? extends BaseInterface> factory) {
        factories.add(factory);

        Class<? extends BaseInterface> controllerClass = factory.getControllerClass();

        /*
         * Sorting by method names since sorting by client-side
         * IDs (with buttons) or names (with commands) is redundant.
         * The methods are sorted for the sake of a consistent order of interfaces.
         */
        List<Method> methods = Arrays.stream(controllerClass.getDeclaredMethods()).sorted(Comparator.comparing(Method::getName)).toList();

        for (Method method : methods) {
            Permissions permissions = method.getAnnotation(Permissions.class);

            SlashHandle slashHandle = method.getAnnotation(SlashHandle.class);
            AutocompleteHandle autocompleteHandle = method.getAnnotation(AutocompleteHandle.class);
            ButtonHandle buttonHandle = method.getAnnotation(ButtonHandle.class);
            SelectionHandle selectionHandle = method.getAnnotation(SelectionHandle.class);
            ModalHandle modalHandle = method.getAnnotation(ModalHandle.class);

            try {
                if (slashHandle != null && SlashInterface.class.isAssignableFrom(controllerClass)) {
                    slashManager.registerMethod((Class<? extends SlashInterface>) controllerClass, method);
                }
                else if (autocompleteHandle != null && AutocompleteInterface.class.isAssignableFrom(controllerClass)) {
                    autocompleteManager.registerMethod((Class<? extends AutocompleteInterface>) controllerClass,
                            method, autocompleteHandle);
                }
                else if (buttonHandle != null && ButtonInterface.class.isAssignableFrom(controllerClass)) {
                    buttonManager.registerMethod((Class<? extends ButtonInterface>) controllerClass,
                            method, buttonHandle);
                }
                else if (selectionHandle != null && SelectionInterface.class.isAssignableFrom(controllerClass)) {
                    selectionManager.registerMethod((Class<? extends SelectionInterface>) controllerClass,
                            method, selectionHandle, permissions);
                }
                else if (modalHandle != null && ModalInterface.class.isAssignableFrom(controllerClass)) {
                    modalManager.registerMethod((Class<? extends ModalInterface>) controllerClass, method, modalHandle);
                }
            }
            catch (RuntimeException e) {
                throw wrapWithMethod(e, method);
            }
        }

        for (BaseInterfaceFactory<? extends BaseInterface> sub : factory.getSubInterfaces()) {
            registerController(sub);
        }
    }

    public void createControllers(ApplicationManager manager, GuildContext guildContext, Guild guild) {
        for (BaseInterfaceFactory<? extends BaseInterface> factory : factories) {
            BaseInterface controller = factory.create(manager, guildContext, guild);

            guildContext.controllers.put(controller.getClass(), controller);
        }
    }

    public void finishSetup() {
        factories.forEach(factory -> factory.finishSetup(manager));
    }

    public SlashInterfaceManager getSlashManager() {
        return slashManager;
    }
    
    public AutocompleteInterfaceManager getAutocompleteManager() {
        return autocompleteManager;
    }
    
    public ButtonInterfaceManager getButtonManager() {
        return buttonManager;
    }

    public SelectionInterfaceManager getSelectionManager() {
        return selectionManager;
    }
    
    public ModalInterfaceManager getModalManager() {
        return modalManager;
    }
    
    public ApplicationManager getApplicationManager() {
        return manager;
    }

    public List<BaseInterfaceFactory<? extends BaseInterface>> getFactories() {
        return Collections.unmodifiableList(factories);
    }

    private RuntimeException wrapWithMethod(RuntimeException e, Method method) {
        RuntimeException ex = new RuntimeException("An exception occurred while building handle for " + method.toString() + ": " + e.toString());
        ex.setStackTrace(e.getStackTrace());
        return ex;
    }
}
