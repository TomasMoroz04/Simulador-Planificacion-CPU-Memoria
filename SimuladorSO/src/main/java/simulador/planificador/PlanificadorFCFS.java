package simulador.planificador;

import simulador.modelo.Proceso;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PlanificadorFCFS implements PlanificadorStrategy {
    private final Queue<Proceso> colaListos = new LinkedList<>();
    private String eventoUltimoTick = "";

    @Override
    public void agregarProcesoListo(Proceso proceso) {
        colaListos.offer(proceso);
    }

    @Override
    public Proceso seleccionarSiguiente(Proceso procesoActual, int tickActual) {
        eventoUltimoTick = "";
        // Al ser NO apropiativo, si hay un proceso ejecutándose y no terminó, continúa en la CPU
        if (procesoActual != null && procesoActual.getTiempoRestante() > 0) {
            return procesoActual;
        }
        return colaListos.poll();
    }

    @Override
    public List<Proceso> obtenerColaListosSnapshot() {
        return new ArrayList<>(colaListos);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "FCFS (First-Come, First-Served)";
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