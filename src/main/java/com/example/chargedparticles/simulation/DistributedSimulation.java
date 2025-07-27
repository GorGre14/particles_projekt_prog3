package com.example.chargedparticles.simulation;

import com.example.chargedparticles.distributed.MasterCoordinator;
import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Implementacija porazdeljene simulacije naelektrenih delcev.
 * Uporablja več računalnikov ali procesov za izračun sil med delci.
 * Master vozlišče koordinira delavska vozlišča preko RMI komunikacije.
 */
public class DistributedSimulation implements Simulation {
    
    private final int expectedWorkers;
    private MasterCoordinator coordinator;
    private boolean initialized = false;
    private SequentialSimulation fallbackSimulation;
    
    public DistributedSimulation(int expectedWorkers) {
        this.expectedWorkers = expectedWorkers;
        this.coordinator = new MasterCoordinator(expectedWorkers);
        this.fallbackSimulation = new SequentialSimulation();
    }
    
    /**
     * Inicializira porazdeljeno simulacijo z iskanjem delavskih vozlišč.
     * @return true če je inicializacija uspešna, false sicer
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }
        
        System.out.println("Inicializacija porazdeljene simulacije...");
        
        try {
            boolean success = coordinator.initializeMaster();
            if (success) {
                initialized = true;
                System.out.println("Porazdeljena simulacija uspešno inicializirana z " + 
                                 coordinator.getConnectedWorkerCount() + " vozlišči");
                return true;
            } else {
                System.err.println("Napaka pri inicializaciji. Preklapljanje na zaporedno simulacijo.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Napaka pri inicializaciji porazdeljene simulacije: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void performOneCycle(List<Particle> particles, SimulationParameters params) {
        // Če porazdeljena simulacija ni inicializirana ali ni delavskih vozlišč,
        // uporabi zaporedno simulacijo kot varnostni ukrep
        if (!initialized || coordinator.getConnectedWorkerCount() == 0) {
            System.err.println("Porazdeljena simulacija ni na voljo. Uporabljam zaporedno simulacijo.");
            fallbackSimulation.performOneCycle(particles, params);
            return;
        }
        
        try {
            // Preveri, ali so delavska vozlišča še vedno aktivna
            if (!coordinator.areWorkersAlive()) {
                System.err.println("Delavska vozlišča niso več aktivna. Preklapljam na zaporedno simulacijo.");
                initialized = false;
                fallbackSimulation.performOneCycle(particles, params);
                return;
            }
            
            // Izvedi porazdeljeni cikel simulacije
            coordinator.performDistributedCycle(particles, params);
            
        } catch (Exception e) {
            System.err.println("Napaka v porazdeljeni simulaciji: " + e.getMessage());
            System.err.println("Preklapljam na zaporedno simulacijo za ta cikel");
            
            // V primeru napake uporabi zaporedno simulacijo
            fallbackSimulation.performOneCycle(particles, params);
            
            // Označi kot neinicializirano, da se poizkusi ponovno povezati
            initialized = false;
        }
    }
    
    @Override
    public void shutdown() {
        // Pri distributed simulaciji ne zaustavljamo delavskih vozlišč,
        // ker jih želimo ohraniti za naslednje simulacije.
        // Samo označimo, da ta simulacija ni več inicializirana.
        if (coordinator != null) {
            coordinator.resetForNewSimulation();
        }
        if (fallbackSimulation != null) {
            fallbackSimulation.shutdown();
        }
        initialized = false;
        System.out.println("Porazdeljena simulacija končana (delavska vozlišča ostajajo aktivna)");
    }
    
    @Override
    public String getDescription() {
        if (!initialized) {
            return String.format("Porazdeljena (%d vozlišč pričakovanih, ne inicializirano)", expectedWorkers);
        }
        
        int connectedWorkers = coordinator.getConnectedWorkerCount();
        return String.format("Porazdeljena (%d/%d vozlišč)", connectedWorkers, expectedWorkers);
    }
    
    /**
     * Vrne ali je simulacija uspešno inicializirana.
     * @return true če je inicializirana
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Vrne število povezanih delavskih vozlišč.
     * @return število vozlišč
     */
    public int getConnectedWorkerCount() {
        return coordinator != null ? coordinator.getConnectedWorkerCount() : 0;
    }
    
    /**
     * Vrne število pričakovanih delavskih vozlišč.
     * @return pričakovano število vozlišč
     */
    public int getExpectedWorkerCount() {
        return expectedWorkers;
    }
    
    /**
     * Poskusi ponovno inicializirati povezavo z delavskimi vozlišči.
     * @return true če je ponovno povezovanje uspešno
     */
    public boolean reconnect() {
        if (initialized) {
            shutdown();
        }
        
        this.coordinator = new MasterCoordinator(expectedWorkers);
        return initialize();
    }
    
    /**
     * Ponastavi simulacijo za nov zagon.
     * To se kliče, ko uporabnik spremeni parametre v GUI.
     */
    public void resetForNewSimulation() {
        if (coordinator != null) {
            coordinator.resetForNewSimulation();
        }
    }
    
    /**
     * Popolnoma zaustavi distribuirani sistem vključno z vsemi delavskimi vozlišči.
     * To se kliče samo ob izhodu iz aplikacije.
     */
    public void shutdownCompletely() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
        if (fallbackSimulation != null) {
            fallbackSimulation.shutdown();
        }
        initialized = false;
        System.out.println("Porazdeljena simulacija in vsa delavska vozlišča zaustavljena");
    }
}