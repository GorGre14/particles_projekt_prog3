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
