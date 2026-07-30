# Razred: DistributedRunner (`DistributedRunner.java`)

## Vloga v simulaciji
Vstopna točka za **porazdeljeni** način. Zažene se prek skripte:

```bash
./run_distributed.sh 4 --particles 1000 --cycles 2000
```

## SPMD — Single Program, Multiple Data
Vsi procesi zaženejo **isti program** in iz **istega seeda** zgenerirajo enake delce. Razlikujejo se le po svojem rangu, ki določa, kateri odsek delcev računajo.

> [!NOTE]
> **Zakaj ločen vstopni razred?**
> MPJ Express zahteva `MPI.Init(args)` na samem začetku programa. MPI okolje mora obstajati **že ob zagonu**, zato se obstoječi proces ne more sam "razmnožiti" v MPI skupino.

## Obdelava argumentov
```java
String[] userArgs = MPI.Init(args);
```
`MPI.Init()` odstrani svoje argumente (rang, velikost, ime naprave) in vrne samo uporabniške. Te nato prebere ista metoda `SimulationParameters.fromArgs()` kot pri ostalih dveh načinih.

## Vloga ranga 0 (master)
- odpre **grafično okno** (ostali procesi računajo brez izrisa),
- izpiše rezultate in čas izvajanja.

Vsi procesi računajo enakovredno — master se razlikuje samo po izpisu in izrisu.

## Potek
1. `MPI.Init()` — inicializacija MPI okolja
2. branje parametrov, način nastavljen na `DISTRIBUTED`
3. generiranje delcev iz seeda 42 (enako na vseh procesih)
4. `new DistributedSimulation(particles, params, rank, size)`
5. `SimulationRunner.runCycles(...)` — **ista zanka kot ostala dva načina**
6. `MPI.Finalize()`

## Koda razreda

```java
package com.example.chargedparticles;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.DistributedSimulation;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.ui.SimulationUI;

import mpi.MPI;

import java.util.List;

/**
 * Vstopna tocka za porazdeljeni nacin (MPJ Express).
 *
 * Zagon: ./run_distributed.sh <stevilo_procesov> [opcije]
 *
 * Vsi procesi izvedejo isti program in iz istega seeda zgenerirajo enake
 * delce. Vsak proces racuna svoj odsek, po vsakem ciklu pa se stanje z
 * Allgatherv zdruzi. Rezultat izpise in izrise samo proces z rangom 0.
 */
public class DistributedRunner {

    public static void main(String[] args) throws Exception {
        // MPI.Init odstrani svoje argumente in vrne samo nase.
        String[] userArgs = MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        SimulationParameters params;
        try {
            params = SimulationParameters.fromArgs(userArgs);
        } catch (IllegalArgumentException e) {
            if (rank == 0) {
                System.err.println(e.getMessage());
                SimulationRunner.printUsage();
            }
            MPI.Finalize();
            return;
        }

        params.setSimulationMode(SimulationMode.DISTRIBUTED);
        // Okno odpre samo rang 0; ostali procesi racunajo brez izrisa.
        boolean master = (rank == 0);
        params.setEnableUI(params.isEnableUI() && master);

        List<Particle> particles = SimulationRunner.generateParticles(params);
        DistributedSimulation simulation = new DistributedSimulation(particles, params, rank, size);

        if (master) {
            System.out.println("Porazdeljena simulacija (MPJ Express)");
            System.out.printf(" - Stevilo procesov: %d%n", size);
            System.out.printf(" - Delci na proces: priblizno %d%n", params.getNumParticles() / size);
            System.out.printf(" - Proces 0 racuna delce [%d, %d)%n",
                    simulation.getMyStart(), simulation.getMyEnd());
        }

        if (params.isEnableUI()) {
            SimulationRunner.showWindow(new SimulationUI(particles, params),
                    "Simulacija naelektrenih delcev - porazdeljena (" + size + " procesov)");
        }

        SimulationRunner.runCycles(simulation, particles, params, master);

        MPI.Finalize();
    }
}
```
