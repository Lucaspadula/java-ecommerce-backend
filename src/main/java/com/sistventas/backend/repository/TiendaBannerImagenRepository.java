package com.sistventas.backend.repository;

import com.sistventas.backend.entity.TiendaBannerImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaBannerImagenRepository extends JpaRepository<TiendaBannerImagen, Long> {
    List<TiendaBannerImagen> findByEmpresaIdOrderByOrden(Long empresaId);

    // Para validar que una imagen pertenece a la empresa del usuario antes de
    // borrarla (nunca confiar en el id que manda el cliente sin este chequeo).
    Optional<TiendaBannerImagen> findByIdAndEmpresaId(Long id, Long empresaId);

    // Usado como el próximo `orden` al agregar una imagen nueva, y para
    // validar el límite de 6 imágenes por empresa antes de guardar.
    long countByEmpresaId(Long empresaId);

    // Separa hero de vertical para armar las 2 galerías del admin y las 2
    // listas del DTO público (ver PublicTiendaServiceImpl.obtenerEmpresa).
    List<TiendaBannerImagen> findByEmpresaIdAndTipoOrderByOrden(Long empresaId, String tipo);

    // Próximo `orden` dentro de un tipo puntual, y límite de cantidad por
    // tipo (6 para HERO, 12 para VERTICAL — ver
    // PerfilServiceImpl.MAX_BANNER_IMAGENES / MAX_BANNER_VERTICAL_IMAGENES).
    long countByEmpresaIdAndTipo(Long empresaId, String tipo);
}
