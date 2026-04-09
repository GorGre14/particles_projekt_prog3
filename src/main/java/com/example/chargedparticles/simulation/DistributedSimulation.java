package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import mpi.*;

import java.util.List;

/**
 * Porazdeljena simulacija naelektrenih delcev z uporabo MPJ Express.
 *
 * Strategija porazdelitve:
 * - Vsak MPI proces dobi podnabor delcev za izracun sil.
 * - Vsi procesi imajo kopijo VSEH delcev (potrebno za izracun sil).
 * - Po izracunu sil in posodobitvi pozicij se uporabi Allgatherv
 *   za sinhronizacijo posodobljenih podatkov med vsemi procesi.
 *
 * Podatki delcev so shranjeni v ravnem double[] polju za MPI komunikacijo:
 *   [x, y, vx, vy, charge, mass] -> 6 doublov na delec
 */
public class DistributedSimulation {

    // Stevilo double vrednosti na delec
    private static final int FIELDS_PER_PARTICLE = 6;

    private final int rank;           // Rang trenutnega procesa
    private final int size;           // Skupno stevilo procesov
    private final int myStart;        // Zacetni indeks delcev za ta proces
    private final int myEnd;          // Koncni indeks (ekskluzivno)
    private final int totalParticles;

    // Ravno polje z vsemi podatki delcev
    private double[] allData;

    // Polja za Allgatherv komunikacijo
    private final int[] recvCounts;
    private final int[] displacements;

    /**
     * Konstruktor za porazdeljeno simulacijo.
     *
     * @param particles  seznam delcev (enak na vseh procesih)
     * @param rank       rang trenutnega MPI procesa
     * @param size       skupno stevilo MPI procesov
     */
    public DistributedSimulation(List<Particle> particles, int rank, int size) {
        this.rank = rank;
        this.size = size;
        this.totalParticles = particles.size();

        // === Razdelitev delcev med procese ===
        // Procesi z rangom < remainder dobijo en delec vec
        int baseCount = totalParticles / size;
        int remainder = totalParticles % size;

        if (rank < remainder) {
            myStart = rank * (baseCount + 1);
            myEnd = myStart + baseCount + 1;
        } else {
            myStart = remainder * (baseCount + 1) + (rank - remainder) * baseCount;
            myEnd = myStart + baseCount;
        }

        // === Inicializacija ravnega polja iz seznama delcev ===
        allData = new double[totalParticles * FIELDS_PER_PARTICLE];
        for (int i = 0; i < totalParticles; i++) {
            Particle p = particles.get(i);
            int offset = i * FIELDS_PER_PARTICLE;
            allData[offset]     = p.getX();
            allData[offset + 1] = p.getY();
            allData[offset + 2] = p.getVx();
            allData[offset + 3] = p.getVy();
            allData[offset + 4] = p.getCharge();
            allData[offset + 5] = p.getMass();
        }

        // === Priprava podatkov za Allgatherv ===
        // Vsak proces poslje razlicno stevilo elementov
        recvCounts = new int[size];
        displacements = new int[size];
        for (int r = 0; r < size; r++) {
            int rStart, rEnd;
            if (r < remainder) {
                rStart = r * (baseCount + 1);
                rEnd = rStart + baseCount + 1;
            } else {
                rStart = remainder * (baseCount + 1) + (r - remainder) * baseCount;
                rEnd = rStart + baseCount;
            }
            recvCounts[r] = (rEnd - rStart) * FIELDS_PER_PARTICLE;
            displacements[r] = rStart * FIELDS_PER_PARTICLE;
        }
    }

