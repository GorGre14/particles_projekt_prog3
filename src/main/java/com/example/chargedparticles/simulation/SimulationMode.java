package com.example.chargedparticles.simulation;

/**
 * Nacini izvajanja simulacije.
 */
public enum SimulationMode {

    SEQUENTIAL("sequential", "Sekvencna"),
    PARALLEL("parallel", "Vzporedna (niti)"),
    DISTRIBUTED("distributed", "Porazdeljena (MPI)");

    private final String commandLineArg;
    private final String description;

    SimulationMode(String commandLineArg, String description) {
        this.commandLineArg = commandLineArg;
        this.description = description;
    }

    public String getCommandLineArg() {
        return commandLineArg;
    }

    public String getDescription() {
        return description;
    }

    /** Vrne nacin za argument ukazne vrstice ali null, ce ga ni. */
    public static SimulationMode fromCommandLineArg(String arg) {
        for (SimulationMode mode : values()) {
            if (mode.commandLineArg.equalsIgnoreCase(arg)) {
                return mode;
            }
        }
        return null;
    }

    /** Opis se uporabi tudi kot besedilo v spustnem seznamu vmesnika. */
    @Override
    public String toString() {
        return description;
    }
}
