import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class AdministradorBloqueos {
    public enum EstadoAsiento { DISPONIBLE, EN_COBRO, OCUPADO_PAGADO }
    private static final Map<Sala, AdministradorBloqueos> POR_SALA = new WeakHashMap<>();
    private final Map<String, Asiento> catalogo = new HashMap<>();
    private final Map<String, Reservacion> bloqueos = new HashMap<>();
    private final Map<java.util.UUID, Reservacion> operaciones = new HashMap<>();

    public static synchronized AdministradorBloqueos paraSala(Sala sala) {
        Objects.requireNonNull(sala, "La sala es obligatoria.");
        return POR_SALA.computeIfAbsent(sala, AdministradorBloqueos::new);
    }

    private AdministradorBloqueos(Sala sala) {
        for (Asiento asiento : sala.getAsientos()) {
            catalogo.put(asiento.getIdentificador(), asiento);
        }
    }

    public synchronized Reservacion iniciar(Cliente cliente, Collection<Asiento> seleccion) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio.");
        if (seleccion == null || seleccion.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un asiento.");
        }
        Map<String, Asiento> solicitados = new HashMap<>();
        for (Asiento asiento : seleccion) {
            if (asiento == null || catalogo.get(asiento.getIdentificador()) != asiento) {
                throw new IllegalArgumentException("El asiento no pertenece a esta sala.");
            }
            String id = asiento.getIdentificador();
            if (solicitados.put(id, asiento) != null) {
                throw new IllegalArgumentException("Hay asientos repetidos.");
            }
            if (bloqueos.containsKey(id)) {
                throw new IllegalStateException("El asiento " + id + " está protegido por otro cobro.");
            }
        }
        Reservacion reservacion = new Reservacion(cliente, new ArrayList<>(seleccion));
        operaciones.put(reservacion.getId(), reservacion);
        for (String id : solicitados.keySet()) {
            bloqueos.put(id, reservacion);
        }
        return reservacion;
    }

    // Validación, pago simulado y confirmación ocurren bajo el mismo monitor.
    public synchronized boolean pagarEfectivo(Reservacion reservacion, BigDecimal recibido) {
        verificarActiva(reservacion);
        if (recibido == null || recibido.signum() < 0 || recibido.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Importe inválido; usa como máximo dos decimales.");
        }
        if (recibido.compareTo(reservacion.getTotal()) < 0) {
            return false;
        }
        reservacion.confirmarPago(Reservacion.MetodoPago.EFECTIVO);
        return true;
    }

    public synchronized boolean pagarTerminal(Reservacion reservacion, boolean aprobada) {
        verificarActiva(reservacion);
        if (aprobada) {
            reservacion.confirmarPago(Reservacion.MetodoPago.TERMINAL);
        }
        return aprobada;
    }

    public synchronized void cancelar(Reservacion reservacion) {
        verificarPropietario(reservacion);
        if (reservacion.getEstado() != Reservacion.Estado.EN_COBRO) {
            return;
        }
        bloqueos.entrySet().removeIf(e -> e.getValue() == reservacion);
        reservacion.cancelar();
    }

    public synchronized EstadoAsiento consultarEstado(String identificador) {
        if (!catalogo.containsKey(identificador)) {
            throw new IllegalArgumentException("El asiento no pertenece a esta sala.");
        }
        Reservacion propietario = bloqueos.get(identificador);
        if (propietario == null) { return EstadoAsiento.DISPONIBLE; }
        return propietario.getEstado() == Reservacion.Estado.CONFIRMADA
                ? EstadoAsiento.OCUPADO_PAGADO : EstadoAsiento.EN_COBRO;
    }

    public synchronized List<Asiento> getDisponibles() {
        return catalogo.values().stream()
                .filter(a -> !bloqueos.containsKey(a.getIdentificador()))
                .sorted(java.util.Comparator.comparing(Asiento::getIdentificador)).toList();
    }

    // Recuperación tras un error de pantalla: consulta las confirmadas sin liberarlas ni repetir pagos.
    public synchronized List<Reservacion> getConfirmadas() {
        return operaciones.values().stream()
                .filter(r -> r.getEstado() == Reservacion.Estado.CONFIRMADA).toList();
    }

    private void verificarPropietario(Reservacion reservacion) {
        if (reservacion == null || operaciones.get(reservacion.getId()) != reservacion) {
            throw new IllegalArgumentException("La operación no pertenece a este administrador.");
        }
    }

    private void verificarActiva(Reservacion reservacion) {
        verificarPropietario(reservacion);
        if (reservacion.getEstado() != Reservacion.Estado.EN_COBRO) {
            throw new IllegalStateException("La operación ya terminó.");
        }
        if (Thread.currentThread().isInterrupted()) {
            cancelar(reservacion);
            throw new IllegalStateException("Cobro interrumpido.");
        }
    }
}
