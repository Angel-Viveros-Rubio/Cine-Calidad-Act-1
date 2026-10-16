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

    // Demostración independiente: los asientos de ejemplo no crean una reservación.
    public static void main(String[] args) {
        Taquilla taquilla = new Taquilla(new Scanner(System.in), System.out);
        Optional<Cliente> cliente = taquilla.capturarCliente();
        if (cliente.isEmpty()) {
            return;
        }
        System.out.println("Cliente: " + cliente.get().getNombre());
        System.out.println("Teléfono: " + cliente.get().getTelefono());
        System.out.println("Demostración con asientos de ejemplo A1 y A2:");
        List<Asiento> asientos = new Sala(new BigDecimal("70.00"))
                .getAsientos().subList(0, 2);
        taquilla.mostrarResumen(asientos);
    }
}
