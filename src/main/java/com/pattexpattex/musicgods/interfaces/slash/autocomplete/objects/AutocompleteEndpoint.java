package com.pattexpattex.musicgods.interfaces.slash.autocomplete.objects;

import com.pattexpattex.musicgods.annotations.slash.autocomplete.AutocompleteHandle;

import java.lang.reflect.Method;

public class AutocompleteEndpoint {
    
    private final String id;
    private final Class<? extends AutocompleteInterface> controller;
    private final Method method;
    
    private AutocompleteEndpoint(String id, Class<? extends AutocompleteInterface> controller, Method method) {
        this.id = id;
        this.controller = controller;
        this.method = method;
    }
    
    public String getId() {
        return id;
    }
    
    public Class<? extends AutocompleteInterface> getController() {
        return controller;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public static AutocompleteEndpoint of(Class<? extends AutocompleteInterface> controller,
                                          AutocompleteHandle handle, Method method) {
        return new AutocompleteEndpoint(handle.value(), controller, method);
    }
}
