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

// V64 (retiro de tips y banners VERTICAL) y su reversión R64 contra el esquema
// REAL. Flyway ya aplicó V64 al levantar el contexto. V63 dejó de poder
// probarse acá: sus tablas de origen ya no existen (su verificación se hizo en
// el PR1, antes de V64). Los tests de ida y vuelta ejecutan el SQL REAL de R64
// (reconstruye el esquema viejo desde las cards con origen_legacy) y luego el
// SQL REAL de V64 (lo vuelve a retirar), y @AfterEach garantiza que el esquema
// quede en el estado posterior a V64.
@SpringBootTest
class V64RetiroTest {

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> empresas = new ArrayList<>();
    private final List<Long> productos = new ArrayList<>();
    private final List<Long> categorias = new ArrayList<>();

    @AfterEach
    void limpiar() {
        // Estado posterior a V64, pase lo que pase en el test.
        if (existeTabla("tienda_tip")) {
            jdbc.execute("DROP TABLE tienda_tip");
        }
        jdbc.update("DELETE FROM tienda_banner_imagen WHERE tipo = 'VERTICAL'");
        if (existeColumna("empresa", "tienda_banner_vertical_posicion")) {
            jdbc.execute("ALTER TABLE empresa DROP COLUMN tienda_banner_vertical_posicion");
        }
        for (Long e : empresas) {
            jdbc.update("DELETE FROM tienda_bloque_card WHERE bloque_id IN (SELECT id FROM tienda_bloque WHERE empresa_id = ?)", e);
            jdbc.update("DELETE FROM tienda_bloque WHERE empresa_id = ?", e);
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
    void elEsquemaViejoYaNoExisteYLosBloquesSiExisten() {
        assertThat(existeTabla("tienda_tip")).isFalse();
        assertThat(existeColumna("empresa", "tienda_banner_vertical_posicion")).isFalse();
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE tipo = 'VERTICAL'")).isZero();
        assertThat(columnas("tienda_bloque")).containsExactlyInAnyOrder(
                "id", "empresa_id", "titulo", "slot", "ancho", "orden", "activo", "origen_legacy");
        assertThat(columnas("tienda_bloque_card")).containsExactlyInAnyOrder(
                "id", "bloque_id", "imagen_url", "orientacion", "titulo", "texto", "orden", "accion",
                "accion_valor", "origen_legacy");
    }

    @Test
    void r64ReconstruyeTipsBannersVerticalesYPosicionDesdeLasCards() {
        Long empresa = crearEmpresa();
        Long producto = crearProducto(empresa);
        insertarHero(empresa);
        Long bloqueBanners = crearBloque(empresa, "BANNER_VERTICAL", "HOME_ANTES_FOOTER", true);
        crearCard(bloqueBanners, "/uploads/b/0.jpg", null, null, 0, "PRODUCTO", String.valueOf(producto), "BANNER:9001");
        crearCard(bloqueBanners, "/uploads/b/1.jpg", null, null, 1, "NINGUNA", null, "BANNER:9002");
        Long bloqueTips = crearBloque(empresa, "TIPS", "HOME_ANTES_FOOTER", true);
        crearCard(bloqueTips, "/uploads/tips/a.jpg", "Tip A", "Contenido A", 0, "MODAL", null, "TIP:9101");
        crearCard(bloqueTips, null, "Tip B", "Contenido B", 1, "MODAL", null, "TIP:9102");
        crearCard(bloqueTips, "/uploads/x.jpg", "Nueva", "Sin origen", 2, "MODAL", null, null);

        ejecutar("db/rollback/R64__restaurar_tips_y_verticales.sql");

        List<Map<String, Object>> tips = jdbc.queryForList(
                "SELECT * FROM tienda_tip WHERE empresa_id = ? ORDER BY orden", empresa);
        assertThat(tips).hasSize(2);
        assertThat(((Number) tips.get(0).get("id")).longValue()).isEqualTo(9101L);
        assertThat(tips.get(0)).containsEntry("titulo", "Tip A").containsEntry("contenido", "Contenido A")
                .containsEntry("foto_url", "/uploads/tips/a.jpg");
        assertThat(tips.get(1).get("foto_url")).isNull();

        List<Map<String, Object>> banners = jdbc.queryForList(
                "SELECT * FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'VERTICAL' ORDER BY orden", empresa);
        assertThat(banners).hasSize(2);
        assertThat(((Number) banners.get(0).get("id")).longValue()).isEqualTo(9001L);
        assertThat(banners.get(0)).containsEntry("imagen_url", "/uploads/b/0.jpg");
        assertThat(((Number) banners.get(0).get("producto_id")).longValue()).isEqualTo(producto);
        assertThat(banners.get(1).get("producto_id")).isNull();

        assertThat(jdbc.queryForObject("SELECT tienda_banner_vertical_posicion FROM empresa WHERE id = ?", String.class, empresa))
                .isEqualTo("ANTES_FOOTER");
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'HERO'", empresa)).isEqualTo(1);
    }

    @Test
    void r64ConBloqueDeBannersInactivoDejaLaPosicionEnNull() {
        Long empresa = crearEmpresa();
        Long bloque = crearBloque(empresa, "BANNER_VERTICAL", "HOME_DESPUES_DESTACADOS", false);
        crearCard(bloque, "/uploads/b/0.jpg", null, null, 0, "NINGUNA", null, "BANNER:9003");

        ejecutar("db/rollback/R64__restaurar_tips_y_verticales.sql");

        assertThat(jdbc.queryForObject("SELECT tienda_banner_vertical_posicion FROM empresa WHERE id = ?", String.class, empresa))
                .isNull();
    }

    @Test
    void v64DespuesDeR64RetiraTodoSinTocarBloquesHeroNiCatalogo() {
        Long empresa = crearEmpresa();
        Long producto = crearProducto(empresa);
        insertarHero(empresa);
        Long bloque = crearBloque(empresa, "BANNER_VERTICAL", "HOME_ANTES_FOOTER", true);
        crearCard(bloque, "/uploads/b/0.jpg", null, null, 0, "PRODUCTO", String.valueOf(producto), "BANNER:9004");
        int productosAntes = count("SELECT COUNT(*) FROM producto");
        int categoriasAntes = count("SELECT COUNT(*) FROM categoria");
        int subcategoriasAntes = count("SELECT COUNT(*) FROM subcategoria");

        ejecutar("db/rollback/R64__restaurar_tips_y_verticales.sql");
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'VERTICAL'", empresa)).isEqualTo(1);
        ejecutar("db/migration/V64__retirar_tips_y_verticales.sql");

        assertThat(existeTabla("tienda_tip")).isFalse();
        assertThat(existeColumna("empresa", "tienda_banner_vertical_posicion")).isFalse();
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE tipo = 'VERTICAL'")).isZero();
        // R8: HERO, bloques/cards y catálogo intactos.
        assertThat(count("SELECT COUNT(*) FROM tienda_banner_imagen WHERE empresa_id = ? AND tipo = 'HERO'", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM tienda_bloque WHERE empresa_id = ?", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM tienda_bloque_card c JOIN tienda_bloque b ON b.id = c.bloque_id WHERE b.empresa_id = ?", empresa)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM producto")).isEqualTo(productosAntes);
        assertThat(count("SELECT COUNT(*) FROM categoria")).isEqualTo(categoriasAntes);
        assertThat(count("SELECT COUNT(*) FROM subcategoria")).isEqualTo(subcategoriasAntes);
    }

    // --- helpers ---

    // Ejecuta el SQL REAL del recurso (sin comentarios, una sentencia por ';').
    private void ejecutar(String recurso) {
        try {
            String sql = new String(new ClassPathResource(recurso).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder limpio = new StringBuilder();
            for (String linea : sql.split("\n")) {
                if (!linea.trim().startsWith("--")) {
                    limpio.append(linea).append('\n');
                }
            }
            for (String sentencia : limpio.toString().split(";")) {
                if (!sentencia.isBlank()) {
                    jdbc.execute(sentencia);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private boolean existeTabla(String tabla) {
        return count("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tabla + "'") > 0;
    }

    private boolean existeColumna(String tabla, String columna) {
        return columnas(tabla).contains(columna);
    }

    private List<String> columnas(String tabla) {
        return jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class, tabla).stream().map(String::toLowerCase).toList();
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n != null ? n : 0;
    }

    private Long crearEmpresa() {
        jdbc.update("INSERT INTO empresa (nombre, licencia_estado) VALUES (?, 'ACTIVA')", "Empresa Test V64 " + System.nanoTime());
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

    private void insertarHero(Long empresa) {
        jdbc.update("INSERT INTO tienda_banner_imagen (empresa_id, imagen_url, orden, tipo) VALUES (?, '/uploads/b/hero.jpg', 0, 'HERO')", empresa);
    }

    private Long crearBloque(Long empresa, String origen, String slot, boolean activo) {
        jdbc.update("INSERT INTO tienda_bloque (empresa_id, titulo, slot, ancho, orden, activo, origen_legacy) VALUES (?, 'T', ?, 'COMPLETO', 0, ?, ?)",
                empresa, slot, activo, origen);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void crearCard(Long bloque, String imagen, String titulo, String texto, int orden, String accion, String valor, String origen) {
        jdbc.update("INSERT INTO tienda_bloque_card (bloque_id, imagen_url, orientacion, titulo, texto, orden, accion, accion_valor, origen_legacy) "
                + "VALUES (?, ?, 'VERTICAL', ?, ?, ?, ?, ?, ?)", bloque, imagen, titulo, texto, orden, accion, valor, origen);
    }
}
