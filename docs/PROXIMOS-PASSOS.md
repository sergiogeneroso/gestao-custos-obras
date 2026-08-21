# Status e Próximos Passos

Consultar e atualizar ao final de cada sessão de trabalho.

## Feito

- Base: Spring Boot 4.1 (Java 25), Flyway, Security, Bean Validation
- CRUD completo: `imovel` (+ galeria de fotos), `aportante`, `etapaProjeto`,
  `despesa` (+ rateio, PUT, filtros, upload de comprovante), `orcamentoEtapa`
  (orçado vs. realizado)
- `relatorio/`: custo por imóvel/m², extrato de aportantes, orçado vs.
  realizado, exportação CSV (RF05)
- `shared/storage`: upload/download local, extensível para cloud
- `application.properties.example` criado; `ddl-auto=update`
- `auth/`: domínio de autenticação — `UsuarioModel`, `UsuarioRepository`,
  `AuthService`, `AuthController` (`/api/auth/login`), login emite JWT
- `shared/config/JwtService` + `JwtAuthenticationFilter` (stateless, jjwt),
  `BCryptPasswordEncoder`, `JwtAuthenticationEntryPoint` (401 adequado)
- HTTP Basic removido; substituído por JWT (RNF01)
- Seed admin inicial via migration V6 (BCrypt) — admin@gestao.local / admin123
- Flyway restaurado como única fonte de schema (`FlywayConfig` `@Bean`
  manual): o Spring Boot 4.1 removeu a `FlywayAutoConfiguration`, então
  `ddl-auto=validate` e `migrate()` roda por bean antes do EMF (ADR-012).
  `DatabaseCleanupRunner` removido (era para limpar pre-ADR-003/004).
- Flyway **pausado** (ADR-013) até a modelagem do MVP fechar: `ddl-auto=update`,
  `FlywayConfig` sem `@Configuration`, schema direto pelo Hibernate

## Reescopo de Ago 2026 (ADR-019 a ADR-029)

O objetivo do projeto foi reescrito: de "gestão de despesas de obras" para
**resultado financeiro de cada imóvel do começo ao fim** (compra do lote, custos
do ciclo de vida, venda). O roadmap anterior (Etapas 7 a 9) foi removido deste
arquivo em Ago 2026 por já estar cumprido ou superado — o que continua valendo
dele está em "Pendências antigas ainda válidas", no fim. O roadmap atual começa
aqui e continua em "Revisão do domínio Imóvel":

### Etapa A — Registro das decisões ✅
- [x] ADR-019 a ADR-029 em `docs/DECISOES.md`
- [x] Rules novas `ciclo-vida-imovel.md` e `contratos-financeiros.md`;
      `regras-negocio-financeiras.md` reescrita com a regra de custo
- [x] `AGENTS.md`, `CLAUDE.md`, `REQUISITOS.md`, `MODELO-DADOS.md` e
      `ARQUITETURA.md` atualizados

Cada etapa abaixo é retomável de forma independente, numa sessão própria: diz o
que ler antes, o que fazer e quando está pronta. As Etapas B a D mexem em código
que se referencia mutuamente, então, se forem feitas em sessões separadas, é
esperado que o backend só volte a compilar ao final da D.

### Etapa B — Backend: pessoa e fornecedor ✅
**Ler antes:** ADR-021, ADR-022.
- [x] Renomear o pacote `aportante/` → `pessoa/` (7 arquivos: Model, Repository,
      Service, Mapper, Controller e os dois DTOs); endpoint `/api/pessoas`
- [x] `PessoaModel`: `nome`, `tipoPessoa` (enum novo em `shared/enums/`),
      `documento` (único), `email`, `telefone`, `ativo`. Remover
      `tipoParticipacao`. Documento sem validação de dígito — comentário
      `ponytail:` no service
- [x] Novo domínio `fornecedor/` pela skill `gerar-crud-dominio`:
      `@OneToOne` para `PessoaModel` + `areaAtuacao`, `observacoes`, soft delete;
      endpoint `/api/fornecedores`

**Pronto quando:** `/api/pessoas` e `/api/fornecedores` respondem e nenhuma
classe referencia `Aportante`.

