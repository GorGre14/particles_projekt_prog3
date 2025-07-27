package com.example.chargedparticles.simulation;

import java.io.Serializable;

public class SimulationParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    // UI vkloplje True / False
    private boolean enableUI;
    // sirina okna in dolzina
    private int windowWidth;
    private int windowHeight;

    // stevilo delcev
    private int numParticles;
    // stevilo ciklov
    private int numCycles;

    // robovi
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    // fps
    private int fps;
    
    // simulation mode
    private SimulationMode simulationMode;

    public SimulationParameters(boolean enableUI,
                                int windowWidth,
                                int windowHeight,
                                int numParticles,
                                int numCycles,
                                double minX,
                                double maxX,
                                double minY,
                                double maxY,
                                int fps,
                                SimulationMode simulationMode) {
        this.enableUI = enableUI;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.numParticles = numParticles;
        this.numCycles = numCycles;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.fps = fps;
        this.simulationMode = simulationMode;
    }

    // getterji in setterji
    public boolean isEnableUI() {
        return enableUI;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getNumParticles() {
        return numParticles;
    }

    public int getNumCycles() {
        return numCycles;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public void setNumParticles(int numParticles) {
        this.numParticles = numParticles;
    }

    public void setNumCycles(int numCycles) {
        this.numCycles = numCycles;
    }

    public double getMaxY() {
        return maxY;
    }

    public int getFps() {
        return fps;
    }
    
    public SimulationMode getSimulationMode() {
        return simulationMode;
    }
    
    public void setSimulationMode(SimulationMode simulationMode) {
        this.simulationMode = simulationMode;
    }
}