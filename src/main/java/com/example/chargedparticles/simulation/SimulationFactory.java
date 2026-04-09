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
            default:
                throw new IllegalArgumentException("Neznan nacin simulacije: " + mode);
        }
    }

    /**
     * Ustvari vzporedno simulacijo z dolocenim stevilom niti.
     */
    public static Simulation createParallelSimulation(int numThreads) {
        return new ParallelSimulation(numThreads);
    }
}
