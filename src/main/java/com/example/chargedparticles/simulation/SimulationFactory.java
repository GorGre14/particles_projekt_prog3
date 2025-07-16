package com.example.chargedparticles.simulation;

/**
 * Factory za ustvarjanje simulacijskih instanc.
 */
public class SimulationFactory {
    
    /**
     * Ustvari simulacijo na podlagi izbranega nacina.
     * @param mode nacin simulacije
     * @return instanca simulacije
     */
    public static Simulation createSimulation(SimulationMode mode) {
        switch (mode) {
            case SEQUENTIAL:
                return new SequentialSimulation();
            case PARALLEL:
                return new ParallelSimulation();
            case DISTRIBUTED:
                throw new UnsupportedOperationException("Distributed simulation not implemented yet");
            default:
                throw new IllegalArgumentException("Unknown simulation mode: " + mode);
        }
    }
    
    /**
     * Ustvari vzporedno simulacijo z dolocenim stevilom nitk.
     * @param numThreads stevilo nitk
     * @return instanca vzporedne simulacije
     */
    public static Simulation createParallelSimulation(int numThreads) {
        return new ParallelSimulation(numThreads);
    }
}