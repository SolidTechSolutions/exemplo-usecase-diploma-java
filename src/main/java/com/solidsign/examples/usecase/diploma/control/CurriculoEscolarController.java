package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * [EN]    Document 4/5 — Currículo Escolar Digital (layout v1.05). Signed separately from the
 *         other documents, over the ENTIRE document (no signatureNodeName), in 2 steps:
 *         Coordenador do Curso (e-CPF) then IES Emissora (e-CNPJ, XPath filter removed).
 * [PT-BR] Documento 4/5 — Currículo Escolar Digital (leiaute v1.05). Assinado separado dos
 *         demais documentos, sobre o DOCUMENTO INTEIRO (sem signatureNodeName), em 2 etapas:
 *         Coordenador do Curso (e-CPF) e então IES Emissora (e-CNPJ, filtro XPath removido).
 */
@RestController
@RequestMapping("/api/diploma/curriculo-escolar")
public class CurriculoEscolarController {

    @Autowired
    private XmlKmsSigningService signingService;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;
    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;
    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    @Value("${solidsign.curriculoEscolar.step1.profile}")
    private String step1Profile;
    @Value("${solidsign.curriculoEscolar.step1.removeXPathFilter}")
    private boolean step1RemoveXPathFilter;

    @Value("${solidsign.curriculoEscolar.step2.profile}")
    private String step2Profile;
    @Value("${solidsign.curriculoEscolar.step2.removeXPathFilter}")
    private boolean step2RemoveXPathFilter;

    @CrossOrigin
    @PostMapping("/step1-coordenador")
    public ResponseEntity<byte[]> step1(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                null, null, step1Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step1RemoveXPathFilter, "curriculo_escolar_step1_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step2-envelope-final")
    public ResponseEntity<byte[]> step2(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                null, null, step2Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step2RemoveXPathFilter, "curriculo_escolar_step2_signed.xml");
    }
}
