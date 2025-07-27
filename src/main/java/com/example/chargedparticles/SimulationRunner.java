package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.*;
import com.example.chargedparticles.distributed.*;
import com.example.chargedparticles.ui.SimulationUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * SimulationRunner class.
 * Inicializira parametre, ustvari UI in zažene simulacijo.
 * Omogoca dinamicni ponovni zagon simulacije preko UI.
 */
public class SimulationRunner {
    public static Thread simulationThread;
    public static Simulation simulation;
    public static List<Particle> particles;
    public static SimulationParameters params;
    public static int expectedWorkers = 2;
    public static final long RANDOM_SEED = 42L; // Deterministic seed for reproducible results

    /**
     * Metoda za ponovno zagon simulacije z uporabo trenutnih parametrov.
     */
    public static void restartSimulation() {
        // Če simulacijska nit teče, jo prekinemo
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Shutdown old simulation to free resources
        if (simulation != null) {
            simulation.shutdown();
        }
        
        // Recreate simulation based on current mode
        SimulationMode currentMode = params.getSimulationMode();
        if (currentMode == SimulationMode.DISTRIBUTED) {
            simulation = SimulationFactory.createDistributedSimulation(expectedWorkers);
            if (simulation instanceof DistributedSimulation) {
                ((DistributedSimulation) simulation).initialize();
            }
        } else {
            simulation = SimulationFactory.createSimulation(currentMode);
        }

        simulationThread = new Thread(() -> {
            long startTime = System.nanoTime();
            final int finalNumCycles = params.getNumCycles();
            for (int cycle = 0; cycle < finalNumCycles; cycle++) {
                // Preverimo, ce je nit prekinjena
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                simulation.performOneCycle(particles, params);
                // Only sleep if UI is enabled for visualization
                if (params.isEnableUI()) {
                    try {
                        Thread.sleep(10); // upocasnitev za boljso vizualizacijo
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            long endTime = System.nanoTime();
            double elapsedSeconds = (endTime - startTime) / 1e9;
            System.out.println("Simulation completed (" + simulation.getDescription() + "):");
            System.out.printf(" - Particles: %d%n", params.getNumParticles());
            System.out.printf(" - Cycles: %d%n", finalNumCycles);
            System.out.printf(" - Elapsed time: %.3f s%n", elapsedSeconds);

            for (int i = 0; i < Math.min(5, particles.size()); i++) {
                System.out.println("Particle " + i + ": " + particles.get(i));
            }
        });
        simulationThread.start();
    }

    public static void main(String[] args) {
        // Privzete vrednosti
        boolean enableUI = true;
        int windowW = 800;
        int windowH = 600;
        int numParticles = 400;
        int numCycles = 1000;
        double minX = 0.0, maxX = 800.0;
        double minY = 0.0, maxY = 600.0;
        int fps = 60;
        SimulationMode simulationMode = SimulationMode.SEQUENTIAL;
        
        // Distributed configuration
        String nodeRole = "master"; // master or worker

        // Parsamo argumente iz terminala
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--ui":
                    enableUI = Boolean.parseBoolean(args[++i]);
                    break;
                case "--window":
                    windowW = Integer.parseInt(args[++i]);
                    windowH = Integer.parseInt(args[++i]);
                    break;
                case "--particles":
                    numParticles = Integer.parseInt(args[++i]);
                    break;
                case "--cycles":
                    numCycles = Integer.parseInt(args[++i]);
                    break;
                case "--bounds":
                    minX = Double.parseDouble(args[++i]);
                    maxX = Double.parseDouble(args[++i]);
                    minY = Double.parseDouble(args[++i]);
                    maxY = Double.parseDouble(args[++i]);
                    break;
                case "--fps":
                    fps = Integer.parseInt(args[++i]);
                    break;
                case "--mode":
                    String modeStr = args[++i];
                    SimulationMode mode = SimulationMode.fromCommandLineArg(modeStr);
                    if (mode != null) {
                        simulationMode = mode;
                    } else {
                        System.err.println("Unknown simulation mode: " + modeStr);
                        System.err.println("Available modes: sequential, parallel, distributed");
                        System.exit(1);
                    }
                    break;
                // Registry host and port are handled by DistributedConfig constants
                case "--workers":
                    expectedWorkers = Integer.parseInt(args[++i]);
                    break;
                case "--role":
                    nodeRole = args[++i];
                    if (!nodeRole.equals("master") && !nodeRole.equals("worker")) {
                        System.err.println("Invalid role: " + nodeRole + ". Must be 'master' or 'worker'");
                        System.exit(1);
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
                    break;
            }
        }

        // Če je worker mode, zaženemo worker node namesto simulacije
        if (nodeRole.equals("worker")) {
            startWorkerNode();
            return; // Worker node ne potrebuje UI ali simulacije
        }

        // Inicializacija parametrov, delcev in simulacije (master mode)
        params = new SimulationParameters(
                enableUI,
                windowW, windowH,
                numParticles, numCycles,
                minX, maxX, minY, maxY,
                fps,
                simulationMode
        );

        // Generate particles first
        particles = generateParticles(numParticles, minX, maxX, minY, maxY);
        
        // Don't create simulation here - let restartSimulation handle it
        // This avoids double initialization for distributed mode

        SimulationUI simUI = null;
        JFrame frame = null;
        if (enableUI) {
            simUI = new SimulationUI(particles, params);
            frame = new JFrame("Charged Particles Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(simUI);
            frame.pack();
            frame.setVisible(true);
            simUI.startRendering();
        }

        // Začetek prvega zagona simulacije
        restartSimulation();
        
        // Track whether simulation was already shutdown
        final boolean[] simulationShutdown = {false};
        
        // If running without UI, wait for simulation to complete
        if (!enableUI && simulationThread != null) {
            try {
                simulationThread.join();
                // Shutdown simulation after completion
                if (simulation != null) {
                    simulation.shutdown();
                    simulationShutdown[0] = true;
                }
                // Exit explicitly for non-UI mode
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Shutdown hook za proper cleanup (for UI mode or abnormal termination)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (simulation != null && !simulationShutdown[0]) {
                simulation.shutdown();
            }
        }));
    }
    
    /**
     * Generira delce z determinističnim seed-om za reproducibilne rezultate.
     */
    public static List<Particle> generateParticles(int numParticles, double minX, double maxX, double minY, double maxY) {
        List<Particle> particles = new ArrayList<>();
        Random random = new Random(RANDOM_SEED);
        
        for (int i = 0; i < numParticles; i++) {
            particles.add(Particle.randomParticle(minX, maxX, minY, maxY, random));
        }
        
        return particles;
    }
    
    /**
     * Zažene worker node za porazdeljeno simulacijo.
     */
    private static void startWorkerNode() {
        try {
            // Ustvari worker node z ID-jem 0 (ker v trenutni implementaciji ni dinamičnega ID-ja)
            int workerId = findAvailableWorkerId();
            WorkerNodeImpl worker = new WorkerNodeImpl(workerId);
            
            // Registriraj worker v RMI registry
            java.rmi.registry.Registry registryTemp;
            try {
                registryTemp = java.rmi.registry.LocateRegistry.getRegistry(
                    DistributedConfig.RMI_REGISTRY_HOST, 
                    DistributedConfig.RMI_REGISTRY_PORT
                );
                registryTemp.list(); // Test connection
            } catch (Exception e) {
                System.out.println("RMI Registry not found, creating new one...");
                registryTemp = java.rmi.registry.LocateRegistry.createRegistry(DistributedConfig.RMI_REGISTRY_PORT);
            }
            final java.rmi.registry.Registry registry = registryTemp;
            
            final String workerName = DistributedConfig.getWorkerServiceName(workerId);
            registry.bind(workerName, worker);
            
            System.out.println("Worker node " + workerId + " started and registered as '" + workerName + "'");
            System.out.println("Registry: " + DistributedConfig.RMI_REGISTRY_HOST + ":" + DistributedConfig.RMI_REGISTRY_PORT);
            System.out.println("Worker is ready to receive tasks. Press Ctrl+C to shutdown.");
            
            // Dodaj shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind(workerName);
                    worker.shutdown();
                    System.out.println("Worker node shutdown completed.");
                } catch (Exception e) {
                    System.err.println("Error during worker shutdown: " + e.getMessage());
                }
            }));
            
