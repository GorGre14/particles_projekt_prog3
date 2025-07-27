package com.example.chargedparticles.simulation;

import java.io.Serializable;

/**
 * Enumeracija za razlicne nacine simulacije.
 */
public enum SimulationMode implements Serializable {
    SEQUENTIAL("sequential", "Sequential simulation"),
    PARALLEL("parallel", "Parallel simulation"),
    DISTRIBUTED("distributed", "Distributed simulation");
    
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
     * @param arg command line argument
     * @return SimulationMode ali null, ce ni najden
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