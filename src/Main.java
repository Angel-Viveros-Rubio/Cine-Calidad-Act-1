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
        ejecutar(sala, new Scanner(System.in));
    }

    // Entrada separada para probar disponibilidad con la misma sala compartida.
    static void ejecutar(Sala sala, Scanner entrada) {
        AdministradorBloqueos administrador = AdministradorBloqueos.paraSala(sala);
        CalculadoraPrecio calculadora = new CalculadoraPrecio();
        List<Asiento> seleccionados = new ArrayList<>();
        for (Asiento asiento : sala.getAsientos()) {
            System.out.println(asiento.getIdentificador() + ": $" + asiento.getPrecio() + " MXN");
        }
        Taquilla taquilla = new Taquilla(entrada, System.out);
        mostrarDisponibilidad(sala, administrador);
        System.out.println("DISPONIBILIDAD muestra los estados; NUEVO inicia una selección vacía para otro cliente.");
        System.out.println("Escribe CONFIRMAR para revisar la selección y continuar a taquilla.");
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
                mostrarDisponibilidad(sala, administrador);
                mostrarSeleccion(seleccionados, calculadora);
                continue;
            }
            if (comando.equals("DISPONIBILIDAD")) {
                mostrarDisponibilidad(sala, administrador);
                continue;
            }
            if (comando.equals("NUEVO")) {
                seleccionados.clear();
                calculadora.calcularTotal(seleccionados);
                System.out.println("Nueva reservación. Selección vacía.");
                mostrarDisponibilidad(sala, administrador);
                mostrarSeleccion(seleccionados, calculadora);
                continue;
            }
            if (comando.equals("CONFIRMAR")) {
                if (seleccionados.isEmpty()) {
                    System.out.println("Selecciona al menos un asiento antes de continuar.");
                    continue;
                }
                boolean disponible = true;
                for (Asiento asiento : seleccionados) {
                    if (administrador.consultarEstado(asiento.getIdentificador())
                            != AdministradorBloqueos.EstadoAsiento.DISPONIBLE) {
                        rechazarNoDisponible(asiento, administrador);
                        disponible = false;
                    }
                }
                if (!disponible) { continue; }
                taquilla.mostrarResumen(seleccionados);
                String decision;
                while (true) {
                    System.out.println("Escribe CONTINUAR para capturar al cliente o VOLVER para modificar asientos:");
                    if (!entrada.hasNextLine()) { return; }
                    decision = entrada.nextLine().strip().toUpperCase(Locale.ROOT);
                    if (decision.equals("CONTINUAR") || decision.equals("VOLVER")) { break; }
                    System.out.println("Opción inválida. Usa CONTINUAR o VOLVER.");
                }
                if (decision.equals("VOLVER")) { continue; }
                var cliente = taquilla.capturarCliente();
                if (cliente.isEmpty()) { return; }
                var reservacion = taquilla.cobrar(cliente.get(), sala, List.copyOf(seleccionados));
                if (reservacion.isPresent()) {
                    System.out.println("Compra finalizada. Los asientos pagados permanecen protegidos.");
                    seleccionados.clear();
                    calculadora.calcularTotal(seleccionados);
                    System.out.println("Puedes comenzar la reservación del siguiente cliente.");
                } else {
                    System.out.println("No se confirmó una compra. Puedes modificar la selección o reintentar.");
                }
                mostrarDisponibilidad(sala, administrador);
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
            } else if (!eliminar && administrador.consultarEstado(elegido.getIdentificador())
                    != AdministradorBloqueos.EstadoAsiento.DISPONIBLE) {
                rechazarNoDisponible(elegido, administrador);
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

    private static void rechazarNoDisponible(Asiento asiento, AdministradorBloqueos administrador) {
        System.out.println("El asiento " + asiento.getIdentificador() + " está protegido por otro cobro. Estado: "
                + administrador.consultarEstado(asiento.getIdentificador()));
    }

    private static void mostrarDisponibilidad(Sala sala, AdministradorBloqueos administrador) {
        System.out.println("Disponibilidad actual de la sala:");
        for (Asiento asiento : sala.getAsientos()) {
            System.out.println(asiento.getIdentificador() + " - "
                    + administrador.consultarEstado(asiento.getIdentificador()));
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