    /**
     * Izvede en cikel porazdeljene simulacije.
     *
     * Algoritem:
     * 1. Vsak proces izracuna sile za svoje delce (proti vsem delcem)
     * 2. Vsak proces posodobi pozicije in hitrosti svojih delcev
     * 3. Allgatherv sinhronizira posodobljene podatke med vsemi procesi
     */
    public void performOneCycle(SimulationParameters params) throws MPIException {
        int myCount = myEnd - myStart;
        double[][] forces = new double[myCount][2];

        // === FAZA 1: Izracun sil za delce tega procesa ===
        for (int i = 0; i < myCount; i++) {
            int globalI = myStart + i;
            int offsetI = globalI * FIELDS_PER_PARTICLE;

            double x1 = allData[offsetI];
            double y1 = allData[offsetI + 1];
            double c1 = allData[offsetI + 4];

            double fxSum = 0.0;
            double fySum = 0.0;

            // Sila od vseh ostalih delcev
            for (int j = 0; j < totalParticles; j++) {
                if (j == globalI) continue;

                int offsetJ = j * FIELDS_PER_PARTICLE;
                double x2 = allData[offsetJ];
                double y2 = allData[offsetJ + 1];
                double c2 = allData[offsetJ + 4];

                // Coulombova sila (enako kot ForceUtils)
                double dx = x2 - x1;
                double dy = y2 - y1;
                double r2 = dx * dx + dy * dy;
                if (r2 < 1e-12) r2 = 1e-12;
                double r = Math.sqrt(r2);

                double magnitude = Math.abs(c1 * c2) / r2;
                double sign = (c1 * c2 >= 0) ? 1.0 : -1.0;

                fxSum += sign * magnitude * (dx / r);
                fySum += sign * magnitude * (dy / r);
            }

            // Mejna sila
            double buffer = 5.0;
            double repelFactor = 10.0;

            if (x1 < params.getMinX() + buffer) {
                double dist = Math.max(1e-12, x1 - params.getMinX());
                fxSum += repelFactor / (dist * dist);
            }
            if (x1 > params.getMaxX() - buffer) {
                double dist = Math.max(1e-12, params.getMaxX() - x1);
                fxSum -= repelFactor / (dist * dist);
            }
            if (y1 < params.getMinY() + buffer) {
                double dist = Math.max(1e-12, y1 - params.getMinY());
                fySum += repelFactor / (dist * dist);
            }
            if (y1 > params.getMaxY() - buffer) {
                double dist = Math.max(1e-12, params.getMaxY() - y1);
                fySum -= repelFactor / (dist * dist);
            }

            forces[i][0] = fxSum;
            forces[i][1] = fySum;
        }

        // === FAZA 2: Posodobitev pozicij za delce tega procesa ===
        for (int i = 0; i < myCount; i++) {
            int globalI = myStart + i;
            int offset = globalI * FIELDS_PER_PARTICLE;

            double mass = allData[offset + 5];
            double ax = forces[i][0] / mass;
            double ay = forces[i][1] / mass;

            double newVx = allData[offset + 2] + ax;
            double newVy = allData[offset + 3] + ay;

            allData[offset]     += newVx;  // x = x + vx
            allData[offset + 1] += newVy;  // y = y + vy
            allData[offset + 2] = newVx;   // vx
            allData[offset + 3] = newVy;   // vy
        }

        // === FAZA 3: Sinhronizacija med vsemi procesi ===
        // Vsak proces poslje svoje posodobljene delce, prejme vse
        int sendOffset = myStart * FIELDS_PER_PARTICLE;
        int sendCount = (myEnd - myStart) * FIELDS_PER_PARTICLE;

        MPI.COMM_WORLD.Allgatherv(
                allData, sendOffset, sendCount, MPI.DOUBLE,
                allData, 0, recvCounts, displacements, MPI.DOUBLE
        );
    }

    /**
     * Zapise podatke iz ravnega polja nazaj v seznam Particle objektov.
     * Uporabljeno za prikaz v UI.
     */
    public void writeBackToParticles(List<Particle> particles) {
        for (int i = 0; i < totalParticles; i++) {
            int offset = i * FIELDS_PER_PARTICLE;
            Particle p = particles.get(i);
            p.setX(allData[offset]);
            p.setY(allData[offset + 1]);
            p.setVx(allData[offset + 2]);
            p.setVy(allData[offset + 3]);
        }
    }

    public int getRank() { return rank; }
    public int getMyStart() { return myStart; }
    public int getMyEnd() { return myEnd; }
}
