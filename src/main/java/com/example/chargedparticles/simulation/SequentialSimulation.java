package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Sekvencna simulacija naelektrenih delcev.
 * Izvaja izracune na enem samem nizu (single-threaded).
 *
 * Algoritem:
 * 1. Faza: Izracun sil med vsemi pari delcev - O(n^2)
 * 2. Faza: Posodobitev pozicij in hitrosti - O(n)
 */
public class SequentialSimulation implements Simulation {

    /**
     * Izvede en cikel simulacije.
     *
     * @param particles seznam delcev
     * @param params    parametri simulacije
     */
    @Override
    public void performOneCycle(List<Particle> particles, SimulationParameters params) {
        int n = particles.size();
        double[][] forces = new double[n][2]; // Shramba sil za vsak delec [Fx, Fy]

        // === FAZA 1: Izracun sil za vsak delec ===
        for (int i = 0; i < n; i++) {
            Particle p1 = particles.get(i);
            double fxSum = 0.0;
            double fySum = 0.0;

            // Izracunamo silo od vseh ostalih delcev
            for (int j = 0; j < n; j++) {
                if (i == j) continue; // Delec ne vpliva sam nase

                Particle p2 = particles.get(j);
                double[] f = ForceUtils.computeParticleForce(p1, p2);
                fxSum += f[0];
                fySum += f[1];
            }

            // Dodamo mejno silo (da delci ostanejo v oknu)
            double[] bf = ForceUtils.computeBoundaryForce(p1, params);
            fxSum += bf[0];
            fySum += bf[1];

            // Shranimo izracunane sile
            forces[i][0] = fxSum;
            forces[i][1] = fySum;
        }

        // === FAZA 2: Posodobitev pozicij in hitrosti ===
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);

            double fx = forces[i][0];
            double fy = forces[i][1];

            // Pospesek: a = F / m (Newton)
            double ax = fx / p.getMass();
            double ay = fy / p.getMass();

            // Nova hitrost: v = v + a
            double newVx = p.getVx() + ax;
            double newVy = p.getVy() + ay;

            // Nova pozicija: x = x + v
            double newX = p.getX() + newVx;
            double newY = p.getY() + newVy;

            // Posodobimo delec
            p.setVx(newVx);
            p.setVy(newVy);
            p.setX(newX);
            p.setY(newY);
        }
    }

    @Override
    public String getDescription() {
        return "Sekvencna";
    }
}
