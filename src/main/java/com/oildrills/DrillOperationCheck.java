package com.oildrills;

enum DrillOperationCheck {
    OK(""),
    DISABLED("Drill is turned off."),
    CHUNKS_UNLOADED("Part of the drill rig is in an unloaded chunk."),
    CORE_UNAVAILABLE("Drill core is still loading or missing."),
    STRUCTURE_INCOMPLETE("Rig structure is broken or missing."),
    NO_HOPPER("Coal hopper is missing (behind the drill)."),
    NO_CHEST("Output chest is missing (right of the drill)."),
    NO_COAL("Not enough coal in the hopper."),
    OUTPUT_FULL("Output chest is full."),
    NO_OIL("No oil reserves left in this chunk."),
    NOT_SURVEYED("Use a Dowsing Rod within 50 blocks of this oil field first."),
    NOT_READY("Drill is still warming up.");

    private final String message;

    DrillOperationCheck(String message) {
        this.message = message;
    }

    String message() {
        return message;
    }

    boolean canRun() {
        return this == OK;
    }
}
