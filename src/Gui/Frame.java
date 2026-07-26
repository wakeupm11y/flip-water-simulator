package Gui;

import Solver.Solver;
import Solver.Grid;
import Particles.ParticleFactory;
import Particles.Particle;
import Boundaries.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;

/**
 * SimFrame contains:
 *  1. A static factory method that shows a setup dialog and returns configured options.
 *  2. The main simulation JFrame that hosts the Canvas and a control bar.
 */
public class Frame extends JFrame {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(10,  12,  20);
    private static final Color BG_PANEL   = new Color(18,  22,  35);
    private static final Color ACCENT     = new Color(100, 180, 255);
    private static final Color ACCENT_DIM = new Color(60,  110, 170);
    private static final Color TEXT       = new Color(200, 215, 235);
    private static final Color TEXT_DIM   = new Color(100, 120, 150);
    private static final Font  MONO       = new Font("JetBrains Mono", Font.PLAIN, 13);
    private static final Font  MONO_SM    = new Font("JetBrains Mono", Font.PLAIN, 11);
    private static final Font  LABEL_FONT = MONO_SM;

    // ── simulation state ─────────────────────────────────────────────────────
    private final Canvas     canvas;
    private final Solver     solver;
    private Timer timer;
    private       boolean    running = false;
    private       JLabel     fpsLabel;
    private       long       lastFrameMs = System.currentTimeMillis();

    // ─────────────────────────────────────────────────────────────────────────
    // Setup dialog
    // ─────────────────────────────────────────────────────────────────────────

    /** Holds the values collected from the setup dialog. */
    public record SimConfig(
        int    particleCount,
        int    gridX,
        int    gridY,
        float cellSize,
        float particleRadius,
        float overrelaxation,
        float timestep
    ) {}

