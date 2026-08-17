package com.sistventas.backend.service.impl;

import com.sistventas.backend.entity.Empresa;
import com.sistventas.backend.exception.IaGeneracionFallidaException;
import com.sistventas.backend.exception.IaNoConfiguradaException;
import com.sistventas.backend.repository.EmpresaRepository;
import com.sistventas.backend.security.UserPrincipal;
import com.sistventas.backend.service.DescripcionIaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Genera la descripción sugerida de un producto mandando su foto principal a
 * la API de Gemini (Google AI Studio). Mismo patrón que
 * AuthServiceImpl.verificarTokenGoogle: RestClient.Builder inyectado (no
 * RestClient.create()) para poder testear con MockRestServiceServer.
 */
@Service
public class DescripcionIaServiceImpl implements DescripcionIaService {

    private final RestClient restClient;
    private final EmpresaRepository empresaRepository;
    private final String apiKeyGlobal;
    private final String model;

    public DescripcionIaServiceImpl(
            RestClient.Builder restClientBuilder,
            EmpresaRepository empresaRepository,
            @Value("${sistventas.gemini.api-key}") String apiKeyGlobal,
            @Value("${sistventas.gemini.model}") String model) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.empresaRepository = empresaRepository;
        this.apiKeyGlobal = apiKeyGlobal;
        this.model = model;
    }

    @Override
    public String generar(MultipartFile foto, String nombreProducto, UserPrincipal principal) {
        String apiKey = resolverApiKey(principal);
        // Sin key (ni propia de la empresa ni global): falla clarito acá en
        // vez de mandarle una key vacía a Google (que devolvería un 400
        // críptico) — ver GEMINI_API_KEY en SECRETS.md / setup-env.bat, o
        // Configuración > Integraciones para la key propia por empresa.
        if (apiKey == null || apiKey.isBlank()) {
            throw new IaNoConfiguradaException();
        }

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(foto.getBytes());
        } catch (IOException ex) {
            throw new IaGeneracionFallidaException();
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", construirPrompt(nombreProducto)),
                                Map.of("inline_data", Map.of(
                                        "mime_type", foto.getContentType(),
                                        "data", base64))
                        )
                ))
        );

        Map<?, ?> respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            throw new IaGeneracionFallidaException();
        }

        String texto = extraerTexto(respuesta);
        if (texto == null || texto.isBlank()) {
            throw new IaGeneracionFallidaException();
        }
        return texto.trim();
    }

    // Key propia de la empresa (cargada en Configuración) si existe, si no
    // la global de sistventas.gemini.api-key. Super Admin (sin empresa) o
    // un principal null caen directo al fallback global.
    private String resolverApiKey(UserPrincipal principal) {
        if (principal != null && principal.empresaId() != null) {
            String propia = empresaRepository.findById(principal.empresaId())
                    .map(Empresa::getGeminiApiKey)
                    .orElse(null);
            if (propia != null && !propia.isBlank()) {
                return propia;
            }
        }
        return apiKeyGlobal;
    }

    // Estructura fija pedida por el usuario (2 párrafos + "Características"
    // con viñetas de emoji), calcada del estilo real que ya usan a mano en
    // el catálogo — ver ejemplo del Mate Ranchero "Vaquita" en la
    // conversación que originó este prompt. La sección de materiales por
    // categoría es la parte que más impacto tiene: sin esa guía, el modelo
    // tiende a quedarse en genérico ("materiales de calidad") en vez de
    // nombrar lo que realmente se ve (algarrobo, calabaza repujada, virola
    // de alpaca, etc.).
    private static final String INSTRUCCIONES_ESTRUCTURA = """
            Escribí la descripción de un producto artesanal para una tienda online argentina, \
            en español rioplatense, con un tono cálido y directo, sin exagerar con adjetivos ni \
            inventar nada que no se vea en la foto. Usá EXACTAMENTE esta estructura, sin agregar \
            títulos ni secciones que no estén acá:

            1. Un primer párrafo CORTO (1 a 2 oraciones, sin relleno) que enganche: estilo, \
            tradición o el contexto de uso del producto.
            2. Un segundo párrafo CORTO (1 oración) sobre materiales, calidad y uso diario.
            3. Un encabezado literal "Características" en su propia línea (sin numerar, sin \
            markdown como ## o **).
            4. Entre 5 y 8 viñetas, cada una en su propia línea, que empiecen con UN emoji \
            relevante seguido de la característica en pocas palabras. Cubrí: modelo o nombre, \
            diseño/estilo visual, terminaciones, agarre/resistencia/uso diario, si sirve para \
            regalo o uso personal, y origen o tradición argentina.

            Prestá atención especial a los MATERIALES que se ven en la foto y nombralos en las \
            viñetas cuando puedas identificarlos con confianza — nunca los inventes si no se nota:
            - Mates: si el cuerpo es de madera (algarrobo u otra), calabaza/porongo, si la \
            calabaza está repujada en cuero, cincelada (grabado a mano) o lisa, y si la virola/boca \
            es de alpaca, acero u otro metal.
            - Bombillas: material del cuerpo (alpaca, acero inoxidable, caña) y si el pico es de \
            otro material.
            - Termos: material del cuerpo (acero inoxidable, aluminio), tipo de tapa/pico, si \
            tiene mango o funda.
            - Sets materos: qué piezas incluye (mate, bombilla, termo, bolso) y de qué material \
            es cada una.

            No inventes certificaciones, medidas exactas ni materiales que no se puedan ver en la foto.""";

    private String construirPrompt(String nombreProducto) {
        if (nombreProducto != null && !nombreProducto.isBlank()) {
            return INSTRUCCIONES_ESTRUCTURA + "\n\nEl producto se llama \"" + nombreProducto + "\".";
        }
        return INSTRUCCIONES_ESTRUCTURA;
    }

    /** Camino feliz: candidates[0].content.parts[0].text. Cualquier forma inesperada -> null (no 500 crudo). */
    private String extraerTexto(Map<?, ?> respuesta) {
        try {
            List<?> candidates = (List<?>) respuesta.get("candidates");
            Map<?, ?> primerCandidato = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) primerCandidato.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> primeraParte = (Map<?, ?>) parts.get(0);
            return (String) primeraParte.get("text");
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
