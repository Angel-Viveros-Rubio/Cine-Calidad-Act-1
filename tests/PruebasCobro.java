import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebasCobro {
    private static int pruebas;
    private static final Cliente CLIENTE = new Cliente("Ana", "555");

    public static void main(String[] args) throws Exception {
        for (String entrada : List.of("1\n140\n", "2\n1\n", "x\n1\nabc\n1\n-1\n1\n1.001\n1\n1e3\n1\n200\n",
                "1\n100\n2\n2\n2\nx\n2\n1\n")) {
            Sala sala = nuevaSala();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            var resultado = taquilla(entrada, salida).cobrar(CLIENTE, sala, sala.getAsientos().subList(0, 2));
            verificar(resultado.isPresent(), "Pago válido después de posibles reintentos");
            verificar(resultado.orElseThrow().getEstado() == Reservacion.Estado.CONFIRMADA, "Confirmación");
            AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
            admin.cancelar(resultado.get());
            falla(() -> admin.iniciar(CLIENTE, sala.getAsientos().subList(0, 1)), "Pago conserva protección");
            falla(() -> admin.pagarTerminal(resultado.get(), true), "No repetir pago");
            verificar(admin.getConfirmadas().contains(resultado.get()), "Recuperación de confirmada");
        }
        for (String entrada : List.of("0\n", "", "1\n", "2\n", "1\n0\n", "2\n0\n",
                "1\n50\n0\n", "2\n2\n0\n")) {
            Sala sala = nuevaSala();
            var resultado = taquilla(entrada, new ByteArrayOutputStream()).cobrar(CLIENTE, sala, sala.getAsientos());
            verificar(resultado.isEmpty(), "Cancelación o rechazo no confirma");
            AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
            verificar(admin.getConfirmadas().isEmpty(), "Sin pagos registrados");
            Reservacion siguiente = admin.iniciar(CLIENTE, sala.getAsientos());
            admin.cancelar(siguiente);
            verificar(siguiente.getEstado() == Reservacion.Estado.CANCELADA, "Liberación completa");
        }
        Sala sala = nuevaSala();
        AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
        verificar(admin == AdministradorBloqueos.paraSala(sala), "Administrador compartido");
        List<Asiento> lugares = sala.getAsientos();
        Reservacion primera = admin.iniciar(CLIENTE, List.of(lugares.get(1)));
        falla(() -> admin.iniciar(CLIENTE, lugares.subList(0, 2)), "Bloqueo atómico");
        Reservacion libre = admin.iniciar(CLIENTE, List.of(lugares.get(0)));
        admin.cancelar(libre);
        falla(() -> admin.cancelar(new Reservacion(CLIENTE, List.of(lugares.get(1)))), "Sin liberar otro propietario");
        falla(() -> admin.iniciar(CLIENTE, List.of(lugares.get(1))), "El propietario original sigue protegido");
        admin.cancelar(primera);
        falla(() -> admin.iniciar(CLIENTE, List.of(lugares.get(0), lugares.get(0))), "Duplicados");
        falla(() -> admin.iniciar(CLIENTE, List.of()), "Sin asientos");
        falla(() -> admin.iniciar(CLIENTE, nuevaSala().getAsientos()), "Otra sala");
        Reservacion importe = admin.iniciar(CLIENTE, List.of(lugares.get(0)));
        falla(() -> admin.pagarEfectivo(importe, new BigDecimal("-1")), "Importe negativo");
        falla(() -> admin.pagarEfectivo(importe, new BigDecimal("70.001")), "Fracción de centavo");
        verificar(!admin.pagarEfectivo(importe, BigDecimal.ONE), "Insuficiente");
        verificar(!admin.pagarTerminal(importe, false), "Terminal rechazada");
        verificar(importe.getEstado() == Reservacion.Estado.EN_COBRO, "Rechazo sigue pendiente");
        admin.cancelar(importe);

        // Dos operaciones arrancan juntas; exactamente una obtiene el mismo asiento.
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        Runnable competir = () -> {
            try {
                inicio.await();
                admin.iniciar(CLIENTE, List.of(lugares.get(2)));
                exitos.incrementAndGet();
            } catch (IllegalStateException e) {
                conflictos.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Thread uno = new Thread(competir);
        Thread dos = new Thread(competir);
        uno.start(); dos.start(); inicio.countDown(); uno.join(); dos.join();
        verificar(exitos.get() == 1 && conflictos.get() == 1, "Competencia real sin doble reserva");

        Sala compartida = nuevaSala();
        AtomicInteger intentos = new AtomicInteger();
        PrintStream pantallaCompartida = new PrintStream(new ByteArrayOutputStream()) {
            @Override public void println(String texto) {
                if (texto.startsWith("Pago:")) {
                    var segunda = taquilla("2\n1\n", new ByteArrayOutputStream())
                            .cobrar(CLIENTE, compartida, compartida.getAsientos());
                    verificar(segunda.isEmpty(), "Segunda taquilla rechazada durante el cobro");
                    intentos.incrementAndGet();
                }
            }
        };
        verificar(new Taquilla(new Scanner("1\n1\n2\n1\n"), pantallaCompartida)
                .cobrar(CLIENTE, compartida, compartida.getAsientos()).isPresent(), "Primera taquilla confirma");
        verificar(intentos.get() == 2, "Protección mantenida tras efectivo insuficiente");

        Sala interrumpida = nuevaSala();
        Thread.currentThread().interrupt();
        try {
            verificar(taquilla("2\n1\n", new ByteArrayOutputStream())
                    .cobrar(CLIENTE, interrumpida, interrumpida.getAsientos()).isEmpty(), "Interrupción no confirma");
        } finally {
            Thread.interrupted();
        }
        var recuperada = AdministradorBloqueos.paraSala(interrumpida).iniciar(CLIENTE, interrumpida.getAsientos());
        verificar(recuperada.getEstado() == Reservacion.Estado.EN_COBRO, "Interrupción libera");

        // Error inesperado de pantalla antes y después del pago.
        for (boolean despues : List.of(false, true)) {
            Sala conError = nuevaSala();
            PrintStream pantalla = new PrintStream(new ByteArrayOutputStream()) {
                @Override public void println(String texto) {
                    if ((!despues && texto.startsWith("Asientos")) ||
                            (despues && texto.startsWith("Reservación confirmada"))) {
                        throw new IllegalStateException("Error simulado de pantalla");
                    }
                }
            };
            falla(() -> new Taquilla(new Scanner("2\n1\n"), pantalla)
                    .cobrar(CLIENTE, conError, conError.getAsientos()), "Error simulado");
            AdministradorBloqueos recuperacion = AdministradorBloqueos.paraSala(conError);
            if (despues) {
                verificar(recuperacion.getConfirmadas().size() == 1, "Pago recuperable tras error");
                falla(() -> recuperacion.iniciar(CLIENTE, conError.getAsientos()), "Pago no se libera por error");
            } else {
                verificar(recuperacion.iniciar(CLIENTE, conError.getAsientos()) != null, "Error previo libera");
            }
        }
        System.out.println("OK: " + pruebas + " verificaciones de cobro.");
    }

    private static Sala nuevaSala() { return new Sala(new BigDecimal("70.00")); }
    private static Taquilla taquilla(String entrada, ByteArrayOutputStream salida) {
        return new Taquilla(new Scanner(entrada), new PrintStream(salida));
    }
    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        pruebas++;
    }
    private static void falla(Runnable accion, String caso) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException e) { pruebas++; return; }
        throw new AssertionError(caso);
    }
}
