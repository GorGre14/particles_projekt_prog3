package com.example.chargedparticles.ui;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.SimulationRunner;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Graficni vmesnik za simulacijo naelektrenih delcev.
 * Omogoca vizualizacijo delcev in kontrolo simulacije.
 */
public class SimulationUI extends JPanel {

    private List<Particle> particles;
    private SimulationParameters params;

    // Kontrole
    private JTextField particlesField;
    private JTextField cyclesField;
    private JComboBox<String> modeComboBox;
    private JButton startButton;
    private JButton resetButton;
    private Timer renderTimer;

    /**
     * Konstruktor za SimulationUI.
     *
     * @param particles seznam delcev za prikaz
     * @param params    parametri simulacije
     */
    public SimulationUI(List<Particle> particles, SimulationParameters params) {
        super(new BorderLayout());
        this.particles = particles;
        this.params = params;

        // Kontrolni panel
        JPanel controlsPanel = new JPanel(new FlowLayout());

        // Vnos stevila delcev
        controlsPanel.add(new JLabel("Delci:"));
        particlesField = new JTextField(String.valueOf(params.getNumParticles()), 6);
        controlsPanel.add(particlesField);

        // Vnos stevila ciklov
        controlsPanel.add(new JLabel("Cikli:"));
        cyclesField = new JTextField(String.valueOf(params.getNumCycles()), 6);
        controlsPanel.add(cyclesField);

        // Izbira nacina simulacije (samo sekvencna in vzporedna - porazdeljena se zazene loceno)
        controlsPanel.add(new JLabel("Nacin:"));
        modeComboBox = new JComboBox<>(new String[]{"sequential", "parallel"});
        modeComboBox.setSelectedItem(params.getSimulationMode().getCommandLineArg());
        controlsPanel.add(modeComboBox);

        // Start gumb
        startButton = new JButton("Start");
        controlsPanel.add(startButton);

        // Reset gumb
        resetButton = new JButton("Reset");
        controlsPanel.add(resetButton);

        add(controlsPanel, BorderLayout.NORTH);
        setPreferredSize(new Dimension(params.getWindowWidth(), params.getWindowHeight()));

        // Listener za Start gumb
        startButton.addActionListener(e -> {
            try {
                int newParticles = Integer.parseInt(particlesField.getText());
                int newCycles = Integer.parseInt(cyclesField.getText());
                String selectedMode = (String) modeComboBox.getSelectedItem();
                SimulationMode newMode = SimulationMode.fromCommandLineArg(selectedMode);

                params.setNumParticles(newParticles);
                params.setNumCycles(newCycles);
                params.setSimulationMode(newMode);

                // Ponastavimo delce
                particles.clear();
                particles.addAll(SimulationRunner.generateParticles(newParticles,
                        params.getMinX(), params.getMaxX(),
                        params.getMinY(), params.getMaxY()));

                // Zazenemo simulacijo
                SimulationRunner.restartSimulation();

                JOptionPane.showMessageDialog(this,
                        "Simulacija zagnana (" + newMode.getDescription() + "): "
                        + newParticles + " delcev, " + newCycles + " ciklov.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Napacen format stevila!", "Napaka", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Listener za Reset gumb
        resetButton.addActionListener(e -> {
            // Ustavimo simulacijo
            if (SimulationRunner.simulationThread != null &&
                SimulationRunner.simulationThread.isAlive()) {
                SimulationRunner.simulationThread.interrupt();
            }

            // Ponastavimo delce
            particles.clear();
            particles.addAll(SimulationRunner.generateParticles(params.getNumParticles(),
                    params.getMinX(), params.getMaxX(),
                    params.getMinY(), params.getMaxY()));

            // Posodobimo polja
            particlesField.setText(String.valueOf(params.getNumParticles()));
            cyclesField.setText(String.valueOf(params.getNumCycles()));

            JOptionPane.showMessageDialog(this,
                    "Simulacija ponastavljena: " + params.getNumParticles() + " delcev.");
        });
    }

    /**
     * Izrise delce na zaslon.
     * Rdeci krogi = pozitivni naboj, modri = negativni naboj.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Particle p : particles) {
            int x = (int) Math.round(p.getX());
            int y = (int) Math.round(p.getY());

            // Barva glede na naboj
            if (p.getCharge() > 0) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.BLUE);
            }

            g.fillOval(x, y, 4, 4);
        }
    }

    /**
     * Zacne renderiranje z dolocenim FPS.
     */
    public void startRendering() {
        int delay = 1000 / params.getFps();
        renderTimer = new Timer(delay, e -> repaint());
        renderTimer.start();
    }

    /**
     * Ustavi renderiranje.
     */
    public void stopRendering() {
        if (renderTimer != null) {
            renderTimer.stop();
        }
    }
}
