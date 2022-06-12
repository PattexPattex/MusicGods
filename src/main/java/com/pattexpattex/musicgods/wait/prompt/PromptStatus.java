package com.pattexpattex.musicgods.wait.prompt;

public enum PromptStatus {
    ACCEPT,
    REJECT,
    CANCEL;
    
    
    static PromptStatus fromComponentId(String id) {
        if (id.contains("yes"))
            return ACCEPT;
        else if (id.contains("no"))
            return REJECT;
        else
            return CANCEL;
    }
}
