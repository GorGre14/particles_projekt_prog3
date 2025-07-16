package com.example.chargedparticles.simulation;
import com.example.chargedparticles.model.Particle;

/**
 * Pomozni razred za izracun sil v simulaciji naelektrenih delcev
 * Vključuje metode za izračun sil med delci in sil, ki jih povzročajo meje simulacijskega prostora.
 */

public class ForceUtils {

    /**
     * formula:
     *    F = |c1 * c2| / d^2
     * ce imata delca enak predznak naboja se privlacita.
     *
     * @param p1 delec ki izkusa silo
     * @param p2 delec, ki povzroca silo
     * @return 2D vektor sile [Fx, Fy], kjer sta Fx in Fy komponenti sile
     */
    public static double[] computeParticleForce(Particle p1, Particle p2) {
        // Izračun komponent razdalje med delcema
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        // Izračun kvadrata razdalje (da se izognemo nepotrebnemu korenjenju)
        double r2 = dx*dx + dy*dy;

        // Preprečevanje deljenja z ničlo z uporabo minimalne razdalje
        if (r2 < 1e-12) {
            r2 = 1e-12;
        }
        double r = Math.sqrt(r2);       // dejanska razdalja

        double c1 = p1.getCharge();
        double c2 = p2.getCharge();

        // izracun magnitude sile
        double magnitude = Math.abs(c1 * c2) / r2;

        // Določitev predznaka sile (privlačenje, če imata enak predznak, odbijanje sicer)

        double sign = (c1 * c2 >= 0) ? 1.0 : -1.0;

        // Izračun komponent sile
        double Fx = sign * magnitude * (dx / r);
        double Fy = sign * magnitude * (dy / r);

        return new double[]{Fx, Fy};
    }

    /**
     * Izracuna silo ob mejah simulacijskega prostora, da prepreci izhod delcev iz obmcja.
     * ce je delcec blizu ali preko meje, se uporabi odbojna sila, ki ga potisne nazaj
     * v simulacijsko obmocje.
     *
     * @param p      Delec, za katerega izračunavamo mejno silo.
     * @param params Parametri simulacije, ki dolocajo meje simulacijskega prostora.
     * @return 2D vektor sile [Fx, Fy], kjer sta Fx in Fy komponenti mejne sile.
     */
    public static double[] computeBoundaryForce(Particle p, SimulationParameters params) {
        double fx = 0.0;    //sila v smeri x
        double fy = 0.0;    // sila v smeri y
        double buffer = 5.0;      // razdalja od meje ki sprozi odbojno silo
        double repelFactor = 10.0; // faktor za odbojno silo

        // Preverjanje posameznih mej in uporaba odbojnih sil, ce je delec preblizu

        if (p.getX() < params.getMinX() + buffer) {
            double dist = Math.max(1e-12, p.getX() - params.getMinX());
            fx += repelFactor / (dist * dist);
        }
        // desni rob
        if (p.getX() > params.getMaxX() - buffer) {
            double dist = Math.max(1e-12, params.getMaxX() - p.getX());
            fx -= repelFactor / (dist * dist);
        }
        // zgornji
        if (p.getY() < params.getMinY() + buffer) {
            double dist = Math.max(1e-12, p.getY() - params.getMinY());
            fy += repelFactor / (dist * dist);
        }
        // levi
        if (p.getY() > params.getMaxY() - buffer) {
            double dist = Math.max(1e-12, params.getMaxY() - p.getY());
            fy -= repelFactor / (dist * dist);
        }

        return new double[]{fx, fy};
    }
}
