package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.*;
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
    public static final long RANDOM_SEED = 42L; // Deterministic seed for reproducible results

    /**
     * Metoda za ponovno zagon simulacije z uporabo trenutnih parametrov.
     */
    public static void restartSimulation() {
        // Če simulacijska nit teče, jo prekinemo
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
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
                try {
                    Thread.sleep(10); // upocasnitev za boljso vizualizacijo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
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
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    break;
            }
        }

        // Inicializacija parametrov, delcev in simulacije
        params = new SimulationParameters(
                enableUI,
                windowW, windowH,
                numParticles, numCycles,
                minX, maxX, minY, maxY,
                fps,
                simulationMode
        );

        simulation = SimulationFactory.createSimulation(simulationMode);
        particles = generateParticles(numParticles, minX, maxX, minY, maxY);
        
        System.out.println("Initialized " + simulation.getDescription() + " simulation");

        SimulationUI simUI = null;
        JFrame frame = null;
        if (enableUI) {
            simUI = new SimulationUI(particles, params);
            frame = new JFrame("Charged Particles - Sequential");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(simUI);
            frame.pack();
            frame.setVisible(true);
            simUI.startRendering();
        }

        // Začetek prvega zagona simulacije
        restartSimulation();
        
        // Shutdown hook za proper cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (simulation != null) {
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
}