### Etapa C — Backend: despesa ✅
**Ler antes:** ADR-023, ADR-027, ADR-028 e
`.agents/rules/regras-negocio-financeiras.md`.
- [x] `DespesaModel`: `imovel` **opcional**; adicionar `pagador` (obrigatório),
      `beneficiario` (opcional), `faseImovel` (opcional — não existe para gasto
      geral), `contratoFinanceiro` (opcional) e `ativo`; trocar `etapaProjeto`
      por `categoriaDespesa`; remover `pagamentos` e `comprovanteUrl`
- [x] Apagar `DespesaPagamentoModel`, `DespesaPagamentoRepository` e os dois DTOs
      de pagamento; `DespesaService` perde todo o laço de rateio e a validação da
      soma; `deletar()` vira `inativar()`
- [x] `DespesaAnexoModel` tipado (COMPROVANTE/NOTA_FISCAL/RECIBO/CONTRATO/OUTRO),
      espelhando `ImovelFotoModel` e reusando `StorageService`; endpoints de
      upload, listagem por tipo e exclusão
- [x] Filtro de gastos gerais na listagem (`GET /api/despesas?semImovel=true`)

**Pronto quando:** dá para lançar despesa com pagador e beneficiário, anexar
comprovante e nota fiscal separadamente, e lançar despesa sem imóvel.

### Etapa D — Backend: categoria e imóvel ✅
**Ler antes:** ADR-020, ADR-024, ADR-026 e `.agents/rules/ciclo-vida-imovel.md`.
- [x] Renomear `etapaProjeto/` → `categoriaDespesa/` (endpoint
      `/api/categorias-despesa`) e `orcamentoEtapa/` → `orcamentoCategoria/`
- [x] Seed das categorias: Aquisição, ITBI/Escritura, Documentação, IPTU,
      Material, Mão de obra, Custos de financiamento, Corretagem, Impostos sobre
      a venda (via `CategoriaDespesaSeedRunner`, já que o Flyway está pausado)
- [x] `ImovelModel`: `tipo` → `fase` (`FaseImovel`) e `status` → `situacao`
      (`SituacaoImovel`); `dataInicioLote` (removida depois pela ADR-032), `dataInicioConstrucao`,
      `dataConclusaoObra`; `custoEstimadoObra`, `previsaoConclusao`;
      `@Embedded DadosCompra` e `DadosVenda`
- [x] `PATCH /api/imoveis/{id}/fase` e `/situacao` — só por aqui a fase e a
      situação mudam, porque gravam as datas de transição
- [x] `ImovelDocumentoModel` tipado, por fase, com endpoints próprios

**Pronto quando:** o backend compila, `./mvnw test` passa, e o ciclo
lote → construção → casa avança gravando as datas.

**Nota da implementação (Ago 2026):** a Etapa C depende de `categoriaDespesa/`
(renomeação da Etapa D) e de uma FK opcional para `contratoFinanceiro`
(domínio da Etapa E, ainda não implementada) — por isso o rename de
`etapaProjeto/` foi antecipado e `contratoFinanceiro/ContratoFinanceiroModel`
foi criado como **stub mínimo** (só `id`), só para a FK compilar; o CRUD
completo (tipo, contraparte, parcelas, quitação) continua sendo a Etapa E.
`relatorio/` também precisou de ajuste mínimo (não é a reescrita da Etapa F)
para voltar a compilar: `ExtratoAportanteDTO` virou `ExtratoPessoaDTO`
(usando `despesa.pagador` no lugar do `despesa_pagamento` removido) e os
parâmetros de etapa viraram `categoriaDespesaId`. O banco local foi recriado
do zero (`DROP SCHEMA public CASCADE`) porque tinha dados de teste residuais
que bloqueavam as colunas `NOT NULL` novas — isso apagou o usuário admin
seedado pela migration V6 (inerte com o Flyway pausado); rodar o `INSERT` do
`V6__seed_usuario_admin.sql` manualmente para voltar a logar.

