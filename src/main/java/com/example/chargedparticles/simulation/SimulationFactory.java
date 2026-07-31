package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Factory za ustvarjanje simulacij s skupnim pomnilnikom.
 *
 * Porazdeljene simulacije tu ni, ker potrebuje rang in stevilo MPI procesov;
 * ustvari jo {@link com.example.chargedparticles.DistributedRunner}.
 */
public final class SimulationFactory {

    private SimulationFactory() {
    }

    /** Ustvari simulacijo za izbrani nacin. */
    public static Simulation createSimulation(SimulationMode mode,
                                              List<Particle> particles,
                                              SimulationParameters params) {
        switch (mode) {
            case SEQUENTIAL:
                return new SequentialSimulation(particles, params);
            case PARALLEL:
                return new ParallelSimulation(particles, params);
            default:
                throw new IllegalArgumentException("Nacin " + mode
                        + " se zazene z DistributedRunner in mpjrun.sh");
        }
    }
}
