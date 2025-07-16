package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Vmesnik za simulacijo naelektrenih delcev.
 * Omogoca enotno uporabo sekvencne in vzporedne simulacije.
 */
public interface Simulation {
    
    /**
     * Izvede en cikel simulacije.
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
     * Za sekvencno simulacijo ni potrebno, za vzporedno pa je potrebno.
     */
    default void shutdown() {
        // Privzeto ni potrebno nic naredit
    }
    
    /**
     * Vrne opis simulacije (npr. "Sequential", "Parallel (8 threads)")
     * @return opis simulacije
     */
    String getDescription();
}