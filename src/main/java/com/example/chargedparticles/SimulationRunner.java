package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.Simulation;
import com.example.chargedparticles.simulation.SimulationFactory;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.ui.SimulationUI;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vstopna tocka za sekvencni in vzporedni nacin simulacije.
 *
 * Razred vsebuje tudi skupno simulacijsko zanko in generator delcev, ki ju
 * uporablja tudi {@link DistributedRunner}, tako da vsi trije nacini tecejo
 * po istem postopku in nad istim zacetnim stanjem.
 */
public class SimulationRunner {

    /** Fiksen seed zagotavlja, da vsi nacini startajo iz enakega stanja. */
    public static final long RANDOM_SEED = 42L;

    /** Upocasnitev izrisa, da je gibanje delcev vidno (v milisekundah). */
    private static final long UI_FRAME_DELAY_MS = 10;

    // Trenutna simulacija; graficni vmesnik jo lahko ustavi in znova zazene.
    private static Thread simulationThread;
    private static Simulation simulation;
    private static List<Particle> particles;
    private static SimulationParameters params;

    /**
     * Izvede zahtevano stevilo ciklov in izpise rezultat.
     * Uporabljajo jo vsi trije nacini simulacije.
     *
     * @param printResults ali naj ta proces izpise rezultat (pri MPI samo rang 0)
     */
    public static void runCycles(Simulation simulation, List<Particle> particles,
                                 SimulationParameters params, boolean printResults) {
        long startTime = System.nanoTime();

        for (int cycle = 0; cycle < params.getNumCycles(); cycle++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            simulation.performOneCycle();

            // Za izris moramo stanje prepisati v Particle objekte.
            if (params.isEnableUI()) {
                simulation.syncToParticles(particles);
                try {
                    Thread.sleep(UI_FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        double elapsedSeconds = (System.nanoTime() - startTime) / 1e9;

        if (printResults) {
            simulation.syncToParticles(particles);
            System.out.println("Simulacija zakljucena (" + simulation.getDescription() + "):");
            System.out.printf(" - Stevilo delcev: %d%n", params.getNumParticles());
            System.out.printf(" - Stevilo ciklov: %d%n", params.getNumCycles());
            System.out.printf(" - Cas izvajanja: %.3f s%n", elapsedSeconds);
            for (int i = 0; i < Math.min(5, particles.size()); i++) {
                System.out.println("Delec " + i + ": " + particles.get(i));
            }
        }
    }

    /** Generira delce iz fiksnega seeda, zato je zacetno stanje vedno enako. */
    public static List<Particle> generateParticles(SimulationParameters params) {
        List<Particle> list = new ArrayList<>();
        Random random = new Random(RANDOM_SEED);
        for (int i = 0; i < params.getNumParticles(); i++) {
            list.add(Particle.randomParticle(params.getMinX(), params.getMaxX(),
                    params.getMinY(), params.getMaxY(), random));
        }
        return list;
    }

    /** Ustavi tekoco simulacijo in sprosti njene vire. */
    public static void stopSimulation() {
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (simulation != null) {
            simulation.shutdown();
        }
    }

    /**
     * Ustavi morebitno tekoco simulacijo, ponastavi delce na zacetno stanje in
     * zazene nov tek v locenih niti, da graficni vmesnik ostane odziven.
     */
    public static void restartSimulation() {
        stopSimulation();

        particles.clear();
        particles.addAll(generateParticles(params));

        simulation = SimulationFactory.createSimulation(params.getSimulationMode(), particles, params);
        simulationThread = new Thread(
                () -> runCycles(simulation, particles, params, true),
                "simulacija");
        simulationThread.start();
    }

    public static void main(String[] args) {
        try {
            params = SimulationParameters.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(1);
            return;
        }

        if (params.getSimulationMode() == SimulationMode.DISTRIBUTED) {
            System.err.println("Porazdeljeni nacin zazenite z ./run_distributed.sh <stevilo_procesov>");
            System.exit(1);
            return;
        }

        particles = generateParticles(params);

        if (params.isEnableUI()) {
            showWindow(new SimulationUI(particles, params), "Simulacija naelektrenih delcev");
        }

        restartSimulation();

        // Brez vmesnika pocakamo na zakljucek in koncamo.
        if (!params.isEnableUI()) {
            try {
                simulationThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            simulation.shutdown();
            System.exit(0);
        }
    }

    /** Odpre okno s podanim vmesnikom in zacne izris. */
    public static void showWindow(SimulationUI ui, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(ui);
        frame.pack();
        frame.setVisible(true);
        ui.startRendering();
    }

    /** Skupna navodila za vse tri nacine. */
    public static void printUsage() {
        System.out.println("Simulacija naelektrenih delcev");
        System.out.println();
        System.out.println("Opcije:");
        System.out.println("  --mode <sequential|parallel>  Nacin izvajanja (privzeto: sequential)");
        System.out.println("  --particles <N>               Stevilo delcev (privzeto: 400)");
        System.out.println("  --cycles <N>                  Stevilo ciklov (privzeto: 1000)");
        System.out.println("  --ui <true|false>             Graficni vmesnik (privzeto: true)");
        System.out.println("  --window <W> <H>              Velikost okna (privzeto: 800 600)");
        System.out.println("  --bounds <x1> <x2> <y1> <y2>  Meje prostora (privzeto: 0 800 0 600)");
        System.out.println("  --fps <N>                     Hitrost izrisa (privzeto: 60)");
        System.out.println();
        System.out.println("Primeri:");
        System.out.println("  ./run_sequential.sh  --particles 1000 --cycles 2000 --ui false");
        System.out.println("  ./run_parallel.sh    --particles 1000 --cycles 2000 --ui false");
        System.out.println("  ./run_distributed.sh 4 --particles 1000 --cycles 2000 --ui false");
    }
}
