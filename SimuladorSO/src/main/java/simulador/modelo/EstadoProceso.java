package simulador.modelo;

public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EN_EJECUCION("En Ejecucion"),
    BLOQUEADO("Bloqueado"),
    TERMINADO("Terminado");

    private final String descripcion;

    EstadoProceso(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}