### Etapa E — Backend: contratos financeiros ✅
**Ler antes:** ADR-025 e `.agents/rules/contratos-financeiros.md`.
- [x] `contratoFinanceiro/` pela skill `gerar-crud-dominio`:
      `ContratoFinanceiroModel` (imóvel, tipo, contraparte, valor contratado,
      situação, data e valor de quitação) e `ParcelaContratoModel` (número,
      vencimento, valor, `valorJuros`, data e valor de pagamento)
- [x] `quitar(id, data, valor)`: registra a quitação e encerra as parcelas em
      aberto **sem alterar os valores originais delas**
- [x] Aviso (sem bloqueio) ao iniciar construção com `PARCELAMENTO_COMPRA` ativo

**Pronto quando:** dá para encadear parcelamento da compra → quitação antecipada
→ financiamento de construção no mesmo imóvel.

**Nota da implementação (Ago 2026):** sem `DELETE`/soft delete em
`ContratoFinanceiro` — a ADR-028 lista soft delete só para `Imovel`, `Pessoa`,
`Fornecedor` e `Despesa`, e contrato nesse negócio só progride de `ATIVO` para
`QUITADO`, nunca é removido. `quitar()` só grava os três campos do contrato
(`situacao`, `dataQuitacao`, `valorQuitacao`) e não toca em nenhuma parcela —
"encerrar as parcelas em aberto" é tratado como leitura no relatório (parcela
com vencimento após a quitação deixa de contar como pendente), não como
mutação de dado, para respeitar a regra de nunca alterar valores originais. O
aviso de construção viaja como campo `aviso` (nullable) só na
`ImovelResponseDTO` devolvida por `PATCH /api/imoveis/{id}/fase` — as outras
respostas de imóvel continuam com `aviso: null`.

**Pronto quando:** dá para encadear parcelamento da compra → quitação antecipada
→ financiamento de construção no mesmo imóvel.

### Etapa F — Backend: relatórios ✅
**Ler antes:** a regra de custo na ADR-025.
- [x] `ExtratoAportanteDTO` → `ExtratoPessoaDTO` (mantendo filtro `> 0` e CSV `;`)
- [x] `GET /api/relatorios/resultado-imovel`: despesas por fase, juros pagos,
      custo total, lucro, margem, tempo por fase, dias em carteira,
      rentabilidade anualizada, `resultadoProvisorio` e posição dos contratos
- [x] `GET /api/relatorios/historico-fornecedor` e `GET /api/relatorios/carteira`
- [x] Testes cobrindo: prestação não vira custo, saldo devedor não entra no
      custo, gasto geral não entra no custo de imóvel nenhum

**Pronto quando:** os testes da regra de custo passam.

**Nota da implementação (Ago 2026):** `extratoPessoas` e `historicoFornecedor`
compartilham um helper privado `extratoPorPapel` (parametrizado por
`DespesaModel::getPagador`/`getBeneficiario`) para não duplicar a soma de
dinheiro em dois lugares. `margem` é lucro sobre o valor de venda (não sobre o
custo). `saldoDevedor` de um contrato `QUITADO` é sempre zero — a quitação
liquida o contrato, e o valor negociado (`valorQuitacao`) entra em `totalPago`,
nunca em `custoTotal`. `rentabilidadeAnualizada` usa `double`/`Math.pow` só por
ser indicador percentual, com comentário no ponto de uso.

### Etapa G — Frontend ✅
- [x] Renomear features placeholder (`aportantes/` → `pessoas/`,
      `etapas-projeto/` → `categorias-despesa/`, `orcamento-etapa/` →
      `orcamento-categoria/`), novas rotas `fornecedores/` e `contratos/`,
      ajustar `app.routes.ts`, o menu do `shell.ts` e os textos de landing/login
- [x] `imovel.model.ts` e a tela de imóveis com fase, situação e compra/venda
- [x] `dashboard.service.ts` passa a consumir `/api/relatorios/carteira` em vez
      de recalcular no frontend
- [x] Telas CRUD de Pessoa, Fornecedor, Despesa, Contratos e Categoria de
      Despesa pela skill `gerar-crud-frontend`

