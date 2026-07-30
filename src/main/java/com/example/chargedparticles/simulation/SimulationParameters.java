package com.example.chargedparticles.simulation;

/**
 * Parametri simulacije naelektrenih delcev.
 * Vsebuje vse nastavitve, potrebne za izvajanje in izris simulacije.
 */
public class SimulationParameters {

    private boolean enableUI = true;              // ali je graficni vmesnik vklopljen
    private int windowWidth = 800;                // sirina okna
    private int windowHeight = 600;               // visina okna
    private int numParticles = 400;               // stevilo delcev
    private int numCycles = 1000;                 // stevilo simulacijskih ciklov
    private double minX = 0.0, maxX = 800.0;      // meje prostora v smeri X
    private double minY = 0.0, maxY = 600.0;      // meje prostora v smeri Y
    private int fps = 60;                         // hitrost osvezevanja izrisa
    private SimulationMode simulationMode = SimulationMode.SEQUENTIAL;

    /**
     * Prebere parametre iz argumentov ukazne vrstice. Neomenjeni parametri
     * obdrzijo privzete vrednosti. Metodo uporabljata oba vstopna razreda,
     * zato so opcije v vseh nacinih enake.
     *
     * @throws IllegalArgumentException ob neznanem ali nepopolnem argumentu
     */
    public static SimulationParameters fromArgs(String[] args) {
        SimulationParameters p = new SimulationParameters();
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--ui":
                        p.enableUI = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--window":
                        p.windowWidth = Integer.parseInt(args[++i]);
                        p.windowHeight = Integer.parseInt(args[++i]);
                        break;
                    case "--particles":
                        p.numParticles = Integer.parseInt(args[++i]);
                        break;
                    case "--cycles":
                        p.numCycles = Integer.parseInt(args[++i]);
                        break;
                    case "--bounds":
                        p.minX = Double.parseDouble(args[++i]);
                        p.maxX = Double.parseDouble(args[++i]);
                        p.minY = Double.parseDouble(args[++i]);
                        p.maxY = Double.parseDouble(args[++i]);
                        break;
                    case "--fps":
                        p.fps = Integer.parseInt(args[++i]);
                        break;
                    case "--mode":
                        String arg = args[++i];
                        SimulationMode mode = SimulationMode.fromCommandLineArg(arg);
                        if (mode == null) {
                            throw new IllegalArgumentException("Neznan nacin simulacije: " + arg);
                        }
                        p.simulationMode = mode;
                        break;
                    default:
                        throw new IllegalArgumentException("Neznan argument: " + args[i]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Manjka vrednost pri zadnjem argumentu");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Napacen format stevila: " + e.getMessage());
        }
        return p;
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
    public SimulationMode getSimulationMode() { return simulationMode; }

    // Setterji za spreminjanje iz graficnega vmesnika
    public void setEnableUI(boolean enableUI) { this.enableUI = enableUI; }
    public void setNumParticles(int numParticles) { this.numParticles = numParticles; }
    public void setNumCycles(int numCycles) { this.numCycles = numCycles; }
    public void setSimulationMode(SimulationMode simulationMode) { this.simulationMode = simulationMode; }
}
