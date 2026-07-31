package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Skupna osnova vseh treh nacinov simulacije.
 *
 * Hrani stanje delcev v ravnem double[] polju (postavitev opisana v
 * {@link ForceKernel}) in polje sil. Ker je stanje pri vseh nacinih enako,
 * se izvedbe razlikujejo samo po tem, kako razdelijo delo med izvajalce.
 */
public abstract class AbstractSimulation implements Simulation {

    /** Stevilo delcev. */
    protected final int n;

    /** Stanje vseh delcev: 6 doublov na delec. */
    protected final double[] state;

    /** Sile, globalno indeksirane: forces[i*2], forces[i*2+1]. */
    protected final double[] forces;

    /** Parametri simulacije (meje prostora, stevilo ciklov ...). */
    protected final SimulationParameters params;

    protected AbstractSimulation(List<Particle> particles, SimulationParameters params) {
        this.n = particles.size();
        this.state = ForceKernel.toFlatArray(particles);
        this.forces = new double[n * 2];
        this.params = params;
    }

    @Override
    public void syncToParticles(List<Particle> particles) {
        ForceKernel.writeBack(state, particles);
    }
}
