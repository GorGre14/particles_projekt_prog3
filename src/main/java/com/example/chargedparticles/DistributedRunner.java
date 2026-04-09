package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.*;
import com.example.chargedparticles.ui.SimulationUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import mpi.*;

/**
 * Vstopna tocka za porazdeljeno simulacijo z MPJ Express.
 *
 * Zagon:
 *   mpjrun.sh -np 4 com.example.chargedparticles.DistributedRunner [opcije]
 *
 * Vsi MPI procesi zazenejo isti program. Vsak proces izracuna sile za
 * svoj podnabor delcev in nato z Allgatherv posodobi skupno stanje.
 * Proces z rangom 0 (master) upravlja z UI in izpisom rezultatov.
 */
public class DistributedRunner {

    public static final long RANDOM_SEED = 42L;

    public static void main(String[] args) throws Exception {
        // === Inicializacija MPI ===
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // === Parsanje argumentov ===
        boolean enableUI = false;   // UI samo za rank 0
        int windowW = 800;
        int windowH = 600;
        int numParticles = 400;
        int numCycles = 1000;
        double minX = 0.0, maxX = 800.0;
        double minY = 0.0, maxY = 600.0;
        int fps = 60;

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
                    if (rank == 0) {
                        printUsage();
                    }
                    MPI.Finalize();
                    return;
            }
        }

        // UI samo na rank 0
        if (rank != 0) {
            enableUI = false;
        }

        SimulationParameters params = new SimulationParameters(
                enableUI, windowW, windowH, numParticles, numCycles,
                minX, maxX, minY, maxY, fps, SimulationMode.DISTRIBUTED
        );

        // === Generiranje delcev (enak seed na vseh procesih) ===
        List<Particle> particles = generateParticles(numParticles, minX, maxX, minY, maxY);

        // Izpis informacij samo na rank 0
        if (rank == 0) {
            System.out.println("Porazdeljena simulacija (MPJ Express)");
            System.out.println(" - Stevilo procesov: " + size);
            System.out.println(" - Stevilo delcev: " + numParticles);
            System.out.println(" - Stevilo ciklov: " + numCycles);
        }

        // === Ustvarjanje porazdeljene simulacije ===
        DistributedSimulation distSim = new DistributedSimulation(particles, rank, size);

        if (rank == 0) {
            System.out.println(" - Proces " + rank + ": delci [" + distSim.getMyStart() + ", " + distSim.getMyEnd() + ")");
        }

        // === UI (samo rank 0) ===
        SimulationUI simUI = null;
        if (enableUI && rank == 0) {
            simUI = new SimulationUI(particles, params);
            JFrame frame = new JFrame("Simulacija naelektrenih delcev (Porazdeljena)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(simUI);
            frame.pack();
            frame.setVisible(true);
            simUI.startRendering();
        }

        // === Glavna zanka simulacije ===
        long startTime = System.nanoTime();

        for (int cycle = 0; cycle < numCycles; cycle++) {
            // Izvedemo en cikel
            distSim.performOneCycle(params);

            // Posodobimo Particle objekte za UI (samo rank 0)
            if (enableUI && rank == 0) {
                distSim.writeBackToParticles(particles);
                Thread.sleep(10);
            }
        }

        long endTime = System.nanoTime();
        double elapsedSeconds = (endTime - startTime) / 1e9;

        // === Izpis rezultatov (samo rank 0) ===
        if (rank == 0) {
            // Posodobimo Particle objekte za koncni izpis
            distSim.writeBackToParticles(particles);

            System.out.println("Simulacija zakljucena (Porazdeljena, " + size + " procesov):");
            System.out.printf(" - Stevilo delcev: %d%n", numParticles);
            System.out.printf(" - Stevilo ciklov: %d%n", numCycles);
            System.out.printf(" - Cas izvajanja: %.3f s%n", elapsedSeconds);

            for (int i = 0; i < Math.min(5, particles.size()); i++) {
                System.out.println("Delec " + i + ": " + particles.get(i));
            }
        }

        // === Zakljucek MPI ===
        MPI.Finalize();
    }

    /**
     * Generira seznam delcev z deterministicnim seed-om.
     * Vsi procesi generirajo enake delce (enak seed).
     */
    private static List<Particle> generateParticles(int numParticles,
            double minX, double maxX, double minY, double maxY) {
        List<Particle> list = new ArrayList<>();
        Random random = new Random(RANDOM_SEED);

        for (int i = 0; i < numParticles; i++) {
            list.add(Particle.randomParticle(minX, maxX, minY, maxY, random));
        }
        return list;
    }

    private static void printUsage() {
        System.out.println("Porazdeljena simulacija naelektrenih delcev (MPJ Express)");
        System.out.println("Uporaba: mpjrun.sh -np <P> com.example.chargedparticles.DistributedRunner [opcije]");
        System.out.println();
        System.out.println("Opcije:");
        System.out.println("  --ui <true|false>     Vklopi/izklopi GUI na rank 0 (privzeto: false)");
        System.out.println("  --window <W> <H>      Velikost okna (privzeto: 800 600)");
        System.out.println("  --particles <N>       Stevilo delcev (privzeto: 400)");
        System.out.println("  --cycles <N>          Stevilo ciklov (privzeto: 1000)");
        System.out.println("  --bounds <x1> <x2> <y1> <y2>  Meje simulacije");
        System.out.println("  --fps <N>             Hitrost osvezevanja (privzeto: 60)");
        System.out.println();
        System.out.println("Primeri:");
        System.out.println("  mpjrun.sh -np 4 com.example.chargedparticles.DistributedRunner --particles 500 --cycles 1000");
        System.out.println("  mpjrun.sh -np 2 com.example.chargedparticles.DistributedRunner --ui true --particles 100");
    }
}