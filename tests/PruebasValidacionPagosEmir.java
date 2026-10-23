import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Preparación para revisión; no representa pruebas realizadas o aprobadas por Emir. */
public class PruebasValidacionPagosEmir {
    private static int verificaciones;

    public static void main(String[] args) {
        // PruebasCobro ya cubre letras, negativos, insuficiente, cancelación y reintento.
        // Las pruebas de integración ya comprueban $140 con $200 y cambio de $60.
        probarLimitesDeUnCentavo();
        probarFormatosInvalidosAntesDeCancelar();
        probarReintentoConCentavosDesdeTaquilla();

        // Activar los métodos pendiente... solo cuando sus comprobaciones estén completas.
        System.out.println("OK: " + verificaciones + " verificaciones nuevas de validación de pagos.");
        System.out.println("PENDIENTES: 2 casos para completar y revisar; no ejecutados.");
    }

    private static void probarLimitesDeUnCentavo() {
        // La diferencia de un centavo debe respetarse sin redondear ni modificar el total.
        Sala sala = nuevaSala();
        AdministradorBloqueos administrador = AdministradorBloqueos.paraSala(sala);
        Reservacion reservacion = administrador.iniciar(new Cliente("Ejemplo", "555"), dosAsientos(sala));
        comprobar(!administrador.pagarEfectivo(reservacion, new BigDecimal("139.99")), "Falta un centavo: rechazar");
        comprobar(reservacion.getTotal().equals(new BigDecimal("140.00")), "El rechazo no cambia el total");
        comprobar(reservacion.getEstado() == Reservacion.Estado.EN_COBRO, "Insuficiente no confirma");
        comprobar(reservacion.getMetodoPago() == null && reservacion.getFechaConfirmacion() == null,
                "Insuficiente no registra método ni fecha de pago");
        comprobar(administrador.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.EN_COBRO,
                "El intento insuficiente conserva el bloqueo");
        comprobar(administrador.pagarEfectivo(reservacion, new BigDecimal("140.01")), "Sobra un centavo: aceptar");
        comprobar(reservacion.getTotal().equals(new BigDecimal("140.00")), "El excedente no aumenta el precio");
        comprobar(reservacion.getEstado() == Reservacion.Estado.CONFIRMADA, "Solo el segundo intento confirma");
        comprobar(administrador.getConfirmadas().size() == 1, "Solo una reservación pagada");
    }

    private static void probarFormatosInvalidosAntesDeCancelar() {
        // La consola acepta números con punto decimal, no símbolos ni formatos especiales.
        for (String importe : List.of("", "140,00", "$200", "NaN", "Infinity", "+200", "200 00", "-0.01")) {
            Sala sala = nuevaSala();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Taquilla taquilla = crearTaquilla("1\n" + importe + "\n0\n", buffer);
            Optional<Reservacion> resultado = taquilla.cobrar(new Cliente("Ejemplo", "555"), sala, dosAsientos(sala));
            String salida = texto(buffer);
            comprobar(resultado.isEmpty(), "No confirmar importe inválido: " + importe);
            comprobar(salida.contains("Importe inválido. Intenta de nuevo."), "Explicar el formato inválido: " + importe);
            comprobar(salida.contains("Monto total: $140.00 MXN"), "Conservar el precio mostrado: " + importe);
            comprobar(!salida.contains("Cambio:") && !salida.contains("Reservación confirmada:"),
                    "Sin cambio ni confirmación antes del pago: " + importe);
            AdministradorBloqueos administrador = AdministradorBloqueos.paraSala(sala);
            comprobar(administrador.getConfirmadas().isEmpty(), "Cancelar no registra pagos: " + importe);
            comprobar(administrador.consultarEstado("A1") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE
                            && administrador.consultarEstado("A2") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE,
                    "Cancelar después del error libera los dos asientos: " + importe);
        }
    }

    private static void probarReintentoConCentavosDesdeTaquilla() {
        // Comprobar el cambio exacto por consola después de faltar un centavo.
        Sala sala = nuevaSala();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Optional<Reservacion> resultado = crearTaquilla("1\n139.99\n1\n140.01\n", buffer)
                .cobrar(new Cliente("Ejemplo", "555"), sala, dosAsientos(sala));
        String salida = texto(buffer);
        comprobar(salida.contains("Efectivo insuficiente."), "Permite corregir 139.99");
        comprobar(salida.contains("Cambio: $0.01 MXN"), "Cambio exacto de un centavo");
        comprobar(resultado.isPresent(), "Reintento exitoso");
        comprobar(resultado.orElseThrow().getTotal().equals(new BigDecimal("140.00")), "Total original conservado");
        comprobar(resultado.get().getMetodoPago() == Reservacion.MetodoPago.EFECTIVO, "Método de pago correcto");
        comprobar(salida.lines().filter(linea -> linea.startsWith("Reservación confirmada:")).count() == 1,
                "Un solo mensaje de confirmación");
        comprobar(AdministradorBloqueos.paraSala(sala).getConfirmadas().size() == 1, "Sin compra duplicada");
    }

    private static void pendienteIntentosInsuficientesNoSeSumanComoAbonos() {
        // PENDIENTE para revisión y desarrollo personal: no se ejecuta desde main.
        // 1. Crear sala, buffer y taquilla con entrada "1\n100\n1\n40\n1\n140\n".
        // 2. Una compra de $140 debe rechazar 100 y 40: el programa no gestiona abonos.
        // 3. Comprobar dos mensajes de insuficiente y una sola confirmación al recibir 140.
        // 4. Revisar total $140 y cambio $0.00; agregar comprobaciones y activar en main.
    }

    private static void pendienteCambiarDeTerminalRechazadaAEfectivo() {
        // PENDIENTE para revisión y desarrollo personal: no se ejecuta desde main.
        // 1. Usar entrada "2\n2\n1\n200\n" para los mismos dos asientos de $140.
        // 2. Comprobar rechazo de terminal, seguido de cambio de $60 y una confirmación.
        // 3. Verificar método final EFECTIVO, total $140 y ambos asientos OCUPADO_PAGADO.
        // 4. Comprobar que solo existe una compra pagada; después activar en main.
    }

    private static Sala nuevaSala() { return new Sala(new BigDecimal("70.00")); }

    private static List<Asiento> dosAsientos(Sala sala) { return sala.getAsientos().subList(0, 2); }

    private static Taquilla crearTaquilla(String entrada, ByteArrayOutputStream buffer) {
        return new Taquilla(new Scanner(entrada), new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    private static String texto(ByteArrayOutputStream buffer) { return buffer.toString(StandardCharsets.UTF_8); }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
