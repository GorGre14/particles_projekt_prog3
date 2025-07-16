package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

public class SequentialSimulation implements Simulation {

    /**
     * razrede izvede samo en cikel simulacije. Vsak cikel vsebuje
     * 1) izracun sil za vsak delec
     * 2) posodabljanje pozicije in hitrosti
     */
    @Override
    public void performOneCycle(List<Particle> particles, SimulationParameters params) {
        int n = particles.size();
        double[][] forces = new double[n][2];

        // 1. izracun sil na vsak delec
        for (int i = 0; i < n; i++) {
            Particle p1 = particles.get(i);
            double fxSum = 0.0;             // sila v x smer
            double fySum = 0.0;             // sila v y smer

            for (int j = 0; j < n; j++) {
                if (i == j) continue;       // delec samega sebe ne vkljuci v izracun sil
                Particle p2 = particles.get(j);
                //izracun sile ki jo delec j povzorca na delelc i
                double[] f = ForceUtils.computeParticleForce(p1, p2);
                fxSum += f[0];
                fySum += f[1];
            }

            // dodamo robove zato da delci ostanejo v simulacijskem oknu
            double[] bf = ForceUtils.computeBoundaryForce(p1, params);
            fxSum += bf[0];
            fySum += bf[1];
            // hranimo sile za delec i
            forces[i][0] = fxSum;
            forces[i][1] = fySum;
        }

        // 2. posodobimo pozicije
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);
            // pridobimo sile
            double fx = forces[i][0];
            double fy = forces[i][1];
            // izracunamo pospesek ( F = ma)
            double ax = fx / p.getMass();
            double ay = fy / p.getMass();
            // posodibmo hitrost z uporabo pospeska ( v = v + a)
            double newVx = p.getVx() + ax;
            double newVy = p.getVy() + ay;
            // posodobimo pozicijo z uporabo hitrosti (x = x + v)
            double newX = p.getX() + newVx;
            double newY = p.getY() + newVy;
            //nastavimo posodobljeno hitrost in pozicijo delca
            p.setVx(newVx);
            p.setVy(newVy);
            p.setX(newX);
            p.setY(newY);
        }
    }
    
    @Override
    public String getDescription() {
        return "Sequential";
    }
}
