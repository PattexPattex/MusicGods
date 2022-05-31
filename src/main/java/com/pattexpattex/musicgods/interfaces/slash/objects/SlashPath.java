package com.pattexpattex.musicgods.interfaces.slash.objects;

public class SlashPath {

    private final String[] elements;

    public SlashPath(String compiled) {
        this.elements = compiled.split("/");

        if (this.elements.length > 3)
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String element : elements)
            sb.append(element).append("/");

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
