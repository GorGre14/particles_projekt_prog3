
package com.example.chargedparticles.ui;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.SimulationParameters;
import com.example.chargedparticles.simulation.SimulationMode;
import com.example.chargedparticles.SimulationRunner;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
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

        // Izbira nacina simulacije
        controlsPanel.add(new JLabel("Nacin:"));
        modeComboBox = new JComboBox<>(new String[]{"sequential", "parallel", "distributed"});
        modeComboBox.setSelectedItem(params.getSimulationMode().getCommandLineArg());
        controlsPanel.add(modeComboBox);

        // Polje za stevilo MPI procesov (vidno samo pri porazdeljenem nacinu)
        JLabel processesLabel = new JLabel("Procesi:");
        JTextField processesField = new JTextField("4", 3);
        processesLabel.setVisible(false);
        processesField.setVisible(false);
        controlsPanel.add(processesLabel);
        controlsPanel.add(processesField);

        modeComboBox.addActionListener(e -> {
            boolean dist = "distributed".equals(modeComboBox.getSelectedItem());
            processesLabel.setVisible(dist);
            processesField.setVisible(dist);
            controlsPanel.revalidate();
            controlsPanel.repaint();
        });

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

                if (newMode == SimulationMode.DISTRIBUTED) {
                    int np = Integer.parseInt(processesField.getText());
                    String msg = "<html><body style='width: 450px; font-family: sans-serif;'>"
                            + "<h2 style='color: #2c3e50; margin-top: 0; border-bottom: 2px solid #3498db; padding-bottom: 5px;'>Porazdeljeni način (MPI)</h2>"
                            + "<p style='color: #34495e; font-size: 12px; line-height: 1.4;'>"
                            + "Porazdeljeni način deluje preko knjižnice <b>MPJ Express</b> v ločenih JVM pomnilniških prostorih, "
                            + "kar zagotavlja vrhunsko zmogljivost pri večjem številu delcev (npr. 4000) brez contentiona na pomnilniškem vodilu.<br>"
                            + "Zato ga ni mogoče neposredno pognati znotraj tega istega okna."
                            + "</p>"
                            + "<div style='background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 4px; padding: 10px; margin-bottom: 12px; color: #8a6d3b; font-size: 11px;'>"
                            + "<b>Predpogoji za zagon:</b><br>"
                            + "• Nameščen MPJ Express (različica 0.44)<br>"
                            + "• Nastavljena spremenljivka <code>MPJ_HOME</code> (npr. <code>export MPJ_HOME=/usr/local/mpj</code>)"
                            + "</div>"
                            + "<b style='color: #2c3e50; font-size: 12px;'>Zagon iz terminala (priporočeno):</b><br>"
                            + "<div style='background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; padding: 8px; font-family: monospace; font-size: 11px; margin-top: 4px; margin-bottom: 10px; color: #333;'>"
                            + "./run_distributed.sh " + np + " --particles " + newParticles + " --cycles " + newCycles + " --ui true"
                            + "</div>"
                            + "<p style='font-size: 11px; color: #34495e;'>"
                            + "Lahko pa kliknete spodnji gumb <b>\"Zaženi samodejno\"</b>, ki bo v ozadju zagnal skripto in samodejno odprl novo vizualizacijsko okno za porazdeljeno simulacijo."
                            + "</p>"
                            + "<p style='font-size: 10px; color: #7f8c8d; margin-top: 5px;'>"
                            + "<i>Nasvet: Besedilo zgoraj lahko enostavno izberete in prekopirate (Ctrl+C / Cmd+C).</i>"
                            + "</p>"
                            + "</body></html>";

                    JEditorPane editorPane = new JEditorPane("text/html", msg);
                    editorPane.setEditable(false);
                    editorPane.setBackground(new Color(0, 0, 0, 0)); // transparent
                    editorPane.setOpaque(false);
                    editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

                    JScrollPane scrollPane = new JScrollPane(editorPane);
                    scrollPane.setBorder(null);
                    scrollPane.setPreferredSize(new Dimension(480, 420));

                    Object[] options = {"Zaženi samodejno", "Zapri / Kopiraj navodila"};
                    int choice = JOptionPane.showOptionDialog(
                            this,
                            scrollPane,
                            "Zagon porazdeljenega načina (MPI)",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

                    if (choice == 0) { // Zaženi samodejno
                        try {
                            ProcessBuilder pb = new ProcessBuilder("./run_distributed.sh",
                                    String.valueOf(np),
                                    "--particles", String.valueOf(newParticles),
                                    "--cycles", String.valueOf(newCycles),
                                    "--ui", "true");
                            pb.directory(new java.io.File(System.getProperty("user.dir")));

                            // Nastavimo okoljske spremenljivke
                            java.util.Map<String, String> env = pb.environment();
                            env.putAll(System.getenv());

                            // Nastavimo MPJ_HOME, če ni nastavljen, a obstaja privzeta pot
                            if (env.get("MPJ_HOME") == null) {
                                java.io.File defaultMpj = new java.io.File("/Users/gregorantonaz/mpj");
                                if (defaultMpj.exists()) {
                                    env.put("MPJ_HOME", defaultMpj.getAbsolutePath());
                                }
                            }

                            // Nastavimo JAVA_HOME, če ni nastavljen, a obstaja Homebrew Java
                            if (env.get("JAVA_HOME") == null) {
                                java.io.File homebrewJava = new java.io.File("/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home");
                                if (homebrewJava.exists()) {
                                    env.put("JAVA_HOME", homebrewJava.getAbsolutePath());
                                    env.put("PATH", homebrewJava.getAbsolutePath() + "/bin:" + env.get("PATH"));
                                }
                            }

                            pb.start();
                            JOptionPane.showMessageDialog(this,
                                    "Zagon uspešno sprožen v ozadju!\nV kratkem se bo odprlo novo okno s porazdeljeno simulacijo.",
                                    "Zagon uspešen",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this,
                                    "Napaka pri samodejnem zagonu: " + ex.getMessage() + "\nProsimo, zaženite skripto ročno v terminalu.",
                                    "Napaka pri zagonu",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    // Ustavimo staro simulacijo preden posegamo v seznam delcev
                    SimulationRunner.stopSimulation();

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
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Napacen format stevila!", "Napaka", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Listener za Reset gumb
        resetButton.addActionListener(e -> {
            // Ustavimo simulacijo in pocakamo na zakljucek
            SimulationRunner.stopSimulation();

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
