package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * [EN]    Shared multipart-in/XML-out plumbing used by every document controller: writes the
 *         uploaded file to a temp path, calls the signing service, and wraps the signed bytes
 *         (or a 500) into a ResponseEntity.
 * [PT-BR] Encanamento compartilhado de multipart-in/XML-out usado por todos os controllers de
 *         documento: grava o arquivo enviado num caminho temporário, chama o service de
 *         assinatura, e empacota os bytes assinados (ou um 500) num ResponseEntity.
 */
final class SigningEndpointSupport {

    private SigningEndpointSupport() {}

    static ResponseEntity<byte[]> signAndReturn(XmlKmsSigningService signingService,
            MultipartFile document, String kmsCode, String nodeName, String namespace, String profile,
            String hashAlgorithm, String signaturePackaging, String canonicalizationMethod,
            boolean removeXPathFilter, String outputName) throws IOException {
        Path tmp = Files.createTempFile("diploma-usecase-", ".xml");
        try {
            document.transferTo(tmp);
            byte[] signed = signingService.sign(tmp.toFile(), kmsCode, nodeName, namespace, profile,
                    hashAlgorithm, signaturePackaging, canonicalizationMethod, removeXPathFilter);
            if (signed == null) return ResponseEntity.internalServerError().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .header("Content-Disposition", "attachment; filename=\"" + outputName + "\"")
                    .body(signed);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    static File toTempFile(MultipartFile file, String prefix) throws IOException {
        Path tmp = Files.createTempFile(prefix, ".xml");
        file.transferTo(tmp);
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }
}
