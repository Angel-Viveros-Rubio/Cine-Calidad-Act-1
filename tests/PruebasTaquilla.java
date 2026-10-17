import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class PruebasTaquilla {
    private static int verificaciones;

    public static void main(String[] args) {
        Cliente cliente = new Cliente("  Ana López  ", "  0123456789  ");
        comprobar(cliente.getNombre().equals("Ana López"), "Nombre normalizado");
        comprobar(cliente.getTelefono().equals("0123456789"), "Teléfono conserva ceros");
        for (String invalido : Arrays.asList(null, "", " \t ")) {
            rechaza(() -> new Cliente(invalido, "555"), "Nombre obligatorio");
            rechaza(() -> new Cliente("Ana", invalido), "Teléfono obligatorio");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Taquilla taquilla = crear("\n  \n Ana López \n\n \t \n 0123456789 \n", buffer);
        Optional<Cliente> capturado = taquilla.capturarCliente();
        comprobar(capturado.isPresent(), "Permite corregir campos vacíos");
        comprobar(capturado.orElseThrow().getNombre().equals("Ana López"), "Captura nombre");
        comprobar(capturado.orElseThrow().getTelefono().equals("0123456789"), "Captura teléfono");
        comprobar(texto(buffer).contains("El campo Nombre es obligatorio"), "Aviso de nombre vacío");
        comprobar(texto(buffer).contains("El campo Teléfono es obligatorio"), "Aviso de teléfono vacío");

        for (String entrada : List.of("", "Ana\n", "\n\n", "Ana\n\n")) {
            buffer.reset();
            comprobar(crear(entrada, buffer).capturarCliente().isEmpty(), "Fin de entrada sin cliente parcial");
            comprobar(texto(buffer).contains("Captura cancelada"), "Aviso de cancelación");
        }

        buffer.reset();
        taquilla.mostrarResumen(List.of(new Asiento("A1", new BigDecimal("70.00")),
                new Asiento("B3", new BigDecimal("85.50"))));
        comprobar(texto(buffer).contains("- A1") && texto(buffer).contains("- B3"), "Muestra asientos");
        comprobar(texto(buffer).contains("Monto total: $155.50 MXN"), "Total con precios distintos");
        buffer.reset();
        taquilla.mostrarResumen(List.of(new Asiento("C5", new BigDecimal("70.00"))));
        comprobar(texto(buffer).contains("Monto total: $70.00 MXN"), "Total individual");
        buffer.reset();
        taquilla.mostrarResumen(List.of());
        comprobar(texto(buffer).contains("No hay asientos seleccionados."), "Selección vacía");
        comprobar(texto(buffer).contains("Monto total: $0.00 MXN"), "Total vacío");
        buffer.reset();
        rechaza(() -> taquilla.mostrarResumen(null), "Selección nula");
        rechaza(() -> taquilla.mostrarResumen(Arrays.asList((Asiento) null)), "Asiento nulo");
        comprobar(buffer.size() == 0, "Sin resumen parcial si los datos son inválidos");
        System.out.println("OK: " + verificaciones + " verificaciones de taquilla.");
    }

    private static Taquilla crear(String entrada, ByteArrayOutputStream buffer) {
        return new Taquilla(new Scanner(entrada), new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    private static String texto(ByteArrayOutputStream buffer) {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
        verificaciones++;
    }

    private static void rechaza(Runnable accion, String caso) {
        try {
            accion.run();
        } catch (IllegalArgumentException e) {
            verificaciones++;
            return;
        }
        throw new AssertionError("No se rechazó: " + caso);
    }
}