**Nota da implementação (Ago 2026):** a tela de imóveis ganhou duas ações de
ciclo de vida que não estavam em nenhum domínio anterior —
`imovel-fase-dialog/` (PATCH `/fase`, avança para a próxima fase com data) e
`imovel-venda-dialog/` (PATCH `/situacao=VENDIDO`, pede valor/data/comprador);
`ADQUIRIDO ⇄ A_VENDA` é uma troca direta sem dialog. `Contratos` foge do
padrão CRUD da skill porque o backend não tem `PUT`/editar — só `criar`
(com `FormArray` de parcelas), `quitar` e `pagarParcela` — então
`contrato-detalhe-dialog` faz a baixa de parcela e a quitação como formulários
inline, sem dialogs extras. `despesas` reaproveita o padrão de upload de
`imoveis` (fotos → anexos tipados) porque `DespesaAnexoModel` pede a mesma
coisa. Bug pré-existente encontrado e corrigido durante a verificação: o
Hibernate devolvia `null` (em vez de um objeto com campos null) para
`ImovelModel.compra`/`.venda` quando todas as colunas ficavam nulas no banco
— `DadosVenda` tem uma associação `@ManyToOne`, fora do que
`hibernate.create_empty_composites.enabled` cobre. Corrigido com getters
manuais em `ImovelModel` que nunca devolvem `null` (ver comentário no
código); a config do Hibernate ficou também, como defesa adicional.
`categorias-despesa` (placeholder desde a Etapa D) ganhou a tela completa
depois: é o único CRUD com delete físico (`DELETE` real, sem `ativo`), então
o botão de exclusão chama `excluir()`/`service.deletar()` em vez do
`inativar()` dos demais domínios.

### Etapa H — Schema e skills ✅
- [x] Tabelas órfãs do `ddl-auto=update` — `aportante`, `despesa_pagamento` e
      `etapa_projeto` já haviam sumido quando o banco foi recriado na Etapa D;
      sobrou só `fornecedor`, removida com `DROP TABLE`
- [x] Atualizar os exemplos que citam `aportante`/`etapa` em
      `.agents/skills/gerar-crud-dominio/SKILL.md` e `gerar-crud-frontend/SKILL.md`
- [x] Atualizar a estrutura de pastas em `docs/FRONTEND.md`

**Nota da implementação (Ago 2026):** as skills também citavam `fornecedor/`
como exemplo canônico (domínio extinto pela ADR-034), `TIPO_IMOVEL_LABEL`/
`STATUS_IMOVEL_LABEL` (renomeados para `FASE_`/`SITUACAO_` na Etapa D) e
"`contratos/` não tem `PUT`" (ganhou `PUT` na ADR-036) — tudo corrigido junto,
porque exemplo errado em skill contamina todo código gerado depois.

## Revisão do domínio Imóvel (Ago 2026)

Origem: análise do domínio `imovel` contra o uso real do cliente — cadastro do
lote, conversão em construção, conclusão como casa e venda. Cada etapa abaixo é
fechável e revisável sozinha, na ordem em que estão.

### Etapa I — Parcelamento de venda não é dívida ✅
- [x] `ehDivida()` em `RelatorioService` como único ponto de decisão: juros de
      `PARCELAMENTO_VENDA` saem do `custoTotal` (o comprador é quem paga) e as
      parcelas em aberto viram "a receber", não saldo devedor
- [x] `CarteiraDTO` ganha `saldoAReceberTotal` e `parcelasAReceber30Dias`;
      dashboard mostra o card "A receber"
- [x] Regra registrada em `.agents/rules/contratos-financeiros.md`; dois testes

### Etapa J — Área do lote × área construída ✅ (ADR-030)
- [x] `area` → `areaLote`, mais `areaConstruida`; migração manual da coluna
- [x] `custo-por-m2` passa a devolver `custoObraPorM2` (despesas da fase
      `CONSTRUCAO` ÷ área construída) além do custo por m² de lote

### Etapa K — Tela de resultado por imóvel ✅
- [x] `features/relatorios/` deixa de ser placeholder: seletor de imóvel, KPIs,
      composição do custo, obra, tempo por fase e posição dos contratos
- [x] Exportação CSV e "Salvar em PDF" via impressão do navegador (`@media
      print` em `relatorios.scss` e `styles.scss`), sem dependência nova

