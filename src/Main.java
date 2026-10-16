import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    // Cambiar este valor permite configurar el precio de la sala de ejemplo.
    private static final BigDecimal PRECIO_POR_ASIENTO = new BigDecimal("70.00");

    public static void main(String[] args) {
        System.out.println("Sistema de Reservaciones de Cine - Sprint 1");
        Sala sala = new Sala(PRECIO_POR_ASIENTO);
        CalculadoraPrecio calculadora = new CalculadoraPrecio();
        List<Asiento> seleccionados = new ArrayList<>();
        for (Asiento asiento : sala.getAsientos()) {
            System.out.println(asiento.getIdentificador() + ": $" + asiento.getPrecio() + " MXN");
        }
        Scanner entrada = new Scanner(System.in);
        while (true) {
            System.out.println("Escribe un asiento (ejemplo A1) o SALIR:");
            if (!entrada.hasNextLine()) {
                return;
            }
            String identificador = entrada.nextLine().strip().toUpperCase(Locale.ROOT);
            if (identificador.equals("SALIR")) {
                return;
            }
            Asiento elegido = null;
            for (Asiento asiento : sala.getAsientos()) {
                if (asiento.getIdentificador().equals(identificador)) {
                    elegido = asiento;
                    break;
                }
            }
            if (elegido == null) {
                System.out.println("Asiento inexistente. Usa A1 a C5.");
            } else if (seleccionados.contains(elegido)) {
                System.out.println("Ese asiento ya está seleccionado.");
            } else {
                seleccionados.add(elegido);
                System.out.println("Precio de la selección: $"
                        + calculadora.calcularTotal(seleccionados) + " MXN");
            }
        }
    }
}
