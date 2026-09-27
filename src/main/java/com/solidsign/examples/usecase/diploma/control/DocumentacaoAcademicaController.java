package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * [EN]    Document 1/5 — Documentação Acadêmica de Registro: 3 isolated signer endpoints
 *         (IES Representantes → IES Emissora dados → IES Emissora envelope final).
 * [PT-BR] Documento 1/5 — Documentação Acadêmica de Registro: 3 endpoints isolados por
 *         assinante (IES Representantes → IES Emissora dados → IES Emissora envelope final).
 */
@RestController
@RequestMapping("/api/diploma/documentacao-academica")
public class DocumentacaoAcademicaController {

    @Autowired
    private XmlKmsSigningService signingService;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;
    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;
    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    @Value("${solidsign.documentacaoAcademica.step1.nodeName}")
    private String step1NodeName;
    @Value("${solidsign.documentacaoAcademica.step1.namespace}")
    private String step1Namespace;
    @Value("${solidsign.documentacaoAcademica.step1.profile}")
    private String step1Profile;
    @Value("${solidsign.documentacaoAcademica.step1.removeXPathFilter}")
    private boolean step1RemoveXPathFilter;

    @Value("${solidsign.documentacaoAcademica.step2.nodeName}")
    private String step2NodeName;
    @Value("${solidsign.documentacaoAcademica.step2.namespace}")
    private String step2Namespace;
    @Value("${solidsign.documentacaoAcademica.step2.profile}")
    private String step2Profile;
    @Value("${solidsign.documentacaoAcademica.step2.removeXPathFilter}")
    private boolean step2RemoveXPathFilter;

    @Value("${solidsign.documentacaoAcademica.step3.nodeName}")
    private String step3NodeName;
    @Value("${solidsign.documentacaoAcademica.step3.namespace}")
    private String step3Namespace;
    @Value("${solidsign.documentacaoAcademica.step3.profile}")
    private String step3Profile;
    @Value("${solidsign.documentacaoAcademica.step3.removeXPathFilter}")
    private boolean step3RemoveXPathFilter;

    @CrossOrigin
    @PostMapping("/step1-representante")
    public org.springframework.http.ResponseEntity<byte[]> step1(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step1NodeName, step1Namespace, step1Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step1RemoveXPathFilter, "documentacao_academica_step1_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step2-emissora-dados")
    public org.springframework.http.ResponseEntity<byte[]> step2(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step2NodeName, step2Namespace, step2Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step2RemoveXPathFilter, "documentacao_academica_step2_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step3-envelope-final")
    public org.springframework.http.ResponseEntity<byte[]> step3(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step3NodeName, step3Namespace, step3Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step3RemoveXPathFilter, "documentacao_academica_step3_signed.xml");
    }
}