### Etapa L — Backend: propriedades por fase ✅ (ADR-031, ADR-032, ADR-033)
**Ler antes:** ADR-031 a ADR-033 e `.agents/rules/ciclo-vida-imovel.md`.
- [x] `DadosLote`, `DadosConstrucao` e `DadosCasa` como `@Embedded` em
      `ImovelModel`, reaproveitando as colunas que já existem (`area_lote`,
      `area_construida`, `data_inicio_construcao`, `custo_estimado_obra`,
      `previsao_conclusao`, `data_conclusao_obra`) e acrescentando as novas
- [x] Getters manuais para os três, como `compra`/`venda` já tinham
- [x] Remover `dataInicioLote`; `compra.data` obrigatória e assumindo o papel de
      marco inicial em `RelatorioService` (dias em carteira e tempo por fase)
- [x] `PATCH /fase` aceita os dados da fase de destino e `PATCH /situacao` o valor
      pretendido; `POST` só aceita dados do lote
- [x] Migração manual no banco de dev
- [x] Endereço na raiz: logradouro, número, bairro, cidade, UF, CEP e observação
- [x] Frontend ajustado ao contrato novo (só o necessário para seguir funcionando)

**Nota da implementação (Ago 2026):** os DTOs viraram records aninhados
(`DadosLoteDTO`, `DadosConstrucaoDTO`, `DadosCasaDTO`), reusados entre o cadastro
e o `PATCH /fase` para a lista de campos de cada fase existir num lugar só. A
regra "o PUT não preenche fase futura" mora em `ImovelMapper.updateEntityFromDto`,
que só aplica um grupo quando `imovel.fase` já alcançou aquela fase. O
`ddl-auto=update` **não** aplica `NOT NULL` em coluna existente, então o
`ALTER TABLE imovel ALTER COLUMN compra_data SET NOT NULL` foi manual, junto do
`DROP COLUMN data_inicio_lote`.

### Etapa M — Frontend: cadastro e transições por fase ✅
- [x] Formulário de criação só com os campos do lote
- [x] `imovel-fase-dialog` pede os dados da fase de destino junto com a data
- [x] Colocar à venda deixa de ser troca direta e passa a pedir o valor pretendido
- [x] O `PUT` (editar) mostra apenas as fases já alcançadas pelo imóvel
- [x] Detalhe do imóvel ganha os blocos "Registro do lote", "Obra" e "Casa"

**Nota da implementação (Ago 2026):** o formulário passou de grade única para
seções (`Lote`, `Construção`, `Casa`), condicionadas por `imovel.fase`; o valor
de venda pretendido só aparece quando a situação já não é `ADQUIRIDO`. Colocar à
venda ganhou dialog próprio (`imovel-a-venda-dialog`) em vez de virar um modo do
`imovel-venda-dialog`, que pede valor, data e comprador obrigatórios.

### Etapa N — Bloco financeiro no detalhe do imóvel ✅
- [x] Custo acumulado e composição (compra, despesas por fase, juros) na aba
      Financeiro do detalhe, consumindo `resultado-imovel`
- [x] Contratos do imóvel com o rótulo "a receber" para `PARCELAMENTO_VENDA`
- [x] Últimas 5 despesas e botão "Lançar despesa" já com o imóvel selecionado
- [ ] Atalho para a tela de resultado — deixado de fora por decisão do usuário

**Nota da implementação (Ago 2026):** o detalhe passou a usar `MatTabsModule`
em dois níveis — **Cadastro** (dados gerais, lote, obra, casa) e **Financeiro**
(dados gerais, contratos, últimas despesas) —, a pedido do usuário, para nenhum
bloco precisar de rolagem própria. Quem rola é só o `mat-dialog-content`: o
`mat-tab-body-wrapper` precisa de `height: auto` e **tanto `.mat-mdc-tab-body`
quanto `.mat-mdc-tab-body-content`** de `overflow: visible` — tratar só o
`-content`, como foi feito na primeira tentativa, deixa o `mat-tab-body` rolando
e a barra dentro da barra continua. O custo vem de
`RelatoriosService.resultado()` — a mesma fonte da tela de resultado, de
propósito: não existe uma segunda conta de custo no frontend.
`DespesasService.listar()` ganhou `imovelId?` e `DespesaFormDialogData` ganhou
`imovelId?` para a pré-seleção.

