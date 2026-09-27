# 🇧🇷 SolidSign API - Caso de Uso: Diploma Digital (MEC) — Java

Este projeto demonstra a integração com a **SolidSign API** para o caso de uso completo do **Diploma Digital** do MEC, cobrindo os **5 documentos** da trilha, cada um com seus próprios assinantes e etapas, usando certificados custodiados no **KMS SolidSign**.

Diferente dos [exemplos genéricos de assinatura XML](https://github.com/SolidTechSolutions?q=integracao-xml), que expõem um único endpoint parametrizável, este repositório expõe **um endpoint isolado por etapa real de cada documento**, já pré-configurado com os valores corretos de `signatureNodeName`, `profile` e `isRemoveXPathExclusionFilter`.

## Os 5 documentos

| # | Documento | Endpoints | Assinantes |
| :-: | :--- | :--- | :--- |
| 1 | Documentação Acadêmica de Registro | `documentacao-academica/step{1,2,3}-*` | IES Representantes (e-CPF, 1..n) → IES Emissora dados (e-CNPJ) → IES Emissora envelope final (e-CNPJ) |
| 2 | **Diploma Digital** | `diploma/assemble`, `diploma/step{1,2}-*` | *(montado a partir do doc. 1)* → Representante da Registradora (e-CPF) → IES Registradora envelope final (e-CNPJ) |
| 3 | Histórico Escolar Digital | `historico-escolar/step{1,2}-*` | *(parcial: só step2)* Representante da Secretaria (e-CPF) → IES Emissora envelope final (e-CNPJ) |
| 4 | Currículo Escolar Digital | `curriculo-escolar/step{1,2}-*` | Coordenador do Curso (e-CPF) → IES Emissora (e-CNPJ) — documento inteiro |
| 5 | Lista de Diplomas Anulados / Arquivo de Fiscalização | `lista-anulados/sign` | Instituição (e-CNPJ) — documento inteiro, etapa única |

## Documento 2 — Diploma Digital: montagem + assinatura

Este é o único documento que **não parte de um arquivo novo**: ele é montado copiando o nó `<DadosDiploma>` de dentro da Documentação Acadêmica de Registro já assinada.

1. `POST /api/diploma/diploma/assemble` — recebe a Documentação Acadêmica assinada (`signedDocumentacaoAcademica`), copia `<DadosDiploma>` pro template do envelope Diploma (`src/main/resources/templates/diploma-template.xml`, logo antes de `<DadosRegistro>`) via manipulação DOM real (`DiplomaAssemblyService`), e retorna o Diploma **ainda não assinado**.
2. `POST /api/diploma/diploma/step1-registradora-dados` — Representante da Registradora (e-CPF) assina `DadosRegistro`.
3. `POST /api/diploma/diploma/step2-envelope-final` — IES Registradora (e-CNPJ) sela o envelope `Diploma` (AD-RA).

> Os campos de `DadosRegistro` no template são placeholders ilustrativos — substitua pelo schema real da sua registradora conforme o XSD do MEC antes de usar em produção.

## Fluxo completo (curl)

```bash
# 1) Documentação Acadêmica de Registro
curl -X POST http://localhost:8080/api/diploma/documentacao-academica/step1-representante \
  -F "document=@doc-academica.xml" -F "kmsCode=$KMS_REPRESENTANTE" -o academica-step1.xml
curl -X POST http://localhost:8080/api/diploma/documentacao-academica/step2-emissora-dados \
  -F "document=@academica-step1.xml" -F "kmsCode=$KMS_IES_EMISSORA" -o academica-step2.xml
curl -X POST http://localhost:8080/api/diploma/documentacao-academica/step3-envelope-final \
  -F "document=@academica-step2.xml" -F "kmsCode=$KMS_IES_EMISSORA" -o academica-final.xml

# 2) Diploma Digital — montagem + assinatura
curl -X POST http://localhost:8080/api/diploma/diploma/assemble \
  -F "signedDocumentacaoAcademica=@academica-final.xml" -o diploma-montado.xml
curl -X POST http://localhost:8080/api/diploma/diploma/step1-registradora-dados \
  -F "document=@diploma-montado.xml" -F "kmsCode=$KMS_REPRESENTANTE_REGISTRADORA" -o diploma-step1.xml
curl -X POST http://localhost:8080/api/diploma/diploma/step2-envelope-final \
  -F "document=@diploma-step1.xml" -F "kmsCode=$KMS_IES_REGISTRADORA" -o diploma-final.xml

# 3) Histórico Escolar (modalidade integral)
curl -X POST http://localhost:8080/api/diploma/historico-escolar/step1-secretaria-dados \
  -F "document=@historico.xml" -F "kmsCode=$KMS_SECRETARIA" -o historico-step1.xml
curl -X POST http://localhost:8080/api/diploma/historico-escolar/step2-envelope-final \
  -F "document=@historico-step1.xml" -F "kmsCode=$KMS_IES_EMISSORA" -o historico-final.xml

# 4) Currículo Escolar
curl -X POST http://localhost:8080/api/diploma/curriculo-escolar/step1-coordenador \
  -F "document=@curriculo.xml" -F "kmsCode=$KMS_COORDENADOR" -o curriculo-step1.xml
curl -X POST http://localhost:8080/api/diploma/curriculo-escolar/step2-envelope-final \
  -F "document=@curriculo-step1.xml" -F "kmsCode=$KMS_IES_EMISSORA" -o curriculo-final.xml

# 5) Lista de Diplomas Anulados / Arquivo de Fiscalização
curl -X POST http://localhost:8080/api/diploma/lista-anulados/sign \
  -F "document=@lista-anulados.xml" -F "kmsCode=$KMS_IES" -o lista-anulados-final.xml
```

## Configuração (application.properties)

| Atributo | Descrição |
| :--- | :--- |
| `solidsign.api.base-url` / `authorization` | URL base e token JWT (Bearer) da SolidSign API. |
| `solidsign.diploma.hashAlgorithm` / `signaturePackaging` / `canonicalizationMethod` | Parâmetros de assinatura compartilhados por todos os documentos. |
| `solidsign.documentacaoAcademica.step{1,2,3}.*` | Documento 1. |
| `solidsign.diplomaDoc.step{1,2}.*` | Documento 2 (Diploma). |
| `solidsign.historicoEscolar.step{1,2}.*` | Documento 3. |
| `solidsign.curriculoEscolar.step{1,2}.*` | Documento 4 (sem nodeName — documento inteiro). |
| `solidsign.listaAnulados.*` | Documento 5 (sem nodeName — documento inteiro). |

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+
4. `javax.xml`/DOM (JDK padrão) para a montagem do Diploma — nenhuma dependência extra.

## Como Executar

1. Preencha `solidsign.api.authorization` em `src/main/resources/application.properties`.
2. `mvn clean install && mvn spring-boot:run`
3. Siga o fluxo de curl acima, documento por documento.

## Outros métodos de certificação

Este exemplo usa certificado **custodiado no KMS**. Para HSM em nuvem de terceiros ou assinatura via navegador (PKCS#1), use os mesmos parâmetros de etapa nos exemplos genéricos [`exemplo-integracao-xml-cloud`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-cloud) e [`exemplo-integracao-xml-pkcs1`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-pkcs1).

## Tratamento de Erros
O sistema intercepta erros **400 Bad Request** e **500 Internal Server Error** e loga o JSON detalhado da SolidSign para facilitar o debug de credenciais ou parâmetros inválidos.

---

# 🇬🇧 SolidSign API - Use Case: Digital Diploma (MEC) — Java

This project demonstrates the integration with the **SolidSign API** for the complete **Digital Diploma** use case from the Brazilian Ministry of Education (MEC), covering all **5 documents** in the trail, each with its own signers and steps, using KMS-custodied certificates.

## The 5 documents

| # | Document | Endpoints | Signers |
| :-: | :--- | :--- | :--- |
| 1 | Academic Registration Documentation | `documentacao-academica/step{1,2,3}-*` | Institution representatives (e-CPF, 1..n) → Issuing institution data (e-CNPJ) → Issuing institution final envelope (e-CNPJ) |
| 2 | **Digital Diploma** | `diploma/assemble`, `diploma/step{1,2}-*` | *(assembled from doc. 1)* → Registrar representative (e-CPF) → Registering institution final envelope (e-CNPJ) |
| 3 | Digital School Transcript | `historico-escolar/step{1,2}-*` | *(partial: step2 only)* Registry representative (e-CPF) → Issuing institution final envelope (e-CNPJ) |
| 4 | Digital School Curriculum | `curriculo-escolar/step{1,2}-*` | Course coordinator (e-CPF) → Issuing institution (e-CNPJ) — entire document |
| 5 | Annulled Diplomas List / Audit File | `lista-anulados/sign` | Institution (e-CNPJ) — entire document, single step |

## Document 2 — Digital Diploma: assembly + signing

This is the only document that does **not** start from a fresh file: it is assembled by copying the `<DadosDiploma>` node out of the already-signed Academic Registration Documentation.

1. `POST /api/diploma/diploma/assemble` — takes the signed Academic Documentation (`signedDocumentacaoAcademica`), copies `<DadosDiploma>` into the Diploma envelope template (`src/main/resources/templates/diploma-template.xml`, right before `<DadosRegistro>`) via real DOM manipulation (`DiplomaAssemblyService`), and returns the **unsigned** Diploma.
2. `POST /api/diploma/diploma/step1-registradora-dados` — Registrar representative (e-CPF) signs `DadosRegistro`.
3. `POST /api/diploma/diploma/step2-envelope-final` — Registering institution (e-CNPJ) seals the `Diploma` envelope (AD-RA).

> The `DadosRegistro` fields in the template are illustrative placeholders — replace with your registrar's real schema per the MEC XSD before production use.

## Configuration (application.properties)

See `solidsign.documentacaoAcademica.*`, `solidsign.diplomaDoc.*`, `solidsign.historicoEscolar.*`, `solidsign.curriculoEscolar.*` and `solidsign.listaAnulados.*` for each document's per-step values, already pre-filled with the real MEC values.

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+
4. `javax.xml`/DOM (JDK standard library) for the Diploma assembly — no extra dependency.

## How to Run

1. Fill in `solidsign.api.authorization` in `src/main/resources/application.properties`.
2. `mvn clean install && mvn spring-boot:run`
3. Follow each document's flow (see the Portuguese section above for the full curl chain).

## Other certification methods

For a third-party cloud HSM or browser-side (PKCS#1) signing, apply the same per-step parameters to the generic [`exemplo-integracao-xml-cloud`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-cloud) and [`exemplo-integracao-xml-pkcs1`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-pkcs1) examples.

## Error Handling
The system intercepts **400 Bad Request** and **500 Internal Server Error** responses and logs the detailed JSON from SolidSign to assist in debugging invalid credentials or parameters.

---

# 🇪🇸 SolidSign API - Caso de Uso: Diploma Digital (MEC) — Java

Este proyecto demuestra la integración con la **SolidSign API** para el caso de uso completo del **Diploma Digital** del MEC, cubriendo los **5 documentos** de la ruta, cada uno con sus propios firmantes y etapas, usando certificados custodiados en el KMS.

## Los 5 documentos

| # | Documento | Endpoints | Firmantes |
| :-: | :--- | :--- | :--- |
| 1 | Documentación Académica de Registro | `documentacao-academica/step{1,2,3}-*` | Representantes de la IES (e-CPF, 1..n) → IES Emisora datos (e-CNPJ) → IES Emisora sobre final (e-CNPJ) |
| 2 | **Diploma Digital** | `diploma/assemble`, `diploma/step{1,2}-*` | *(armado a partir del doc. 1)* → Representante de la Registradora (e-CPF) → IES Registradora sobre final (e-CNPJ) |
| 3 | Historial Escolar Digital | `historico-escolar/step{1,2}-*` | *(parcial: solo step2)* Representante de la Secretaría (e-CPF) → IES Emisora sobre final (e-CNPJ) |
| 4 | Currículo Escolar Digital | `curriculo-escolar/step{1,2}-*` | Coordinador del Curso (e-CPF) → IES Emisora (e-CNPJ) — documento entero |
| 5 | Lista de Diplomas Anulados / Archivo de Fiscalización | `lista-anulados/sign` | Institución (e-CNPJ) — documento entero, etapa única |

## Documento 2 — Diploma Digital: armado + firma

Este es el único documento que **no** parte de un archivo nuevo: se arma copiando el nodo `<DadosDiploma>` de dentro de la Documentación Académica de Registro ya firmada.

1. `POST /api/diploma/diploma/assemble` — recibe la Documentación Académica firmada, copia `<DadosDiploma>` al template del sobre Diploma, y devuelve el Diploma **aún sin firmar**.
2. `POST /api/diploma/diploma/step1-registradora-dados` — Representante de la Registradora firma `DadosRegistro`.
3. `POST /api/diploma/diploma/step2-envelope-final` — IES Registradora sella el sobre `Diploma` (AD-RA).

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+

## Cómo Ejecutar

1. Complete `solidsign.api.authorization` en `application.properties`.
2. `mvn clean install && mvn spring-boot:run`
3. Siga el flujo de cada documento (vea la sección en portugués arriba para la cadena completa de curl).

## Otros métodos de certificación

Para HSM en la nube o navegador (PKCS#1), aplique los mismos parámetros a los ejemplos genéricos [`exemplo-integracao-xml-cloud`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-cloud) y [`exemplo-integracao-xml-pkcs1`](https://github.com/SolidTechSolutions/exemplo-integracao-xml-pkcs1).

## Gestión de Errores
El sistema intercepta errores **400 Bad Request** y **500 Internal Server Error** y registra el JSON detallado de SolidSign.
