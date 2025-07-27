package com.example.chargedparticles.distributed;

import java.io.Serializable;

/**
 * Serializabilna predstava delca za omrežno komunikacijo.
 * Vsebuje vse potrebne podatke o delcu, ki jih je treba prenesti med vozlišči.
 */
public class ParticleState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int id;
    private final double x;
    private final double y;
    private final double vx;
    private final double vy;
    private final double charge;
    private final double mass;
    
    public ParticleState(int id, double x, double y, double vx, double vy, double charge, double mass) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.charge = charge;
        this.mass = mass;
    }
    
    // Getters
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getCharge() { return charge; }
    public double getMass() { return mass; }
    
    @Override
    public String toString() {
        return String.format("ParticleState[id=%d, pos=(%.2f,%.2f), vel=(%.2f,%.2f), charge=%.2f]", 
                           id, x, y, vx, vy, charge);
    }
}