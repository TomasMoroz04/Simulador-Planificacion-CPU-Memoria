package simulador.dao;

import simulador.modelo.Proceso;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

public class ProcesoDaoImpl implements IProcesoDao {
    // Permite el acceso a los objetos de persistencia y contexto de transacciones[cite: 680]
    private final EntityManagerFactory emf;

    public ProcesoDaoImpl() {
        this.emf = Persistence.createEntityManagerFactory("SimuladorPU");
    }

    @Override
    public void guardarProceso(Proceso proceso) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(proceso);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Proceso> obtenerTodos(int tamanoPaginaKB) {
        EntityManager em = emf.createEntityManager();
        try {
            // JPQL: Lenguaje de consulta sobre objetos
            List<Proceso> procesos = em.createQuery("SELECT p FROM Proceso p ORDER BY p.tiempoLlegada ASC", Proceso.class).getResultList();
            
            // Re-inicializar variables @Transient basándose en el tamaño de página actual de la simulación
            for (Proceso p : procesos) {
                p.inicializarTransients(tamanoPaginaKB);
            }
            return procesos;
        } finally {
            em.close();
        }
    }

    @Override
    public void vaciarProcesos() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Proceso").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}