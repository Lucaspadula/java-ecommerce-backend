package com.sistventas.backend.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Construye JPEGs sintéticos mínimos (SOI + APP1/Exif con el tag
// Orientation + nada más) para verificar el parseo binario de
// ExifOrientationReader sin depender de un archivo de imagen real — es la
// única forma de probar esto en CI, ya que no hay forma de "ver" el efecto
// visual de la rotación en un PDF generado.
class ExifOrientationReaderTest {

    @TempDir
    Path tempDir;

    private Path escribirJpegConOrientacion(int orientacion) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xD8); // SOI

        // --- Cuerpo del APP1 (TIFF little-endian con un solo tag: Orientation) ---
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write("Exif\0\0".getBytes());
        tiff.write('I');
        tiff.write('I'); // byte order: little-endian
        tiff.write(0x2A);
        tiff.write(0x00); // 42
        tiff.write(0x08);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00); // offset de IFD0 = 8
        tiff.write(0x01);
        tiff.write(0x00); // 1 entrada
        // Entrada: tag=0x0112, type=3 (SHORT), count=1, value=orientacion
        tiff.write(0x12);
        tiff.write(0x01);
        tiff.write(0x03);
        tiff.write(0x00);
        tiff.write(0x01);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(orientacion & 0xFF);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00);
        tiff.write(0x00); // siguiente IFD = 0

        byte[] cuerpoApp1 = tiff.toByteArray();
        int longitud = cuerpoApp1.length + 2; // la longitud incluye sus propios 2 bytes

        out.write(0xFF);
        out.write(0xE1); // APP1
        out.write((longitud >> 8) & 0xFF);
        out.write(longitud & 0xFF);
        out.write(cuerpoApp1);

        Path archivo = tempDir.resolve("foto-" + orientacion + ".jpg");
        Files.write(archivo, out.toByteArray());
        return archivo;
    }

    @Test
    void jpegSinOrientacionEspecialDevuelve1() throws IOException {
        assertThat(ExifOrientationReader.leer(escribirJpegConOrientacion(1))).isEqualTo(1);
    }

    @Test
    void jpegRotado180Devuelve3() throws IOException {
        assertThat(ExifOrientationReader.leer(escribirJpegConOrientacion(3))).isEqualTo(3);
    }

    @Test
    void jpegRotado90HorarioDevuelve6() throws IOException {
        assertThat(ExifOrientationReader.leer(escribirJpegConOrientacion(6))).isEqualTo(6);
    }

    @Test
    void jpegRotado270HorarioDevuelve8() throws IOException {
        assertThat(ExifOrientationReader.leer(escribirJpegConOrientacion(8))).isEqualTo(8);
    }

    @Test
    void archivoQueNoEsJpegDevuelve1(@TempDir Path dir) throws IOException {
        Path archivo = dir.resolve("no-es-jpeg.txt");
        Files.writeString(archivo, "esto no es un jpeg");

        assertThat(ExifOrientationReader.leer(archivo)).isEqualTo(1);
    }

    @Test
    void archivoInexistenteDevuelve1() {
        assertThat(ExifOrientationReader.leer(tempDir.resolve("no-existe.jpg"))).isEqualTo(1);
    }
}
