package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.DiplomaAssemblyService;
import com.solidsign.examples.usecase.diploma.service.XmlKmsSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * [EN]    Document 2/5 — Diploma Digital. Unlike the other documents, this one does not start
 *         from a fresh file: it is ASSEMBLED by copying the <DadosDiploma> node out of the
 *         already-signed Documentação Acadêmica de Registro into the Diploma envelope template
 *         (see DiplomaAssemblyService) — then signed in 2 steps (Representante da Registradora
 *         → IES Registradora envelope final).
 * [PT-BR] Documento 2/5 — Diploma Digital. Diferente dos demais documentos, este não parte de
 *         um arquivo novo: ele é MONTADO copiando o nó <DadosDiploma> de dentro da Documentação
 *         Acadêmica de Registro já assinada para o template do envelope Diploma (ver
 *         DiplomaAssemblyService) — e então assinado em 2 etapas (Representante da Registradora
 *         → envelope final da IES Registradora).
 */
@RestController
@RequestMapping("/api/diploma/diploma")
public class DiplomaController {

    @Autowired
    private XmlKmsSigningService signingService;

    @Autowired
    private DiplomaAssemblyService assemblyService;

    @Value("${solidsign.diploma.hashAlgorithm}")
    private String hashAlgorithm;
    @Value("${solidsign.diploma.signaturePackaging}")
    private String signaturePackaging;
    @Value("${solidsign.diploma.canonicalizationMethod}")
    private String canonicalizationMethod;

    @Value("${solidsign.diplomaDoc.step1.nodeName}")
    private String step1NodeName;
    @Value("${solidsign.diplomaDoc.step1.namespace}")
    private String step1Namespace;
    @Value("${solidsign.diplomaDoc.step1.profile}")
    private String step1Profile;
    @Value("${solidsign.diplomaDoc.step1.removeXPathFilter}")
    private boolean step1RemoveXPathFilter;

    @Value("${solidsign.diplomaDoc.step2.nodeName}")
    private String step2NodeName;
    @Value("${solidsign.diplomaDoc.step2.namespace}")
    private String step2Namespace;
    @Value("${solidsign.diplomaDoc.step2.profile}")
    private String step2Profile;
    @Value("${solidsign.diplomaDoc.step2.removeXPathFilter}")
    private boolean step2RemoveXPathFilter;

    /**
     * [EN]    Assembles the (unsigned) Diploma XML from a signed Documentação Acadêmica de
     *         Registro document. Call this before step1.
     * [PT-BR] Monta o XML (ainda não assinado) do Diploma a partir de uma Documentação Acadêmica
     *         de Registro já assinada. Chame antes da etapa 1.
     */
    @CrossOrigin
    @PostMapping("/assemble")
    public ResponseEntity<byte[]> assemble(
            @RequestPart("signedDocumentacaoAcademica") MultipartFile signedDocumentacaoAcademica) throws IOException {
        Path tmp = Files.createTempFile("documentacao-academica-", ".xml");
        try {
            signedDocumentacaoAcademica.transferTo(tmp);
            byte[] assembled = assemblyService.assemble(tmp.toFile());
            if (assembled == null) return ResponseEntity.badRequest().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .header("Content-Disposition", "attachment; filename=\"diploma_montado.xml\"")
                    .body(assembled);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @CrossOrigin
    @PostMapping("/step1-registradora-dados")
    public ResponseEntity<byte[]> step1(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step1NodeName, step1Namespace, step1Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step1RemoveXPathFilter, "diploma_step1_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step2-envelope-final")
    public ResponseEntity<byte[]> step2(
            @RequestPart("document") MultipartFile document, @RequestPart("kmsCode") String kmsCode) throws IOException {
        return SigningEndpointSupport.signAndReturn(signingService, document, kmsCode,
                step2NodeName, step2Namespace, step2Profile, hashAlgorithm, signaturePackaging,
                canonicalizationMethod, step2RemoveXPathFilter, "diploma_step2_signed.xml");
    }
}
