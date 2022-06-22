package com.pattexpattex.musicgods.interfaces.modal;

import com.pattexpattex.musicgods.annotations.modal.ModalHandle;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.modal.objects.Modal;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalInterface;
import com.pattexpattex.musicgods.interfaces.modal.objects.ModalResponseHandler;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModalInterfaceManager {

    private static final Logger log = LoggerFactory.getLogger(ModalInterfaceManager.class);
    
    private final Map<String, Modal> modals;
    private final InterfaceManagerConnector connector;
    
    public ModalInterfaceManager(InterfaceManagerConnector connector) {
        this.modals = new HashMap<>();
        this.connector = connector;
    }
    
    public Map<String, Modal> getModals() {
        return Collections.unmodifiableMap(modals);
    }
    
    public InterfaceManagerConnector getConnector() {
        return connector;
    }
    
    public void dispatch(Map<Class<? extends ModalInterface>, ModalInterface> controllers,
                         ModalInteractionEvent event, ModalResponseHandler handler) {
        String id = event.getModalId();
        Modal modal = modals.get(id);
        
        if (modal == null) {
            handler.notFound(event, id);
            log.warn("Received an unknown modal interaction: {}", id);
            return;
        }
        
        Object[] args = new Object[modal.getMethod().getParameterCount()];
        
        parseArgs(event, args);
        
        try {
            modal.getMethod().invoke(controllers.get(modal.getController()), args);
        }
        catch (InvocationTargetException e) {
            handler.modalException(event, id, e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void registerMethod(Class<? extends ModalInterface> controllerClass,
                               Method method, ModalHandle handle) {
        Parameter[] parameters = method.getParameters();
        
        if (parameters.length == 0 || !parameters[0].getType().isAssignableFrom(ModalInteractionEvent.class))
            return;
    
        for (int i = 1; i < parameters.length; i++) {
            if (!parameters[i].getType().isAssignableFrom(String.class))
                return;
        }
        
        method.setAccessible(true);
        Modal modal = Modal.of(controllerClass, handle, method);
        modals.put(modal.getMetadata().getId(), modal);
    }
    
    private void parseArgs(ModalInteractionEvent event, Object[] args) {
        args[0] = event;
        List<ModalMapping> mappings = event.getValues();
        
        for (int i = 0; i < mappings.size(); i++)
            args[i + 1] = mappings.get(i).getAsString();
    }
    
    public net.dv8tion.jda.api.interactions.components.Modal buildModal(String id) {
        Modal modal = modals.get(id);
        
        if (modal == null)
            return null;
        
        return modal.build();
    }
}
