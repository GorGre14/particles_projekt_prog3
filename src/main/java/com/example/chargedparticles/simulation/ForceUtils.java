package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;

/**
 * Pomozni razred za izracun sil v simulaciji naelektrenih delcev.
 * Vsebuje metode za izracun Coulombove sile med delci in mejne sile.
 */
public class ForceUtils {

    /**
     * Izracuna silo med dvema delcema po modificiranem Coulombovem zakonu.
     * Formula: F = |c1 * c2| / d^2
     *
     * Ce imata delca enak predznak naboja, se privlacita.
     * Ce imata razlicen predznak, se odbijata.
     *
     * @param p1 delec, ki izkusa silo
     * @param p2 delec, ki povzroca silo
     * @return 2D vektor sile [Fx, Fy]
     */
    public static double[] computeParticleForce(Particle p1, Particle p2) {
        // Izracun razdalje med delcema
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        // Kvadrat razdalje (izognemo se korenjenju)
        double r2 = dx * dx + dy * dy;

        // Minimum 1 pixel apart to prevent force explosion
        if (r2 < 1.0) {
            r2 = 1.0;
        }
        double r = Math.sqrt(r2);

        double c1 = p1.getCharge();
        double c2 = p2.getCharge();

        // Magnituda sile po Coulombovem zakonu
        double magnitude = Math.abs(c1 * c2) / r2;

        // Dolocitev predznaka (enak naboj = odbijanje, razlicen = privlacenje)
        // To ustreza Coulombovemu zakonu: enaki naboji se odbijajo, nasprotni privlacijo
        double sign = (c1 * c2 >= 0) ? 1.0 : -1.0;

        // Komponente sile
        double Fx = sign * magnitude * (dx / r);
        double Fy = sign * magnitude * (dy / r);

        return new double[]{Fx, Fy};
    }

    /**
     * Izracuna odbojno silo ob mejah simulacijskega prostora.
     * Prepreci, da delci zapustijo simulacijsko obmocje.
     *
     * @param p      delec
     * @param params parametri simulacije z mejami prostora
     * @return 2D vektor mejne sile [Fx, Fy]
     */
    public static double[] computeBoundaryForce(Particle p, SimulationParameters params) {
        double fx = 0.0;
        double fy = 0.0;
        double buffer = 5.0;       // Razdalja od meje, ki sprozi odboj
        double repelFactor = 10.0; // Moc odbojne sile

        // Levi rob
        if (p.getX() < params.getMinX() + buffer) {
            double dist = Math.max(0.1, p.getX() - params.getMinX());
            fx += repelFactor / (dist * dist);
        }
        // Desni rob
        if (p.getX() > params.getMaxX() - buffer) {
            double dist = Math.max(0.1, params.getMaxX() - p.getX());
            fx -= repelFactor / (dist * dist);
        }
        // Zgornji rob
        if (p.getY() < params.getMinY() + buffer) {
            double dist = Math.max(0.1, p.getY() - params.getMinY());
            fy += repelFactor / (dist * dist);
        }
        // Spodnji rob
        if (p.getY() > params.getMaxY() - buffer) {
            double dist = Math.max(0.1, params.getMaxY() - p.getY());
            fy -= repelFactor / (dist * dist);
        }

        return new double[]{fx, fy};
    }
}
