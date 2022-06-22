package com.pattexpattex.musicgods.interfaces.button;

import com.pattexpattex.musicgods.annotations.button.ButtonHandle;
import com.pattexpattex.musicgods.interfaces.InterfaceManagerConnector;
import com.pattexpattex.musicgods.interfaces.button.objects.Button;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonInterface;
import com.pattexpattex.musicgods.interfaces.button.objects.ButtonResponseHandler;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ButtonInterfaceManager {

    private static final Logger log = LoggerFactory.getLogger(ButtonInterfaceManager.class);

    private final Map<String, Button> buttons;
    private final InterfaceManagerConnector connector;

    public ButtonInterfaceManager(InterfaceManagerConnector connector) {
        this.buttons = new HashMap<>();
        this.connector = connector;
    }

    public Map<String, Button> getButtons() {
        return Collections.unmodifiableMap(buttons);
    }

    public InterfaceManagerConnector getConnector() {
        return connector;
    }

    public void dispatch(Map<Class<? extends ButtonInterface>, ButtonInterface> controllers,
                         ButtonInteractionEvent event, ButtonResponseHandler handler) {
        String name = event.getComponentId();
        Button button = buttons.get(name);

        if (name.startsWith(Button.DUMMY_PREFIX))
            return;

        if (button == null) {
            handler.notFound(event, name);
            log.warn("Received an unknown button interaction: {}", name);
            return;
        }

        try {
            button.getMethod().invoke(controllers.get(button.getController()), event);
        }
        catch (InvocationTargetException e) {
            handler.buttonException(event, button.getMetadata().getIdentifier(), e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerMethod(Class<? extends ButtonInterface> controller,
                               Method method, ButtonHandle annotation) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0 || !parameters[0].getType().isAssignableFrom(ButtonInteractionEvent.class))
            return;

        method.setAccessible(true);

        Button button = Button.of(controller, annotation, method);
        buttons.put(button.getMetadata().getIdentifier(), button);
    }

    public net.dv8tion.jda.api.interactions.components.buttons.Button buildButton(String identifier, boolean disabled) {
        return buttons.get(identifier).build(disabled);
    }

}
