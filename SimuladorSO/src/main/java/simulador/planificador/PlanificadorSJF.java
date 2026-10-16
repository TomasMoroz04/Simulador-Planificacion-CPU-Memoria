package simulador.planificador;

import simulador.modelo.Proceso;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PlanificadorSJF implements PlanificadorStrategy {
    private final PriorityQueue<Proceso> colaPrioridad;
    private String eventoUltimoTick = "";

    public PlanificadorSJF() {
        this.colaPrioridad = new PriorityQueue<>(
                Comparator.comparingInt(Proceso::getRafagaCPU)
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
        // No apropiativo: no suelta la CPU hasta terminar su ráfaga
        if (procesoActual != null && procesoActual.getTiempoRestante() > 0) {
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
        return "SJF (Shortest Job First - No Apropiativo)";
    }

    @Override
    public boolean esApropiativo() {
        return false;
    }

    @Override
    public String getEventoUltimoTick() {
        return eventoUltimoTick;
    }
}