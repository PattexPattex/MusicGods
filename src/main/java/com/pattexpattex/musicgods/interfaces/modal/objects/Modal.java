package com.pattexpattex.musicgods.interfaces.modal.objects;

import com.pattexpattex.musicgods.annotations.modal.Element;
import com.pattexpattex.musicgods.annotations.modal.ModalHandle;
import com.pattexpattex.musicgods.annotations.slash.parameter.Range;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.internal.interactions.modal.ModalImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Modal {
    
    public static final String DEF_VALUE = "";
    public static final String DEF_PLACEHOLDER = "";
    public static final boolean DEF_REQUIRED = true;
    public static final TextInputStyle DEF_STYLE = TextInputStyle.SHORT;
    
    private final Metadata metadata;
    private final Class<? extends ModalInterface> controller;
    private final Method method;
    
    private Modal(Metadata metadata, Class<? extends ModalInterface> controller, Method method) {
        this.metadata = metadata;
        this.controller = controller;
        this.method = method;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    
    public Class<? extends ModalInterface> getController() {
        return controller;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public net.dv8tion.jda.api.interactions.modals.Modal build() {
        return new ModalImpl(metadata.id, metadata.title, metadata.actionRows);
    }
    
    public static Modal of(Class<? extends ModalInterface> controller,
                           ModalHandle handle, Method method) {
        String id = handle.id();
        String title = handle.title();
        List<ActionRow> list = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        
        for (Parameter parameter : parameters) {
            if (parameter.getType().isAssignableFrom(ModalInteractionEvent.class))
                continue;
            
            Element element = parameter.getAnnotation(Element.class);
            Range range = parameter.getAnnotation(Range.class);
            TextInput.Builder input = TextInput.create(id + "." + parameter.getName(), parameter.getName(), TextInputStyle.SHORT);
            
            if (element != null) {
                String prefill = element.prefill();
                String placeholder = element.placeholder();
                
                if (!prefill.isBlank())
                    input.setValue(prefill);
                
                if (!placeholder.isBlank())
                    input.setPlaceholder(placeholder);
                
                input.setLabel(element.label())
                        .setRequired(element.required())
                        .setStyle(element.style());
            }
            if (range != null) {
                input.setRequiredRange((int) range.min(), (int) range.max());
            }
            
            list.add(ActionRow.of(input.build()));
        }
        
        return new Modal(new Metadata(id, title, list), controller, method);
    }
    
    public static class Metadata {
    
        private final String id;
    
        private final String title;
        private final List<ActionRow> actionRows;
        private Metadata(String id, String title, List<ActionRow> actionRows) {
            this.id = id;
            this.title = title;
            this.actionRows = actionRows;
        }
    
        public String getId() {
            return id;
        }
    
        public String getTitle() {
            return title;
        }
    
        public List<ActionRow> getActionRows() {
            
            return actionRows;
        }
    
    }
}