### Etapa O — Documentos do imóvel ✅
- [x] Sub-aba "Documentos" no detalhe do imóvel (dentro de Cadastro), com envio,
      filtro por fase, abrir e remover
- [x] `ImovelDocumentoModel` ganha `nomeArquivo`, `descricao`, `dataEmissao` e
      `dataValidade`; `nomeArquivo` vem do próprio upload
- [x] Documento com validade vencida aparece destacado na lista

**Nota da implementação (Ago 2026):** os metadados vão como `@RequestParam` no
POST multipart, junto de `tipoDocumento` e `faseImovel`, sem DTO de request — o
arquivo já obriga o endpoint a ser multipart.

**Duas dívidas confirmadas durante a verificação, ambas anteriores a esta etapa
e valendo igual para fotos e anexos de despesa:**
1. **Arquivo órfão:** remover documento/foto/anexo apaga só o registro; o arquivo
   fica no disco para sempre. `StorageService` não tem operação de exclusão.
2. **URL absoluta gravada no banco:** `ServletUriComponentsBuilder.fromCurrentContextPath()`
   grava o host que atendeu o upload (`localhost:4200` pela tela via proxy,
   `localhost:8080` se o upload for direto no backend). Muda de host ou de porta
   e as URLs antigas quebram; o certo é guardar o caminho relativo e montar a URL
   na leitura.

### Etapa P — Datas: validar no PUT e na edição ✅
- [x] `ImovelService.validarOrdemDatas` checa `compra.data ≤
      dataInicioConstrucao ≤ dataConclusaoObra` e é chamada tanto por `atualizar`
      quanto por `avancarFase`
- [x] Datas incoerentes com a fase atual (conclusão preenchida num lote) deixam de
      passar

**Nota da implementação (Ago 2026):** a validação roda **sobre o estado já
aplicado** pelo mapper, o que a fez servir aos dois caminhos sem duplicar regra —
com isso o `switch` de `dataAnterior` de `avancarFase` virou redundância e foi
apagado, sobrando lá só o guard de `LOTE` como destino, que é outra regra. O
segundo item não precisou de código: `ImovelMapper.updateEntityFromDto` já
aplicava o grupo de construção/casa só quando a fase foi alcançada (ADR-033).

### Etapa Q — Ajustes pontuais do resultado ✅
- [x] `resultadoProvisorio` passa a ser `vendido && fase == CONSTRUCAO`
      (ADR-038) — lote revendido sem obra tem resultado definitivo
- [x] Guarda contra `fase_imovel` nula no `groupingBy` de `resultadoImovel`
- [x] `identificador` único no banco e checagem case-insensitive
- [~] Valor de compra na composição por fase — **descartado**: a tela de
      resultado já mostra a linha "Compra do lote" a partir de `valorCompra`, e
      empurrar a compra para dentro de `despesasPorFase` duplicaria o número no
      mesmo quadro

**Nota da implementação (Ago 2026):** o identificador único ficou num **índice
funcional** (`ux_imovel_identificador_lower`, em
`db/manual/2026-08-identificador-imovel-unico.sql`) em vez de `unique = true` na
coluna: a regra é "LOTE-01 e lote-01 são o mesmo imóvel", e a constraint que o
`ddl-auto=update` criaria seria case-sensitive — resolveria outra coisa. O
service usa `existsByIdentificadorIgnoreCase` só para dar a mensagem amigável
antes de o banco reclamar. Cobertura nova em `ImovelServiceTest` (8 testes,
semente da rede de testes das pendências antigas) e em `RelatorioServiceTest`.

## Pendências antigas ainda válidas

Do roadmap anterior ao reescopo, removido daqui por já estar cumprido ou
superado; sobrou o que continua valendo:

- [ ] Testes unitários dos demais services — falta `DespesaService` (fase ×
      etapa de construção). Já existem `RelatorioServiceTest`,
      `ContratoFinanceiroServiceTest` e `ImovelServiceTest`. Teste de integração
      de controller (`MockMvc`) foi descartado: custo alto, cobertura baixa
