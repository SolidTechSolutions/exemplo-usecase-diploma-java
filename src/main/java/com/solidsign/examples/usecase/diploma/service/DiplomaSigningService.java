package com.solidsign.examples.usecase.diploma.service;

import com.solidsign.examples.usecase.diploma.response.SignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;

/**
 * [EN]    Signs the Diploma Digital XML at a specific stage of the MEC "Diploma Digital" flow
 *         using a SolidSign KMS-custodied certificate (POST /solidsign/dsig/xml/sign-kms).
 *         Each stage below corresponds to a different real-world signer role and is exposed
 *         as an isolated endpoint in DiplomaController — this service does not chain the
 *         3 stages together, since each one is normally executed by a different signer/system.
 *
 * [PT-BR] Assina o XML do Diploma Digital em uma etapa específica do fluxo do MEC "Diploma
 *         Digital" usando um certificado custodiado no KMS da SolidSign
 *         (POST /solidsign/dsig/xml/sign-kms). Cada etapa abaixo corresponde a um papel real
 *         de assinante distinto e é exposta como um endpoint isolado no DiplomaController —
 *         este service não encadeia as 3 etapas, já que cada uma normalmente é executada por
 *         um assinante/sistema diferente.
 *
 * [ES]    Firma el XML del Diploma Digital en una etapa específica del flujo del MEC "Diploma
 *         Digital" usando un certificado custodiado en el KMS de SolidSign
 *         (POST /solidsign/dsig/xml/sign-kms). Cada etapa corresponde a un rol real de
 *         firmante distinto y se expone como un endpoint aislado en DiplomaController — este
 *         service no encadena las 3 etapas, ya que cada una normalmente la ejecuta un
 *         firmante/sistema diferente.
 */
@Service
public class DiplomaSigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiplomaSigningService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${solidsign.api.base-url}")
    private String baseUrl;

    @Value("${solidsign.api.authorization}")
    private String authorization;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;

    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;

    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    // ─── Etapa 1 — IES Representantes (e-CPF) assina os dados acadêmicos ───────
    @Value("${solidsign.diploma.step1.nodeName}")
    private String step1NodeName;
    @Value("${solidsign.diploma.step1.namespace}")
    private String step1Namespace;
    @Value("${solidsign.diploma.step1.profile}")
    private String step1Profile;
    @Value("${solidsign.diploma.step1.removeXPathFilter}")
    private boolean step1RemoveXPathFilter;

    // ─── Etapa 2 — IES Emissora (e-CNPJ) também assina os dados acadêmicos ─────
    @Value("${solidsign.diploma.step2.nodeName}")
    private String step2NodeName;
    @Value("${solidsign.diploma.step2.namespace}")
    private String step2Namespace;
    @Value("${solidsign.diploma.step2.profile}")
    private String step2Profile;
    @Value("${solidsign.diploma.step2.removeXPathFilter}")
    private boolean step2RemoveXPathFilter;

    // ─── Etapa 3 — IES Emissora (e-CNPJ) sela o envelope final (AD-RA) ─────────
    @Value("${solidsign.diploma.step3.nodeName}")
    private String step3NodeName;
    @Value("${solidsign.diploma.step3.namespace}")
    private String step3Namespace;
    @Value("${solidsign.diploma.step3.profile}")
    private String step3Profile;
    @Value("${solidsign.diploma.step3.removeXPathFilter}")
    private boolean step3RemoveXPathFilter;

    public byte[] signStep1Representante(File xmlFile, String kmsCode) throws IOException {
        return sign(xmlFile, kmsCode, step1NodeName, step1Namespace, step1Profile, step1RemoveXPathFilter);
    }

    public byte[] signStep2EmissoraDados(File xmlFile, String kmsCode) throws IOException {
        return sign(xmlFile, kmsCode, step2NodeName, step2Namespace, step2Profile, step2RemoveXPathFilter);
    }

    public byte[] signStep3EnvelopeFinal(File xmlFile, String kmsCode) throws IOException {
        return sign(xmlFile, kmsCode, step3NodeName, step3Namespace, step3Profile, step3RemoveXPathFilter);
    }

    /**
     * [EN]    Calls SolidSign's XML KMS signing endpoint for a single document and returns the
     *         signed XML bytes downloaded from the response link.
     * [PT-BR] Chama o endpoint de assinatura XML via KMS da SolidSign para um único documento e
     *         retorna os bytes do XML assinado baixados do link da resposta.
     * [ES]    Llama al endpoint de firma XML vía KMS de SolidSign para un único documento y
     *         devuelve los bytes del XML firmado descargados del enlace de la respuesta.
     */
    private byte[] sign(File xmlFile, String kmsCode, String nodeName, String namespace,
                         String profile, boolean removeXPathFilter) throws IOException {
        LOGGER.info("Signing '{}' with kmsCode='{}', node='{}', profile='{}'.",
                xmlFile.getName(), kmsCode, nodeName, profile);

        String url = baseUrl + "/solidsign/dsig/xml/sign-kms";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document[0]", new FileSystemResource(xmlFile));
        body.add("kmsCode", kmsCode);
        body.add("profile", profile);
        body.add("hashAlgorithm", hashAlgorithm);
        body.add("signaturePackaging", signaturePackaging);
        body.add("canonicalizationMethod", canonicalizationMethod);
        body.add("signatureNodeName[0]", nodeName);
        body.add("signatureNodeNamespace[0]", namespace);
        body.add("isRemoveXPathExclusionFilter", String.valueOf(removeXPathFilter));

        try {
            ResponseEntity<SignResponse> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), SignResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null
                    && !resp.getBody().documents.isEmpty()) {
                String downloadUrl = resp.getBody().documents.get(0).links.stream()
                        .filter(l -> "self".equals(l.rel))
                        .findFirst()
                        .map(l -> l.href)
                        .orElse(null);
                if (downloadUrl == null) {
                    LOGGER.error("SolidSign response had no download link.");
                    return null;
                }
                HttpHeaders dh = new HttpHeaders();
                dh.set("Authorization", authorization);
                ResponseEntity<byte[]> download = restTemplate.exchange(
                        downloadUrl, HttpMethod.GET, new HttpEntity<>(dh), byte[].class);
                return download.getStatusCode() == HttpStatus.OK ? download.getBody() : null;
            }
        } catch (HttpStatusCodeException e) {
            LOGGER.error("SolidSign API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during Diploma signing: {}", e.getMessage(), e);
        }
        return null;
    }
}
