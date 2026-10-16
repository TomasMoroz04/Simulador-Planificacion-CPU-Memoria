package simulador.planificador;

import simulador.modelo.Proceso;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PlanificadorRoundRobin implements PlanificadorStrategy {
    private final Queue<Proceso> colaCircular = new LinkedList<>();
    private final int quantum;
    private int ticksConsumidosQuantum;
    private String eventoUltimoTick = "";

    public PlanificadorRoundRobin(int quantum) {
        this.quantum = Math.max(1, quantum);
        this.ticksConsumidosQuantum = 0;
    }

    @Override
    public void agregarProcesoListo(Proceso proceso) {
        colaCircular.offer(proceso);
    }

    @Override
    public Proceso seleccionarSiguiente(Proceso procesoActual, int tickActual) {
        eventoUltimoTick = "";

        // Si el proceso anterior ya terminó o no había proceso
        if (procesoActual == null || procesoActual.getTiempoRestante() == 0) {
            ticksConsumidosQuantum = 0;
            Proceso siguiente = colaCircular.poll();
            if (siguiente != null) {
                ticksConsumidosQuantum = 1;
            }
            return siguiente;
        }

        // Si sigue vivo, verificamos si agotó su Quantum
        if (ticksConsumidosQuantum >= quantum) {
            if (!colaCircular.isEmpty()) {
                procesoActual.registrarDesalojoCPU(tickActual);
                colaCircular.offer(procesoActual);
                Proceso siguiente = colaCircular.poll();
                ticksConsumidosQuantum = 1;
                eventoUltimoTick = String.format("Fin Quantum (%d) de %s -> Entra %s",
                        quantum, procesoActual.getPid(), siguiente.getPid());
                return siguiente;
            } else {
                // Si no hay nadie más en la cola, renueva su quantum
                ticksConsumidosQuantum = 1;
                return procesoActual;
            }
        }

        ticksConsumidosQuantum++;
        return procesoActual;
    }

    @Override
    public List<Proceso> obtenerColaListosSnapshot() {
        return new ArrayList<>(colaCircular);
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Round Robin (Quantum = " + quantum + ")";
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