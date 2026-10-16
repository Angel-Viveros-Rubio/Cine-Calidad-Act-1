import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class PruebasComprobante {
    private static int pruebas;

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(Path.of("out"), "pruebas-comprobante-");
        GeneradorComprobante generador = new GeneradorComprobante();
        for (boolean efectivo : List.of(true, false)) {
            Sala sala = new Sala(new BigDecimal("70.00"));
            AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
            verificar(admin.getDisponibles().size() == 15, "Disponibilidad inicial");
            verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE, "Disponible");
            Reservacion r = admin.iniciar(new Cliente("María López", "0123456789"), sala.getAsientos().subList(0, 2));
            verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.EN_COBRO, "En cobro");
            verificar(admin.getDisponibles().size() == 13, "Exclusión durante cobro");
            falla(() -> generador.generar(r, carpeta), "Pendiente sin comprobante");
            verificar(!admin.pagarTerminal(r, false), "Rechazo");
            verificar(!admin.pagarEfectivo(r, BigDecimal.ONE), "Insuficiente");
            verificar(r.getMetodoPago() == null && r.getFechaConfirmacion() == null, "Sin metadatos de pago");
            verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.EN_COBRO, "Rechazo no ocupa");
            verificar(efectivo ? admin.pagarEfectivo(r, new BigDecimal("200")) : admin.pagarTerminal(r, true), "Pago exitoso");
            for (Asiento asiento : r.getAsientos()) {
                verificar(admin.consultarEstado(asiento.getIdentificador()) == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO,
                        "Todos los asientos pagados");
            }
            verificar(r.getEstado() == Reservacion.Estado.CONFIRMADA, "Confirmación");
            verificar(r.getMetodoPago() == (efectivo ? Reservacion.MetodoPago.EFECTIVO : Reservacion.MetodoPago.TERMINAL), "Método correcto");
            verificar(r.getFechaConfirmacion().getZone().getId().equals("America/Mexico_City"), "Zona horaria");
            var fecha = r.getFechaConfirmacion();
            admin.cancelar(r);
            verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO, "Cancelar no libera pago");
            verificar(admin.getDisponibles().stream().noneMatch(a -> a.getIdentificador().equals("A1") || a.getIdentificador().equals("A2")),
                    "Pagados no disponibles");
            falla(() -> admin.iniciar(new Cliente("Otro", "555"), r.getAsientos()), "No doble reservación");
            falla(() -> admin.pagarTerminal(r, true), "No doble cobro");
            Path obstaculo = Files.createTempFile(carpeta, "no-directorio", ".txt");
            Taquilla taquilla = new Taquilla(new Scanner(""), new PrintStream(new ByteArrayOutputStream()));
            verificar(taquilla.generarComprobante(r, obstaculo).isEmpty(), "Error de escritura controlado");
            verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO, "Error conserva ocupación");
            verificar(r.getEstado() == Reservacion.Estado.CONFIRMADA, "Error conserva pago");
            Path archivo = generador.generar(r, carpeta);
            String texto = Files.readString(archivo, StandardCharsets.UTF_8);
            for (String dato : List.of(r.getId().toString(), "María López", "0123456789", "A1, A2", "$140.00 MXN",
                    r.getMetodoPago().toString(), fecha.toString().substring(0, 10), "America/Mexico_City")) {
                verificar(texto.contains(dato), "Contenido: " + dato);
            }
            verificar(generador.generar(r, carpeta).equals(archivo), "Reintento mismo archivo");
            verificar(Files.readString(archivo).equals(texto) && r.getFechaConfirmacion().equals(fecha), "Contenido y fecha estables");
            CountDownLatch inicio = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Path concurrente = carpeta.resolve("concurrente-" + efectivo);
            Runnable generar = () -> {
                try { inicio.await(); new GeneradorComprobante().generar(r, concurrente); }
                catch (Throwable e) { error.set(e); }
            };
            Thread uno = new Thread(generar), dos = new Thread(generar);
            uno.start(); dos.start(); inicio.countDown(); uno.join(); dos.join();
            verificar(error.get() == null, "Generación simultánea consistente");
            verificar(Files.readString(concurrente.resolve(archivo.getFileName())).equals(texto), "Publicación completa");
            Files.writeString(archivo, "contenido ajeno");
            falla(() -> generador.generar(r, carpeta), "No sobrescribe contenido distinto");
            verificar(Files.readString(archivo).equals("contenido ajeno"), "Conserva archivo ajeno");
            falla(() -> generador.generar(r, Path.of("src")), "Fuera de fuentes");
            falla(() -> generador.generar(r, Path.of("tests")), "Fuera de pruebas fuente");
        }
        Sala sala = new Sala(new BigDecimal("70.00"));
        var admin = AdministradorBloqueos.paraSala(sala);
        var cancelada = admin.iniciar(new Cliente("Ana", "555"), sala.getAsientos());
        admin.cancelar(cancelada);
        verificar(admin.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE, "Cancelar pendiente libera");
        falla(() -> generador.generar(cancelada, carpeta), "Cancelada sin comprobante");
        falla(() -> admin.consultarEstado("Z9"), "Asiento inexistente");
        System.out.println("OK: " + pruebas + " verificaciones de ocupación y comprobantes.");
    }

    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        pruebas++;
    }
    private static void falla(Accion accion, String caso) throws Exception {
        try { accion.ejecutar(); }
        catch (IOException | IllegalArgumentException | IllegalStateException e) { pruebas++; return; }
        throw new AssertionError(caso);
    }
    private interface Accion { void ejecutar() throws Exception; }
}
