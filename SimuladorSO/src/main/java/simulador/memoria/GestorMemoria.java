package simulador.memoria;

import simulador.modelo.EntradaTablaPagina;
import simulador.modelo.MarcoRAM;
import simulador.modelo.Proceso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GestorMemoria {
    public enum AlgoritmoReemplazo { FIFO, LRU }

    private final int tamanoTotalKB;
    private final int tamanoPaginaKB;
    private final int totalMarcos;
    private final MarcoRAM[] marcos;
    private final AlgoritmoReemplazo algoritmoReemplazo;
    private int totalFallosPaginaGlobal;
    private int totalReemplazosGlobal;

    public GestorMemoria(int tamanoTotalKB, int tamanoPaginaKB, AlgoritmoReemplazo algoritmoReemplazo) {
        this.tamanoTotalKB = tamanoTotalKB;
        this.tamanoPaginaKB = tamanoPaginaKB;
        this.totalMarcos = tamanoTotalKB / tamanoPaginaKB;
        this.marcos = new MarcoRAM[totalMarcos];
        for (int i = 0; i < totalMarcos; i++) {
            this.marcos[i] = new MarcoRAM(i);
        }
        this.algoritmoReemplazo = algoritmoReemplazo;
        this.totalFallosPaginaGlobal = 0;
        this.totalReemplazosGlobal = 0;
    }

    /**
     * Intenta asignar marcos iniciales al proceso recién llegado.
     * Si hay marcos libres, carga todas las páginas que quepan.
     * Incluso si la RAM está llena, permite admisión por paginación bajo demanda (Swap).
     */
    public List<String> admitirProcesoEnMemoria(Proceso proceso, int tickActual) {
        List<String> eventos = new ArrayList<>();
        int paginasCargadas = 0;

        for (EntradaTablaPagina entrada : proceso.getTablaPaginas()) {
            int marcoLibre = buscarMarcoLibre();
            if (marcoLibre != -1) {
                marcos[marcoLibre].ocupar(proceso.getPid(), entrada.getNumeroPagina(), tickActual);
                entrada.asignarMarco(marcoLibre, tickActual);
                paginasCargadas++;
            } else {
                break; // El resto queda en Memoria Virtual (Swap/Disco)
            }
        }

        if (paginasCargadas == proceso.getCantidadPaginas()) {
            eventos.add(String.format("RAM: %s cargo sus %d págs (%d KB, Frag.Int: %.0f KB)",
                    proceso.getPid(), proceso.getCantidadPaginas(),
                    proceso.getTamanoMemoriaKB(), proceso.getFragmentacionInternaKB()));
        } else {
            eventos.add(String.format("RAM: %s cargo %d/%d págs en RAM (resto en Swap/Disco)",
                    proceso.getPid(), paginasCargadas, proceso.getCantidadPaginas()));
        }
        return eventos;
    }

    /**
     * Verifica que la página activa que requiere la CPU esté en un marco físico.
     * Si no está presente, dispara un FALLO DE PÁGINA (Page Fault) y ejecuta reemplazo si no hay marcos libres.
     */
    public String asegurarPaginaEnRAM(Proceso procesoActivo, int numeroPagina, int tickActual, Map<String, Proceso> mapaProcesos) {
        EntradaTablaPagina entrada = procesoActivo.getTablaPaginas().get(numeroPagina);

        if (entrada.isPresenteEnRAM()) {
            // Acierto de página (Page Hit): actualizamos tiempo de uso para LRU
            entrada.registrarAcceso(tickActual);
            marcos[entrada.getNumeroMarco()].actualizarUso(tickActual);
            return null;
        }

        // ¡FALLO DE PÁGINA (Page Fault)!
        procesoActivo.incrementarFallosPagina();
        totalFallosPaginaGlobal++;

        int marcoLibre = buscarMarcoLibre();
        if (marcoLibre != -1) {
            marcos[marcoLibre].ocupar(procesoActivo.getPid(), numeroPagina, tickActual);
            entrada.asignarMarco(marcoLibre, tickActual);
            return String.format("FALLO DE PAGINA [%s-P%d] -> Cargada desde Disco en Marco Libre M%02d",
                    procesoActivo.getPid(), numeroPagina, marcoLibre);
        }

        // No hay marcos libres -> Ejecutar Algoritmo de Reemplazo de Páginas (FIFO o LRU)
        int indiceVictima = seleccionarMarcoVictima();
        MarcoRAM victima = marcos[indiceVictima];
        String pidVictima = victima.getPidPropietario();
        int pagVictima = victima.getNumeroPagina();

        // Actualizar tabla de páginas del proceso víctima enviando su página a Swap (Disco)
        Proceso procVictima = mapaProcesos.get(pidVictima);
        if (procVictima != null) {
            procVictima.getTablaPaginas().get(pagVictima).desalojarMarco();
        }

        // Cargar la nueva página en el marco liberado
        victima.ocupar(procesoActivo.getPid(), numeroPagina, tickActual);
        entrada.asignarMarco(indiceVictima, tickActual);
        totalReemplazosGlobal++;

        return String.format("FALLO DE PAGINA [%s-P%d] -> Reemplazo %s en M%02d (Sale %s-P%d a Swap)",
                procesoActivo.getPid(), numeroPagina, algoritmoReemplazo, indiceVictima, pidVictima, pagVictima);
    }

    /**
     * Traduce una dirección lógica a física usando la Tabla de Páginas (MMU).
     */
    public String traducirDireccion(Proceso p, int direccionLogicaBytes) {
        int tamanoPaginaBytes = tamanoPaginaKB * 1024;
        int numeroPagina = (direccionLogicaBytes / tamanoPaginaBytes) % p.getCantidadPaginas();
        int desplazamiento = direccionLogicaBytes % tamanoPaginaBytes;

        EntradaTablaPagina entrada = p.getTablaPaginas().get(numeroPagina);
        if (!entrada.isPresenteEnRAM()) {
            return String.format("Dir.Logica(P=%d, d=%d) -> No presente en RAM", numeroPagina, desplazamiento);
        }
        int marco = entrada.getNumeroMarco();
        int direccionFisica = (marco * tamanoPaginaBytes) + desplazamiento;
        return String.format("MMU[P=%d, d=%d -> M=%02d, Fis=0x%04X]", numeroPagina, desplazamiento, marco, direccionFisica);
    }

    /**
     * Libera todos los marcos de RAM cuando un proceso pasa a estado TERMINADO.
     */
    public void liberarMemoriaProceso(Proceso proceso) {
        for (MarcoRAM marco : marcos) {
            if (!marco.isLibre() && marco.getPidPropietario().equals(proceso.getPid())) {
                marco.liberar();
            }
        }
        for (EntradaTablaPagina entrada : proceso.getTablaPaginas()) {
            entrada.desalojarMarco();
        }
    }

    private int buscarMarcoLibre() {
        for (int i = 0; i < totalMarcos; i++) {
            if (marcos[i].isLibre()) {
                return i;
            }
        }
        return -1;
    }

    private int seleccionarMarcoVictima() {
        int mejorIndice = 0;
        int menorValor = Integer.MAX_VALUE;

        for (int i = 0; i < totalMarcos; i++) {
            int criterio = (algoritmoReemplazo == AlgoritmoReemplazo.FIFO)
                    ? marcos[i].getTickCarga()
                    : marcos[i].getUltimoUsoTick();

            if (criterio < menorValor) {
                menorValor = criterio;
                mejorIndice = i;
            }
        }
        return mejorIndice;
    }

    public String obtenerMapaVisualMarcos() {
        StringBuilder sb = new StringBuilder();
        int ocupados = 0;
        for (MarcoRAM m : marcos) {
            if (!m.isLibre()) ocupados++;
            sb.append(m.toString());
        }
        return String.format("RAM (%d/%d marcos): %s", ocupados, totalMarcos, sb.toString());
    }

    public int getTamanoPaginaKB() { return tamanoPaginaKB; }
    public int getTamanoTotalKB() { return tamanoTotalKB; }
    public int getTotalMarcos() { return totalMarcos; }
    public int getTotalFallosPaginaGlobal() { return totalFallosPaginaGlobal; }
    public int getTotalReemplazosGlobal() { return totalReemplazosGlobal; }
    public AlgoritmoReemplazo getAlgoritmoReemplazo() { return algoritmoReemplazo; }
}