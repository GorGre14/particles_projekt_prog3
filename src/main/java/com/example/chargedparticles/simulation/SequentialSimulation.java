package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Sekvencna simulacija: ena sama nit izracuna sile za vse delce.
 * Sluzi kot referenca za merjenje pohitritve ostalih dveh nacinov.
 */
public class SequentialSimulation extends AbstractSimulation {

    public SequentialSimulation(List<Particle> particles, SimulationParameters params) {
        super(particles, params);
    }

    @Override
    public void performOneCycle() {
        ForceKernel.computeForces(state, n, 0, n, forces, params);
        ForceKernel.integrate(state, 0, n, forces);
    }

    @Override
    public String getDescription() {
        return "Sekvencna";
    }
}
