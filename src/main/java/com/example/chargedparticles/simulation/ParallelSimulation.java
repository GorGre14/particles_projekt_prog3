package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;
import java.util.concurrent.*;

/**
 * Vzporedna implementacija simulacije naelektrenih delcev.
 * Uporablja thread pool za paralelizacijo izracuna sil med delci.
 * Avtomatsko se prilagodi strojni opremi (stevilo CPU jeder).
 *
 * Algoritem:
 * 1. Razdelimo delce v kose (chunks) - vsaka nit dobi en kos
 * 2. Vsaka nit izracuna sile za svoje delce (proti vsem ostalim) - O(n^2 / numThreads)
 * 3. Ko so vse sile izracunane, sekvencno posodobimo pozicije - O(n)
 */
public class ParallelSimulation implements Simulation {

    private final ExecutorService executor;
    private final int numThreads;

    /**
     * Konstruktor - avtomatsko zazna stevilo CPU jeder.
     */
    public ParallelSimulation() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Vzporedna simulacija inicializirana z " + numThreads + " nitmi.");
    }

    /**
     * Konstruktor z dolocenim stevilom niti.
     */
    public ParallelSimulation(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Vzporedna simulacija inicializirana z " + numThreads + " nitmi.");
    }

    @Override
    public void performOneCycle(List<Particle> particles, SimulationParameters params) {
        int n = particles.size();
        double[][] forces = new double[n][2];

        // === FAZA 1: Vzporedni izracun sil ===
        // Razdelimo delce v kose za vsako nit
        int chunkSize = (n + numThreads - 1) / numThreads;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunkSize;
            final int end = Math.min(start + chunkSize, n);

            // Ce je kos prazen (vec niti kot delcev), samo zmanjsamo latch
            if (start >= n) {
                latch.countDown();
                continue;
            }

            executor.submit(() -> {
                try {
                    // Vsaka nit izracuna sile za svoj kos delcev
                    for (int i = start; i < end; i++) {
                        Particle p1 = particles.get(i);
                        double fxSum = 0.0;
                        double fySum = 0.0;

                        // Sile od vseh ostalih delcev
                        for (int j = 0; j < n; j++) {
                            if (i == j) continue;
                            Particle p2 = particles.get(j);
                            double[] f = ForceUtils.computeParticleForce(p1, p2);
                            fxSum += f[0];
                            fySum += f[1];
                        }

                        // Mejna sila
                        double[] bf = ForceUtils.computeBoundaryForce(p1, params);
                        fxSum += bf[0];
                        fySum += bf[1];

                        // Vsaka nit pise v svoj indeks - ni potrebe po sinhronizaciji
                        forces[i][0] = fxSum;
                        forces[i][1] = fySum;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Pocakamo da vse niti koncajo izracun sil
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // === FAZA 2: Sekvencna posodobitev pozicij ===
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);

            double fx = forces[i][0];
            double fy = forces[i][1];

            // Pospesek: a = F / m
            double ax = fx / p.getMass();
            double ay = fy / p.getMass();

            // Nova hitrost: v = v + a
            double newVx = p.getVx() + ax;
            double newVy = p.getVy() + ay;

            // Nova pozicija: x = x + v
            double newX = p.getX() + newVx;
            double newY = p.getY() + newVy;

            // Posodobimo delec
            p.setVx(newVx);
            p.setVy(newVy);
            p.setX(newX);
            p.setY(newY);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getDescription() {
        return "Vzporedna (" + numThreads + " niti)";
    }
}
