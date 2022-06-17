package com.pattexpattex.musicgods.wait.confirmation;

public enum ConfirmationStatus {
    CONFIRMED,
    DENIED,
    CANCELLED;
    
    static ConfirmationStatus fromComponentId(String id) {
        if (id.contains("yes"))
            return CONFIRMED;
        else if (id.contains("no"))
            return DENIED;
        else
            return CANCELLED;
    }
}