- [ ] Lançamento de despesa mobile-friendly (uso no canteiro)
- [ ] RF07 — tema configurável: endpoint de config (`ROLE_ADMIN`), 5 paletas
      curadas (ADR-016/ADR-018) e painel para trocar
- [ ] Gráficos do dashboard (Custo Total, Orçado vs. Realizado — ADR-017/Chart.js)

## Módulos pós-MVP (ADR-029)

- [ ] Orçamento por categoria (`orcamentoCategoria/` já existe no código, mas
      está fora do MVP)
- [ ] Cotações e banco de orçamentos de fornecedor — o **cadastro** de fornecedor
      entrou no núcleo (RF08); só as cotações ficaram para depois
- [ ] Diário de obra (clima, equipe, ocorrências, fotos com timestamp)
- [ ] OCR de notas fiscais
- [ ] Alertas de orçamento e de parcelas a vencer (e-mail/push)
- [ ] Trilha de auditoria (quem alterou o quê e quando)
- [ ] Reativação do Flyway com baseline V1 única — risco registrado na ADR-029:
      fica mais caro depois do primeiro imóvel real lançado

## Ajustes de uso diário (Ago 2026) ✅

Doze itens levantados pelo usuário a partir do uso real das telas, entregues em
cinco etapas. Detalhe das decisões nas ADR-034 a ADR-036.

- [x] **E1 — Frontend transversal**: `MatDatepicker` em pt-BR nos 22 campos de
      data (com `DataPtBrAdapter` para aceitar data digitada), diretiva `appMoeda`
      no lugar dos três pares duplicados de formatação, e `floatLabel: 'always'`
      global — que era a causa da label sobrepondo o prefixo "R$"
- [x] **E2 — Despesa**: `EtapaConstrucao` (ADR-035) e tela de visualização
      (`despesa-detalhe-dialog`) no clique da linha, em vez da edição
- [x] **E3 — Contrato financeiro**: `PUT` com histórico protegido (ADR-036),
      gerador de cronograma de parcelas e documentos anexos
- [x] **E4 — Anexo no cadastro**: fotos do imóvel e anexos da despesa passam a ser
      escolhidos antes do primeiro save, subindo logo depois do POST
- [x] **E5 — Fornecedor vira marca em Pessoa** (ADR-034) e menu de Orçamentos
      oculto (a rota e o componente placeholder continuam no código)

**Pendência operacional — encerrada na Etapa H:** a tabela `fornecedor` estava
vazia (nenhum fornecedor chegou a ser cadastrado), então não houve dado a migrar
e o `INSERT`/`UPDATE` de
`backend/src/main/resources/db/manual/2026-08-migrar-fornecedor-para-pessoa.sql`
**não foi executado** — só o `DROP TABLE fornecedor`. As colunas novas em
`pessoa` já existiam via `ddl-auto=update`. O script fica no repositório apenas
como registro; se outro ambiente tiver dados na tabela, rodá-lo lá antes de
dropar.

## Compra parcelada do lote (Ago 2026) ✅ — ADR-037

Escopo restrito a `PARCELAMENTO_COMPRA`.

- [x] `DadosCompra.parcelada`; entrada como parcela nº 0 já baixada;
      `compra_valor` deduzido do cronograma na criação do contrato
- [x] Ajuste de quitação no custo, calculado contra o **principal** em aberto
- [x] Bloco Desembolso (`totalDesembolsado`, `saldoAPagar`) no resultado
- [x] Cadastro do imóvel com toggle à vista/parcelado; na criação parcelada o
      valor do lote não é pedido, e o contrato abre pré-preenchido em seguida
- [x] Linha de reconciliação no contrato, com distribuição de juros só quando a
      diferença existe
- [x] 10 testes novos (`RelatorioServiceTest`, `ContratoFinanceiroServiceTest`)

**Pendência conhecida, fora do escopo por decisão:** a mesma matemática do ajuste
de quitação valeria para `FINANCIAMENTO_CONSTRUCAO`, onde a parte de juros
embutida no valor negociado da quitação se perde hoje.
