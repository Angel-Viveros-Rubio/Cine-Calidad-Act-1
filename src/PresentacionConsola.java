import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Presentación de CineFlow; no modifica selecciones, pagos ni estados reales. */
public final class PresentacionConsola {
    private final PrintStream salida;

    public PresentacionConsola(PrintStream salida) { this.salida = salida; }

    public void mostrarMenu() {
        salida.println("CINEFLOW | OPERACIÓN DE TAQUILLA");
        TablaAscii.imprimir(salida, List.of("PALABRA CLAVE", "FUNCIONALIDAD"), List.of(
                List.of("DISPONIBILIDAD", "Mostrar el mapa y estado de los asientos."),
                List.of("NUEVO", "Iniciar una operación para otro cliente."),
                List.of("CONFIRMAR", "Revisar selección y continuar con datos y pago."),
                List.of("COMPROBANTES", "Consultar las reservaciones pagadas de la sesión."),
                List.of("REEMITIR", "Recuperar un comprobante mediante su folio."),
                List.of("AGREGAR", "Añadir un asiento disponible. Ejemplo: AGREGAR A1."),
                List.of("ELIMINAR", "Retirar un asiento seleccionado. Ejemplo: ELIMINAR A1."),
                List.of("VER", "Consultar sala, selección, cantidad e importe total."),
                List.of("SALIR", "Terminar la aplicación.")), 14, 59);
    }

    public void mostrarSala(Sala sala, AdministradorBloqueos administrador, Collection<Asiento> seleccion) {
        salida.println("MAPA DE LA SALA | DISPONIBILIDAD");
        Map<String, List<String>> filas = new LinkedHashMap<>();
        for (Asiento asiento : sala.getAsientos()) {
            var estado = administrador.consultarEstado(asiento.getIdentificador());
            // El estado real siempre prevalece sobre una selección local anterior.
            String simbolo = switch (estado) {
                case EN_COBRO -> "B";
                case OCUPADO_PAGADO -> "P";
                case DISPONIBLE -> seleccion.contains(asiento) ? "S" : "L";
            };
            String fila = asiento.getIdentificador().replaceFirst("\\d+$", "");
            filas.computeIfAbsent(fila, clave -> new ArrayList<>())
                    .add(asiento.getIdentificador() + "[" + simbolo + "]");
        }
        int asientosPorFila = filas.values().stream().mapToInt(List::size).max().orElse(0);
        List<String> encabezados = new ArrayList<>(List.of("FILA"));
        int[] anchos = new int[asientosPorFila + 1];
        anchos[0] = 4;
        for (int i = 1; i <= asientosPorFila; i++) { encabezados.add(String.valueOf(i)); anchos[i] = 7; }
        List<List<String>> celdas = new ArrayList<>();
        filas.forEach((nombre, asientos) -> {
            List<String> fila = new ArrayList<>(List.of(nombre));
            fila.addAll(asientos);
            while (fila.size() < encabezados.size()) { fila.add(""); }
            celdas.add(fila);
        });
        TablaAscii.imprimir(salida, encabezados, celdas, anchos);
        salida.println("Leyenda: [L] DISPONIBLE | [S] SELECCIONADO (temporal)");
        salida.println("         [B] EN COBRO / BLOQUEADO | [P] OCUPADO / PAGADO");
    }

    public void mostrarSeleccion(List<Asiento> seleccionados, CalculadoraPrecio calculadora) {
        salida.println("RESUMEN DEL CLIENTE | SELECCIÓN ACTUAL");
        if (seleccionados.isEmpty()) { salida.println("Selección vacía."); }
        else {
            salida.println("Asientos seleccionados: " + String.join(", ",
                    seleccionados.stream().map(Asiento::getIdentificador).toList()));
        }
        salida.println("Cantidad de asientos: " + seleccionados.size());
        salida.println("Precio de la selección: $" + calculadora.getTotalAcumulado() + " MXN");
    }

    public void mostrarAyuda(boolean haySeleccion) {
        salida.println(haySeleccion
                ? "Siguiente paso: VER o CONFIRMAR. Para modificar: AGREGAR A1 o ELIMINAR A1."
                : "Para comenzar: AGREGAR A1 (también puedes escribir A1). Consulta DISPONIBILIDAD.");
    }

    public void mostrarComprobantes(List<Reservacion> pagadas) {
        salida.println("COMPROBANTES | RESERVACIONES PAGADAS DE LA SESIÓN");
        if (pagadas.isEmpty()) {
            salida.println("No hay reservaciones pagadas en esta ejecución.");
        }
        for (Reservacion pagada : pagadas) {
            salida.println("------------------------------------------------------------");
            salida.println("Folio: " + pagada.getId());
            salida.println("Cliente: " + pagada.getCliente().getNombre());
            salida.println("Asientos: " + String.join(", ", pagada.getAsientos().stream()
                    .map(Asiento::getIdentificador).toList()));
            salida.println("Importe pagado: $" + pagada.getTotal() + " MXN");
            salida.println("Método: " + pagada.getMetodoPago());
            salida.println("Fecha: " + pagada.getFechaConfirmacion());
        }
        salida.println("Para recuperar un TXT: REEMITIR y después copia el folio completo.");
    }
}
