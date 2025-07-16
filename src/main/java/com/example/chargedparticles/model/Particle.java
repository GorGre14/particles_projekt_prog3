package com.example.chargedparticles.model;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

/**
 * prikaz delcev v 2d prostoru
 * vsak delec ima neko pozicijo, hitrost, naboj in maso
 * razred vsebuje utility metode za kreacijo delcev z nakljucnimi lastnostmi
 */
public class Particle {

    // Position coordinates
    private double x; // X-koordinata delca
    private double y; // Y-koordinata delca

    // Velocity components
    private double vx; // hitrsot v smeri X
    private double vy; // hitrost v smeri y

    // nabitost delca (pozitiven ali negativen)
    private double charge;

    // masa delca
    private double mass = 1.0;

    /**
     * konstruiramo delec
     *
     * @param x     zacetna x koordinata
     * @param y     zacetna y koordinata
     * @param vx    zacetna hitrost v smeri x
     * @param vy   zacetna hitrost v smeri y
     * @param charge nabitost delca (poz. ali neg.)
     */
    public Particle(double x, double y, double vx, double vy, double charge) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.charge = charge;
    }

    /**
     * metoda za ustvarjanje naključnega delca znotraj določenih meja
     * Položaj je naključno porazdeljen znotraj določenega obsega in hitrosti
     * komponente so enakomerno razporejene med 1 in -1
     * naključno assignamo naboj delcev
     * med 0.5 in 1.5
     *
     * @param minX minimalna x koordinata.
     * @param maxX maksimalna
     * @param minY minimalna y
     * @param maxY maksimalna
     * @return delec z nakljucnimi lastnostmi
     */
    public static Particle randomParticle(double minX, double maxX, double minY, double maxY) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double x = rand.nextDouble(minX, maxX); // nakljucna X pozicija
        double y = rand.nextDouble(minY, maxY); // nakljucna Y pozicija
        double vx = rand.nextDouble(-1.0, 1.0); // nakljucna hitrost v x
        double vy = rand.nextDouble(-1.0, 1.0); // nakljucna hitrost v y
        // nakljucen naboj: pozitiven alpa negative
        double charge = rand.nextBoolean() ? rand.nextDouble(0.5, 1.5) : -rand.nextDouble(0.5, 1.5);
        return new Particle(x, y, vx, vy, charge);
    }
    
    /**
     * metoda za ustvarjanje naključnega delca z determinističnim seed-om
     * Zagotavlja, da so vsi načini simulacije zagnani z isto začetno razporeditvijo
     *
     * @param minX minimalna x koordinata.
     * @param maxX maksimalna
     * @param minY minimalna y
     * @param maxY maksimalna
     * @param random Random objekt z določenim seed-om
     * @return delec z nakljucnimi lastnostmi
     */
    public static Particle randomParticle(double minX, double maxX, double minY, double maxY, Random random) {
        double x = minX + random.nextDouble() * (maxX - minX); // nakljucna X pozicija
        double y = minY + random.nextDouble() * (maxY - minY); // nakljucna Y pozicija
        double vx = random.nextDouble() * 2.0 - 1.0; // nakljucna hitrost v x [-1, 1]
        double vy = random.nextDouble() * 2.0 - 1.0; // nakljucna hitrost v y [-1, 1]
        // nakljucen naboj: pozitiven alpa negative
        double charge = random.nextBoolean() ? 0.5 + random.nextDouble() : -(0.5 + random.nextDouble());
        return new Particle(x, y, vx, vy, charge);
    }


    //getterji in setterji za lastnosti delcev
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getVx() {
        return vx;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public double getCharge() {
        return charge;
    }

    public double getMass() {
        return mass;
    }

    /**
     * v stringu izpisemo stanje delcev za potrebe debugganja
     * @return string, ki vsebuje podatke o dolocenem delcu
     */
    @Override
    public String toString() {
        return String.format("Particle(x=%.2f, y=%.2f, vx=%.2f, vy=%.2f, charge=%.2f)",
                x, y, vx, vy, charge);
    }
}
