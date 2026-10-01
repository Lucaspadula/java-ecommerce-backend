package com.sistventas.backend.migration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Migración V63 (tips y banners VERTICAL -> bloques) contra el esquema REAL:
// Flyway ya aplicó V62/V63 al levantar el contexto, así que acá se re-ejecuta
// el SQL REAL de V63 (leído del classpath, sin copiarlo) sobre empresas
// propias creadas y borradas por el test. Como V63 es idempotente, correrla
// sobre todo el dev no altera los datos ya migrados. Patrón de
// ProductoFotoPoolMigrationTest.
@SpringBootTest
class BloquesMigracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> empresas = new ArrayList<>();
    private final List<Long> productos = new ArrayList<>();
    private final List<Long> categorias = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (Long e : empresas) {
            jdbc.update("DELETE FROM tienda_bloque_card WHERE bloque_id IN (SELECT id FROM tienda_bloque WHERE empresa_id = ?)", e);
            jdbc.update("DELETE FROM tienda_bloque WHERE empresa_id = ?", e);
            jdbc.update("DELETE FROM tienda_tip WHERE empresa_id = ?", e);
            jdbc.update("DELETE FROM tienda_banner_imagen WHERE empresa_id = ?", e);
        }
        for (Long p : productos) {
            jdbc.update("DELETE FROM producto WHERE id = ?", p);
        }
        for (Long c : categorias) {
            jdbc.update("DELETE FROM categoria WHERE id = ?", c);
        }
        for (Long e : empresas) {
            jdbc.update("DELETE FROM empresa WHERE id = ?", e);
        }
    }

    @Test
    void tablasDeBloquesExistenConLasColumnasEsperadas() {
        assertThat(columnas("tienda_bloque")).containsExactlyInAnyOrder(
                "id", "empresa_id", "titulo", "slot", "ancho", "orden", "activo", "origen_legacy");
        assertThat(columnas("tienda_bloque_card")).containsExactlyInAnyOrder(
                "id", "bloque_id", "imagen_url", "orientacion", "titulo", "texto", "orden", "accion",
                "accion_valor", "origen_legacy");
    }

    @Test
    void bannersVerticalesConPosicionGeneranUnBloqueActivoTituladoConCardsEnOrden() {
        Long empresa = crearEmpresa("ANTES_FOOTER");
        insertarBanner(empresa, "VERTICAL", "/uploads/b/1.jpg", 1, null);
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, null);

        migrar();

        Map<String, Object> bloque = jdbc.queryForMap(
                "SELECT titulo, slot, ancho, activo, orden FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'BANNER_VERTICAL'", empresa);
        assertThat(bloque).containsEntry("titulo", "Elegidos para vos").containsEntry("slot", "HOME_ANTES_FOOTER")
                .containsEntry("ancho", "COMPLETO");
        assertThat(bloque.get("activo")).isEqualTo(true);
        List<Map<String, Object>> cards = cardsDe(empresa, "BANNER_VERTICAL");
        assertThat(cards).hasSize(2);
        assertThat(cards.get(0)).containsEntry("imagen_url", "/uploads/b/0.jpg").containsEntry("orientacion", "VERTICAL")
                .containsEntry("accion", "NINGUNA");
        assertThat(cards.get(1)).containsEntry("imagen_url", "/uploads/b/1.jpg");
    }

    @Test
    void posicionNullMapeaADespuesDestacadosPeroInactivoPorqueHoyLaSeccionNoSeMuestra() {
        Long empresa = crearEmpresa(null);
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, null);

        migrar();

        Map<String, Object> bloque = jdbc.queryForMap(
                "SELECT slot, activo FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'BANNER_VERTICAL'", empresa);
        assertThat(bloque).containsEntry("slot", "HOME_DESPUES_DESTACADOS");
        assertThat(bloque.get("activo")).isEqualTo(false);
    }

    @Test
    void posicionNoReconocidaTambienMapeaADespuesDestacadosInactivo() {
        Long empresa = crearEmpresa("ARRIBA_DE_TODO");
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, null);

        migrar();

        Map<String, Object> bloque = jdbc.queryForMap(
                "SELECT slot, activo FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'BANNER_VERTICAL'", empresa);
        assertThat(bloque).containsEntry("slot", "HOME_DESPUES_DESTACADOS");
        assertThat(bloque.get("activo")).isEqualTo(false);
    }

    @Test
    void bannerConProductoGeneraCardConAccionProductoYSinProductoNinguna() {
        Long empresa = crearEmpresa("DESPUES_BANNER");
        Long producto = crearProducto(empresa);
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, producto);
        insertarBanner(empresa, "VERTICAL", "/uploads/b/1.jpg", 1, null);

        migrar();

        List<Map<String, Object>> cards = cardsDe(empresa, "BANNER_VERTICAL");
        assertThat(cards.get(0)).containsEntry("accion", "PRODUCTO").containsEntry("accion_valor", String.valueOf(producto));
        assertThat(cards.get(1)).containsEntry("accion", "NINGUNA");
        assertThat(cards.get(1).get("accion_valor")).isNull();
    }

    @Test
    void tipsGeneranUnBloqueCuidaTuMateEnAntesFooterConCardsModal() {
        Long empresa = crearEmpresa(null);
        insertarTip(empresa, "Tip A", "Contenido A", 0, "/uploads/tips/a.jpg");
        insertarTip(empresa, "Tip B", "Contenido B", 1, null);
        insertarTip(empresa, "Tip C", "Contenido C", 2, null);

        migrar();

        Map<String, Object> bloque = jdbc.queryForMap(
                "SELECT titulo, slot, ancho, activo FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'TIPS'", empresa);
        assertThat(bloque).containsEntry("titulo", "Cuidá tu mate").containsEntry("slot", "HOME_ANTES_FOOTER")
                .containsEntry("ancho", "COMPLETO");
        assertThat(bloque.get("activo")).isEqualTo(true);
        List<Map<String, Object>> cards = cardsDe(empresa, "TIPS");
        assertThat(cards).hasSize(3);
        assertThat(cards.get(0)).containsEntry("titulo", "Tip A").containsEntry("texto", "Contenido A")
                .containsEntry("imagen_url", "/uploads/tips/a.jpg").containsEntry("accion", "MODAL");
        assertThat(cards.get(1).get("imagen_url")).isNull();
    }

    @Test
    void elBloqueDeTipsTieneOrdenMayorQueElDeBanners() {
        Long empresa = crearEmpresa("ANTES_FOOTER");
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, null);
        insertarTip(empresa, "Tip", "Contenido", 0, null);

        migrar();

        Integer ordenBanners = jdbc.queryForObject(
                "SELECT orden FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'BANNER_VERTICAL'", Integer.class, empresa);
        Integer ordenTips = jdbc.queryForObject(
                "SELECT orden FROM tienda_bloque WHERE empresa_id = ? AND origen_legacy = 'TIPS'", Integer.class, empresa);
        assertThat(ordenTips).isGreaterThan(ordenBanners);
    }

    @Test
    void reejecutarLaMigracionNoDuplicaBloquesNiCards() {
        Long empresa = crearEmpresa("ANTES_FOOTER");
        insertarBanner(empresa, "VERTICAL", "/uploads/b/0.jpg", 0, null);
        insertarTip(empresa, "Tip", "Contenido", 0, null);

        migrar();
        int bloquesPrimera = contarBloques(empresa);
        int cardsPrimera = contarCards(empresa);
        migrar();
        migrar();

        assertThat(bloquesPrimera).isEqualTo(2);
        assertThat(cardsPrimera).isEqualTo(2);
        assertThat(contarBloques(empresa)).isEqualTo(bloquesPrimera);
        assertThat(contarCards(empresa)).isEqualTo(cardsPrimera);
    }

    @Test
    void empresaSinBannersVerticalesNiTipsNoRecibeBloques() {
        Long empresa = crearEmpresa("ANTES_FOOTER");

        migrar();

        assertThat(contarBloques(empresa)).isZero();
    }

    @Test
    void migrarNoTocaElEsquemaViejoNiElHeroNiLosProductos() {
        Long empresa = crearEmpresa("ANTES_FOOTER");
        Long producto = crearProducto(empresa);
        insertarBanner(empresa, "HERO", "/uploads/b/hero.jpg", 0, null);
        insertarBanner(empresa, "VERTICAL", "/uploads/b/v.jpg", 0, producto);
        insertarTip(empresa, "Tip", "Contenido", 0, null);

        migrar();

        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'HERO'", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'VERTICAL'", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM tienda_tip WHERE empresa_id = ?", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM producto WHERE empresa_id = ?", empresa)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT tienda_banner_vertical_posicion FROM empresa WHERE id = ?", String.class, empresa))
                .isEqualTo("ANTES_FOOTER");
        // El HERO no se convierte en card.
        assertThat(contarCards(empresa)).isEqualTo(2);
    }

    // --- helpers ---

    // Ejecuta el SQL REAL de V63 (sin comentarios, una sentencia por ';').
    private void migrar() {
        try {
            String sql = new String(new ClassPathResource("db/migration/V63__migrar_tips_y_verticales_a_bloques.sql")
                    .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder limpio = new StringBuilder();
            for (String linea : sql.split("\n")) {
                if (!linea.trim().startsWith("--")) {
                    limpio.append(linea).append('\n');
                }
            }
            for (String sentencia : limpio.toString().split(";")) {
                if (!sentencia.isBlank()) {
                    jdbc.update(sentencia);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private List<String> columnas(String tabla) {
        return jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class, tabla).stream().map(String::toLowerCase).toList();
    }

    private List<Map<String, Object>> cardsDe(Long empresa, String origen) {
        return jdbc.queryForList(
                "SELECT c.* FROM tienda_bloque_card c JOIN tienda_bloque b ON b.id = c.bloque_id "
                        + "WHERE b.empresa_id = ? AND b.origen_legacy = ? ORDER BY c.orden, c.id", empresa, origen);
    }

    private int contarBloques(Long empresa) {
        return count("SELECT COUNT(*) FROM tienda_bloque WHERE empresa_id = ?", empresa);
    }

    private int contarCards(Long empresa) {
        return count("SELECT COUNT(*) FROM tienda_bloque_card c JOIN tienda_bloque b ON b.id = c.bloque_id WHERE b.empresa_id = ?", empresa);
    }

    private int count(String sql, Long empresa) {
        Integer n = jdbc.queryForObject(sql, Integer.class, empresa);
        return n != null ? n : 0;
    }

    private Long crearEmpresa(String posicionVertical) {
        jdbc.update("INSERT INTO empresa (nombre, licencia_estado, tienda_banner_vertical_posicion) VALUES (?, 'ACTIVA', ?)",
                "Empresa Test Bloques " + System.nanoTime(), posicionVertical);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        empresas.add(id);
        return id;
    }

    private Long crearProducto(Long empresa) {
        jdbc.update("INSERT INTO categoria (empresa_id, nombre) VALUES (?, ?)", empresa, "Cat Test " + System.nanoTime());
        Long categoria = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        categorias.add(categoria);
        jdbc.update("INSERT INTO producto (empresa_id, nombre, categoria_id, precio_venta, activo, fecha_alta, stock) "
                + "VALUES (?, 'Producto Test', ?, ?, TRUE, NOW(), 0)", empresa, categoria, new BigDecimal("100.00"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        productos.add(id);
        return id;
    }

    private void insertarBanner(Long empresa, String tipo, String url, int orden, Long productoId) {
        jdbc.update("INSERT INTO tienda_banner_imagen (empresa_id, imagen_url, orden, tipo, producto_id) VALUES (?, ?, ?, ?, ?)",
                empresa, url, orden, tipo, productoId);
    }

    private void insertarTip(Long empresa, String titulo, String contenido, int orden, String foto) {
        jdbc.update("INSERT INTO tienda_tip (empresa_id, titulo, contenido, orden, foto_url) VALUES (?, ?, ?, ?, ?)",
                empresa, titulo, contenido, orden, foto);
    }
}
