package simulador.planificador;

import simulador.modelo.Proceso;
import java.util.List;

public interface PlanificadorStrategy {
    void agregarProcesoListo(Proceso proceso);
    Proceso seleccionarSiguiente(Proceso procesoActual, int tickActual);
    List<Proceso> obtenerColaListosSnapshot();
    String getNombreAlgoritmo();
    boolean esApropiativo();
    String getEventoUltimoTick();
}