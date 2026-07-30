# Razred: AbstractSimulation (`AbstractSimulation.java`)

## Vloga v simulaciji
Skupna osnova vseh treh načinov. Hrani stanje, ki je pri vseh enako, in izvede `syncToParticles()`, ki je prav tako pri vseh enak.

Ker je stanje skupno, se podrazredi razlikujejo **samo po tem, kako razdelijo delo** — kar je natanko tisto, kar poročilo primerja.

## Skupno stanje

| Polje | Pomen |
|---|---|
| `n` | število delcev |
| `state` | stanje vseh delcev, 6 doublov na delec |
| `forces` | sile, globalno indeksirane: `forces[i*2]`, `forces[i*2+1]` |
| `params` | parametri simulacije (meje prostora, število ciklov) |

Vsa polja so `final` in `protected`: podrazredi jih berejo in pišejo vanje, nihče pa jih ne zamenja.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Skupna osnova vseh treh nacinov simulacije.
 *
 * Hrani stanje delcev v ravnem double[] polju (postavitev opisana v
 * {@link ForceKernel}) in polje sil. Ker je stanje pri vseh nacinih enako,
 * se izvedbe razlikujejo samo po tem, kako razdelijo delo med izvajalce.
 */
public abstract class AbstractSimulation implements Simulation {

    /** Stevilo delcev. */
    protected final int n;

    /** Stanje vseh delcev: 6 doublov na delec. */
    protected final double[] state;

    /** Sile, globalno indeksirane: forces[i*2], forces[i*2+1]. */
    protected final double[] forces;

    /** Parametri simulacije (meje prostora, stevilo ciklov ...). */
    protected final SimulationParameters params;

    protected AbstractSimulation(List<Particle> particles, SimulationParameters params) {
        this.n = particles.size();
        this.state = ForceKernel.toFlatArray(particles);
        this.forces = new double[n * 2];
        this.params = params;
    }

    @Override
    public void syncToParticles(List<Particle> particles) {
        ForceKernel.writeBack(state, particles);
    }
}
```
