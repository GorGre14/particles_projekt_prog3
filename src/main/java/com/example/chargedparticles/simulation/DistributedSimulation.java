package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import mpi.MPI;
import mpi.MPIException;

import java.util.List;

/**
 * Porazdeljena simulacija z MPJ Express (MPI za Javo).
 *
 * Vsak proces hrani kopijo stanja vseh delcev, izracuna pa sile in nove
 * pozicije samo za svoj odsek. Na koncu cikla kolektivna operacija Allgatherv
 * zdruzi odseke, tako da imajo vsi procesi spet celotno stanje.
 *
 * V nasprotju z vzporednim nacinom si procesi ne delijo pomnilnika, zato je
 * treba stanje ob vsakem ciklu eksplicitno prenesti. Ta prenos je cena
 * porazdeljenega modela.
 */
public class DistributedSimulation extends AbstractSimulation {

    private final int rank;         // rang tega procesa
    private final int size;         // skupno stevilo procesov
    private final int myStart;      // prvi delec tega procesa (vkljucno)
    private final int myEnd;        // zadnji delec tega procesa (izkljucno)

    // Opis odsekov za Allgatherv: koliko doublov poslje in kam jih vpise vsak proces.
    private final int[] recvCounts;
    private final int[] displacements;

    public DistributedSimulation(List<Particle> particles, SimulationParameters params,
                                 int rank, int size) {
        super(particles, params);
        this.rank = rank;
        this.size = size;
        this.myStart = startIndexOf(rank);
        this.myEnd = startIndexOf(rank + 1);

        this.recvCounts = new int[size];
        this.displacements = new int[size];
        for (int r = 0; r < size; r++) {
            int start = startIndexOf(r);
            recvCounts[r] = (startIndexOf(r + 1) - start) * ForceKernel.FIELDS_PER_PARTICLE;
            displacements[r] = start * ForceKernel.FIELDS_PER_PARTICLE;
        }
    }

    /**
     * Zacetni indeks delcev za proces r. Prvih (n mod size) procesov dobi
     * en delec vec, da je delitev cim bolj enakomerna.
     * Za r == size vrne n, zato je konec odseka r kar startIndexOf(r + 1).
     */
    private int startIndexOf(int r) {
        int base = n / size;
        int remainder = n % size;
        return (r < remainder) ? r * (base + 1)
                               : remainder * (base + 1) + (r - remainder) * base;
    }

    @Override
    public void performOneCycle() {
        // FAZA 1 + 2: izracun sil in novih pozicij za lasten odsek.
        ForceKernel.computeForces(state, n, myStart, myEnd, forces, params);
        ForceKernel.integrate(state, myStart, myEnd, forces);

        // FAZA 3: vsak proces poslje svoj odsek in prejme odseke vseh ostalih.
        int sendOffset = myStart * ForceKernel.FIELDS_PER_PARTICLE;
        int sendCount = (myEnd - myStart) * ForceKernel.FIELDS_PER_PARTICLE;
        try {
            MPI.COMM_WORLD.Allgatherv(
                    state, sendOffset, sendCount, MPI.DOUBLE,
                    state, 0, recvCounts, displacements, MPI.DOUBLE);
        } catch (MPIException e) {
            throw new IllegalStateException("Napaka pri MPI komunikaciji", e);
        }
    }

    @Override
    public String getDescription() {
        return "Porazdeljena (" + size + " procesov)";
    }

    public int getRank() {
        return rank;
    }

    public int getMyStart() {
        return myStart;
    }

    public int getMyEnd() {
        return myEnd;
    }
}
