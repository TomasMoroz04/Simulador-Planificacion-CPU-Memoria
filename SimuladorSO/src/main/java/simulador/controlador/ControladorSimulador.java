package simulador.controlador;

import simulador.concurrencia.BufferProcesos;
import simulador.concurrencia.HiloProductor;
import simulador.concurrencia.ImpresoraVirtual;
import simulador.dao.IProcesoDao;
import simulador.memoria.GestorMemoria;
import simulador.memoria.GestorMemoria.AlgoritmoReemplazo;
import simulador.modelo.Proceso;
import simulador.motor.MotorSimulacion;
import simulador.planificador.*;
import simulador.reportes.GeneradorReporte;
import simulador.vista.VentanaPrincipal;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ControladorSimulador implements ActionListener {
    private final VentanaPrincipal vista;
    private final IProcesoDao dao;

    public ControladorSimulador(VentanaPrincipal vista, IProcesoDao dao) {
        this.vista = vista;
        this.dao = dao;
        
        // El controlador se registra como "escuchador" de los botones de la vista
        this.vista.getBtnSimular().addActionListener(this);
        this.vista.getBtnAgregarProceso().addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getBtnSimular()) {
            iniciarSimulacionBackground();
        } else if (e.getSource() == vista.getBtnAgregarProceso()) {
            agregarProcesoManual();
        }
    }

    private void iniciarSimulacionBackground() {
        vista.limpiarConsola();

        // Leer todos los parámetros configurados en la interfaz
        int ram = Integer.parseInt(vista.getTxtRam().getText());
        int pagina = Integer.parseInt(vista.getTxtPagina().getText());
        int quantum = Integer.parseInt(vista.getTxtQuantum().getText());
        AlgoritmoReemplazo reemplazo = vista.getCbxReemplazo().getSelectedIndex() == 0 
                ? AlgoritmoReemplazo.LRU : AlgoritmoReemplazo.FIFO;
        int algIndex = vista.getCbxAlgoritmo().getSelectedIndex();

        // Deshabilitar botón durante la simulación para evitar colapsos
        vista.getBtnSimular().setEnabled(false);

        // Se lanza la simulación en un Thread nuevo para no congelar la ventana visual de Swing
        new Thread(() -> {
            try {
                switch (algIndex) {
                    case 0 -> ejecutarMotor(new PlanificadorFCFS(), ram, pagina, reemplazo);
                    case 1 -> ejecutarMotor(new PlanificadorSJF(), ram, pagina, reemplazo);
                    case 2 -> ejecutarMotor(new PlanificadorSRTF(), ram, pagina, reemplazo);
                    case 3 -> ejecutarMotor(new PlanificadorRoundRobin(quantum), ram, pagina, reemplazo);
                    case 4 -> {
                        ejecutarMotor(new PlanificadorFCFS(), ram, pagina, reemplazo);
                        ejecutarMotor(new PlanificadorSJF(), ram, pagina, reemplazo);
                        ejecutarMotor(new PlanificadorSRTF(), ram, pagina, reemplazo);
                        ejecutarMotor(new PlanificadorRoundRobin(quantum), ram, pagina, reemplazo);
                    }
                }
            } finally {
                // Al terminar, SwingUtilities vuelve a habilitar el botón de forma segura
                SwingUtilities.invokeLater(() -> vista.getBtnSimular().setEnabled(true));
            }
        }).start();
    }

    private void ejecutarMotor(PlanificadorStrategy estrategia, int ram, int pagina, AlgoritmoReemplazo reemplazo) {
        List<Proceso> procesos = dao.obtenerTodos(pagina);
        if (procesos.isEmpty()) {
            System.out.println("[ERROR] No hay procesos en la Base de Datos para simular.");
            return;
        }

        BufferProcesos buffer = new BufferProcesos(15);
        ImpresoraVirtual impresora = new ImpresoraVirtual();

        HiloProductor hiloProductor = new HiloProductor(buffer, procesos);
        hiloProductor.start();

        GestorMemoria gestorMemoria = new GestorMemoria(ram, pagina, reemplazo);
        MotorSimulacion motor = new MotorSimulacion(procesos, estrategia, gestorMemoria, buffer, impresora, true);
        motor.ejecutarSimulacion();
        
        System.out.println("\n\n");

        // NUEVA LÓGICA: Preguntamos al usuario si desea generar el reporte PDF
        int opcion = JOptionPane.showConfirmDialog(vista,
                "Simulación de " + estrategia.getNombreAlgoritmo() + " finalizada.\n¿Desea generar y guardar el reporte en PDF?",
                "Generar Reporte PDF",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) {
            GeneradorReporte.generarReportePDF(estrategia.getNombreAlgoritmo(), procesos, motor.getHistorialGantt());
            JOptionPane.showMessageDialog(vista, "¡Reporte PDF generado exitosamente en la carpeta del proyecto!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void agregarProcesoManual() {
        // Cuadro de diálogo interactivo para ingresar nuevos datos en la BD
        JTextField txtPid = new JTextField();
        JTextField txtLlegada = new JTextField();
        JTextField txtRafaga = new JTextField();
        JTextField txtMemoria = new JTextField();

        Object[] mensaje = {
            "PID (ej. P6):", txtPid,
            "Tiempo Llegada (Ticks):", txtLlegada,
            "Rafaga CPU (Ticks):", txtRafaga,
            "Memoria (KB):", txtMemoria
        };

        int opcion = JOptionPane.showConfirmDialog(vista, mensaje, "Agregar Nuevo Proceso a BD", JOptionPane.OK_CANCEL_OPTION);
        if (opcion == JOptionPane.OK_OPTION) {
            try {
                int pagina = Integer.parseInt(vista.getTxtPagina().getText());
                Proceso p = new Proceso(
                        txtPid.getText(),
                        Integer.parseInt(txtLlegada.getText()),
                        Integer.parseInt(txtRafaga.getText()),
                        Integer.parseInt(txtMemoria.getText()),
                        pagina
                );
                dao.guardarProceso(p);
                System.out.println("[INFO] Proceso " + p.getPid() + " insertado en H2. Se usara en la proxima simulacion.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(vista, "Error en los datos ingresados.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}