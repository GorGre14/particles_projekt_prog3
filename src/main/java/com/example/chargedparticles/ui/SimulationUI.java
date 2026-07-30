package com.example.chargedparticles.ui;

import com.example.chargedparticles.SimulationRunner;
import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.simulation.SimulationParameters;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Graficni vmesnik simulacije: izris delcev in kontrole za zagon.
 *
 * Sekvencni in vzporedni nacin tecejo v tem procesu. Porazdeljeni nacin
 * potrebuje vec MPI procesov, zato ga vmesnik zazene s skripto
 * run_distributed.sh, ki odpre svoje okno.
 */
public class SimulationUI extends JPanel {

    private static final int PARTICLE_SIZE = 4;

    private final List<Particle> particles;
    private final SimulationParameters params;

    private final JTextField particlesField;
    private final JTextField cyclesField;
    private final JComboBox<SimulationMode> modeBox;
    private final JLabel processesLabel = new JLabel("Procesi:");
    private final JTextField processesField = new JTextField("4", 3);

    private Timer renderTimer;

    public SimulationUI(List<Particle> particles, SimulationParameters params) {
        super(new BorderLayout());
        this.particles = particles;
        this.params = params;

        particlesField = new JTextField(String.valueOf(params.getNumParticles()), 6);
        cyclesField = new JTextField(String.valueOf(params.getNumCycles()), 6);
        modeBox = new JComboBox<>(SimulationMode.values());
        modeBox.setSelectedItem(params.getSimulationMode());

        JPanel controls = new JPanel(new FlowLayout());
        controls.add(new JLabel("Delci:"));
        controls.add(particlesField);
        controls.add(new JLabel("Cikli:"));
        controls.add(cyclesField);
        controls.add(new JLabel("Nacin:"));
        controls.add(modeBox);
        controls.add(processesLabel);
        controls.add(processesField);

        JButton startButton = new JButton("Start");
        JButton resetButton = new JButton("Reset");
        controls.add(startButton);
        controls.add(resetButton);

        // Stevilo procesov je smiselno samo pri porazdeljenem nacinu.
        modeBox.addActionListener(e -> updateProcessesVisibility());
        updateProcessesVisibility();

        startButton.addActionListener(e -> onStart());
        resetButton.addActionListener(e -> onReset());

        add(controls, BorderLayout.NORTH);
        setPreferredSize(new Dimension(params.getWindowWidth(), params.getWindowHeight()));
    }

    private SimulationMode selectedMode() {
        return (SimulationMode) modeBox.getSelectedItem();
    }

    private void updateProcessesVisibility() {
        boolean distributed = selectedMode() == SimulationMode.DISTRIBUTED;
        processesLabel.setVisible(distributed);
        processesField.setVisible(distributed);
        revalidate();
        repaint();
    }

    /** Prebere vnosna polja v parametre; vrne false ob napacnem vnosu. */
    private boolean readInputFields() {
        try {
            params.setNumParticles(Integer.parseInt(particlesField.getText().trim()));
            params.setNumCycles(Integer.parseInt(cyclesField.getText().trim()));
            params.setSimulationMode(selectedMode());
            return true;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Stevilo delcev in ciklov mora biti celo stevilo.",
                    "Napacen vnos", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void onStart() {
        if (!readInputFields()) {
            return;
        }
        if (selectedMode() == SimulationMode.DISTRIBUTED) {
            startDistributed();
        } else {
            SimulationRunner.restartSimulation();
        }
    }

    private void onReset() {
        if (!readInputFields()) {
            return;
        }
        SimulationRunner.stopSimulation();
        particles.clear();
        particles.addAll(SimulationRunner.generateParticles(params));
        repaint();
    }

    /**
     * Zazene porazdeljeni nacin. Skripta run_distributed.sh prevede projekt in
     * ga pozene z mpjrun.sh v zahtevanem stevilu MPI procesov; proces z rangom
     * 0 odpre svoje okno.
     */
    private void startDistributed() {
        int processes;
        try {
            processes = Integer.parseInt(processesField.getText().trim());
        } catch (NumberFormatException ex) {
            processes = 0;
        }
        if (processes < 1) {
            JOptionPane.showMessageDialog(this, "Stevilo procesov mora biti vsaj 1.",
                    "Napacen vnos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] command = {
                "./run_distributed.sh", String.valueOf(processes),
                "--particles", String.valueOf(params.getNumParticles()),
                "--cycles", String.valueOf(params.getNumCycles()),
                "--ui", "true"
        };

        int choice = JOptionPane.showConfirmDialog(this,
                "Porazdeljeni nacin tece v " + processes + " locenih MPI procesih,\n"
                        + "zato se odpre v svojem oknu.\n\n"
                        + "Zagnal bom ukaz:\n" + String.join(" ", command) + "\n\n"
                        + "Zagon lahko traja nekaj sekund (prevajanje in zagon MPJ Express).",
                "Porazdeljeni nacin (MPJ Express)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            new ProcessBuilder(command)
                    .directory(new File(System.getProperty("user.dir")))
                    .inheritIO()
                    .start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Zagona ni bilo mogoce sprozit: " + ex.getMessage() + "\n\n"
                            + "Skripto lahko zazenete rocno v terminalu:\n"
                            + String.join(" ", command),
                    "Napaka pri zagonu", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Izris delcev: rdeci so pozitivno, modri negativno nabiti. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Particle p : particles) {
            g.setColor(p.getCharge() > 0 ? Color.RED : Color.BLUE);
            g.fillOval((int) Math.round(p.getX()), (int) Math.round(p.getY()),
                    PARTICLE_SIZE, PARTICLE_SIZE);
        }
    }

    /** Zacne osvezevanje izrisa z nastavljeno hitrostjo. */
    public void startRendering() {
        renderTimer = new Timer(1000 / params.getFps(), e -> repaint());
        renderTimer.start();
    }

}
