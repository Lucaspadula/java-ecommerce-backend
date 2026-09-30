package com.sistventas.backend.migration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Test de integración sobre el esquema REAL (Flyway ya corrió V43 contra la
// base de desarrollo al levantar el contexto): verifica el comportamiento de
// backfill de V43__producto_foto_pool.sql ejecutando el MISMO bloque de SQL
// que la migración (no se puede volver a correr la migración vía Flyway —
// ya quedó registrada en el historial), sobre datos propios creados y
// borrados en este test. No usa @Transactional con rollback automático a
// propósito: se limpia a mano en @AfterEach porque las FK con AUTO_INCREMENT
// necesitan verse confirmadas para el assert de "columnas viejas intactas".
@SpringBootTest
class ProductoFotoPoolMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Long empresaId;
    private Long categoriaId;
    private Long productoId;
    private Long varianteId;

    @AfterEach
    void limpiar() {
        if (productoId != null) {
            jdbc.update("DELETE FROM producto_foto WHERE producto_id = ?", productoId);
            jdbc.update("DELETE FROM producto_variante WHERE producto_id = ?", productoId);
            jdbc.update("DELETE FROM producto WHERE id = ?", productoId);
        }
        if (categoriaId != null) {
            jdbc.update("DELETE FROM categoria WHERE id = ?", categoriaId);
        }
        if (empresaId != null) {
            jdbc.update("DELETE FROM empresa WHERE id = ?", empresaId);
        }
    }

    @Test
    void tablaProductoFotoExisteConLasColumnasEsperadas() {
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'producto_foto'");
        List<String> nombres = columnas.stream().map(c -> String.valueOf(c.get("COLUMN_NAME")).toLowerCase()).toList();
        assertThat(nombres).containsExactlyInAnyOrder("id", "producto_id", "variante_id", "url", "orden", "agrandada");
    }

    @Test
    void backfillCompletoDesdeLas4ColumnasDeProductoYLaDeVarianteRespetaColorYOrden() {
        crearProductoConFotosYVariante();

        ejecutarBackfill();

        List<Map<String, Object>> fotos = jdbc.queryForList(
                "SELECT variante_id, url, orden FROM producto_foto WHERE producto_id = ? ORDER BY orden", productoId);

        assertThat(fotos).hasSize(5);
        assertThat(fotos.get(0)).containsEntry("url", "/uploads/productos/f1.jpg").containsEntry("orden", 0);
        assertThat(fotos.get(0).get("variante_id")).isNull();
        assertThat(fotos.get(1)).containsEntry("url", "/uploads/productos/f2.jpg").containsEntry("orden", 1);
        assertThat(fotos.get(2)).containsEntry("url", "/uploads/productos/f3.jpg").containsEntry("orden", 2);
        assertThat(fotos.get(3)).containsEntry("url", "/uploads/productos/f4.jpg").containsEntry("orden", 3);
        // La foto de la variante entra con SU variante_id, orden 4 (después de las 4 fijas).
        assertThat(fotos.get(4)).containsEntry("url", "/uploads/productos/variante.jpg").containsEntry("orden", 4);
        assertThat(fotos.get(4).get("variante_id")).isEqualTo(varianteId);
    }

    @Test
    void backfillEsIdempotenteAlReejecutarseNoDuplicaFilas() {
        crearProductoConFotosYVariante();

        ejecutarBackfill();
        int totalTrasPrimeraCorrida = contarFotos();

        ejecutarBackfill();
        int totalTrasSegundaCorrida = contarFotos();

        assertThat(totalTrasSegundaCorrida).isEqualTo(totalTrasPrimeraCorrida).isEqualTo(5);
    }

    @Test
    void backfillNoTocaLasColumnasViejasDeProductoNiDeVariante() {
        crearProductoConFotosYVariante();

        ejecutarBackfill();

        Map<String, Object> producto = jdbc.queryForMap(
                "SELECT foto_url, foto_url_2, foto_url_3, foto_url_4 FROM producto WHERE id = ?", productoId);
        assertThat(producto.get("foto_url")).isEqualTo("/uploads/productos/f1.jpg");
        assertThat(producto.get("foto_url_2")).isEqualTo("/uploads/productos/f2.jpg");
        assertThat(producto.get("foto_url_3")).isEqualTo("/uploads/productos/f3.jpg");
        assertThat(producto.get("foto_url_4")).isEqualTo("/uploads/productos/f4.jpg");

        String fotoVariante = jdbc.queryForObject(
                "SELECT foto_url FROM producto_variante WHERE id = ?", String.class, varianteId);
        assertThat(fotoVariante).isEqualTo("/uploads/productos/variante.jpg");
    }

    @Test
    void productoSinNingunaFotoNoGeneraFilasEnElPool() {
        empresaId = insertarEmpresa();
        categoriaId = insertarCategoria(empresaId);
        productoId = insertarProducto(empresaId, categoriaId, null, null, null, null);

        ejecutarBackfill();

        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto_foto WHERE producto_id = ?", Integer.class, productoId);
        assertThat(total).isZero();
    }

    private void crearProductoConFotosYVariante() {
        empresaId = insertarEmpresa();
        categoriaId = insertarCategoria(empresaId);
        productoId = insertarProducto(empresaId, categoriaId,
                "/uploads/productos/f1.jpg", "/uploads/productos/f2.jpg",
                "/uploads/productos/f3.jpg", "/uploads/productos/f4.jpg");
        varianteId = insertarVariante(productoId, "/uploads/productos/variante.jpg");
    }

    private int contarFotos() {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto_foto WHERE producto_id = ?", Integer.class, productoId);
        return total != null ? total : 0;
    }

    // Mismo bloque de SQL que V43__producto_foto_pool.sql (con las mismas
    // guardas NOT EXISTS de idempotencia): se ejecuta acá directo porque
    // Flyway ya corrió la migración real una única vez al levantar el
    // contexto y no la vuelve a ejecutar por historial de checksums.
    private void ejecutarBackfill() {
        jdbc.update("INSERT INTO producto_foto (producto_id, variante_id, url, orden) "
                + "SELECT p.id, NULL, p.foto_url, 0 FROM producto p "
                + "WHERE p.foto_url IS NOT NULL AND p.foto_url <> '' "
                + "AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 0 AND f.variante_id IS NULL)");

        jdbc.update("INSERT INTO producto_foto (producto_id, variante_id, url, orden) "
                + "SELECT p.id, NULL, p.foto_url_2, 1 FROM producto p "
                + "WHERE p.foto_url_2 IS NOT NULL AND p.foto_url_2 <> '' "
                + "AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 1 AND f.variante_id IS NULL)");

        jdbc.update("INSERT INTO producto_foto (producto_id, variante_id, url, orden) "
                + "SELECT p.id, NULL, p.foto_url_3, 2 FROM producto p "
                + "WHERE p.foto_url_3 IS NOT NULL AND p.foto_url_3 <> '' "
                + "AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 2 AND f.variante_id IS NULL)");

        jdbc.update("INSERT INTO producto_foto (producto_id, variante_id, url, orden) "
                + "SELECT p.id, NULL, p.foto_url_4, 3 FROM producto p "
                + "WHERE p.foto_url_4 IS NOT NULL AND p.foto_url_4 <> '' "
                + "AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.producto_id = p.id AND f.orden = 3 AND f.variante_id IS NULL)");

        jdbc.update("INSERT INTO producto_foto (producto_id, variante_id, url, orden) "
                + "SELECT v.producto_id, v.id, v.foto_url, 3 + ROW_NUMBER() OVER (PARTITION BY v.producto_id ORDER BY v.id) "
                + "FROM producto_variante v "
                + "WHERE v.foto_url IS NOT NULL AND v.foto_url <> '' "
                + "AND NOT EXISTS (SELECT 1 FROM producto_foto f WHERE f.variante_id = v.id)");
    }

    private Long insertarEmpresa() {
        jdbc.update("INSERT INTO empresa (nombre, licencia_estado) VALUES (?, 'ACTIVA')",
                "Empresa Test Fotos " + System.nanoTime());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertarCategoria(Long empresaId) {
        jdbc.update("INSERT INTO categoria (empresa_id, nombre) VALUES (?, ?)",
                empresaId, "Categoria Test " + System.nanoTime());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertarProducto(Long empresaId, Long categoriaId, String f1, String f2, String f3, String f4) {
        jdbc.update("INSERT INTO producto (empresa_id, nombre, categoria_id, precio_venta, foto_url, foto_url_2, "
                        + "foto_url_3, foto_url_4, activo, fecha_alta, stock) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, NOW(), 0)",
                empresaId, "Producto Test", categoriaId, new BigDecimal("100.00"), f1, f2, f3, f4);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertarVariante(Long productoId, String fotoUrl) {
        jdbc.update("INSERT INTO producto_variante (producto_id, color, stock, foto_url) VALUES (?, 'Rojo', 5, ?)",
                productoId, fotoUrl);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
