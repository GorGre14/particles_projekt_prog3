package com.example.chargedparticles.ui;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.simulation.SimulationFactory;
import com.example.chargedparticles.SimulationRunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * graficni interface za simulacijo
 * ima kontrol panel da lahko uporabnik spreminja lastnosti
 */
public class SimulationUI extends JPanel {

    // polja za urejanje simulacije
    private List<Particle> particles; // seznam delcev
    private SimulationParameters params; // parametri za simulacijo

    // === Fields for UI controls ===
    private JTextField particlesField; // text okno za vnos stevila delcev
    private JTextField cyclesField; // za vnos stevila ciklov
    private JComboBox<SimulationMode> modeComboBox; // dropdown za nacin simulacije
    private JButton startButton; // gumb za start
    private JButton resetButton; // gumb za reset

    // timer
    private Timer renderTimer;

    /**
     * konstruktor za SimulationUI razred
     * ustvari layout z top panelom za vnost podatkov in osrednji del za prikaz simulacije
     *
     * @param particles seznam delcev ki jih bom prikazali
     * @param params    parametri simulacije
     */
    public SimulationUI(List<Particle> particles, SimulationParameters params) {
        // Border da locimo top panel od simulacijskega okna
        super(new BorderLayout());
        this.particles = particles;
        this.params = params;

        // ustvarimo control panel za uporabniski vnos
        JPanel controlsPanel = new JPanel(new FlowLayout());

        // koliko delcev - input
        controlsPanel.add(new JLabel("Particles:"));
        particlesField = new JTextField(String.valueOf(params.getNumParticles()), 6);
        controlsPanel.add(particlesField);

        // inpuz za cikle
        controlsPanel.add(new JLabel("Cycles:"));
        cyclesField = new JTextField(String.valueOf(params.getNumCycles()), 6);
        controlsPanel.add(cyclesField);
        
        // dropdown za nacin simulacije
        controlsPanel.add(new JLabel("Mode:"));
        modeComboBox = new JComboBox<>(new SimulationMode[]{SimulationMode.SEQUENTIAL, SimulationMode.PARALLEL});
        modeComboBox.setSelectedItem(params.getSimulationMode());
        controlsPanel.add(modeComboBox);

        // start gumb za zagon simulacije
        startButton = new JButton("Start");
        controlsPanel.add(startButton);
        
        // reset gumb za ponastavitev simulacije
        resetButton = new JButton("Reset");
        controlsPanel.add(resetButton);

        // control panel postavimo na vrh
        add(controlsPanel, BorderLayout.NORTH);

        // nasstavimo zeljeno velikost za prikazno okno (se ujema z oknom simulacije)
        setPreferredSize(new Dimension(params.getWindowWidth(), params.getWindowHeight()));

        // dodamo action listenerja za start gumb
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Parsamo uporabnikov vnos
                    int newParticles = Integer.parseInt(particlesField.getText());
                    int newCycles = Integer.parseInt(cyclesField.getText());
                    SimulationMode newMode = (SimulationMode) modeComboBox.getSelectedItem();

                    // Posodobimo parametre
                    params.setNumParticles(newParticles);
                    params.setNumCycles(newCycles);
                    params.setSimulationMode(newMode);

                    // Ponastavimo delce glede na novo stevilo
                    particles.clear();
                    List<Particle> newParticles2 = SimulationRunner.generateParticles(newParticles, 
                            params.getMinX(), params.getMaxX(), params.getMinY(), params.getMaxY());
                    particles.addAll(newParticles2);

                    // Unici staro simulacijo in ustvari novo
                    if (SimulationRunner.simulation != null) {
                        SimulationRunner.simulation.shutdown();
                    }
                    SimulationRunner.simulation = SimulationFactory.createSimulation(newMode);

                    // Zaženemo simulacijo
                    SimulationRunner.restartSimulation();
                    
                    JOptionPane.showMessageDialog(SimulationUI.this,
                            "Started " + newMode.getDescription() + " simulation with " + newParticles + " particles, " + newCycles + " cycles.");

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(SimulationUI.this,
                            "Invalid number format!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // dodamo action listenerja za reset gumb
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ustavimo trenutno simulacijo
                if (SimulationRunner.simulationThread != null && SimulationRunner.simulationThread.isAlive()) {
                    SimulationRunner.simulationThread.interrupt();
                }
                
                // Ponastavimo delce na zacetne pozicije
                particles.clear();
                List<Particle> newParticles = SimulationRunner.generateParticles(params.getNumParticles(), 
                        params.getMinX(), params.getMaxX(), params.getMinY(), params.getMaxY());
                particles.addAll(newParticles);
                
                // Posodobimo text polja z trenutnimi parametri
                particlesField.setText(String.valueOf(params.getNumParticles()));
                cyclesField.setText(String.valueOf(params.getNumCycles()));
                modeComboBox.setSelectedItem(params.getSimulationMode());
                
                JOptionPane.showMessageDialog(SimulationUI.this,
                        "Simulation reset with " + params.getNumParticles() + " particles.");
            }
        });
    }

    /**
     * povozi paintComponent da zrendera delce
     * vsak delec je prikazen kot majhen krogec rdece ali modre barve
     *
     * @param g graficni objekt za izrisovanje
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // narisi vsak delec kot kroge
        for (Particle p : particles) {
            int x = (int) Math.round(p.getX());
            int y = (int) Math.round(p.getY());

            // nastavimo barvo
            if (p.getCharge() > 0) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.BLUE);
            }

            // izrisemo krogec
            g.fillOval(x, y, 4, 4);
        }
    }

    /**
     * zacnemo cas renderja v timerju
     * timer poskrbi da repaintamo na tolocenem FPS-ju
     */
    public void startRendering() {
        int delay = 1000 / params.getFps(); // izracunamo delay v ms za vsak frame
        renderTimer = new Timer(delay, e -> {
            // repaintamo zaslon
            repaint();
        });
        renderTimer.start(); // iniciliziramo timer
    }

    /**
     * ustavimo proces renderanja
     */
    public void stopRendering() {
        if (renderTimer != null) {
            renderTimer.stop(); // ustavimo timer
        }
    }
}
