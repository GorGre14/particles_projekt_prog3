package com.example.chargedparticles.simulation;

/**
 * Parametri simulacije naelektrenih delcev.
 * Vsebuje vse nastavitve potrebne za izvajanje simulacije.
 */
public class SimulationParameters {

    private boolean enableUI;    // Ali je graficni vmesnik vklopljen
    private int windowWidth;     // Sirina okna
    private int windowHeight;    // Visina okna
    private int numParticles;    // Stevilo delcev
    private int numCycles;       // Stevilo simulacijskih ciklov
    private double minX, maxX;   // Meje prostora v X smeri
    private double minY, maxY;   // Meje prostora v Y smeri
    private int fps;             // Hitrost osvezevanja (frames per second)

    public SimulationParameters(boolean enableUI, int windowWidth, int windowHeight,
                                int numParticles, int numCycles,
                                double minX, double maxX, double minY, double maxY,
                                int fps) {
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
    }

    // Getterji
    public boolean isEnableUI() { return enableUI; }
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public int getNumParticles() { return numParticles; }
    public int getNumCycles() { return numCycles; }
    public double getMinX() { return minX; }
    public double getMaxX() { return maxX; }
    public double getMinY() { return minY; }
    public double getMaxY() { return maxY; }
    public int getFps() { return fps; }

    // Setterji za dinamicno spreminjanje
    public void setNumParticles(int numParticles) { this.numParticles = numParticles; }
    public void setNumCycles(int numCycles) { this.numCycles = numCycles; }
}
