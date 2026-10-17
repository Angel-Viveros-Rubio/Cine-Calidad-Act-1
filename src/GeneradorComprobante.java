import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public final class GeneradorComprobante {
    public Path generar(Reservacion reservacion, Path directorio) throws IOException {
        synchronized (GeneradorComprobante.class) {
            return generarSeguro(reservacion, directorio);
        }
    }

    private Path generarSeguro(Reservacion reservacion, Path directorio) throws IOException {
        if (reservacion == null || reservacion.getEstado() != Reservacion.Estado.CONFIRMADA) {
            throw new IllegalArgumentException("Solo se generan comprobantes de pagos confirmados.");
        }
        Path destino = directorio.toAbsolutePath().normalize();
        verificarFuentes(destino);
        Files.createDirectories(destino);
        destino = destino.toRealPath();
        verificarFuentes(destino);
        Path archivo = destino.resolve("comprobante-" + reservacion.getId() + ".txt");
        String contenido = contenido(reservacion);
        if (Files.exists(archivo, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            comprobarExistente(archivo, contenido);
            return archivo;
        }
        Path temporal = Files.createTempFile(destino, "comprobante-", ".tmp");
        try {
            Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
            // Renombrar dentro del mismo directorio publica el archivo ya escrito.
            // Sin REPLACE_EXISTING: nunca reemplaza un comprobante existente.
            try {
                Files.move(temporal, archivo);
            } catch (java.nio.file.FileAlreadyExistsException e) {
                comprobarExistente(archivo, contenido);
            }
            return archivo;
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void comprobarExistente(Path archivo, String esperado) throws IOException {
        if (Files.isSymbolicLink(archivo) || !Files.isRegularFile(archivo)
                || !Files.readString(archivo, StandardCharsets.UTF_8).equals(esperado)) {
            throw new IOException("El folio ya existe con un contenido diferente; no se sobrescribió.");
        }
    }

    private void verificarFuentes(Path directorio) throws IOException {
        Path raiz = Path.of("").toAbsolutePath().normalize();
        for (String carpeta : new String[] { "src", "tests" }) {
            Path fuente = raiz.resolve(carpeta);
            if (Files.exists(fuente)) { fuente = fuente.toRealPath(); }
            if (directorio.startsWith(fuente)) {
                throw new IOException("Los comprobantes deben guardarse fuera del código fuente.");
            }
        }
    }

    private String contenido(Reservacion r) {
        String asientos = String.join(", ", r.getAsientos().stream().map(Asiento::getIdentificador).toList());
        return "COMPROBANTE DE RESERVACIÓN\n"
                + "Folio: " + r.getId() + "\n"
                + "Cliente: " + unaLinea(r.getCliente().getNombre()) + "\n"
                + "Teléfono: " + unaLinea(r.getCliente().getTelefono()) + "\n"
                + "Asientos: " + asientos + "\n"
                + "Importe total: $" + r.getTotal().toPlainString() + " MXN\n"
                + "Método de pago: " + r.getMetodoPago() + "\n"
                + "Fecha de confirmación: " + r.getFechaConfirmacion()
                        .format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + "\n";
    }

    private String unaLinea(String valor) { return valor.replaceAll("\\R", " "); }
}
