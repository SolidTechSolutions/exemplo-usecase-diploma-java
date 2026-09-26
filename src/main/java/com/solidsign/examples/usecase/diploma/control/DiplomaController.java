package com.solidsign.examples.usecase.diploma.control;

import com.solidsign.examples.usecase.diploma.service.DiplomaSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * [EN]    REST controller demonstrating the 3-signer "Diploma Digital" (MEC) use case, one
 *         isolated endpoint per real-world step. Each endpoint takes the XML output of the
 *         previous step (except step 1, which takes the original academic document) plus the
 *         KMS credential code of that step's signer, and returns the newly signed XML.
 *
 * [PT-BR] Controller REST que demonstra o caso de uso "Diploma Digital" (MEC) com 3 assinantes,
 *         um endpoint isolado por etapa real. Cada endpoint recebe o XML de saída da etapa
 *         anterior (exceto a etapa 1, que recebe o documento acadêmico original) mais o código
 *         de credencial KMS do assinante daquela etapa, e retorna o novo XML assinado.
 *
 * [ES]    Controller REST que demuestra el caso de uso "Diploma Digital" (MEC) con 3 firmantes,
 *         un endpoint aislado por etapa real. Cada endpoint recibe el XML de salida de la etapa
 *         anterior (excepto la etapa 1, que recibe el documento académico original) más el
 *         código de credencial KMS del firmante de esa etapa, y devuelve el nuevo XML firmado.
 */
@RestController
@RequestMapping("/api/diploma")
public class DiplomaController {

    @Autowired
    private DiplomaSigningService service;

    @CrossOrigin
    @PostMapping("/step1-representante")
    public ResponseEntity<byte[]> step1(@RequestPart("document") MultipartFile document,
                                         @RequestPart("kmsCode") String kmsCode) throws IOException {
        return signAndReturn(document, kmsCode, service::signStep1Representante, "diploma_step1_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step2-emissora-dados")
    public ResponseEntity<byte[]> step2(@RequestPart("document") MultipartFile document,
                                         @RequestPart("kmsCode") String kmsCode) throws IOException {
        return signAndReturn(document, kmsCode, service::signStep2EmissoraDados, "diploma_step2_signed.xml");
    }

    @CrossOrigin
    @PostMapping("/step3-envelope-final")
    public ResponseEntity<byte[]> step3(@RequestPart("document") MultipartFile document,
                                         @RequestPart("kmsCode") String kmsCode) throws IOException {
        return signAndReturn(document, kmsCode, service::signStep3EnvelopeFinal, "diploma_step3_signed.xml");
    }

    private interface SignFn {
        byte[] apply(File file, String kmsCode) throws IOException;
    }

    private ResponseEntity<byte[]> signAndReturn(MultipartFile document, String kmsCode,
                                                  SignFn signFn, String outputName) throws IOException {
        Path tmp = Files.createTempFile("diploma-", ".xml");
        try {
            document.transferTo(tmp);
            byte[] signed = signFn.apply(tmp.toFile(), kmsCode);
            if (signed == null) return ResponseEntity.internalServerError().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .header("Content-Disposition", "attachment; filename=\"" + outputName + "\"")
                    .body(signed);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
