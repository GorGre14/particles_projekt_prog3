# Razred: ParallelSimulation (`ParallelSimulation.java`)

## Vloga v simulaciji
Vzporedna različica s **skupnim pomnilnikom**. Delce razdeli na $T$ enako velikih odsekov; vsak odsek izračuna svoja nit iz thread poola.

## Delitev dela
```java
int chunk = (n + numThreads - 1) / numThreads;
// nit 0: delci [0, chunk)
// nit 1: delci [chunk, 2*chunk)
// ...
```
Vsaka nit izračuna sile za **svoj kos** delcev, pri tem pa prebere **vse** delce (vsak delec vpliva na vsakega drugega).

Naloge so vedno enake, zato jih zgradimo **enkrat v konstruktorju** in jih v vsakem ciklu samo oddamo.

## Sinhronizacija

```java
executor.invokeAll(tasks);                     // FAZA 1 + pregrada
ForceKernel.integrate(state, 0, n, forces);    // FAZA 2
```

`invokeAll()` odda vse naloge in se vrne **šele ko so vse končane** — deluje torej kot pregrada (barrier) med fazama.

> [!NOTE]
> **Zakaj `invokeAll()` in ne `submit()` s `CountDownLatch`?**
> `invokeAll()` že vsebuje pregrado, zato števca ni treba ročno ustvarjati, zmanjševati in čakati nanj. Manj kode pomeni manj možnosti za napako: pozabljen `countDown()` v veji z izjemo bi glavno nit blokiral za vedno.

> [!IMPORTANT]
> **Zakaj ni tekme za vire (race condition)?**
> Vsaka nit piše v **ekskluziven del** polja `forces`. Nit $t$ piše samo v indekse $[t \cdot chunk, (t+1) \cdot chunk)$. Ker se indeksi ne prekrivajo, zaklepanje ni potrebno. To je primer **prostorske dekompozicije**.

## Zakaj je Faza 2 sekvenčna
Faza 2 je $O(n)$ — pri $n = 3000$ je to 3000 operacij, medtem ko je Faza 1 $O(n^2) = 9\,000\,000$ operacij. Paralelizacija Faze 2 bi prinesla zanemarljiv prihranek, uvedla pa dodatno sinhronizacijo.

## Izmerjeno
Vzporedna različica je **najhitrejša na enem računalniku pri vseh izmerjenih velikostih problema**. Največja izmerjena pohitritev je $4{,}8\times$ pri $n = 2000$.

Pohitritev ne doseže $10\times$, ker ima Apple M4 **4 zmogljiva (P) in 6 varčnih (E) jeder**. Prve štiri niti dobijo P-jedra, naslednjih šest pa bistveno počasnejša E-jedra. Ker so kosi enako veliki in je na koncu cikla pregrada, vsi čakajo na najpočasnejšega.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Vzporedna simulacija s skupnim pomnilnikom.
 *
 * Delce razdeli na T enako velikih odsekov, vsak odsek izracuna svoja nit iz
 * thread poola. Niti pisejo v disjunktne dele polja sil, zato zaklepanje ni
 * potrebno. Klic invokeAll deluje kot pregrada (barrier) na koncu vsakega cikla.
 */
public class ParallelSimulation extends AbstractSimulation {

    private final int numThreads;
    private final ExecutorService executor;

    /** Naloge so vedno enake, zato jih zgradimo enkrat. */
    private final List<Callable<Void>> tasks = new ArrayList<>();

    /** Privzeto uporabi vsa razpolozljiva jedra. */
    public ParallelSimulation(List<Particle> particles, SimulationParameters params) {
        this(particles, params, Runtime.getRuntime().availableProcessors());
    }

    public ParallelSimulation(List<Particle> particles, SimulationParameters params, int numThreads) {
        super(particles, params);
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);

        int chunk = (n + numThreads - 1) / numThreads;
        for (int t = 0; t < numThreads; t++) {
            final int start = Math.min(t * chunk, n);
            final int end = Math.min(start + chunk, n);
            if (start < end) {
                tasks.add(() -> {
                    ForceKernel.computeForces(state, n, start, end, forces, params);
                    return null;
                });
            }
        }
    }

    @Override
    public void performOneCycle() {
        // FAZA 1: vzporeden izracun sil; invokeAll pocaka na vse niti.
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // FAZA 2: posodobitev pozicij - O(n), zanemarljivo glede na O(n^2).
        ForceKernel.integrate(state, 0, n, forces);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getDescription() {
        return "Vzporedna (" + numThreads + " niti)";
    }
}
```
