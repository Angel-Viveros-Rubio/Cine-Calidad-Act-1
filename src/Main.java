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
        PresentacionConsola presentacion = new PresentacionConsola(System.out);
        presentacion.mostrarMenu();
        sala.getAsientos().stream().map(Asiento::getPrecio).distinct()
                .forEach(precio -> System.out.println("Precio por asiento: $" + precio + " MXN"));
        Taquilla taquilla = new Taquilla(entrada, System.out);
        presentacion.mostrarSala(sala, administrador, seleccionados);
        System.out.println("Selección vacía. Precio de la selección: $0.00 MXN");
        presentacion.mostrarAyuda(false);
        while (true) {
            System.out.println("Comando:");
            if (!entrada.hasNextLine()) {
                return;
            }
            String comando = entrada.nextLine().strip().toUpperCase(Locale.ROOT);
            if (comando.equals("SALIR")) {
                return;
            }
            if (comando.equals("COMPROBANTES")) {
                presentacion.mostrarComprobantes(administrador.getConfirmadas());
                continue;
            }
            if (comando.equals("REEMITIR")) {
                System.out.println("Folio de la reservación pagada:");
                if (!entrada.hasNextLine()) { return; }
                java.util.Optional<Reservacion> pagada;
                try {
                    pagada = administrador.buscarConfirmada(java.util.UUID.fromString(entrada.nextLine().strip()));
                } catch (IllegalArgumentException e) {
                    System.out.println("Folio inválido. Copia el UUID completo que aparece en COMPROBANTES.");
                    continue;
                }
                if (pagada.isEmpty()) {
                    System.out.println("No existe una reservación pagada con ese folio en esta ejecución. Consulta COMPROBANTES.");
                    continue;
                }
                System.out.println("Directorio destino (Enter = comprobantes):");
                if (!entrada.hasNextLine()) { return; }
                String directorio = entrada.nextLine().strip();
                try {
                    taquilla.generarComprobante(pagada.get(), java.nio.file.Path.of(
                            directorio.isEmpty() ? "comprobantes" : directorio));
                } catch (java.nio.file.InvalidPathException e) {
                    System.out.println("Ruta inválida. Puedes reintentar REEMITIR sin volver a pagar.");
                }
                continue;
            }
            if (comando.equals("VER")) {
                presentacion.mostrarSala(sala, administrador, seleccionados);
                presentacion.mostrarSeleccion(seleccionados, calculadora);
                presentacion.mostrarAyuda(!seleccionados.isEmpty());
                continue;
            }
            if (comando.equals("DISPONIBILIDAD")) {
                presentacion.mostrarSala(sala, administrador, seleccionados);
                continue;
            }
            if (comando.equals("NUEVO")) {
                seleccionados.clear();
                calculadora.calcularTotal(seleccionados);
                System.out.println("Nueva reservación. Selección vacía.");
                presentacion.mostrarSala(sala, administrador, seleccionados);
                presentacion.mostrarSeleccion(seleccionados, calculadora);
                presentacion.mostrarAyuda(false);
                continue;
            }
            if (comando.equals("CONFIRMAR")) {
                if (seleccionados.isEmpty()) {
                    System.out.println("Selecciona al menos un asiento antes de continuar.");
                    presentacion.mostrarAyuda(false);
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
                presentacion.mostrarSala(sala, administrador, seleccionados);
                presentacion.mostrarAyuda(!seleccionados.isEmpty());
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
                System.out.println("Formato: AGREGAR A1 o A1. Consulta el mapa con DISPONIBILIDAD.");
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
                presentacion.mostrarSeleccion(seleccionados, calculadora);
                presentacion.mostrarAyuda(!seleccionados.isEmpty());
            }
        }
    }

    private static void rechazarNoDisponible(Asiento asiento, AdministradorBloqueos administrador) {
        System.out.println("El asiento " + asiento.getIdentificador() + " está protegido por otro cobro. Estado: "
                + administrador.consultarEstado(asiento.getIdentificador()));
        System.out.println("No está disponible. Elige un lugar [L] en DISPONIBILIDAD; tu total no cambia.");
    }
}
