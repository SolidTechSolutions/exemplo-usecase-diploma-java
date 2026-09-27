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
 * [EN]    Generic single-document XAdES signing call via a SolidSign KMS-custodied certificate
 *         (POST /solidsign/dsig/xml/sign-kms). Shared by every document/step of the Diploma
 *         Digital use case (Documentação Acadêmica, Diploma, Histórico Escolar, Currículo
 *         Escolar, Lista de Diplomas Anulados) — each controller supplies the node/profile
 *         values for its own step; when nodeName is null/blank, the whole document is signed
 *         (no signatureNodeName/signatureNodeNamespace sent), as required by Currículo Escolar
 *         and Lista de Diplomas Anulados.
 * [PT-BR] Chamada genérica de assinatura XAdES de um único documento via certificado
 *         custodiado no KMS da SolidSign (POST /solidsign/dsig/xml/sign-kms). Compartilhada por
 *         todos os documentos/etapas do caso de uso Diploma Digital (Documentação Acadêmica,
 *         Diploma, Histórico Escolar, Currículo Escolar, Lista de Diplomas Anulados) — cada
 *         controller informa os valores de nó/perfil da sua etapa; quando nodeName é nulo/vazio,
 *         o documento inteiro é assinado (sem signatureNodeName/signatureNodeNamespace), como
 *         exigido pelo Currículo Escolar e pela Lista de Diplomas Anulados.
 */
@Service
public class XmlKmsSigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlKmsSigningService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${solidsign.api.base-url}")
    private String baseUrl;

    @Value("${solidsign.api.authorization}")
    private String authorization;

    /**
     * [EN]    Signs the given XML file and returns the signed XML bytes downloaded from the
     *         response link.
     * [PT-BR] Assina o arquivo XML informado e retorna os bytes do XML assinado baixados do link
     *         da resposta.
     */
    public byte[] sign(File xmlFile, String kmsCode, String nodeName, String namespace,
                        String profile, String hashAlgorithm, String signaturePackaging,
                        String canonicalizationMethod, boolean removeXPathFilter) throws IOException {
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
        if (nodeName != null && !nodeName.isBlank()) {
            body.add("signatureNodeName[0]", nodeName);
            body.add("signatureNodeNamespace[0]", namespace);
        }
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
            LOGGER.error("Unexpected error during signing: {}", e.getMessage(), e);
        }
        return null;
    }
}