    /**
     * Shows a modal setup dialog.
     * @return a SimConfig, or null if the user cancelled.
     */
    public static SimConfig showSetupDialog(Frame owner) {
        JDialog dlg = new JDialog(owner, "FLIP — Simulation Setup", true);
        dlg.setUndecorated(false);
        dlg.getContentPane().setBackground(BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_PANEL);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── title ──
        JLabel title = new JLabel("Simulation Setup");
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 18));
        title.setForeground(ACCENT);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        root.add(title, BorderLayout.NORTH);

        // ── fields ──
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(BG_PANEL);
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(6, 0, 6, 16);
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill   = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;
        fc.insets  = new Insets(6, 0, 6, 0);

        String[][] rows = {
            { "Particle count",   "400"   },
            { "Grid columns",     "50"    },
            { "Grid rows",        "50"    },
            { "Cell size (px)",   "12"    },
            { "Particle radius",  "3.0"   },
            { "Timestep (s)",     "0.033" },
        };

        JTextField[] tfs = new JTextField[rows.length];
        for (int i = 0; i < rows.length; i++) {
            lc.gridy = fc.gridy = i;
            lc.gridx = 0; fc.gridx = 1;

            JLabel lbl = new JLabel(rows[i][0]);
            lbl.setFont(LABEL_FONT);
            lbl.setForeground(TEXT);
            fields.add(lbl, lc);

            JTextField tf = styledField(rows[i][1]);
            tfs[i] = tf;
            fields.add(tf, fc);
        }
        root.add(fields, BorderLayout.CENTER);

        // ── buttons ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(BG_PANEL);
        btnRow.setBorder(new EmptyBorder(18, 0, 0, 0));

        JButton cancel = styledButton("Cancel", false);
        JButton run    = styledButton("Run Simulation →", true);

        boolean[] confirmed = { false };
        cancel.addActionListener(e -> dlg.dispose());
        run.addActionListener(e -> { confirmed[0] = true; dlg.dispose(); });

        btnRow.add(cancel);
        btnRow.add(run);
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(380, 0));
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);

        if (!confirmed[0]) return null;

        try {
            return new SimConfig(
                Integer.parseInt(tfs[0].getText().trim()),
                Integer.parseInt(tfs[1].getText().trim()),
                Integer.parseInt(tfs[2].getText().trim()),
                Float.parseFloat(tfs[3].getText().trim()),
                Float.parseFloat(tfs[4].getText().trim()),
                (float) 1.9,
                Float.parseFloat(tfs[5].getText().trim())
            );
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner,
                "Invalid input — please enter numeric values.",
                "Setup Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simulation window
    // ─────────────────────────────────────────────────────────────────────────

    public Frame(SimConfig cfg) {
        super("FLIP Water Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);

        // ── build simulation objects ──────────────────────────────────────────
        int    nx = cfg.gridX();
        int    ny = cfg.gridY();
        float cs = cfg.cellSize();
        float r  = cfg.particleRadius();


        Grid       grid       = new Grid(nx, ny, cs);
        // Particle[] particles  = ParticleFactory.createNonOverlapping(
        //     cfg.particleCount(), r, 1.0,
        //     r, r, nx * cs - r, ny * cs - r   // safe region inside boundary
        // );
        float cx = (nx * cs) / 2.0f;
        float cy = (ny * cs) / 2.0f;
        float spawnR = ((nx * cs) / 2.0f) - 50f - r * 2; // just inside the boundary
        Particle[] particles = ParticleFactory.createInCircle(
            cfg.particleCount(), r, 1.0f, cx, cy + 30, spawnR
        );
        // CircularBoundary boundary  = new CircularBoundary((nx * cs)/2, (ny * cs)/2, ((nx * cs)/2) - 50, Color.WHITE);
        BoxBoundary boundary = new BoxBoundary(
            50, 50,  // minX, minY
            nx * cs - 50, ny * cs - 50,  // maxX, maxY
            Color.WHITE,
            0.8f  // restitution (optional)
        );
        solver  = new Solver(particles, grid, boundary, 1.9f , 40, 5, cfg.timestep());
        canvas  = new Canvas(solver, grid, particles);

        // ── layout ───────────────────────────────────────────────────────────
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(buildControlBar(cfg), BorderLayout.SOUTH);

        int simW = (int)(nx * cs);
        int simH = (int)(ny * cs);
        canvas.setPreferredSize(new Dimension(simW, simH));

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private JPanel buildControlBar(SimConfig cfg) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ACCENT_DIM));

        JButton playPause = styledButton("▶  Play", true);
        JButton reset     = styledButton("↺  Reset", false);
        JToggleButton gridToggle = new JToggleButton("Grid");
        styleToggle(gridToggle);
        fpsLabel = new JLabel("0 fps");
        fpsLabel.setFont(MONO_SM);
        fpsLabel.setForeground(TEXT_DIM);

        // ── timer drives the sim ──────────────────────────────────────────────
        int targetFps = (int) Math.round(1.0 / cfg.timestep());
        int delay     = Math.max(8, 1000 / targetFps);


        timer = new Timer(delay, (ActionEvent e) -> {
            solver.update();
            canvas.repaint();
            long now = System.currentTimeMillis();
            long dt  = now - lastFrameMs;
            if (dt > 0) fpsLabel.setText((1000 / dt) + " fps");
            lastFrameMs = now;
        });

        playPause.addActionListener(e -> {
            if (running) {
                timer.stop();
                playPause.setText("▶  Play");
            } else {
                lastFrameMs = System.currentTimeMillis();
                timer.start();
                playPause.setText("⏸  Pause");
            }
            running = !running;
        });

        reset.addActionListener(e -> {
            boolean wasRunning = running;
            if (running) { timer.stop(); running = false; }
            solver.reset();
            canvas.repaint();
            playPause.setText("▶  Play");
            if (wasRunning) { timer.start(); running = true; playPause.setText("⏸  Pause"); }
        });

        gridToggle.addActionListener(e -> canvas.setShowGrid(gridToggle.isSelected()));

        bar.add(playPause);
        bar.add(reset);
        bar.add(gridToggle);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(fpsLabel);

        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("DISPLAY      = " + System.getenv("DISPLAY"));
        System.out.println("WAYLAND      = " + System.getenv("WAYLAND_DISPLAY"));
        System.out.println("XAUTHORITY   = " + System.getenv("XAUTHORITY"));
        System.out.println("Headless     = " + GraphicsEnvironment.isHeadless());
        System.out.println(System.getProperty("user.name"));
        System.out.println(System.getProperty("user.home"));
        SwingUtilities.invokeLater(() -> {
            applyDarkLAF();
            SimConfig cfg = Frame.showSetupDialog(null);
            if (cfg == null) return;
            Frame frame = new Frame(cfg);
            frame.setVisible(true);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Style helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static JTextField styledField(String text) {
        JTextField tf = new JTextField(text, 10);
        tf.setFont(MONO);
        tf.setBackground(new Color(14, 18, 30));
        tf.setForeground(TEXT);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_DIM, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private static JButton styledButton(String label, boolean primary) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = primary
                    ? (getModel().isPressed() ? ACCENT_DIM : ACCENT)
                    : (getModel().isPressed() ? new Color(30, 36, 55) : BG_PANEL);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(primary ? BG_DARK : TEXT);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(MONO_SM);
        btn.setForeground(primary ? BG_DARK : TEXT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        if (!primary) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_DIM, 1),
                new EmptyBorder(6, 14, 6, 14)
            ));
        }
        return btn;
    }

    private static void styleToggle(JToggleButton btn) {
        btn.setFont(MONO_SM);
        btn.setForeground(TEXT_DIM);
        btn.setBackground(BG_PANEL);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_DIM, 1),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btn.addChangeListener(e -> btn.setForeground(btn.isSelected() ? ACCENT : TEXT_DIM));
    }

    private static void applyDarkLAF() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background",    BG_PANEL);
        UIManager.put("Label.foreground",    TEXT);
        UIManager.put("TextField.background", new Color(14, 18, 30));
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
    }
}