# Vmesnik: Simulation (`Simulation.java`)

## Vloga v simulaciji
Vmesnik določa pogodbo, ki jo izpolnjujejo vsi trije načini. Gre za načrtovalski vzorec **Strategy**: `SimulationRunner` dela z referenco tipa `Simulation` in ne ve, katera izvedba dejansko teče.

## Metode

| Metoda | Pomen |
|---|---|
| `performOneCycle()` | izvede en cikel nad internim stanjem |
| `syncToParticles(particles)` | prepiše interno stanje v seznam `Particle` objektov |
| `getDescription()` | opis načina za izpis, npr. `"Vzporedna (10 niti)"` |
| `shutdown()` | sprosti vire (privzeto ne naredi nič) |

> [!NOTE]
> **Zakaj `performOneCycle()` nima parametrov?**
> Delci in parametri se med tekom ne spreminjajo, zato jih simulacija prejme **v konstruktorju** in shrani. Tako v vsakem ciklu ni ponovnega prenosa argumentov niti preverjanja, ali je stanje že inicializirano.

> [!NOTE]
> **Zakaj obstaja `syncToParticles()`?**
> Simulacija računa nad ravnim `double[]` poljem, seznam `Particle` objektov pa med izvajanjem ni ažuren. Prepis je potreben samo takrat, ko podatke kdo gleda (izris) ali izpisuje. Če bi ga izvajali v vsakem ciklu, bi merili tudi ceno prepisovanja in ne le računanja.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Vmesnik za simulacijo naelektrenih delcev.
 *
 * Vse tri izvedbe (sekvencna, vzporedna, porazdeljena) racunajo z istim
 * jedrom {@link ForceKernel} nad isto podatkovno postavitvijo. Razlikujejo se
 * samo v tem, kdo izracuna kateri odsek delcev.
 */
public interface Simulation {

    /**
     * Izvede en cikel simulacije:
     * 1) izracun sil za vsak delec,
     * 2) posodobitev hitrosti in pozicij.
     */
    void performOneCycle();

    /**
     * Prepise interno stanje v seznam Particle objektov.
     *
     * Simulacija racuna nad ravnim double[] poljem, zato seznam delcev med
     * izvajanjem ni azuren. Metodo klicemo pred izrisom ali izpisom, ne v
     * vroci zanki.
     */
    void syncToParticles(List<Particle> particles);

    /** Opis nacina, npr. "Sekvencna" ali "Vzporedna (10 niti)". */
    String getDescription();

    /** Sprosti vire (npr. thread pool). Privzeto ni potrebno nic. */
    default void shutdown() {
    }
}
```
