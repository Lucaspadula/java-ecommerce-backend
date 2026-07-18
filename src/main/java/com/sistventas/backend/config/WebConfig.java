package com.sistventas.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve los archivos subidos (fotos de producto) como recursos estáticos
 * desde el disco local. La carpeta es relativa al working dir del backend —
 * ver ProductoServiceImpl para dónde se escriben los archivos.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/productos/**")
                .addResourceLocations("file:uploads/productos/");
        registry.addResourceHandler("/uploads/ventas/**")
                .addResourceLocations("file:uploads/ventas/");
        registry.addResourceHandler("/uploads/empresas/**")
                .addResourceLocations("file:uploads/empresas/");
        registry.addResourceHandler("/uploads/categorias/**")
                .addResourceLocations("file:uploads/categorias/");
        // SecurityConfig ya permitAll'eaba esta ruta desde la feature de foto
        // de reseña (WhatsApp), pero faltaba el ResourceHandler acá — sin
        // esto el archivo se sube bien pero el navegador nunca puede
        // servirlo (404), la imagen queda rota en la tienda pública.
        registry.addResourceHandler("/uploads/resenas/**")
                .addResourceLocations("file:uploads/resenas/");
        registry.addResourceHandler("/uploads/testimonios/**")
                .addResourceLocations("file:uploads/testimonios/");
        registry.addResourceHandler("/uploads/tips/**")
                .addResourceLocations("file:uploads/tips/");
    }
}
