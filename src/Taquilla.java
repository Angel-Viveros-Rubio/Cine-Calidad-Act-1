import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public class Taquilla {
    private final Scanner entrada;
    private final PrintStream salida;
    private final CalculadoraPrecio calculadora = new CalculadoraPrecio();

    // La entrada y la salida pertenecen a quien crea la taquilla; aquí no se cierran.
    public Taquilla(Scanner entrada, PrintStream salida) {
        this.entrada = Objects.requireNonNull(entrada, "La entrada es obligatoria.");
        this.salida = Objects.requireNonNull(salida, "La salida es obligatoria.");
    }

    // Sin datos parciales: si termina la entrada, no se registra un cliente.
    public Optional<Cliente> capturarCliente() {
        String nombre = leerObligatorio("Nombre");
        if (nombre == null) {
            return Optional.empty();
        }
        String telefono = leerObligatorio("Teléfono");
        if (telefono == null) {
            return Optional.empty();
        }
        return Optional.of(new Cliente(nombre, telefono));
    }

    private String leerObligatorio(String campo) {
        while (true) {
            salida.println(campo + ":");
            if (!entrada.hasNextLine()) {
                salida.println("Captura cancelada: terminó la entrada de datos.");
                return null;
            }
            String valor = entrada.nextLine().strip();
            if (!valor.isBlank()) {
                return valor;
            }
            salida.println("El campo " + campo + " es obligatorio. Intenta de nuevo.");
        }
    }

    public void mostrarResumen(Collection<Asiento> asientos) {
        // Validar y calcular antes de imprimir evita mostrar resúmenes incompletos.
        BigDecimal total = calculadora.calcularTotal(asientos);
        if (asientos.isEmpty()) {
            salida.println("No hay asientos seleccionados.");
        } else {
            salida.println("Asientos seleccionados:");
            for (Asiento asiento : asientos) {
                salida.println("- " + asiento.getIdentificador());
            }
        }
        salida.println("Monto total: $" + total.toPlainString() + " MXN");
    }

    public Optional<Reservacion> cobrar(Cliente cliente, Sala sala, Collection<Asiento> asientos) {
        AdministradorBloqueos administrador = AdministradorBloqueos.paraSala(sala);
        Reservacion reservacion;
        try {
            reservacion = administrador.iniciar(cliente, asientos);
        } catch (IllegalArgumentException | IllegalStateException e) {
            salida.println(e.getMessage());
            return Optional.empty();
        }
        try {
            mostrarResumen(reservacion.getAsientos());
            while (!Thread.currentThread().isInterrupted()) {
                salida.println("Pago: 1 efectivo, 2 terminal simulada, 0 cancelar:");
                String opcion = leerPago();
                if (opcion == null || opcion.equals("0")) {
                    return Optional.empty();
                }
                boolean aprobado;
                if (opcion.equals("1")) {
                    salida.println("Importe recibido en MXN (punto decimal; 0 cancelar):");
                    String importe = leerPago();
                    if (importe == null || importe.equals("0")) {
                        return Optional.empty();
                    }
                    BigDecimal recibido;
                    try {
                        if (!importe.matches("[0-9]+(\\.[0-9]{1,2})?")) {
                            throw new IllegalArgumentException("Importe inválido.");
                        }
                        recibido = new BigDecimal(importe);
                        aprobado = administrador.pagarEfectivo(reservacion, recibido);
                    } catch (IllegalArgumentException e) {
                        salida.println("Importe inválido. Intenta de nuevo.");
                        continue;
                    }
                    if (aprobado) {
                        salida.println("Cambio: $" + recibido.subtract(reservacion.getTotal()).toPlainString() + " MXN");
                    } else {
                        salida.println("Efectivo insuficiente. No se ha confirmado la reservación.");
                    }
                } else if (opcion.equals("2")) {
                    salida.println("Resultado simulado: 1 aprobado, 2 rechazado, 0 cancelar:");
                    String resultado = leerPago();
                    if (resultado == null || resultado.equals("0")) {
                        return Optional.empty();
                    }
                    if (!resultado.equals("1") && !resultado.equals("2")) {
                        salida.println("Resultado inválido. Intenta de nuevo.");
                        continue;
                    }
                    aprobado = administrador.pagarTerminal(reservacion, resultado.equals("1"));
                    if (!aprobado) {
                        salida.println("Pago rechazado. No se ha confirmado la reservación.");
                    }
                } else {
                    salida.println("Opción inválida. Intenta de nuevo.");
                    continue;
                }
                if (aprobado) {
                    generarComprobante(reservacion, java.nio.file.Path.of("comprobantes"));
                    salida.println("Reservación confirmada: " + reservacion.getId());
                    return Optional.of(reservacion);
                }
            }
            return Optional.empty();
        } finally {
            // Libera solo cobros pendientes. Un pago ya confirmado permanece recuperable.
            administrador.cancelar(reservacion);
        }
    }

    private String leerPago() {
        if (Thread.currentThread().isInterrupted() || !entrada.hasNextLine()) {
            return null;
        }
        return entrada.nextLine().strip();
    }

    public Optional<java.nio.file.Path> generarComprobante(Reservacion reservacion,
            java.nio.file.Path directorio) {
        try {
            java.nio.file.Path archivo = new GeneradorComprobante().generar(reservacion, directorio);
            salida.println("Comprobante: " + archivo);
            return Optional.of(archivo);
        } catch (java.io.IOException | SecurityException e) {
            salida.println("No se pudo escribir el comprobante. El pago y los asientos siguen confirmados. "
                    + "Reintenta generar el folio " + reservacion.getId() + ".");
            return Optional.empty();
        }
    }

    // Demostración independiente del flujo principal, con datos únicamente en memoria.
    public static void main(String[] args) {
        Taquilla taquilla = new Taquilla(new Scanner(System.in), System.out);
        Optional<Cliente> cliente = taquilla.capturarCliente();
        if (cliente.isEmpty()) {
            return;
        }
        System.out.println("Cliente: " + cliente.get().getNombre());
        System.out.println("Teléfono: " + cliente.get().getTelefono());
        System.out.println("Demostración con asientos de ejemplo A1 y A2:");
        Sala sala = new Sala(new BigDecimal("70.00"));
        List<Asiento> asientos = sala.getAsientos().subList(0, 2);
        taquilla.cobrar(cliente.get(), sala, asientos);
        AdministradorBloqueos administrador = AdministradorBloqueos.paraSala(sala);
        for (Asiento asiento : asientos) {
            System.out.println(asiento.getIdentificador() + ": "
                    + administrador.consultarEstado(asiento.getIdentificador()));
        }
        System.out.println("Asientos disponibles: " + administrador.getDisponibles().size());
    }
}
