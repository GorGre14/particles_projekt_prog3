package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SequentialSimulation;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.ui.SimulationUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Glavna vstopna tocka za simulacijo naelektrenih delcev.
 * Inicializira parametre, ustvari delce, pozene simulacijo in prikaze UI.
 */
public class SimulationRunner {

    // Javne reference za dostop iz UI
    public static Thread simulationThread;
    public static SequentialSimulation simulation;
    public static List<Particle> particles;
    public static SimulationParameters params;

    // Deterministicen seed za ponovljive rezultate
    public static final long RANDOM_SEED = 42L;

    /**
     * Ponovno zazene simulacijo z trenutnimi parametri.
     * Ustavi prejsnjo simulacijo, ce se izvaja.
     */
    public static void restartSimulation() {
        // Ustavimo prejsnjo simulacijo
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Ustvarimo novo simulacijo
        simulation = new SequentialSimulation();

        // Zazenemo simulacijsko nit
        simulationThread = new Thread(() -> {
            long startTime = System.nanoTime();
            int numCycles = params.getNumCycles();

            for (int cycle = 0; cycle < numCycles; cycle++) {
                // Preverimo prekinitev
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // Izvedemo en cikel simulacije
                simulation.performOneCycle(particles, params);

                // Upocasnitev za vizualizacijo (ce je UI vklopljen)
                if (params.isEnableUI()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // Izpis rezultatov
            long endTime = System.nanoTime();
            double elapsedSeconds = (endTime - startTime) / 1e9;
            System.out.println("Simulacija zakljucena:");
            System.out.printf(" - Stevilo delcev: %d%n", params.getNumParticles());
            System.out.printf(" - Stevilo ciklov: %d%n", numCycles);
            System.out.printf(" - Cas izvajanja: %.3f s%n", elapsedSeconds);

            // Izpis prvih 5 delcev
            for (int i = 0; i < Math.min(5, particles.size()); i++) {
                System.out.println("Delec " + i + ": " + particles.get(i));
            }
        });
        simulationThread.start();
    }

    /**
     * Generira seznam delcev z determinističnim seed-om.
     */
    public static List<Particle> generateParticles(int numParticles,
            double minX, double maxX, double minY, double maxY) {
        List<Particle> list = new ArrayList<>();
        Random random = new Random(RANDOM_SEED);

        for (int i = 0; i < numParticles; i++) {
            list.add(Particle.randomParticle(minX, maxX, minY, maxY, random));
        }
        return list;
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

        // Parsanje argumentov iz ukazne vrstice
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
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Neznan argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        // Inicializacija parametrov
        params = new SimulationParameters(enableUI, windowW, windowH,
                numParticles, numCycles, minX, maxX, minY, maxY, fps);

        // Generiranje delcev
        particles = generateParticles(numParticles, minX, maxX, minY, maxY);

        // Ustvarjanje UI (ce je vklopljen)
        if (enableUI) {
            SimulationUI simUI = new SimulationUI(particles, params);
            JFrame frame = new JFrame("Simulacija naelektrenih delcev");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(simUI);
            frame.pack();
            frame.setVisible(true);
            simUI.startRendering();
        }

        // Zagon simulacije
        restartSimulation();

        // Ce ni UI, pocakamo na zakljucek in koncamo
        if (!enableUI && simulationThread != null) {
            try {
                simulationThread.join();
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void printUsage() {
        System.out.println("Simulacija naelektrenih delcev (sekvencna verzija)");
        System.out.println("Uporaba: java -jar ChargedParticles.jar [opcije]");
        System.out.println();
        System.out.println("Opcije:");
        System.out.println("  --ui <true|false>     Vklopi/izklopi GUI (privzeto: true)");
        System.out.println("  --window <W> <H>      Velikost okna (privzeto: 800 600)");
        System.out.println("  --particles <N>       Stevilo delcev (privzeto: 400)");
        System.out.println("  --cycles <N>          Stevilo ciklov (privzeto: 1000)");
        System.out.println("  --bounds <x1> <x2> <y1> <y2>  Meje simulacije");
        System.out.println("  --fps <N>             Hitrost osvezevanja (privzeto: 60)");
        System.out.println();
        System.out.println("Primeri:");
        System.out.println("  java -jar ChargedParticles.jar --particles 100 --cycles 500");
        System.out.println("  java -jar ChargedParticles.jar --ui false --particles 1000");
    }
}
