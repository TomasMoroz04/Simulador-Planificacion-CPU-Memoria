package simulador.planificador;

import simulador.modelo.Proceso;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PlanificadorSRTF implements PlanificadorStrategy {
    private final PriorityQueue<Proceso> colaPrioridad;
    private String eventoUltimoTick = "";

    public PlanificadorSRTF() {
        this.colaPrioridad = new PriorityQueue<>(
                Comparator.comparingInt(Proceso::getTiempoRestante)
                          .thenComparingInt(Proceso::getTiempoLlegada)
                          .thenComparing(Proceso::getPid)
        );
    }

    @Override
    public void agregarProcesoListo(Proceso proceso) {
        colaPrioridad.offer(proceso);
    }

    @Override
    public Proceso seleccionarSiguiente(Proceso procesoActual, int tickActual) {
        eventoUltimoTick = "";

        if (procesoActual != null && procesoActual.getTiempoRestante() > 0) {
            Proceso masCortoEnCola = colaPrioridad.peek();
            if (masCortoEnCola != null && masCortoEnCola.getTiempoRestante() < procesoActual.getTiempoRestante()) {
                // Apropiación (Preemption)
                procesoActual.registrarDesalojoCPU(tickActual);
                colaPrioridad.offer(procesoActual);
                Proceso elegido = colaPrioridad.poll();
                eventoUltimoTick = String.format("Apropiacion SRTF: [%s (Rest:%d)] desplaza a [%s (Rest:%d)]",
                        elegido.getPid(), elegido.getTiempoRestante(),
                        procesoActual.getPid(), procesoActual.getTiempoRestante());
                return elegido;
            }
            return procesoActual;
        }
        return colaPrioridad.poll();
    }

    @Override
    public List<Proceso> obtenerColaListosSnapshot() {
        List<Proceso> copia = new ArrayList<>(colaPrioridad);
        copia.sort(colaPrioridad.comparator());
        return copia;
    }

    @Override
    public String getNombreAlgoritmo() {
        return "SRTF (Short Remaining Time First - Apropiativo)";
    }

    @Override
    public boolean esApropiativo() {
        return true;
    }

    @Override
    public String getEventoUltimoTick() {
        return eventoUltimoTick;
    }
}