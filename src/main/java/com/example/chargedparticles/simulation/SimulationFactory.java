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
                // Uporablja privzeto število delavskih vozlišč
                return new DistributedSimulation(2);
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
    
    /**
     * Ustvari porazdeljeno simulacijo z določenim številom delavskih vozlišč.
     * @param expectedWorkers število pričakovanih delavskih vozlišč
     * @return instanca porazdeljene simulacije
     */
    public static Simulation createDistributedSimulation(int expectedWorkers) {
        return new DistributedSimulation(expectedWorkers);
    }
}