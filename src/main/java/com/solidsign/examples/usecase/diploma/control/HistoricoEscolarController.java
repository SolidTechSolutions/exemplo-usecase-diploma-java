package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * [EN]    Document 3/5 — Histórico Escolar Digital. Its own document since layout v1.04.1
 *         (no longer part of the Documentação Acadêmica). Signature count depends on the
 *         modality: PARTIAL = call step2 only (final e-CNPJ signature); FULL = call step1
 *         (Representante da Secretaria, e-CPF) then step2.
 * [PT-BR] Documento 3/5 — Histórico Escolar Digital. Documento próprio desde o leiaute v1.04.1
 *         (não faz mais parte da Documentação Acadêmica). A quantidade de assinaturas depende
 *         da modalidade: PARCIAL = chame só a step2 (assinatura final e-CNPJ); INTEGRAL = chame
 *         a step1 (Representante da Secretaria, e-CPF) e depois a step2.
 */
@RestController
@RequestMapping("/api/diploma/historico-escolar")
public class HistoricoEscolarController {

    @Autowired
    private XmlKmsSigningService signingService;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;
    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;
    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    @Value("${solidsign.historicoEscolar.step1.nodeName}")
    private String step1NodeName;
    @Value("${solidsign.historicoEscolar.step1.namespace}")
    private String step1Namespace;
    @Value("${solidsign.historicoEscolar.step1.profile}")
    private String step1Profile;
    @Value("${solidsign.historicoEscolar.step1.removeXPathFilter}")
    private boolean step1RemoveXPathFilter;

    @Value("${solidsign.historicoEscolar.step2.nodeName}")
    private String step2NodeName;
    @Value("${solidsign.historicoEscolar.step2.namespace}")
    private String step2Namespace;
    @Value("${solidsign.historicoEscolar.step2.profile}")
    private String step2Profile;
    @Value("${solidsign.historicoEscolar.step2.removeXPathFilter}")
    private boolean step2RemoveXPathFilter;

    /**
     * [EN]    Only for the FULL modality — skip this and call step2 directly for PARTIAL.
     * [PT-BR] Só pra modalidade INTEGRAL — pule esta e chame a step2 direto pra PARCIAL.
     */
    @CrossOrigin
    @PostMapping("/step1-secretaria-dados")
    public ResponseEntity<byte[]> step1(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step1NodeName, step1Namespace, step1Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step1RemoveXPathFilter, "historico_escolar_step1_signed.xml");
    }

    /**
     * [EN]    Final signature — the only step for PARTIAL modality, the 2nd step for FULL.
     * [PT-BR] Assinatura final — única etapa na modalidade PARCIAL, 2ª etapa na INTEGRAL.
     */
    @CrossOrigin
    @PostMapping("/step2-envelope-final")
    public ResponseEntity<byte[]> step2(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step2NodeName, step2Namespace, step2Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step2RemoveXPathFilter, "historico_escolar_step2_signed.xml");
    }
}
