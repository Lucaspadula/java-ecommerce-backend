package com.sistventas.backend.service.impl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

// Lector mínimo del tag EXIF Orientation (0x0112) de un JPEG, sin depender
// de ninguna librería externa — ver CatalogoServiceImpl.aplicarRotacionExif
// para el porqué (OpenPDF embebe los píxeles crudos del archivo, ignorando
// este tag; una foto vertical tomada con celular llega "acostada" al PDF
// aunque se vea derecha en cualquier otro visor). Devuelve 1 (orientación
// normal) si el archivo no es JPEG, no tiene bloque EXIF, o cualquier otra
// cosa sale mal — mismo criterio best-effort que el resto de la carga de
// imágenes del catálogo.
final class ExifOrientationReader {

    private static final int MARCADOR_SOI = 0xFFD8;
    private static final int MARCADOR_APP1 = 0xFFE1;
    private static final int MARCADOR_SOS = 0xFFDA;
    private static final int TAG_ORIENTATION = 0x0112;

    private ExifOrientationReader() {
    }

    static int leer(Path archivo) {
        try (RandomAccessFile raf = new RandomAccessFile(archivo.toFile(), "r")) {
            if (raf.readUnsignedShort() != MARCADOR_SOI) {
                return 1;
            }
            while (true) {
                int marcador = raf.readUnsignedShort();
                if ((marcador & 0xFF00) != 0xFF00) {
                    return 1;
                }
                if (marcador == MARCADOR_SOS) {
                    // Start of Scan: ya empiezan los datos de píxeles, no hay
                    // más metadata antes.
                    return 1;
                }
                int longitud = raf.readUnsignedShort();
                if (marcador == MARCADOR_APP1) {
                    return leerApp1(raf, longitud);
                }
                raf.skipBytes(longitud - 2);
            }
        } catch (IOException | RuntimeException ex) {
            return 1;
        }
    }

    private static int leerApp1(RandomAccessFile raf, int longitud) throws IOException {
        byte[] datos = new byte[longitud - 2];
        raf.readFully(datos);
        if (datos.length < 10 || datos[0] != 'E' || datos[1] != 'x' || datos[2] != 'i' || datos[3] != 'f') {
            return 1;
        }
        int base = 6; // salta "Exif\0\0"
        boolean bigEndian = datos[base] == 'M';
        int ifdOffset = leerInt32(datos, base + 4, bigEndian);
        int entradas = leerInt16(datos, base + ifdOffset, bigEndian);
        for (int i = 0; i < entradas; i++) {
            int entradaOffset = base + ifdOffset + 2 + i * 12;
            if (entradaOffset + 10 > datos.length) {
                break;
            }
            int tag = leerInt16(datos, entradaOffset, bigEndian);
            if (tag == TAG_ORIENTATION) {
                return leerInt16(datos, entradaOffset + 8, bigEndian);
            }
        }
        return 1;
    }

    private static int leerInt16(byte[] datos, int offset, boolean bigEndian) {
        int b0 = datos[offset] & 0xFF;
        int b1 = datos[offset + 1] & 0xFF;
        return bigEndian ? (b0 << 8) | b1 : (b1 << 8) | b0;
    }

    private static int leerInt32(byte[] datos, int offset, boolean bigEndian) {
        int b0 = datos[offset] & 0xFF;
        int b1 = datos[offset + 1] & 0xFF;
        int b2 = datos[offset + 2] & 0xFF;
        int b3 = datos[offset + 3] & 0xFF;
        return bigEndian
                ? (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
                : (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }
}
