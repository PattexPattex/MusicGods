package com.pattexpattex.musicgods.exceptions;

import com.pattexpattex.musicgods.interfaces.slash.objects.ParameterType;

public class WrongArgumentException extends IllegalArgumentException {

    private final ParameterType type;
    private final String value;

    public WrongArgumentException(String message, ParameterType type, String value) {
        super(message);
        this.type = type;
        this.value = value;
    }

    public ParameterType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
