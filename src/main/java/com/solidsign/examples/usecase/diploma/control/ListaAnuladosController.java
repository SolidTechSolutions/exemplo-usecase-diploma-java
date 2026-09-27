package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * [EN]    Document 5/5 — Lista de Diplomas Anulados / Arquivo de Fiscalização. Both are signed
 *         over the ENTIRE document (no signatureNodeName) with the institution's e-CNPJ
 *         certificate in a single step — same endpoint covers either file.
 * [PT-BR] Documento 5/5 — Lista de Diplomas Anulados / Arquivo de Fiscalização. Ambos são
 *         assinados sobre o DOCUMENTO INTEIRO (sem signatureNodeName) com o certificado e-CNPJ
 *         da instituição em uma única etapa — o mesmo endpoint cobre qualquer um dos dois.
 */
@RestController
@RequestMapping("/api/diploma/lista-anulados")
public class ListaAnuladosController {

    @Autowired
    private XmlKmsSigningService signingService;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;
    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;
    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    @Value("${solidsign.listaAnulados.profile}")
    private String profile;
    @Value("${solidsign.listaAnulados.removeXPathFilter}")
    private boolean removeXPathFilter;

    @CrossOrigin
    @PostMapping("/sign")
    public ResponseEntity<byte[]> sign(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                null, null, profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, removeXPathFilter, "lista_anulados_signed.xml");
    }
}
