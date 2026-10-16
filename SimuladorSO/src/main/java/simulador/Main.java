package simulador;

import simulador.controlador.ControladorSimulador;
import simulador.dao.IProcesoDao;
import simulador.dao.ProcesoDaoImpl;
import simulador.modelo.Proceso;
import simulador.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // 1. Iniciar la consola web de H2 en segundo plano
        try {
            org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
        } catch (SQLException e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor web de H2: " + e.getMessage());
        }

        // 2. Instanciar el MODELO (Base de Datos a través del DAO)
        IProcesoDao dao = new ProcesoDaoImpl();
        
        // Si la base de datos está vacía, cargar el lote de prueba de la cátedra por defecto
        if (dao.obtenerTodos(4).isEmpty()) {
            dao.guardarProceso(new Proceso("A", 0, 8, 8, 4));
            dao.guardarProceso(new Proceso("B", 1, 4, 6, 4));
            dao.guardarProceso(new Proceso("C", 2, 9, 12, 4));
            dao.guardarProceso(new Proceso("D", 3, 5, 8, 4));
            dao.guardarProceso(new Proceso("E", 4, 2, 4, 4));
        }

        // 3. Levantar la VISTA y el CONTROLADOR en el hilo exclusivo para Swing (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal vista = new VentanaPrincipal();
            
            // Se inyectan la Vista y el Modelo al Controlador para enlazarlos
            new ControladorSimulador(vista, dao);
            
            vista.setLocationRelativeTo(null); // Centra la ventana en la pantalla
            vista.setVisible(true);
            
            System.out.println("[INFO] Interfaz Grafica (GUI) iniciada correctamente.");
            System.out.println("[INFO] Acceso a BD H2 en navegador: http://localhost:8082");
        });
    }
}