package com.pattexpattex.musicgods.interfaces.slash.autocomplete;

import com.pattexpattex.musicgods.annotations.slash.autocomplete.AutocompleteHandle;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteEndpoint;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteInterface;
import com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects.AutocompleteResponseHandler;
import com.pattexpattex.musicgods.interfaces.slash.objects.SlashPath;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AutocompleteInterfaceManager {
    
    private static final Logger log = LoggerFactory.getLogger(AutocompleteInterfaceManager.class);
    
    private final Map<String, AutocompleteEndpoint> endpoints;
    private final InterfaceManagerConnector connector;
    
    public AutocompleteInterfaceManager(InterfaceManagerConnector connector) {
        this.endpoints = new HashMap<>();
        this.connector = connector;
    }
    
    public Map<String, AutocompleteEndpoint> getEndpoints() {
        return Collections.unmodifiableMap(endpoints);
    }
    
    public InterfaceManagerConnector getConnector() {
        return connector;
    }
    
    public void dispatch(Map<Class<? extends AutocompleteInterface>, AutocompleteInterface> controllers,
                         CommandAutoCompleteInteractionEvent event, AutocompleteResponseHandler handler) {
        AutoCompleteQuery query = event.getFocusedOption();
        SlashPath path = new SlashPath(event.getCommandPath() + "/" + query.getName());
        AutocompleteEndpoint endpoint = endpoints.get(path.toString());
        
        if (endpoint == null) {
            handler.notFound(event, path.toString());
            log.warn("Received an unknown autocomplete interaction: {}", path);
            return;
        }
        
        try {
            endpoint.getMethod().invoke(controllers.get(endpoint.getController()), event, query);
        }
        catch (InvocationTargetException e) {
            handler.autocompleteException(event, endpoint.getId(), e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void registerMethod(Class<? extends AutocompleteInterface> controller,
                               Method method, AutocompleteHandle handle) {
        Parameter[] parameters = method.getParameters();
        
        if (parameters.length != 2
                || !parameters[0].getType().isAssignableFrom(CommandAutoCompleteInteractionEvent.class)
                || !parameters[1].getType().isAssignableFrom(AutoCompleteQuery.class))
            return;
        
        method.setAccessible(true);
        
        AutocompleteEndpoint endpoint = AutocompleteEndpoint.of(controller, handle, method);
        endpoints.put(endpoint.getId(), endpoint);
    }
}
