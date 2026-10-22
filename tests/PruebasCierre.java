import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

public class PruebasCierre {
    private static int verificaciones;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("out"));
        Path destino = Files.createTempDirectory(Path.of("out"), "cierre-");
        Path obstaculo = Files.createTempFile(destino, "no-es-directorio-", ".txt");
        Sala sala = new Sala(new BigDecimal("70.00"));
        AdministradorBloqueos admin = AdministradorBloqueos.paraSala(sala);
        List<String> pasos = List.of("COMPROBANTES", "REEMITIR", "invalido", "REEMITIR", UUID.randomUUID().toString(),
                "A1", "A2", "A1", "ELIMINAR A2", "VER", "CONFIRMAR", "VOLVER", "A3", "CONFIRMAR", "CONTINUAR",
                "", "Ana", "", "555", "1", "100", "1", "200",
                "COMPROBANTES", "REEMITIR", "@FOLIO", obstaculo.toString(), "REEMITIR", "@FOLIO", destino.toString(),
                "REEMITIR", "@FOLIO", destino.toString(), "NUEVO", "A1", "A3", "A2", "CONFIRMAR", "CONTINUAR",
                "Luis", "666", "2", "2", "2", "1", "B1", "CONFIRMAR", "CONTINUAR", "Eva", "777", "0",
                "NUEVO", "B1", "VER", "DISPONIBILIDAD", "SALIR");
        String salida = ejecutar(sala, pasos);
        cantidad(salida, "Reservación confirmada:", 2);
        cantidad(salida, "Pago:", 5);
        contiene(salida, "No hay reservaciones pagadas en esta ejecución.");
        contiene(salida, "Folio inválido.");
        contiene(salida, "No existe una reservación pagada con ese folio");
        contiene(salida, "Ese asiento ya está seleccionado.");
        contiene(salida, "Asientos seleccionados: A1");
        contiene(salida, "Monto total: $140.00 MXN");
        contiene(salida, "El campo Nombre es obligatorio.");
        contiene(salida, "El campo Teléfono es obligatorio.");
        contiene(salida, "Efectivo insuficiente.");
        contiene(salida, "Cambio: $60.00 MXN");
        contiene(salida, "Pago rechazado.");
        contiene(salida, "No se pudo escribir el comprobante.");
        contiene(salida, "El asiento A1 está protegido");
        contiene(salida, "El asiento A3 está protegido");
        contiene(salida, "B1[L]");
        verificar(admin == AdministradorBloqueos.paraSala(sala), "Administrador conservado");
        verificar(admin.getConfirmadas().size() == 2, "Reemitir no crea reservaciones");
        verificar(admin.getDisponibles().size() == 12, "Solo tres asientos pagados");
        for (String id : List.of("A1", "A2", "A3")) {
            verificar(admin.consultarEstado(id) == AdministradorBloqueos.EstadoAsiento.OCUPADO_PAGADO, "Ocupación " + id);
        }
        verificar(admin.consultarEstado("B1") == AdministradorBloqueos.EstadoAsiento.DISPONIBLE, "Cancelación libera");
        Reservacion ana = admin.getConfirmadas().stream().filter(r -> r.getCliente().getNombre().equals("Ana")).findFirst().orElseThrow();
        verificar(admin.buscarConfirmada(ana.getId()).orElseThrow() == ana, "Localización por folio");
        Path original = Path.of("comprobantes", "comprobante-" + ana.getId() + ".txt");
        Path copia = destino.resolve(original.getFileName());
        verificar(Files.readString(original).equals(Files.readString(copia)), "Recuperación idéntica tras error");
        String recibo = Files.readString(copia, StandardCharsets.UTF_8);
        for (String dato : List.of(ana.getId().toString(), "Ana", "555", "A1, A3", "$140.00 MXN", "EFECTIVO", "America/Mexico_City")) {
            contiene(recibo, dato);
        }
        try (var archivos = Files.list(destino)) {
            verificar(archivos.filter(p -> p.toString().endsWith(".txt") && p.getFileName().toString().startsWith("comprobante-")).count() == 1,
                    "Reemisión repetida usa el mismo archivo");
        }
        var pendiente = admin.iniciar(new Cliente("Pendiente", "888"), sala.getAsientos().subList(5, 6));
        verificar(admin.buscarConfirmada(pendiente.getId()).isEmpty(), "Pendiente no consultable como pagada");
        salida = ejecutar(sala, List.of("REEMITIR", pendiente.getId().toString(), "SALIR"));
        contiene(salida, "No existe una reservación pagada con ese folio");
        admin.cancelar(pendiente);
        verificar(admin.buscarConfirmada(pendiente.getId()).isEmpty(), "Cancelada no consultable como pagada");
        verificar(admin.buscarConfirmada(UUID.randomUUID()).isEmpty(), "Folio inexistente");

        // También ejecuta el punto de entrada público, no solo la sala inyectada.
        salida = ejecutar(null, List.of("C5", "CONFIRMAR", "CONTINUAR", "Eva", "777", "2", "1",
                "COMPROBANTES", "REEMITIR", "@FOLIO", destino.toString(), "SALIR"));
        cantidad(salida, "Reservación confirmada:", 1);
        cantidad(salida, "Pago:", 1);
        cantidad(salida, "Comprobante:", 2);
        contiene(salida, "C5[P]");
        System.out.println("OK: " + verificaciones + " verificaciones de cierre.");
    }

    private static String ejecutar(Sala sala, List<String> pasos) {
        PrintStream originalSalida = System.out;
        InputStream originalEntrada = System.in;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream entrada = new InputStream() {
            int paso;
            byte[] linea = new byte[0];
            int posicion;
            @Override public int read() {
                if (posicion == linea.length) {
                    if (paso == pasos.size()) { return -1; }
                    String texto = pasos.get(paso++);
                    if (texto.equals("@FOLIO")) {
                        var folio = Pattern.compile("Reservación confirmada: ([a-f0-9-]+)")
                                .matcher(buffer.toString(StandardCharsets.UTF_8));
                        if (!folio.find()) { throw new AssertionError("No hay folio confirmado"); }
                        texto = folio.group(1);
                    }
                    linea = (texto + "\n").getBytes(Charset.defaultCharset());
                    posicion = 0;
                }
                return linea[posicion++] & 255;
            }
            // Entrega una línea por lectura para simular interacción después de imprimir el folio.
            @Override public int read(byte[] destino, int inicio, int largo) {
                if (largo == 0) { return 0; }
                int primero = read();
                if (primero < 0) { return -1; }
                destino[inicio] = (byte) primero;
                int cantidad = 1;
                while (cantidad < largo && posicion < linea.length) { destino[inicio + cantidad++] = linea[posicion++]; }
                return cantidad;
            }
        };
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            System.setIn(entrada);
            if (sala == null) { Main.main(new String[0]); }
            else { Main.ejecutar(sala, new Scanner(entrada)); }
            return buffer.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originalSalida);
            System.setIn(originalEntrada);
        }
    }
    private static void contiene(String texto, String esperado) { verificar(texto.contains(esperado), esperado); }
    private static void cantidad(String texto, String prefijo, long esperada) {
        verificar(texto.lines().filter(l -> l.startsWith(prefijo)).count() == esperada, "Cantidad de " + prefijo);
    }
    private static void verificar(boolean condicion, String caso) {
        if (!condicion) { throw new AssertionError(caso); }
        verificaciones++;
    }
}
