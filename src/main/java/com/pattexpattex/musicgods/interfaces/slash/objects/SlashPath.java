package com.pattexpattex.musicgods.interfaces.slash.objects;

import java.util.StringJoiner;

public class SlashPath {

    private final String[] elements;

    public SlashPath(String compiled) {
        this.elements = compiled.split("/");

        if (this.elements.length > 4)
            throw new IllegalArgumentException(compiled + " is not a valid path");
    }

    public String getBase() {
        return elements[0];
    }

    public String getElement(int position) {
        return elements[position];
    }

    public boolean isSubcommand() {
        return elements.length == 2;
    }

    public boolean isSubcommandGroup() {
        return elements.length == 3;
    }
    
    public boolean isCommandOption() {
        return elements.length == 4;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("/");
        
        for (String element : elements)
            joiner.add(element);

        return joiner.toString();
    }
}
