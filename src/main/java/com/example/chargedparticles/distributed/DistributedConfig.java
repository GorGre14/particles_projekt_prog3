package com.example.chargedparticles.distributed;

/**
 * Konfiguracija za porazdeljeno simulacijo.
 * Vsebuje konstante in nastavitve za omrežno komunikacijo.
 */
public class DistributedConfig {
    
    // RMI Registry nastavitve
    public static final String RMI_REGISTRY_HOST = "localhost";
    public static final int RMI_REGISTRY_PORT = 1099;
    
    // Imena storitev v RMI registru
    public static final String WORKER_SERVICE_PREFIX = "ChargedParticlesWorker";
    public static final String MASTER_SERVICE_NAME = "ChargedParticlesMaster";
    
    // Timeout nastavitve (v milisekundah)
    public static final int WORKER_DISCOVERY_TIMEOUT = 10000; // 10 sekund
    public static final int FORCE_CALCULATION_TIMEOUT = 5000; // 5 sekund
    public static final int POSITION_UPDATE_TIMEOUT = 2000;   // 2 sekundi
    
    // Omejitve
    public static final int MAX_WORKERS = 10;
    public static final int MIN_PARTICLES_PER_WORKER = 10;
    
    /**
     * Ustvari ime storitve za delavsko vozlišče.
     * @param workerId ID delavskega vozlišča
     * @return Ime storitve za RMI register
     */
    public static String getWorkerServiceName(int workerId) {
        return WORKER_SERVICE_PREFIX + workerId;
    }
    
    /**
     * Preveri, ali je število delav smiselno za dano število delcev.
     * @param numParticles Število delcev
     * @param numWorkers Število delavskih vozlišč
     * @return true če je razporeditev smiselna
     */
    public static boolean isValidWorkerCount(int numParticles, int numWorkers) {
        if (numWorkers <= 0 || numWorkers > MAX_WORKERS) {
            return false;
        }
        return numParticles / numWorkers >= MIN_PARTICLES_PER_WORKER;
    }
    
    /**
     * Izračuna optimalno število delav za dano število delcev.
     * @param numParticles Število delcev
     * @return Priporočeno število delavskih vozlišč
     */
    public static int getRecommendedWorkerCount(int numParticles) {
        int maxWorkers = Math.min(MAX_WORKERS, numParticles / MIN_PARTICLES_PER_WORKER);
        return Math.max(1, maxWorkers);
    }
}