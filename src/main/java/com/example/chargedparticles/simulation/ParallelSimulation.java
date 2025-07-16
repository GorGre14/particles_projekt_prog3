package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vzporedna implementacija simulacije naelektrenih delcev.
 * Uporablja thread pool za paralelizacijo izracuna sil med delci.
 * Avtomatsko se prilagodi strojni opremi (stevilo CPU jeder).
 */
public class ParallelSimulation implements Simulation {
    
    private final ExecutorService executor;
    private final int numThreads;
    
    /**
     * Konstruktor za vzporedno simulacijo.
     * Avtomatsko zazna stevilo CPU jeder in ustvari thread pool.
     */
    public ParallelSimulation() {
        // Avtomatsko prilagajanje strojni opremi
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        
        System.out.println("ParallelSimulation initialized with " + numThreads + " threads.");
    }
    
    /**
     * Konstruktor z moznostjo specificiranja stevila nitk.
     * @param numThreads stevilo nitk za thread pool
     */
    public ParallelSimulation(int numThreads) {
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
        
        System.out.println("ParallelSimulation initialized with " + numThreads + " threads.");
    }
    
    /**
     * Izvede en cikel vzporedne simulacije.
     * Paralelizira izracun sil med delci z uporabo thread pool-a.
     * 
     * @param particles seznam delcev
     * @param params parametri simulacije
     */
    @Override
    public void performOneCycle(List<Particle> particles, SimulationParameters params) {
        int n = particles.size();
        double[][] forces = new double[n][2];
        
        // 1. Vzporedni izracun sil za vsak delec
        CountDownLatch latch = new CountDownLatch(n);
        
        for (int i = 0; i < n; i++) {
            final int particleIndex = i;
            executor.submit(() -> {
                try {
                    calculateForceForParticle(particleIndex, particles, params, forces);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Cakamo, da se vsi izracuni sil koncajo
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Force calculation interrupted", e);
        }
        
        // 2. Sekvencno posodabljanje pozicij (da se izognemo race condition-om)
        for (int i = 0; i < n; i++) {
            updateParticlePosition(i, particles, forces);
        }
    }
    
    /**
     * Izracuna silo na dolocen delec.
     * Ta metoda se izvaja vzporedno za razlicne delce.
     * 
     * @param particleIndex indeks delca
     * @param particles seznam vseh delcev
     * @param params parametri simulacije
     * @param forces array za shranjevanje sil
     */
    private void calculateForceForParticle(int particleIndex, List<Particle> particles, 
                                         SimulationParameters params, double[][] forces) {
        Particle p1 = particles.get(particleIndex);
        double fxSum = 0.0;
        double fySum = 0.0;
        
        // Izracun sil od vseh ostalih delcev
        for (int j = 0; j < particles.size(); j++) {
            if (particleIndex == j) continue;
            
            Particle p2 = particles.get(j);
            double[] f = ForceUtils.computeParticleForce(p1, p2);
            fxSum += f[0];
            fySum += f[1];
        }
        
        // Dodamo robne sile
        double[] bf = ForceUtils.computeBoundaryForce(p1, params);
        fxSum += bf[0];
        fySum += bf[1];
        
        // Shranimo sile (thread-safe, ker vsaka nit pise v svoj indeks)
        forces[particleIndex][0] = fxSum;
        forces[particleIndex][1] = fySum;
    }
    
    /**
     * Posodobi pozicijo in hitrost delca na podlagi izracunanih sil.
     * 
     * @param particleIndex indeks delca
     * @param particles seznam delcev
     * @param forces array sil
     */
    private void updateParticlePosition(int particleIndex, List<Particle> particles, double[][] forces) {
        Particle p = particles.get(particleIndex);
        
        // Pridobimo sile
        double fx = forces[particleIndex][0];
        double fy = forces[particleIndex][1];
        
        // Izracunamo pospesek (F = ma)
        double ax = fx / p.getMass();
        double ay = fy / p.getMass();
        
        // Posodobimo hitrost z uporabo pospeska (v = v + a)
        double newVx = p.getVx() + ax;
        double newVy = p.getVy() + ay;
        
        // Posodobimo pozicijo z uporabo hitrosti (x = x + v)
        double newX = p.getX() + newVx;
        double newY = p.getY() + newVy;
        
        // Nastavimo posodobljeno hitrost in pozicijo delca
        p.setVx(newVx);
        p.setVy(newVy);
        p.setX(newX);
        p.setY(newY);
    }
    
    /**
     * Unici thread pool in sprosti resurse.
     * Klici to metodo, ko simulation ni vec potrebna.
     */
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
    
    /**
     * Vrne stevilo nitk, ki jih uporablja ta simulacija.
     * @return stevilo nitk
     */
    public int getNumThreads() {
        return numThreads;
    }
    
    @Override
    public String getDescription() {
        return "Parallel (" + numThreads + " threads)";
    }
}