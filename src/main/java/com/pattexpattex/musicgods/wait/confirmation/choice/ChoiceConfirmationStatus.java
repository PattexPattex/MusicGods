package com.pattexpattex.musicgods.wait.confirmation.choice;

public enum ChoiceConfirmationStatus {
    CONFIRMED_ONE(0),
    CONFIRMED_TWO(1),
    CONFIRMED_THREE(2),
    CONFIRMED_FOUR(3),
    CONFIRMED_FIVE(4),
    CANCELLED(-1);
    
    private final int id;
    
    ChoiceConfirmationStatus(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    static ChoiceConfirmationStatus fromComponentId(String id) {
        return switch (id.charAt(26)) {
            case '0' -> CONFIRMED_ONE;
            case '1' -> CONFIRMED_TWO;
            case '2' -> CONFIRMED_THREE;
            case '3' -> CONFIRMED_FOUR;
            case '4' -> CONFIRMED_FIVE;
            case 'c' -> CANCELLED;
            default -> throw new IllegalStateException();
        };
    }
}