package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Vzporedna simulacija s skupnim pomnilnikom.
 *
 * Delce razdeli na T enako velikih odsekov, vsak odsek izracuna svoja nit iz
 * thread poola. Niti pisejo v disjunktne dele polja sil, zato zaklepanje ni
 * potrebno. Klic invokeAll deluje kot pregrada (barrier) na koncu vsakega cikla.
 */
public class ParallelSimulation extends AbstractSimulation {

    private final int numThreads;
    private final ExecutorService executor;

    /** Naloge so vedno enake, zato jih zgradimo enkrat. */
    private final List<Callable<Void>> tasks = new ArrayList<>();

    /** Privzeto uporabi vsa razpolozljiva jedra. */
    public ParallelSimulation(List<Particle> particles, SimulationParameters params) {
        this(particles, params, Runtime.getRuntime().availableProcessors());
    }

    public ParallelSimulation(List<Particle> particles, SimulationParameters params, int numThreads) {
        super(particles, params);
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);

        int chunk = (n + numThreads - 1) / numThreads;
        for (int t = 0; t < numThreads; t++) {
            final int start = Math.min(t * chunk, n);
            final int end = Math.min(start + chunk, n);
            if (start < end) {
                tasks.add(() -> {
                    ForceKernel.computeForces(state, n, start, end, forces, params);
                    return null;
                });
            }
        }
    }

    @Override
    public void performOneCycle() {
        // FAZA 1: vzporeden izracun sil; invokeAll pocaka na vse niti.
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // FAZA 2: posodobitev pozicij - O(n), zanemarljivo glede na O(n^2).
        ForceKernel.integrate(state, 0, n, forces);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getDescription() {
        return "Vzporedna (" + numThreads + " niti)";
    }
}
