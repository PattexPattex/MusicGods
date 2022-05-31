package com.pattexpattex.musicgods.interfaces.button.objects;

import com.pattexpattex.musicgods.annotations.ButtonHandle;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.lang.reflect.Method;

public class Button {

    public static final String DUMMY_PREFIX = "dummy:";

    private final Metadata metadata;
    private final Class<? extends ButtonInterface> controller;
    private final Method method;

    private Button(Metadata metadata, Class<? extends ButtonInterface> controller, Method method) {
        this.metadata = metadata;
        this.controller = controller;
        this.method = method;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public net.dv8tion.jda.api.interactions.components.buttons.Button build(boolean disabled) {
        return new ButtonImpl(metadata.getIdentifier(), metadata.getLabel(),
                metadata.getStyle(), metadata.getUrl(), disabled, metadata.getEmoji());
    }

    public static Button of(Class<? extends ButtonInterface> controller,
                            ButtonHandle annotation, Method method) {
        String identifier = annotation.identifier().isBlank() ? null : annotation.identifier();
        String label = annotation.label().isBlank() ? null : annotation.label();
        Emoji emoji = annotation.emoji().isBlank() ? null : Emoji.fromUnicode(annotation.emoji());
        String url = annotation.url().isBlank() ? null : annotation.url();
        ButtonStyle style = annotation.style() == null ? ButtonStyle.SECONDARY : annotation.style();

        if (identifier == null && url == null)
            throw new IllegalArgumentException("Identifier and URL cannot be null simultaneously");
        if (label == null && emoji == null)
            throw new IllegalArgumentException("Label and Emoji cannot be null simultaneously");

        return new Button(new Metadata(identifier, label, emoji, url, style), controller, method);
    }

    public static net.dv8tion.jda.api.interactions.components.buttons.Button dummy(String identifier, String label,
                                                                                   String emoji, ButtonStyle style,
                                                                                   boolean disabled) {
        return new ButtonImpl((identifier == null ? null : DUMMY_PREFIX + identifier),
                label, style, disabled, (emoji == null ? null : Emoji.fromUnicode(emoji)));
    }

    public static net.dv8tion.jda.api.interactions.components.buttons.Button dummy(String identifier,
                                                                                   String label) {
        return new ButtonImpl((identifier == null ? null : DUMMY_PREFIX + identifier), label,
                ButtonStyle.PRIMARY, false, null);
    }

    public static net.dv8tion.jda.api.interactions.components.buttons.Button url(String label, String emoji,
                                                                                 String url, boolean disabled) {
        return new ButtonImpl(null, label, ButtonStyle.LINK, url, disabled,
                (emoji == null ? null : Emoji.fromUnicode(emoji)));
    }

    public static net.dv8tion.jda.api.interactions.components.buttons.Button url(String label, String url) {
        return new ButtonImpl(null, label, ButtonStyle.LINK, url, false, null);
    }

    public static class Metadata {

        private final String identifier;
        private final String label;
        private final Emoji emoji;
        private final String url;
        private final ButtonStyle style;

        private Metadata(String identifier, String label, Emoji emoji, String url, ButtonStyle style) {
            this.identifier = identifier;
            this.label = label;
            this.emoji = emoji;
            this.url = url;
            this.style = style;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getLabel() {
            return label;
        }

        public Emoji getEmoji() {
            return emoji;
        }

        public String getUrl() {
            return url;
        }

        public ButtonStyle getStyle() {
            return style;
        }
    }
}
