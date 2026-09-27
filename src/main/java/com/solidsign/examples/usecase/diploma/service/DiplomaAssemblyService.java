package com.solidsign.examples.usecase.diploma.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * [EN]    Assembles the Diploma XML by copying the (already signed) <DadosDiploma> node out of
 *         the Documentação Acadêmica de Registro document and inserting it into the Diploma
 *         envelope template (templates/diploma-template.xml), right before <DadosRegistro> —
 *         matching the structure documented in the Diploma Digital trail. The result is an
 *         UNSIGNED Diploma XML, ready for DiplomaController's 2 signing steps.
 * [PT-BR] Monta o XML do Diploma copiando o nó <DadosDiploma> (já assinado) de dentro do
 *         documento de Documentação Acadêmica de Registro e inserindo-o no template do envelope
 *         Diploma (templates/diploma-template.xml), logo antes de <DadosRegistro> — seguindo a
 *         estrutura documentada na trilha do Diploma Digital. O resultado é um XML do Diploma
 *         AINDA NÃO ASSINADO, pronto para as 2 etapas de assinatura do DiplomaController.
 */
@Service
public class DiplomaAssemblyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiplomaAssemblyService.class);
    private static final String TEMPLATE_PATH = "templates/diploma-template.xml";

    public byte[] assemble(File signedDocumentacaoAcademica) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();

            Document academicDoc = builder.parse(signedDocumentacaoAcademica);
            Element dadosDiploma = findElementByLocalName(academicDoc, "DadosDiploma");
            if (dadosDiploma == null) {
                LOGGER.error("Could not find <DadosDiploma> in the supplied Documentação Acadêmica document.");
                return null;
            }

            Document diplomaDoc;
            try (var templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
                diplomaDoc = builder.parse(templateStream);
            }

            Element dadosRegistro = findElementByLocalName(diplomaDoc, "DadosRegistro");
            Element infDiploma = (Element) dadosRegistro.getParentNode();

            Node imported = diplomaDoc.importNode(dadosDiploma, true);
            infDiploma.insertBefore(imported, dadosRegistro);

            return toBytes(diplomaDoc);
        } catch (Exception e) {
            LOGGER.error("Unexpected error assembling the Diploma XML: {}", e.getMessage(), e);
            return null;
        }
    }

    private Element findElementByLocalName(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    private byte[] toBytes(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}
