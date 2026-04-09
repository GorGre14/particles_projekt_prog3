package com.example.chargedparticles.simulation;

/**
 * Enumeracija za razlicne nacine simulacije.
 */
public enum SimulationMode {
    SEQUENTIAL("sequential", "Sekvencna simulacija"),
    PARALLEL("parallel", "Vzporedna simulacija"),
    DISTRIBUTED("distributed", "Porazdeljena simulacija (MPJ)");

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

    /**
     * Najde SimulationMode na podlagi command line argumenta.
     */
    public static SimulationMode fromCommandLineArg(String arg) {
        for (SimulationMode mode : values()) {
            if (mode.commandLineArg.equalsIgnoreCase(arg)) {
                return mode;
            }
        }
        return null;
    }
}