package simulador.modelo;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Basic;
import javax.persistence.Transient;

@Entity // Indica al motor de persistencia que la clase representa una tabla
public class Proceso {
    
    // Datos de Identificación y Control (PCB)
    @Id // Clave primaria en la base de datos
    private String pid;
    
    @Transient // Los atributos Transient son ignorados por la base de datos (no se guardan)
    private EstadoProceso estado;
    
    @Transient
    private int contadorPrograma; // Dirección lógica actual en bytes

    // Parámetros de Planificación
    @Basic // Dato persistente
    private int tiempoLlegada;
    
    @Basic // Dato persistente
    private int rafagaCPU;
    
    @Transient
    private int tiempoRestante;

    // Parámetros de Memoria y Paginación
    @Basic // Dato persistente
    private int tamanoMemoriaKB;
    
    @Transient
    private int cantidadPaginas;
    
    @Transient
    private double fragmentacionInternaKB;
    
    @Transient
    private List<EntradaTablaPagina> tablaPaginas;
    
    @Transient
    private int fallosDePagina;

    // Métricas y trazabilidad de intervalos (Estilo cátedra Ing. Kucuk)
    @Transient
    private List<Integer> tiemposComienzo;
    
    @Transient
    private List<Integer> tiemposFinalizacion;
    
    @Transient
    private int tiempoFinalTotal;
    
    @Transient
    private boolean ejecutandoEnTickAnterior;

    // 1. Constructor vacío obligatorio para JPA
    public Proceso() {
    }

    // 2. Constructor original (utilizado cuando se ingresan a mano por primera vez)
    public Proceso(String pid, int tiempoLlegada, int rafagaCPU, int tamanoMemoriaKB, int tamanoPaginaKB) {
        this.pid = pid;
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tamanoMemoriaKB = tamanoMemoriaKB;
        
        // Delegamos la inicialización de los datos volátiles al método
        inicializarTransients(tamanoPaginaKB);
    }

    // 3. Método para "reconstruir" la simulación después de cargar el proceso desde la BD
    public void inicializarTransients(int tamanoPaginaKB) {
        this.estado = EstadoProceso.NUEVO;
        this.contadorPrograma = 0;
        this.tiempoRestante = this.rafagaCPU;

        // Cálculo de páginas necesarias y fragmentación interna
        this.cantidadPaginas = Math.max(1, (int) Math.ceil((double) this.tamanoMemoriaKB / tamanoPaginaKB));
        this.fragmentacionInternaKB = (this.cantidadPaginas * tamanoPaginaKB) - this.tamanoMemoriaKB;

        this.tablaPaginas = new ArrayList<>();
        for (int i = 0; i < cantidadPaginas; i++) {
            this.tablaPaginas.add(new EntradaTablaPagina(i));
        }

        this.fallosDePagina = 0;
        this.tiemposComienzo = new ArrayList<>();
        this.tiemposFinalizacion = new ArrayList<>();
        this.tiempoFinalTotal = 0;
        this.ejecutandoEnTickAnterior = false;
    }

    /**
     * Ejecuta 1 tick de CPU sobre el proceso, avanzando su Contador de Programa lógico.
     */
    public void ejecutarTick(int tickActual, int tamanoPaginaKB) {
        if (!ejecutandoEnTickAnterior) {
            tiemposComienzo.add(tickActual);
            ejecutandoEnTickAnterior = true;
        }
        this.estado = EstadoProceso.EN_EJECUCION;
        this.tiempoRestante--;

        // Avanza el contador de programa simulando lectura a través de sus páginas lógicas
        int bytesPorTick = Math.max(512, (tamanoMemoriaKB * 1024) / Math.max(1, rafagaCPU));
        this.contadorPrograma = (this.contadorPrograma + bytesPorTick) % Math.max(1, tamanoMemoriaKB * 1024);

        if (this.tiempoRestante == 0) {
            this.estado = EstadoProceso.TERMINADO;
            this.tiempoFinalTotal = tickActual + 1;
            this.tiemposFinalizacion.add(tickActual + 1);
            this.ejecutandoEnTickAnterior = false;
        }
    }

    /**
     * Registra cuando un proceso deja la CPU por apropiación (SRTF o fin de Quantum en Round Robin).
     */
    public void registrarDesalojoCPU(int tickActual) {
        if (ejecutandoEnTickAnterior) {
            this.tiemposFinalizacion.add(tickActual);
            this.ejecutandoEnTickAnterior = false;
        }
        if (this.tiempoRestante > 0) {
            this.estado = EstadoProceso.LISTO;
        }
    }

    /**
     * Determina qué página lógica necesita el proceso en el tick actual según su progreso.
     */
    public int obtenerPaginaRequeridaActual() {
        int progreso = rafagaCPU - tiempoRestante;
        return progreso % cantidadPaginas;
    }

    public void incrementarFallosPagina() {
        this.fallosDePagina++;
    }

    // Cálculo de métricas duras (Fórmulas de cátedra)
    // Tiempo de Retorno = Tiempo de Finalización - Tiempo de Llegada
    public int getTiempoRetorno() {
        return tiempoFinalTotal - tiempoLlegada;
    }

    // Tiempo de Espera = Tiempo de Retorno - Ráfaga de Ejecución
    public int getTiempoEspera() {
        return getTiempoRetorno() - rafagaCPU;
    }

    public String getTiemposComienzoFormateados() {
        return formatearListaTiempos(tiemposComienzo);
    }

    public String getTiemposFinalizacionFormateados() {
        return formatearListaTiempos(tiemposFinalizacion);
    }

    private String formatearListaTiempos(List<Integer> lista) {
        if (lista == null || lista.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append(lista.get(i));
            if (i < lista.size() - 1) sb.append("-");
        }
        return sb.toString();
    }

    // Getters y Setters
    public String getPid() { return pid; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public int getContadorPrograma() { return contadorPrograma; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public int getRafagaCPU() { return rafagaCPU; }
    public int getTiempoRestante() { return tiempoRestante; }
    public int getTamanoMemoriaKB() { return tamanoMemoriaKB; }
    public int getCantidadPaginas() { return cantidadPaginas; }
    public double getFragmentacionInternaKB() { return fragmentacionInternaKB; }
    public List<EntradaTablaPagina> getTablaPaginas() { return tablaPaginas; }
    public int getFallosDePagina() { return fallosDePagina; }
}