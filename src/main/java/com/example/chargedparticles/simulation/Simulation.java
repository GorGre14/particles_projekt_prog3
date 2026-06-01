package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Vmesnik za simulacijo naelektrenih delcev.
 * Omogoca enotno uporabo sekvencne, vzporedne in porazdeljene simulacije.
 */
public interface Simulation {

    /**
     * Izvede en cikel simulacije.
     *
     * Vsak cikel vsebuje:
     * 1) Izracun sil za vsak delec
     * 2) Posodabljanje pozicije in hitrosti
     *
     * @param particles seznam delcev
     * @param params parametri simulacije
     */
    void performOneCycle(List<Particle> particles, SimulationParameters params);

    /**
     * Sprosti resurse (ce jih simulacija uporablja).
     */
    default void shutdown() {
        // Privzeto ni potrebno nic naredit
    }

    /**
     * Vrne opis simulacije (npr. "Sekvencna", "Vzporedna (8 niti)")
     */
    String getDescription();
}