            // Ohrani worker node živ
            Object lock = new Object();
            synchronized (lock) {
                try {
                    lock.wait(); // Čakaj na interrupt
                } catch (InterruptedException e) {
                    System.out.println("Worker node interrupted, shutting down...");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to start worker node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Najde prvi razpoložljiv worker ID v RMI registru.
     */
    private static int findAvailableWorkerId() {
        try {
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry(
                DistributedConfig.RMI_REGISTRY_HOST, 
                DistributedConfig.RMI_REGISTRY_PORT
            );
            
            for (int i = 0; i < DistributedConfig.MAX_WORKERS; i++) {
                String workerName = DistributedConfig.getWorkerServiceName(i);
                try {
                    registry.lookup(workerName);
                    // Če lookup uspe, pomeni da je že registriran
                } catch (java.rmi.NotBoundException e) {
                    // Ni registriran, lahko uporabimo ta ID
                    return i;
                }
            }
            
            // Če ne najdemo prostega ID-ja, uporabimo naključnega
            return (int) (System.currentTimeMillis() % DistributedConfig.MAX_WORKERS);
            
        } catch (Exception e) {
            // V primeru napake uporabimo naključen ID
            return (int) (System.currentTimeMillis() % DistributedConfig.MAX_WORKERS);
        }
    }
    
    /**
     * Prikaže navodila za uporabo command line argumentov.
     */
    private static void printUsage() {
        System.out.println("ChargedParticles Simulator - Distributed Computing Support");
        System.out.println("Usage: java -jar ChargedParticles.jar [options]");
        System.out.println();
        System.out.println("General Options:");
        System.out.println("  --ui <true|false>     Enable/disable GUI (default: true)");
        System.out.println("  --window <width> <height>  Set window size (default: 800 600)");
        System.out.println("  --particles <count>   Number of particles (default: 400)");
        System.out.println("  --cycles <count>      Number of simulation cycles (default: 1000)");
        System.out.println("  --bounds <minX> <maxX> <minY> <maxY>  Simulation boundaries");
        System.out.println("  --fps <rate>          Rendering frame rate (default: 60)");
        System.out.println("  --mode <mode>         Simulation mode: sequential|parallel|distributed");
        System.out.println();
        System.out.println("Distributed Mode Options:");
        System.out.println("  --role <role>         Node role: master|worker (default: master)");
        System.out.println("  --workers <count>     Expected number of workers (master only, default: 2)");
        System.out.println("                        Note: Registry runs on localhost:1099");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Sequential simulation");
        System.out.println("  java -jar ChargedParticles.jar --mode sequential --particles 100");
        System.out.println();
        System.out.println("  # Parallel simulation");
        System.out.println("  java -jar ChargedParticles.jar --mode parallel --particles 500");
        System.out.println();
        System.out.println("  # Distributed simulation - start worker");
        System.out.println("  java -jar ChargedParticles.jar --role worker");
        System.out.println();
        System.out.println("  # Distributed simulation - start master");
        System.out.println("  java -jar ChargedParticles.jar --mode distributed --role master --workers 3");
        System.out.println();
        System.out.println("For distributed simulation:");
        System.out.println("1. Start worker nodes first (in separate terminals/machines)");
        System.out.println("2. Start master node last");
        System.out.println("3. Master will discover workers and distribute particles automatically");
    }
}
