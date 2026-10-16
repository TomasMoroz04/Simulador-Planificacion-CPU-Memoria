package simulador.dao;

import simulador.modelo.Proceso;
import java.util.List;

public interface IProcesoDao {
    void guardarProceso(Proceso proceso);
    List<Proceso> obtenerTodos(int tamanoPaginaKB);
    void vaciarProcesos();
}