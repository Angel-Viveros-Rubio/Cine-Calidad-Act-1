import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class PruebasMenuCineFlow {
    private static int verificaciones;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("out"));
        Path recuperados = Files.createTempDirectory(Path.of("out"), "cineflow-recuperados-");
        Sala sala = new Sala(new BigDecimal("70.00"));
        var administrador = AdministradorBloqueos.paraSala(sala);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        List<String> pasos = List.of("DISPONIBILIDAD", "CONFIRMAR", "AGREGAR A1", "AGREGAR B2", "VER", "ELIMINAR A1",
                "VER", "AGREGAR B2", "AGREGAR Z9", "AGREGAR A1", "DISPONIBILIDAD", "CONFIRMAR", "CONTINUAR",
                "", "Ana", "", "555", "0", "VER", "CONFIRMAR", "CONTINUAR", "Ana", "555", "1", "100", "1", "200",
                "COMPROBANTES", "REEMITIR", "@FOLIO", recuperados.toString(), "NUEVO", "VER", "AGREGAR A1",
                "DISPONIBILIDAD", "AGREGAR C5", "CONFIRMAR", "CONTINUAR", "Luis", "666", "2", "2", "2", "1", "SALIR");
        Readable entrada = new Readable() {
            int paso;
            @Override public int read(CharBuffer destino) {
                if (paso == pasos.size()) { return -1; }
                String valor = pasos.get(paso++);
                if (valor.equals("@FOLIO")) {
                    verificar(administrador.getConfirmadas().size() == 1, "Una compra antes de reemitir");
                    valor = administrador.getConfirmadas().get(0).getId().toString();
                }
                destino.put(valor + "\n");
                return valor.length() + 1;
            }
        };
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.ejecutar(sala, new Scanner(entrada));
        } finally { System.setOut(original); }
        String salida = buffer.toString(StandardCharsets.UTF_8);
        contiene(salida, "A1[S]");
        contiene(salida, "B2[S]");
        contiene(salida, "Cantidad de asientos: 2");
        contiene(salida, "Precio de la selección: $140.00 MXN");
        contiene(salida, "Asientos seleccionados: B2");
        contiene(salida, "Precio de la selección: $70.00 MXN");
        contiene(salida, "Ese asiento ya está seleccionado.");
        contiene(salida, "Asiento inexistente. Usa A1 a C5.");
        contiene(salida, "Selecciona al menos un asiento antes de continuar.");
        contiene(salida, "El campo Nombre es obligatorio.");
        contiene(salida, "El campo Teléfono es obligatorio.");
        contiene(salida, "No se confirmó una compra.");
        contiene(salida, "Efectivo insuficiente.");
        contiene(salida, "Cambio: $60.00 MXN");
        contiene(salida, "Pago rechazado.");
        contiene(salida, "A1[P]");
        contiene(salida, "B2[P]");
        contiene(salida, "C5[P]");
        contiene(salida, "Nueva reservación. Selección vacía.");
        contiene(salida, "No está disponible.");
        contiene(salida, "Folio: ");
        contiene(salida, "Importe pagado: $140.00 MXN");
        verificar(salida.lines().filter(l -> l.startsWith("Reservación confirmada:")).count() == 2, "Solo dos compras");
        verificar(salida.lines().filter(l -> l.startsWith("Comprobante:")).count() == 3, "Dos originales y una recuperación");
        verificar(administrador.getConfirmadas().size() == 2, "NUEVO y REEMITIR conservan compras");
        verificar(administrador.getDisponibles().size() == 12, "Solo tres lugares pagados");
        Reservacion ana = administrador.getConfirmadas().stream()
                .filter(r -> r.getCliente().getNombre().equals("Ana")).findFirst().orElseThrow();
        verificar(ana.getMetodoPago() == Reservacion.MetodoPago.EFECTIVO, "Efectivo");
        verificar(ana.getTotal().equals(new BigDecimal("140.00")), "Total correcto después de cancelar y modificar");
        Path originalTxt = Path.of("comprobantes", "comprobante-" + ana.getId() + ".txt");
        Path recuperado = recuperados.resolve(originalTxt.getFileName());
        verificar(Files.readString(originalTxt).equals(Files.readString(recuperado)), "TXT idéntico sin segundo cobro");
        verificar(administrador.getConfirmadas().stream().anyMatch(r -> r.getCliente().getNombre().equals("Luis")
                && r.getMetodoPago() == Reservacion.MetodoPago.TERMINAL), "Terminal tras rechazo");
        verificar(!salida.contains("$-"), "Sin importes negativos");
        Files.createDirectories(Path.of("out", "kan-19"));
        Files.writeString(Path.of("out", "kan-19", "recorrido-nueve-comandos.txt"), salida, StandardCharsets.UTF_8);
        System.out.println("OK: " + verificaciones + " verificaciones del recorrido de nueve comandos CineFlow.");
    }

    private static void contiene(String texto, String esperado) { verificar(texto.contains(esperado), esperado); }
    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
