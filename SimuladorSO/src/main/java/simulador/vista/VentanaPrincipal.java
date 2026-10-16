package simulador.vista;

import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.*;

public class VentanaPrincipal extends JFrame {
    private final JComboBox<String> cbxAlgoritmo;
    private final JTextField txtRam, txtPagina, txtQuantum;
    private final JComboBox<String> cbxReemplazo;
    private final JButton btnSimular, btnAgregarProceso;
    private final JTextArea txtConsola;

    public VentanaPrincipal() {
        super("Simulador de SO - Patron MVC");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel Superior: Controles de Configuración usando GridLayout
        JPanel pnlConfig = new JPanel(new GridLayout(2, 6, 10, 10));
        pnlConfig.setBorder(BorderFactory.createTitledBorder("Configuracion y Controles"));

        pnlConfig.add(new JLabel("Algoritmo:"));
        cbxAlgoritmo = new JComboBox<>(new String[]{"FCFS", "SJF", "SRTF", "Round Robin", "COMPARATIVA COMPLETA"});
        pnlConfig.add(cbxAlgoritmo);

        pnlConfig.add(new JLabel("RAM (KB):"));
        txtRam = new JTextField("24");
        pnlConfig.add(txtRam);

        pnlConfig.add(new JLabel("Pag/Marco (KB):"));
        txtPagina = new JTextField("4");
        pnlConfig.add(txtPagina);

        pnlConfig.add(new JLabel("Quantum (RR):"));
        txtQuantum = new JTextField("3");
        pnlConfig.add(txtQuantum);

        pnlConfig.add(new JLabel("Reemplazo:"));
        cbxReemplazo = new JComboBox<>(new String[]{"LRU", "FIFO"});
        pnlConfig.add(cbxReemplazo);

        btnSimular = new JButton("Iniciar Simulacion");
        pnlConfig.add(btnSimular);

        btnAgregarProceso = new JButton("Nuevo Proceso (BD)");
        pnlConfig.add(btnAgregarProceso);

        add(pnlConfig, BorderLayout.NORTH);

        // Panel Central: Consola estilo Terminal
        txtConsola = new JTextArea();
        txtConsola.setEditable(false);
        txtConsola.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtConsola.setBackground(Color.BLACK);
        txtConsola.setForeground(Color.GREEN);
        
        JScrollPane scrollPane = new JScrollPane(txtConsola);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Consola de Ejecucion y Diagrama de Gantt"));
        add(scrollPane, BorderLayout.CENTER);

        // Redirigir la salida de consola pura (System.out) hacia el JTextArea visual
        redirigirConsola();
    }

    private void redirigirConsola() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                // SwingUtilities asegura que la GUI se actualice en el hilo correcto (EDT)
                SwingUtilities.invokeLater(() -> {
                    txtConsola.append(String.valueOf((char) b));
                    txtConsola.setCaretPosition(txtConsola.getDocument().getLength());
                });
            }
        };
        System.setOut(new PrintStream(out, true));
    }

    // Getters para que el Controlador pueda acceder y leer los datos de la Vista
    public JComboBox<String> getCbxAlgoritmo() { return cbxAlgoritmo; }
    public JTextField getTxtRam() { return txtRam; }
    public JTextField getTxtPagina() { return txtPagina; }
    public JTextField getTxtQuantum() { return txtQuantum; }
    public JComboBox<String> getCbxReemplazo() { return cbxReemplazo; }
    public JButton getBtnSimular() { return btnSimular; }
    public JButton getBtnAgregarProceso() { return btnAgregarProceso; }
    
    public void limpiarConsola() {
        txtConsola.setText("");
    }
}