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
        System.out.println("Selección vacía. Precio de la selección: $0.00 MXN");
        while (true) {
            System.out.println("Escribe un asiento (A1), AGREGAR A1, ELIMINAR A1, VER o SALIR:");
            if (!entrada.hasNextLine()) {
                return;
            }
            String comando = entrada.nextLine().strip().toUpperCase(Locale.ROOT);
            if (comando.equals("SALIR")) {
                return;
            }
            if (comando.equals("VER")) {
                mostrarSeleccion(seleccionados, calculadora);
                continue;
            }
            String[] partes = comando.split("\\s+");
            boolean eliminar = partes[0].equals("ELIMINAR");
            boolean agregar = partes[0].equals("AGREGAR");
            if ((eliminar || agregar) && partes.length != 2) {
                System.out.println("Usa AGREGAR A1 o ELIMINAR A1.");
                continue;
            }
            String identificador = eliminar || agregar ? partes[1] : comando;
            Asiento elegido = null;
            for (Asiento asiento : sala.getAsientos()) {
                if (asiento.getIdentificador().equals(identificador)) {
                    elegido = asiento;
                    break;
                }
            }
            if (elegido == null) {
                System.out.println("Asiento inexistente. Usa A1 a C5.");
            } else if (eliminar && !seleccionados.contains(elegido)) {
                System.out.println("Ese asiento no está seleccionado.");
            } else if (!eliminar && seleccionados.contains(elegido)) {
                System.out.println("Ese asiento ya está seleccionado.");
            } else {
                // Validar y calcular la nueva selección antes de reemplazar la lista de consola.
                List<Asiento> nuevaSeleccion = new ArrayList<>(seleccionados);
                if (eliminar) {
                    nuevaSeleccion.remove(elegido);
                } else {
                    nuevaSeleccion.add(elegido);
                }
                calculadora.calcularTotal(nuevaSeleccion);
                seleccionados = nuevaSeleccion;
                mostrarSeleccion(seleccionados, calculadora);
            }
        }
    }

    private static void mostrarSeleccion(List<Asiento> seleccionados, CalculadoraPrecio calculadora) {
        if (seleccionados.isEmpty()) {
            System.out.println("Selección vacía.");
        } else {
            System.out.println("Asientos seleccionados: " + String.join(", ",
                    seleccionados.stream().map(Asiento::getIdentificador).toList()));
        }
        System.out.println("Precio de la selección: $" + calculadora.getTotalAcumulado() + " MXN");
    }
}
