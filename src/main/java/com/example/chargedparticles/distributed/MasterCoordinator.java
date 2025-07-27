package com.example.chargedparticles.distributed;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SimulationParameters;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Koordinator za master vozlišče v porazdeljeni simulaciji.
 * Odgovoren je za odkrivanje delavskih vozlišč, razdelitev delcev
 * in sinhronizacijo simulacijskih ciklov.
 */
public class MasterCoordinator {
    
    private final List<WorkerNode> workers = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final int expectedWorkers;
    private boolean initialized = false;
    
    public MasterCoordinator(int expectedWorkers) {
        this.expectedWorkers = expectedWorkers;
    }
    
    /**
     * Inicializira master in išče delavska vozlišča.
     * @return true če je inicializacija uspešna
     */
    public boolean initializeMaster() {
        try {
            System.out.println("Iščem " + expectedWorkers + " delavskih vozlišč...");
            
            // Pridobi RMI registry
            Registry registry = LocateRegistry.getRegistry(
                DistributedConfig.RMI_REGISTRY_HOST, 
                DistributedConfig.RMI_REGISTRY_PORT
            );
            
            // Poizkusi najti delavska vozlišča
            long startTime = System.currentTimeMillis();
            while (workers.size() < expectedWorkers) {
                for (int i = 0; i < DistributedConfig.MAX_WORKERS && workers.size() < expectedWorkers; i++) {
                    String serviceName = DistributedConfig.getWorkerServiceName(i);
                    try {
                        WorkerNode worker = (WorkerNode) registry.lookup(serviceName);
                        if (worker.isAlive() && !workers.contains(worker)) {
                            workers.add(worker);
                            System.out.println("Najdeno delavsko vozlišče: " + serviceName);
                        }
                    } catch (NotBoundException e) {
                        // Vozlišče še ni registrirano, nadaljuj iskanje
                    }
                }
                
                // Preveri timeout
                if (System.currentTimeMillis() - startTime > DistributedConfig.WORKER_DISCOVERY_TIMEOUT) {
                    System.err.println("Timeout pri iskanju vozlišč. Najdenih: " + workers.size() + "/" + expectedWorkers);
                    break;
                }
                
                if (workers.size() < expectedWorkers) {
                    Thread.sleep(1000); // Počakaj sekundo pred ponovnim iskanjem
                }
            }
            
            if (workers.size() == 0) {
                System.err.println("Nobeno delavsko vozlišče ni bilo najdeno!");
                return false;
            }
            
            System.out.println("Povezan z " + workers.size() + " delavskimi vozlišči");
            initialized = true;
            return true;
            
        } catch (Exception e) {
            System.err.println("Napaka pri inicializaciji master vozlišča: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean workersInitialized = false;
    
    /**
     * Inicializiraj delavska vozlišča za novo simulacijo.
     * @param particles Seznam vseh delcev
     * @param params Parametri simulacije
     * @throws Exception če pride do napake
     */
    public void initializeForSimulation(List<Particle> particles, SimulationParameters params) throws Exception {
        if (!initialized || workers.isEmpty()) {
            throw new IllegalStateException("Master ni inicializiran ali ni povezanih vozlišč");
        }
        
        // Pretvori Particle objekte v ParticleState za prenos po omrežju
        List<ParticleState> particleStates = convertToParticleStates(particles);
        
        // Razdeli delce med delavska vozlišča
        List<List<ParticleState>> partitions = partitionParticles(particleStates, workers.size());
        
        // Inicializiraj delavska vozlišča
        initializeWorkers(partitions, particleStates, params);
        
        workersInitialized = true;
    }
    
    /**
     * Izvede en cikel porazdeljene simulacije.
     * @param particles Seznam vseh delcev
     * @param params Parametri simulacije
     * @throws Exception če pride do napake
     */
    public void performDistributedCycle(List<Particle> particles, SimulationParameters params) throws Exception {
        if (!initialized || workers.isEmpty()) {
            throw new IllegalStateException("Master ni inicializiran ali ni povezanih vozlišč");
        }
        
        // Če vozlišča še niso inicializirana za to simulacijo, jih inicializiraj
        if (!workersInitialized) {
            initializeForSimulation(particles, params);
        }
        
        // 1. Pretvori Particle objekte v ParticleState za prenos po omrežju
        List<ParticleState> particleStates = convertToParticleStates(particles);
        
        // 2. Izvedi kombinirani izračun sil in posodobitev pozicij v enem klicu
        List<List<ParticleState>> finalPartitions = calculateAndUpdateParallel(particleStates);
        
        // 3. Združi končne rezultate
        List<ParticleState> finalStates = new ArrayList<>();
        for (List<ParticleState> partition : finalPartitions) {
            finalStates.addAll(partition);
        }
        
        // 4. Posodobi originalne Particle objekte
        updateOriginalParticles(particles, finalStates);
    }
    
    /**
     * Pretvori Particle objekte v ParticleState za omrežno komunikacijo.
     */
    private List<ParticleState> convertToParticleStates(List<Particle> particles) {
        List<ParticleState> states = new ArrayList<>();
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            ParticleState state = new ParticleState(i, p.getX(), p.getY(), p.getVx(), 
                                                  p.getVy(), p.getCharge(), p.getMass());
            states.add(state);
        }
        return states;
    }
    
    /**
     * Razdeli delce med delavska vozlišča.
     */
    private List<List<ParticleState>> partitionParticles(List<ParticleState> particles, int numWorkers) {
        List<List<ParticleState>> partitions = new ArrayList<>();
        int particlesPerWorker = particles.size() / numWorkers;
        int remainder = particles.size() % numWorkers;
        
        int startIndex = 0;
        for (int i = 0; i < numWorkers; i++) {
            int endIndex = startIndex + particlesPerWorker + (i < remainder ? 1 : 0);
            List<ParticleState> partition = particles.subList(startIndex, Math.min(endIndex, particles.size()));
            partitions.add(new ArrayList<>(partition));
            startIndex = endIndex;
        }
        
        return partitions;
    }
    
    /**
     * Inicializiraj vsa delavska vozlišča s svojimi delci.
     */
    private void initializeWorkers(List<List<ParticleState>> partitions, 
                                  List<ParticleState> allParticles, 
                                  SimulationParameters params) throws Exception {
        
        CountDownLatch latch = new CountDownLatch(workers.size());
        
        for (int i = 0; i < workers.size() && i < partitions.size(); i++) {
            final int workerIndex = i;
            executor.submit(() -> {
                try {
                    // Najprej ponastavi vozlišče za novo simulacijo
                    workers.get(workerIndex).reset();
                    // Nato ga inicializiraj z novimi delci
                    workers.get(workerIndex).initialize(partitions.get(workerIndex), allParticles, params);
                } catch (RemoteException e) {
                    System.err.println("Napaka pri inicializaciji vozlišča " + workerIndex + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        if (!latch.await(DistributedConfig.WORKER_DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Timeout pri inicializaciji delavskih vozlišč");
        }
    }
    
    /**
     * Izračunaj sile paralelno na vseh vozliščih.
     */
    private List<List<ParticleState>> calculateForcesParallel(List<ParticleState> allParticles) throws Exception {
        List<Future<List<ParticleState>>> futures = new ArrayList<>();
        
        // Pošlji zahteve za izračun sil vsem vozliščem
        for (WorkerNode worker : workers) {
            Future<List<ParticleState>> future = executor.submit(() -> {
                try {
                    return worker.calculateForces(allParticles);
                } catch (RemoteException e) {
                    throw new RuntimeException("Napaka pri računanju sil: " + e.getMessage(), e);
                }
            });
            futures.add(future);
        }
        
        // Počakaj na rezultate
        List<List<ParticleState>> results = new ArrayList<>();
        for (Future<List<ParticleState>> future : futures) {
            try {
                List<ParticleState> result = future.get(DistributedConfig.FORCE_CALCULATION_TIMEOUT, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                throw new TimeoutException("Timeout pri računanju sil na delavskem vozlišču");
            }
        }
        
        return results;
    }
    
    /**
     * Kombinirani izračun sil in posodobitev pozicij v enem klicu.
     * To zmanjša število omrežnih klicev na polovico.
     */
    private List<List<ParticleState>> calculateAndUpdateParallel(List<ParticleState> allParticles) throws Exception {
        List<Future<List<ParticleState>>> futures = new ArrayList<>();
        
        // Pošlji zahteve za kombinirani izračun vsem vozliščem
        for (WorkerNode worker : workers) {
            Future<List<ParticleState>> future = executor.submit(() -> {
                try {
                    return worker.calculateAndUpdate(allParticles);
                } catch (RemoteException e) {
                    throw new RuntimeException("Napaka pri kombiniranem izračunu: " + e.getMessage(), e);
                }
            });
            futures.add(future);
        }
        
        // Počakaj na rezultate
        List<List<ParticleState>> results = new ArrayList<>();
        for (Future<List<ParticleState>> future : futures) {
            try {
                List<ParticleState> result = future.get(DistributedConfig.FORCE_CALCULATION_TIMEOUT, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                throw new TimeoutException("Timeout pri kombiniranem izračunu na delavskem vozlišču");
            }
        }
        
        return results;
    }
    
    /**
     * Posodobi pozicije paralelno na vseh vozliščih.
     */
    private List<List<ParticleState>> updatePositionsParallel() throws Exception {
        List<Future<List<ParticleState>>> futures = new ArrayList<>();
        
        // Pošlji zahteve za posodobitev pozicij
        for (WorkerNode worker : workers) {
            Future<List<ParticleState>> future = executor.submit(() -> {
                try {
                    return worker.updatePositions();
                } catch (RemoteException e) {
                    throw new RuntimeException("Napaka pri posodabljanju pozicij: " + e.getMessage(), e);
                }
            });
            futures.add(future);
        }
        
        // Počakaj na rezultate
        List<List<ParticleState>> results = new ArrayList<>();
        for (Future<List<ParticleState>> future : futures) {
            try {
                List<ParticleState> result = future.get(DistributedConfig.POSITION_UPDATE_TIMEOUT, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                throw new TimeoutException("Timeout pri posodabljanju pozicij na delavskem vozlišču");
            }
        }
        
        return results;
    }
    
    /**
     * Posodobi originalne Particle objekte z rezultati simulacije.
     */
    private void updateOriginalParticles(List<Particle> originalParticles, List<ParticleState> finalStates) {
        // Ustvari mapo za hitrejše iskanje po ID-jih
        for (ParticleState state : finalStates) {
            if (state.getId() < originalParticles.size()) {
                Particle p = originalParticles.get(state.getId());
                p.setX(state.getX());
                p.setY(state.getY());
                p.setVx(state.getVx());
                p.setVy(state.getVy());
            }
        }
    }
    
    /**
     * Zaustaviti vsa delavska vozlišča.
     */
    public void shutdown() {
        for (WorkerNode worker : workers) {
            try {
                worker.shutdown();
            } catch (RemoteException e) {
                System.err.println("Napaka pri zaustavitvi vozlišča: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        workers.clear();
        initialized = false;
    }
    
    /**
     * Vrne število povezanih delavskih vozlišč.
     */
    public int getConnectedWorkerCount() {
        return workers.size();
    }
    
    /**
     * Preveri, ali so vsa vozlišča še vedno aktivna.
     */
    public boolean areWorkersAlive() {
        for (WorkerNode worker : workers) {
            try {
                if (!worker.isAlive()) {
                    return false;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Ponastavi stanje koordinatorja za novo simulacijo.
     */
    public void resetForNewSimulation() {
        workersInitialized = false;
    }
}