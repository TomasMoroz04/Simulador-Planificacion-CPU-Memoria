package simulador.motor;

import simulador.concurrencia.BufferProcesos;
import simulador.concurrencia.ImpresoraVirtual;
import simulador.memoria.GestorMemoria;
import simulador.modelo.EstadoProceso;
import simulador.modelo.Proceso;
import simulador.planificador.PlanificadorStrategy;
import simulador.reportes.GeneradorReporte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MotorSimulacion {
    private final List<Proceso> todosLosProcesos;
    private final Map<String, Proceso> mapaProcesos;
    private final PlanificadorStrategy planificador;
    private final GestorMemoria gestorMemoria;
    private final boolean mostrarDetalleRAMTickATick;
    
    // Componentes de Concurrencia
    private final BufferProcesos buffer;
    private final ImpresoraVirtual impresora;

    // NUEVO: Atributo de clase para poder enviarlo al reporte
    private List<String> historialGanttCompacto;

    public MotorSimulacion(List<Proceso> procesos, PlanificadorStrategy planificador,
                           GestorMemoria gestorMemoria, BufferProcesos buffer, 
                           ImpresoraVirtual impresora, boolean mostrarDetalleRAMTickATick) {
        this.todosLosProcesos = procesos;
        this.mapaProcesos = new HashMap<>();
        for (Proceso p : procesos) {
            this.mapaProcesos.put(p.getPid(), p);
        }
        this.planificador = planificador;
        this.gestorMemoria = gestorMemoria;
        this.buffer = buffer;
        this.impresora = impresora;
        this.mostrarDetalleRAMTickATick = mostrarDetalleRAMTickATick;
    }

    public void ejecutarSimulacion() {
        int relojGlobal = 0;
        int procesosTerminados = 0;
        int totalProcesos = todosLosProcesos.size();
        Proceso procesoActual = null;
        
        // Inicializamos la lista aquí
        this.historialGanttCompacto = new ArrayList<>();

        System.out.println("\n==========================================================================================================");
        System.out.println("  INICIANDO SIMULACION MULTIHILO | ALGORITMO: " + planificador.getNombreAlgoritmo());
        System.out.printf("  MEMORIA RAM: %d KB (%d Marcos de %d KB) | REEMPLAZO DE PAGINAS: %s%n",
                gestorMemoria.getTamanoTotalKB(), gestorMemoria.getTotalMarcos(),
                gestorMemoria.getTamanoPaginaKB(), gestorMemoria.getAlgoritmoReemplazo());
        System.out.println("==========================================================================================================");

        while (procesosTerminados < totalProcesos) {
            List<String> eventosTick = new ArrayList<>();

            // 1. Consumir procesos que el HiloProductor haya inyectado al Buffer en tiempo real
            Proceso procesoNuevo;
            while ((procesoNuevo = buffer.consumir()) != null) {
                eventosTick.add("Llega " + procesoNuevo.getPid());
                eventosTick.addAll(gestorMemoria.admitirProcesoEnMemoria(procesoNuevo, relojGlobal));
                procesoNuevo.setEstado(EstadoProceso.LISTO);
                planificador.agregarProcesoListo(procesoNuevo);
            }

            // 2. Planificar la CPU (elegir qué proceso se ejecuta en este tick)
            procesoActual = planificador.seleccionarSiguiente(procesoActual, relojGlobal);
            if (!planificador.getEventoUltimoTick().isEmpty()) {
                eventosTick.add(planificador.getEventoUltimoTick());
            }

            // Capturar el estado de la cola de listos antes de ejecutar el tick
            String colaStr = planificador.obtenerColaListosSnapshot().stream()
                    .map(p -> p.getPid() + "(" + p.getTiempoRestante() + ")")
                    .collect(Collectors.joining(", "));

            // 3. Ejecutar el proceso actual y verificar Concurrencia (Impresora) y Paginación
            if (procesoActual != null) {
                
                // Simulación de acceso a Sección Crítica (E/S a la impresora compartida) con Mutex
                if (Math.random() < 0.20) { // 20% de probabilidad de querer imprimir
                    impresora.usarImpresora(procesoActual, relojGlobal);
                }

                int paginaRequerida = procesoActual.obtenerPaginaRequeridaActual();
                String eventoFallo = gestorMemoria.asegurarPaginaEnRAM(procesoActual, paginaRequerida, relojGlobal, mapaProcesos);
                if (eventoFallo != null) {
                    eventosTick.add(eventoFallo);
                }

                String traduccionMMU = gestorMemoria.traducirDireccion(procesoActual, procesoActual.getContadorPrograma());
                procesoActual.ejecutarTick(relojGlobal, gestorMemoria.getTamanoPaginaKB());
                historialGanttCompacto.add(procesoActual.getPid());

                boolean terminoEnEsteTick = (procesoActual.getEstado() == EstadoProceso.TERMINADO);
                if (terminoEnEsteTick) {
                    eventosTick.add("Termina " + procesoActual.getPid() + " (Libera marcos RAM)");
                }

                // 4. Imprimir el estado actual del Tick
                String infoEventos = eventosTick.isEmpty() ? "" : " -> " + String.join(" | ", eventosTick);
                System.out.printf("Tick %02d: [%s] (Ejecutando, Rest:%02d) | %s | Cola: [%s]%s%n",
                        relojGlobal, procesoActual.getPid(), procesoActual.getTiempoRestante(),
                        traduccionMMU, colaStr, infoEventos);

                if (mostrarDetalleRAMTickATick) {
                    System.out.println("         " + gestorMemoria.obtenerMapaVisualMarcos());
                }

                // Si terminó, liberar sus marcos para el próximo tick
                if (terminoEnEsteTick) {
                    gestorMemoria.liberarMemoriaProceso(procesoActual);
                    procesosTerminados++;
                    procesoActual = null;
                }
            } else {
                historialGanttCompacto.add("-");
                String infoEventos = eventosTick.isEmpty() ? "" : " -> " + String.join(" | ", eventosTick);
                System.out.printf("Tick %02d: [IDLE] (CPU Inactiva)      | Cola: [%s]%s%n",
                        relojGlobal, colaStr, infoEventos);
            }

            relojGlobal++;

            // Detener el hilo principal 500ms para simular el paso del tiempo real (1 Tick = 500ms)
            try {
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        imprimirDiagramaGanttHorizontal(historialGanttCompacto);
        imprimirTablaMetricas();
    }

    private void imprimirDiagramaGanttHorizontal(List<String> historial) {
        System.out.println("\n----------------------------------------------------------------------------------------------------------");
        System.out.println("  DIAGRAMA DE GANTT VISUAL (LÍNEA DE TIEMPO DE LA CPU)");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        StringBuilder lineaTiempo = new StringBuilder("Tick: ");
        StringBuilder lineaProcesos = new StringBuilder("CPU : ");

        for (int t = 0; t < historial.size(); t++) {
            lineaTiempo.append(String.format("%-4d", t));
            lineaProcesos.append(String.format("[%-2s]", historial.get(t)));
        }
        lineaTiempo.append(String.format("%-4d", historial.size()));

        System.out.println(lineaProcesos);
        System.out.println(lineaTiempo);
    }

    private void imprimirTablaMetricas() {
        System.out.println("\n----------------------------------------------------------------------------------------------------------");
        System.out.println("  TABLA DE RESULTADOS Y METRICAS   ");
        System.out.println("----------------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-10s | %-10s | %-14s | %-14s | %-10s | %-10s | %-8s | %-8s%n",
                "PROCESO", "EJECUCION", "LLEGADA", "T. COMIENZO", "T. FINALIZAC.", "T. RETORNO", "T. ESPERA", "PAGINAS", "P.FAULTS");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        double sumaRetorno = 0;
        double sumaEspera = 0;

        for (Proceso p : todosLosProcesos) {
            sumaRetorno += p.getTiempoRetorno();
            sumaEspera += p.getTiempoEspera();

            System.out.printf("%-8s | %-10d | %-10d | %-14s | %-14s | %-10d | %-10d | %-8d | %-8d%n",
                    p.getPid(),
                    p.getRafagaCPU(),
                    p.getTiempoLlegada(),
                    p.getTiemposComienzoFormateados(),
                    p.getTiemposFinalizacionFormateados(),
                    p.getTiempoRetorno(),
                    p.getTiempoEspera(),
                    p.getCantidadPaginas(),
                    p.getFallosDePagina());
        }

        System.out.println("----------------------------------------------------------------------------------------------------------");
        double promedioRetorno = sumaRetorno / todosLosProcesos.size();
        double promedioEspera = sumaEspera / todosLosProcesos.size();

        System.out.printf("PROMEDIOS DEL SISTEMA -> Tiempo Medio de Retorno: %.2f ticks | Tiempo Medio de Espera: %.2f ticks%n",
                promedioRetorno, promedioEspera);
        System.out.printf("ESTADISTICAS DE RAM   -> Total Fallos de Pagina (Page Faults): %d | Total Reemplazos (%s): %d%n",
                gestorMemoria.getTotalFallosPaginaGlobal(),
                gestorMemoria.getAlgoritmoReemplazo(),
                gestorMemoria.getTotalReemplazosGlobal());
        System.out.println("==========================================================================================================\n");
    }

    // NUEVO: Getter para que el Controlador pueda armar el PDF de forma externa
    public List<String> getHistorialGantt() {
        return historialGanttCompacto;
    }
}