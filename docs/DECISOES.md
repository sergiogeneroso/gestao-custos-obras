# Decisões Arquiteturais (ADRs)

## ADR-001 — Package by feature
Com 6+ domínios, organizar por camada espalharia cada funcionalidade por
4-5 pastas. Decisão: package by feature (vertical slice), `shared/` como
exceção para código transversal.

## ADR-002 — Soft delete para Imóvel e Aportante
Imóveis e aportantes podem ter despesas/pagamentos vinculados; DELETE físico
quebraria integridade referencial. Decisão: campo `ativo BOOLEAN`, listagens
via `findByAtivoTrue()`, DELETE no controller chama `inativar()`.

## ADR-003 — Nome definitivo: Aportante (não Envolvido)
"Envolvido" era genérico demais. "Aportante" reflete quem aporta recurso
financeiro — termo já usado no requisito RF05.

## ADR-004 — Nome definitivo: EtapaProjeto (não Etapa)
"Etapa" colide com nomes comuns em Java e é ambíguo. "EtapaProjeto" evita a
ambiguidade sem restringir a fases só de construção.

## ADR-005 — EtapaProjeto como catálogo global
Etapas se repetem em todos os imóveis. `etapa_projeto` é tabela independente,
sem FK para `imovel`. Trade-off aceito: não é possível ter etapa exclusiva
de um imóvel.

## ADR-006 — DespesaPagamento separado de Despesa
RF04 exige tanto "um aportante pagou tudo" quanto rateio entre sócios.
Tabela `despesa_pagamento` com FK para `despesa` e `aportante`; soma de
`valor_pago` não pode exceder `despesa.valor` (validado no service).

## ADR-007 — BigDecimal para valores monetários
`double`/`float` causam erro de arredondamento inaceitável para dinheiro.
`BigDecimal` (Java) + `NUMERIC(14,2)` (PostgreSQL); proibidos para dinheiro.

## ADR-008 — Mapper manual (sem MapStruct)
Projeto pequeno, mapeamentos simples — MapStruct adicionaria dependência e
complexidade de build desnecessárias. Pode migrar depois se a complexidade
crescer.

## ADR-009 — Angular Material para o frontend
Framework oficial do Angular, bem mantido, boa integração. Alternativas
avaliadas: PrimeNG, TailwindCSS, Bootstrap.

## ADR-010 — Flyway para migrações
Controle de versão do schema. Migrations em
`backend/src/main/resources/db/migration/`; nunca editar uma já aplicada.

## ADR-011 — Documentação dividida por ferramenta (Ago 2026)
Projeto usa Claude Code e Antigravity. Decisão: `AGENTS.md` na raiz como
fonte única de regras cross-tool; regras específicas por caminho em
`.agents/rules/` (symlink em `.claude/rules/`); skills em `.agents/skills/`
(symlink em `.claude/skills/`); `docs/` para referência sob demanda, nunca
importada via `@` no CLAUDE.md (importar carrega no contexto toda sessão,
o que anula o ganho de token).

## ADR-012 — Flyway por @Bean manual no Spring Boot 4.1 (Ago 2026)
O Spring Boot 4.1 removeu a `FlywayAutoConfiguration` (e `FlywayProperties`)
do `spring-boot-autoconfigure` — não há mais starter nem auto-config para
Flyway; só o BOM gerencia a versão do `flyway-core`. Consequência: ter
`flyway-core` no pom.xml **não** roda migrações nem cria
`flyway_schema_history` — o schema fica só por conta do Hibernate.
Decisão: declarar `Flyway` via `@Bean` em `shared/config/FlywayConfig.java`,
rodar `migrate()` num bean que o `entityManagerFactory` dependa (via
`BeanDefinitionRegistryPostProcessor`) e usar `ddl-auto=validate` (o Flyway
passa a ser a única fonte de schema). A recriação do DB aplicou V1–V6 limpas
(oportunidade para eliminar o `DatabaseCleanupRunner`, que visava limpar
mudanças pré-ADR-003/004). Upgrade:
se o Spring Boot reintroduzir auto-config/starter Flyway, remover
`FlywayConfig` e `spring.flyway.enabled=false` voltando à config nativa.
**Pausado por ADR-013** — ver abaixo; o desenho continua válido para quando
for reativado.

## ADR-013 — Pausa do Flyway durante a modelagem do MVP (Ago 2026)
Projeto em fase inicial, uso solo, sem dado relevante em disco, e a
modelagem de dados do MVP ainda não fechou. Escrever uma migration a cada
ajuste de coluna nesse estágio é atrito pago duas vezes: uma vez agora, outra
no squash quando o modelo estabilizar. Decisão: pausar o Flyway (`FlywayConfig`
sem `@Configuration`, `ddl-auto=update` no lugar de `validate`) e deixar o
Hibernate gerir o schema direto pelas entidades enquanto a modelagem estiver
em fluxo. Reativar (religar `@Configuration`, voltar `ddl-auto=validate`,
escrever uma `V1` baseline única refletindo o modelo final) antes de existir dado
real que importe ou mais alguém mexendo no projeto — retrofitar migrations num
banco já povoado é bem mais caro que começar com elas desde o primeiro dado real.

**Atualização (Ago 2026):** as migrations de schema (V1, V3, V4, V5) foram
**apagadas**, revertendo o "ficam inertes, não são apagadas" da redação
original. Depois do reescopo elas descreviam tabelas que já não existem
(`aportante`, `despesa_pagamento`, `etapa_projeto`) e seriam integralmente
reescritas na baseline de qualquer maneira — mantê-las só oferecia um retrato
enganoso do schema a quem abrisse a pasta. Sobrou `V6__seed_usuario_admin.sql`,
que é dado, não schema. Enquanto a pausa durar, o que o `ddl-auto=update` não faz
sozinho vive em `db/manual/` (ver `.agents/rules/banco-e-migrations.md`), e é o
efeito acumulado desses scripts que a `V1` baseline precisará conter.

## ADR-014 — Frontend no mesmo repositório, Angular standalone sem NgRx (Ago 2026)
Decisões tomadas antes de iniciar a geração do frontend (Etapa 9 do roadmap).
**Localização:** `frontend/` na raiz do mesmo repositório (monorepo), em vez
de repositório separado — projeto solo, um só clone/histórico é mais simples
de manter do que sincronizar dois repositórios. **Standalone components:**
Angular moderno abandonou `NgModule` como padrão desde a v17; manter módulos
aqui seria carregar boilerplate que o próprio framework já descontinuou.
**Sem NgRx:** avaliado e descartado pelo mesmo raciocínio do ADR-008
(MapStruct) — projeto pequeno, CRUDs simples, estado local via `services` +
Angular `signals` resolve sem a complexidade de um store dedicado. Detalhes
de estrutura de pastas, autenticação (JWT em `localStorage`, interceptor
funcional, `authGuard`) e design (tema Material customizado, listagem de
imóveis em cards com foto de capa, desktop-first): `docs/FRONTEND.md`.

## ADR-015 — Backend movido para `backend/`, monorepo simétrico (Ago 2026)
O repositório nasceu só como backend (Spring Boot na raiz: `pom.xml`, `src/`,
`mvnw`). Ao decidir por `frontend/` na raiz (ADR-014), a estrutura ficou
assimétrica — arquivos do backend soltos na raiz, frontend contido numa
pasta. Avaliado o custo de mover agora (rename de ~80 arquivos rastreados,
mais ~15 referências de path em `.agents/rules/`, `docs/` e na skill
`gerar-crud-dominio`) contra o custo de mover depois, com o projeto maior:
decisão foi pagar o custo agora. Backend movido para `backend/` (git detectou
os renames automaticamente, histórico preservado); `frontend/` já nasce no
lugar certo quando for gerado. Comando de teste passa a ser
`cd backend && ./mvnw test`. Efeito colateral descoberto durante a migração:
`backend/src/main/resources/application.properties` estava — e continua —
rastreado no git desde o commit inicial, contrariando a própria regra de
`.agents/rules/seguranca.md`; risco baixo (credenciais são só de dev local:
`postgres`/`admin` em `localhost`, secret JWT com sufixo `dev-only`), mas
fica registrado aqui para decisão futura sobre destrackear/purgar do
histórico.

## ADR-016 — Tema configurável via painel admin, paletas curadas (Ago 2026)
RF07: admin pode trocar a paleta de cores do sistema, valendo pra todos os
usuários. **Persistência:** backend (não `localStorage`) — se ficasse só no
navegador de quem trocou, os demais usuários não veriam a mudança; precisa de
um endpoint de configuração simples (par chave-valor, não é um domínio CRUD
completo). **Mecanismo:** paletas pré-definidas (curadas), não color picker
livre — evita admin quebrar contraste/acessibilidade do tema Material sem
querer. Quatro paletas fechadas nesta sessão (hex e detalhes em
`docs/FRONTEND.md`): Azul corporativo (default), Terracota industrial, Verde
financeiro, Grafite + âmbar — cada uma com light e dark mode (ADR-014 já
previa toggle claro/escuro). **Efeito colateral:** esse endpoint de config
precisa ser admin-only, o que antecipa uma fatia pequena da RBAC que RNF01
tinha deixado pra fase 2 (`.hasRole("ADMIN")` só nesse endpoint específico,
não a regra geral por domínio). Ver ADR-018 — Nocturne entra como paleta
default, ocupando o lugar do Azul corporativo.

## ADR-017 — Tipografia Inter e ícones Lucide (Ago 2026)
Continuação das decisões de design visual (após ADR-016). **Tipografia:**
Inter no lugar da Roboto padrão do Material — sans-serif com boa legibilidade
em tabelas de números (uso pesado no projeto: valores monetários, m²),
comum em produtos financeiros/SaaS. Self-hosted via `@fontsource/inter` em
vez de Google Fonts via CDN, mesma lógica de não depender de terceiro em
runtime. **Ícones:** Lucide no lugar do Material Icons/Symbols — set mais
moderno/minimalista, via `lucide-angular` (wrapper oficial). Convive com
`mat-icon` só onde componentes do Material exigem ícone Material
internamente; nas telas de domínio o uso é direto via `lucide-angular`.
**Gráficos:** Chart.js direto, sem wrapper Angular (`ng2-charts`,
`ngx-echarts`) — os três gráficos do dashboard (RF05: Custo Total, R$/m²,
Orçado vs. Realizado) são simples o bastante pra não justificar mais uma
camada de integração; usado via `ViewChild` num `<canvas>`. Detalhes:
`docs/FRONTEND.md`. Ver ADR-018 — ícones migram de Lucide para Phosphor.

## ADR-018 — Adoção do design system Nocturne como tema default (Ago 2026)

Um design system chamado **Nocturne** foi importado via claude_design MCP
com templates já desenhados especificamente para este domínio (login,
dashboard interno, landing) — mais específico e completo do que a referência
de layout genérica (Berry/Flat Able/Gradient Able) usada até então. Emenda
pontos do ADR-009/016/017 sem invalidá-los:

**Paleta (emenda ADR-016):** Nocturne passa a ser a paleta **default**,
no lugar de Azul corporativo — que continua existindo como opção curada
normal para quando RF07 (troca de paleta pelo admin) for implementado.
Nocturne é um tema fixo dark-only (bg `#161826`, surface `#232532`, texto
`#e9e9ed`, accent `#9184d9`) — exceção documentada à regra do ADR-016 de
"cada paleta precisa de par light/dark": não existe variante light
desenhada para o Nocturne, então selecioná-lo no futuro painel admin
implica dark mode.

**Ícones (emenda ADR-017):** Phosphor substitui Lucide. Sem wrapper Angular
oficial (ao contrário do `lucide-angular`) — usado direto via classe CSS do
pacote `@phosphor-icons/web`.

**Forma e tipografia:** raio de 8px, densidade compacta e botões primários
sempre outline (nunca preenchidos) passam a ser a linha de base do tema
Material — como o Material só expõe cor por paleta (não forma/densidade),
esses três aspectos são globais, independentes de qual paleta de cor está
ativa. Tipografia Inter do ADR-017 (nunca chegou a ser implementada) entra
junto.

**Componentes (ADR-009 não muda):** Angular Material continua sendo o
framework de componentes. A cor exata do Nocturne é aplicada via override
dos tokens de sistema M3 (`--mat-sys-*`), não por paleta nomeada do
Material — nenhuma das paletas nomeadas (`$violet-palette` etc.) bate com a
saturação baixa do accent do Nocturne.

**Escopo desta sessão:** aplicado na tela de login, no shell interno (RF05
Dashboard incluído) e numa landing page nova antes do login — RF07 (troca
de paleta em runtime) continua pendente, sem mudança nesta ADR.

---

# Reescopo do projeto (Ago 2026)

As ADRs de 019 a 029 foram tomadas numa mesma sessão de reformulação, quando o
objetivo do projeto foi reescrito. Elas substituem várias decisões anteriores;
cada uma diz qual e por quê.

## ADR-019 — Reescopo: do "custos de obras" para o ciclo de vida financeiro do imóvel (Ago 2026)

O objetivo original — gestão de despesas de obras — era genérico demais para um
MVP. O projeto tentava cobrir orçamento por etapa, rateio entre sócios,
relatórios de obra e custos de construção ao mesmo tempo, sem que nenhum desses
eixos estivesse completo.

**Decisão:** o objetivo passa a ser acompanhar o **resultado financeiro de cada
imóvel do começo ao fim** — compra do lote, custos ao longo de todo o ciclo de
vida (inclusive construção, quando houver) e venda. O que ficar fora desse eixo
evolui como módulo posterior, não como parte do MVP (ver ADR-029).

**Consequência deliberada:** o pacote raiz (`com.seegeneroso.gestao_custos_obras`)
e o nome do repositório **não** mudam, apesar do reescopo. Renomear custaria
todos os imports do projeto sem nenhum ganho funcional.

## ADR-020 — Ciclo de vida do imóvel em dois eixos ortogonais (Ago 2026)

Substitui a modelagem de `TipoImovel` (LOTE/IMOVEL) e `StatusImovel`
(PLANEJAMENTO/CONSTRUCAO/FINALIZADO), que tratavam a natureza do imóvel como um
atributo fixo e misturavam natureza com situação comercial no mesmo campo.

No negócio real, compra-se um **lote**, que pode ficar anos nessa condição
gerando despesas próprias; esse mesmo lote pode virar uma **construção**, com
despesas específicas; e termina como **casa**. É sempre o mesmo imóvel mudando de
fase, e construir é opcional — parte da carteira é comprada e revendida sem obra.

**Decisão:** dois campos independentes.

- `fase` (`FaseImovel`: LOTE → CONSTRUCAO → CASA), que **só avança**, nunca
  retrocede. Todo imóvel começa como lote: não se compra imóvel pronto para
  reformar neste negócio.
- `situacao` (`SituacaoImovel`: ADQUIRIDO ⇄ A_VENDA → VENDIDO).

São ortogonais de propósito: um lote pode estar à venda, e a venda pode ocorrer
**em qualquer fase** — inclusive na planta (LOTE + VENDIDO) ou com a obra em
andamento (CONSTRUCAO + VENDIDO). **Vender não congela a fase**: a construção
continua avançando depois da venda e novas despesas de obra ainda chegam. Por
isso o resultado de um imóvel vendido com obra pendente é **provisório**, e o
relatório o marca como tal, mostrando apenas o realizado, sem projetar lucro.

**Datas de transição:** o imóvel grava `dataInicioLote`, `dataInicioConstrucao` e
`dataConclusaoObra`. O enum sozinho diria onde o imóvel está, mas nunca quando
ele entrou em cada fase — e esse é um dado que **não pode ser reconstruído
depois**. Sem ele, "quanto tempo ficou parado como lote" e "quanto durou a obra"
ficariam perdidos para sempre.

Essas datas marcam **quando o fato aconteceu**, não quando foi lançado: são
sempre informadas pelo usuário na ação de transição, com a data de hoje entrando
apenas como sugestão no formulário, e permanecem editáveis depois. Lançamento
retroativo é comum — a obra pode ter terminado semanas antes de alguém registrar.
São obrigatórias (nenhuma transição sem data) e validadas em ordem crescente,
porque data fora de ordem produziria tempo negativo por fase no relatório.

**Projeção:** `custoEstimadoObra` e `previsaoConclusao` no imóvel permitem
comparar estimado com realizado sem trazer de volta o módulo de orçamento por
etapa (ADR-029).

**Transições são ações próprias**, não parte do PUT de cadastro:
`PATCH /api/imoveis/{id}/fase` e `PATCH /api/imoveis/{id}/situacao`. Como elas
gravam as datas que alimentam tempo por fase e rentabilidade, deixá-las num PUT
genérico seria convite a gravar data errada sem ninguém perceber.

## ADR-021 — Pessoa (PF/PJ) substitui Aportante (Ago 2026)

Substitui a ADR-003, que havia fixado "Aportante" como nome definitivo.

O conceito não corresponde ao negócio: não existem pessoas que "aportam recurso"
como papel de cadastro. Existem **pessoas** — físicas ou jurídicas — que se
relacionam com cada despesa em papéis distintos: quem é responsável pelo
pagamento e quem recebeu o pagamento. A mesma pessoa jurídica pode aparecer em
papéis diferentes: vender o lote numa operação e fornecer material em outra.

**Decisão:** o domínio `aportante/` vira `pessoa/`, com `tipoPessoa`
(FISICA/JURIDICA) e `documento` (CPF ou CNPJ, único). O campo
`tipoParticipacao` deixa de existir — o papel vem do uso, não do cadastro. Uma
tabela só, sem herança JPA e sem tabela de papéis.

O documento é obrigatório e único, mas **sem validação de dígito verificador** —
marcado com comentário `ponytail:` no service; entra se dado sujo incomodar.

## ADR-022 — Fornecedor por composição com Pessoa, não por herança JPA (Ago 2026) ⚠️ SUPERADA pela ADR-034

Fornecedor é um domínio próprio, e todo fornecedor é uma pessoa. A modelagem
literal seria `Fornecedor extends Pessoa` (herança JOINED ou SINGLE_TABLE).

**Decisão: composição.** `FornecedorModel` tem uma FK para `PessoaModel` mais
seus campos próprios (`areaAtuacao`, `observacoes`).

**Motivo:** o JPA não permite mudar o tipo de uma entidade já persistida. Com
herança, uma linha gravada como Pessoa não poderia ser promovida a Fornecedor
depois sem recriar o registro com outro id — e nesse negócio isso dói, porque a
mesma PJ que vendeu o lote pode passar a fornecer material.

**Consequência:** `despesa.beneficiario` referencia **Pessoa**, não Fornecedor.
Assim também cobre quem recebeu sem ter cadastro de fornecedor — o vendedor do
lote, o banco, o diarista. O relatório de histórico por fornecedor navega
pessoa → fornecedor.

## ADR-023 — Despesa: pagador único, beneficiário, fase e imóvel opcional (Ago 2026)

Substitui a ADR-006 (`DespesaPagamento` separado para rateio entre aportantes).

O rateio nunca foi usado e era a regra de negócio mais frágil do projeto — a
validação "soma dos pagamentos ≤ valor da despesa" existia em dois lugares no
service e era o ponto onde um erro custaria dinheiro.

**Decisão:** a despesa passa a ter `pagador` (Pessoa, obrigatório) e
`beneficiario` (Pessoa, opcional — não travar o lançamento rápido no canteiro
quando não se sabe o fornecedor na hora). A tabela `despesa_pagamento` e a
validação da soma desaparecem. Dividir um custo entre duas pessoas passa a ser
dois lançamentos.

**Fase:** a despesa guarda `faseImovel`, a fase **em que foi incorrida** — não a
fase atual do imóvel — para que lançamento retroativo caia no lugar certo. É
preenchida com a fase atual do imóvel quando o request não informa.

**Imóvel opcional:** existem gastos que não pertencem a nenhum imóvel — contador,
combustível, ferramentas e equipamentos reutilizáveis (betoneira, andaime). O
vínculo com imóvel passa a ser anulável: despesas sem imóvel são gastos gerais,
aparecem em visão própria e **não entram no custo de nenhum imóvel**, nem são
rateadas entre eles.

**Mão de obra** é despesa avulsa: cada diária ou medição é um lançamento próprio,
com a pessoa como beneficiária. Descartada a ideia de contrato de empreitada com
saldo a pagar — o histórico por fornecedor já responde quanto foi pago a cada um.

Uma mesma compra que sirva a dois imóveis é **dividida à mão** em dois
lançamentos; não há rateio automático entre imóveis. Despesa aceita **apenas
valor positivo**; devolução de material se resolve editando ou inativando o
lançamento original.

## ADR-024 — Compra, venda e meta de venda embutidas no Imóvel (Ago 2026)

Com o reescopo, compra e venda passam a ser o centro do sistema, e não mais um
campo solto `valorAquisicaoInicial`.

**Decisão:** dois `@Embeddable` no próprio imóvel — `DadosCompra` (valor, data,
vendedor) e `DadosVenda` (valor, data, comprador, valor pretendido).

**Alternativas descartadas:** uma entidade `Negociacao` separada (acrescentaria
um join a todo relatório sem contrapartida, já que **não há desmembramento de
lote nem permuta** neste negócio — um imóvel entra por uma compra e sai por uma
venda, ele inteiro); e tratar tudo como lançamento financeiro com sinal (perderia
os campos próprios de imóvel e misturaria semânticas na mesma tela).

**Mitigação do risco:** os campos ficam agrupados em `@Embeddable` justamente
para que, se um dia a entidade `Negociacao` for necessária, a extração seja
mecânica em vez de cirurgia no modelo inteiro.

O valor pretendido de venda alimenta o ponto de equilíbrio no relatório de
resultado, sem exigir um módulo de orçamento.

## ADR-025 — Contratos financeiros encadeados e a regra de custo (Ago 2026)

É o ponto mais sutil do modelo, e o que uma primeira versão desta reformulação
errou: modelou-se "parcelas da compra" quando o negócio encadeia **vários
contratos financeiros ao longo da vida do mesmo imóvel**.

Financiamento e parcelamento são **opcionais em cada etapa**: a compra pode ser à
vista ou parcelada, a obra pode sair de recurso próprio ou de financiamento, e a
venda pode ser à vista ou com entrada mais parcelas. Um imóvel comprado à vista,
construído com recurso próprio e vendido à vista **não tem contrato nenhum**, e
esse é um caso normal — para ele, custo = compra + despesas, sem juros, sem saldo
devedor e sem bloco de caixa no relatório.

Quando há contratos, porém, um mesmo imóvel encadeia vários. Um cenário real e
completo: o lote é comprado **parcelado direto com o vendedor**; para obter o
**financiamento de construção do banco**, é preciso **quitar antecipadamente**
esse parcelamento, porque o banco exige o terreno livre para dar em garantia; a
obra corre sob o financiamento; e a estratégia é vender assim que fica pronto,
**quitando o financiamento à vista**.

**Decisão:** `ContratoFinanceiroModel` (imóvel, tipo — PARCELAMENTO_COMPRA /
FINANCIAMENTO_CONSTRUCAO / PARCELAMENTO_VENDA —, contraparte, valor contratado,
situação ATIVO/QUITADO, data e **valor de quitação**) com
`ParcelaContratoModel` filha (número, vencimento, valor, valor de juros, data e
valor de pagamento).

A quitação antecipada tem valor próprio, negociado, **independente da soma das
parcelas em aberto** — e encerrá-la não altera os valores originais das parcelas,
para que o histórico do que foi contratado continue legível.

**Sem validação de soma das parcelas contra o valor contratado**: juros fazem a
soma exceder o principal legitimamente, e uma regra assim quebraria em uso normal.

### A regra de custo (não quebrar)

A distinção que sustenta tudo: **custo** é o que o imóvel consumiu de recursos —
a obra custou o que custou, tenha o dinheiro vindo do banco ou do bolso —,
enquanto **caixa** é quando o dinheiro entra e sai. Financiar muda *quando* se
paga, não *quanto* o imóvel custou.

- **Custo do imóvel** = valor de compra + despesas do imóvel (todas as fases,
  incluindo os custos acessórios do financiamento e o imposto sobre o ganho) +
  juros efetivamente pagos nas parcelas, quando houver contrato.
- **As prestações do financiamento NÃO são despesa.** O dinheiro liberado já
  pagou despesas que foram lançadas; devolvê-lo ao banco não é custo novo.
  Contar os dois dobraria o custo. Pagar prestação é dar baixa na parcela.
- **O saldo devedor quitado na venda NÃO é custo.** É devolução do principal que
  pagou despesas já contadas. Aparece no relatório como posição de caixa, nunca
  somado ao custo.
- **Na venda parcelada vale o espelho:** a receita é o valor da venda, registrado
  na venda; receber cada parcela é caixa entrando, não receita nova.
- **Gastos gerais (despesa sem imóvel) não entram no custo de imóvel nenhum.**

Exemplo: lote 100k + obra 200k lançada como despesa + 3k de vistorias, com
financiamento de 200k quitado por 205k na venda de 380k. Custo = 100 + 200 + 3 +
5 (juros) = **308k**, lucro **72k**. Somar a quitação ao custo daria 513k e um
prejuízo inexistente de 133k.

Num imóvel sem contrato nenhum, a regra encolhe para: custo = compra + despesas.

Como a estratégia é quitar o financiamento à vista na venda, os juros tendem a
ser pequenos; o que pesa de verdade são os **custos acessórios** — vistoria de
engenharia a cada medição, avaliação, tarifas, seguro e registro da hipoteca.
Eles são lançados como despesas comuns, em categoria própria, com a instituição
como beneficiária e uma FK opcional para o contrato, o que permite responder
"quanto me custou usar esse financiamento" sem nenhuma entidade nova.

O imposto sobre o ganho na venda também é lançado como despesa manual, em
categoria própria: cálculo automático dependeria de PF ou PJ, do regime e de
isenções, virando regra fiscal dentro do sistema, que envelhece mal.

**Descartado:** registrar as liberações do banco por medição — não mudam nem o
custo (que são as despesas) nem a dívida (que é o contrato).

## ADR-026 — `EtapaProjeto` vira `CategoriaDespesa` (Ago 2026)

Substitui a ADR-004 (nome definitivo `EtapaProjeto`) e mantém a ADR-005 (catálogo
global, sem FK para imóvel).

"Etapa" é um nome enviesado para obra, e o MVP precisa cobrir igualmente o imóvel
comprado e revendido sem construção nenhuma. Além disso, com a ADR-020 a **fase
do imóvel** passou a ser o eixo temporal do custo — sobrepondo-se ao que "etapa"
sugeria.

**Decisão:** o catálogo global passa a se chamar `CategoriaDespesa` e responde
apenas pela **natureza do gasto**: Aquisição, ITBI/Escritura, Documentação, IPTU,
Material, Mão de obra, Custos de financiamento, Corretagem, Impostos sobre a
venda. Sem hierarquia de categorias — uma FK auto-referente resolve isso depois,
se necessário.

## ADR-027 — Anexos tipados e múltiplos em imóvel e despesa (Ago 2026)

Substitui o campo `despesa.comprovante_url` (um único arquivo, sem tipo), que não
permitia guardar o comprovante de pagamento **e** a nota fiscal do mesmo gasto,
nem listá-los separadamente.

**Decisão:** duas tabelas filhas explícitas, espelhando o padrão que
`ImovelFotoModel` já estabeleceu e reusando o `StorageService`:

- `DespesaAnexoModel`, com `tipoAnexo` (COMPROVANTE / NOTA_FISCAL / RECIBO /
  CONTRATO / OUTRO). `RECIBO` existe porque mão de obra raramente vem com nota.
- `ImovelDocumentoModel`, com `tipoDocumento` (MATRICULA / ESCRITURA / CONTRATO /
  IPTU / ALVARA / PROJETO / ART / HABITE_SE / OUTRO) e a fase a que pertence —
  matrícula e IPTU na fase lote, alvará e ART na construção, habite-se na casa.

**Duas tabelas, não uma polimórfica:** são estruturalmente parecidas, mas uma
tabela `anexo` genérica com discriminador custaria mais em consulta e constraint
do que economizaria. O tipo é enum com `OUTRO` de escape, não catálogo CRUD:
filtro confiável sem mais um domínio para manter.

## ADR-028 — Soft delete estendido à Despesa (Ago 2026)

Revoga a decisão anterior de que `DespesaService.deletar()` faria DELETE físico
até existir um módulo de auditoria. Aquela decisão foi tomada quando não havia
dado real no sistema.

**Motivo:** com dinheiro de verdade lançado, um relatório fechado no mês passado
pode mudar sozinho se alguém apagar uma despesa antiga, e um lançamento apagado
por engano não volta.

**Decisão:** `Despesa` ganha `ativo BOOLEAN` como `Imovel`, `Pessoa` e
`Fornecedor`, e `deletar()` vira `inativar()`. A regra "nunca DELETE físico em
entidade financeira" passa a valer sem exceção. Trilha completa de alterações
(quem mudou o quê e quando) continua fora do escopo enquanto o uso for solo.

## ADR-029 — Escopo do MVP e o que significa "módulo" (Ago 2026)

**Núcleo do MVP:** imóvel com ciclo de vida, compra e venda, contratos
financeiros, pessoas, fornecedores, categorias, despesas, anexos, e os relatórios
de resultado por imóvel, histórico por fornecedor, extrato por pessoa e carteira.

**Fora do MVP, como módulos:** orçamento por etapa (o domínio `orcamentoEtapa/`
continua no repositório, mas sai do roadmap e das telas), cotações e banco de
orçamentos de fornecedor, diário de obra, OCR de notas fiscais, alertas de
orçamento e trilha de auditoria.

**"Módulo" aqui não significa infraestrutura nova.** Continua sendo "um novo
pacote em package by feature", exatamente como a ADR-001 já define — sem Spring
Modulith, sem microserviço, sem plugin system.

**Casos de uso avaliados e descartados nesta sessão**, registrados para não serem
reabertos sem necessidade: contrato de empreitada com saldo a pagar, receitas
fora da venda (aluguel, sinal retido de venda desfeita, venda de sobras), rateio
automático de despesa entre imóveis, compra de imóvel pronto para reforma,
cálculo automático de imposto sobre o ganho, orçamento por categoria e liberações
do banco por medição.

**Sobre o Flyway:** a ADR-013 previa reativá-lo quando a modelagem do MVP
fechasse, e este reescopo é esse momento. A decisão nesta sessão foi **manter
pausado**, com o risco registrado: a partir do primeiro imóvel real lançado com
`ddl-auto=update`, retrofitar a baseline num banco povoado fica mais caro.

## ADR-030 — Área do lote e área construída são campos distintos (Ago 2026)

`ImovelModel` tinha um único campo `area`, herdado da modelagem anterior, em que
"lote" e "imóvel" eram tipos fixos e mutuamente exclusivos. Com o ciclo de vida
da ADR-020 (`LOTE → CONSTRUCAO → CASA` no mesmo registro), um campo só passou a
misturar duas medidas diferentes: a metragem do terreno, conhecida na compra, e a
metragem construída, que só existe a partir da obra e nem sempre é conhecida
quando ela começa.

**Decisão:** `area` vira `areaLote` e o imóvel ganha `areaConstruida`, ambas
opcionais. Nenhuma das duas é obrigatória em nenhuma transição de fase —
lançamento retroativo e obra iniciada sem projeto fechado são normais aqui.

**Consequência nos relatórios:** `custo-por-m2` passa a devolver dois
indicadores. O `custoPorM2` continua sendo as despesas do imóvel sobre a área do
lote, e entra `custoObraPorM2` — despesas da fase `CONSTRUCAO` sobre a área
construída, que é a métrica comparável entre obras. Dividir o custo total pela
área do lote, como era feito antes, produzia um número sem significado para um
imóvel com casa pronta.

**Migração:** com o Flyway pausado (ADR-013), o `ddl-auto=update` criou as
colunas novas sem remover `area`; os dados foram copiados para `area_lote` e a
coluna antiga foi derrubada à mão no banco de desenvolvimento.

## ADR-031 — Propriedades por fase agrupadas em `@Embedded` (Ago 2026)

O `ImovelModel` nasceu com os campos financeiros do ciclo e quase nenhum dado
**documental**: os documentos existiam só como arquivo anexado
(`ImovelDocumentoModel`), sem nenhum número pesquisável. Não dava para conferir
um IPTU lançado, achar o imóvel pela matrícula ou saber se o alvará venceu sem
abrir PDF.

**Decisão:** cada fase do ciclo ganha suas propriedades próprias, agrupadas em um
`@Embedded` dedicado dentro de `imovel` — `DadosLote`, `DadosConstrucao` e
`DadosCasa` —, seguindo o padrão que `DadosCompra` e `DadosVenda` já usam:

- **`DadosLote`**: matrícula, cartório/CRI e data do registro; inscrição
  municipal (IPTU); e a área do lote, que passa a morar aqui mantendo a coluna
  `area_lote`.
- **`DadosConstrucao`**: área construída, data de início, previsão de conclusão e
  custo estimado (colunas já existentes); número, emissão e validade do alvará;
  número da ART/RRT e o responsável técnico (FK para `Pessoa`, reusando o
  cadastro existente); CNO — a matrícula da obra na Receita, sem a qual não se
  emite a CND para averbar.
- **`DadosCasa`**: data de conclusão da obra (coluna já existente); número e data
  do habite-se; data de averbação da construção na matrícula; e as
  características de venda — quartos, suítes, banheiros e vagas.

**O endereço fica todo na raiz do imóvel, no formato convencional**: logradouro,
número, bairro, cidade, UF, CEP e uma observação livre para complemento ou ponto
de referência. Chegou a ser considerado decompor a localização em
loteamento/quadra/lote dentro de `DadosLote`, e isso foi descartado: quem
identifica o imóvel juridicamente é a matrícula, o endereço serve para achar o
imóvel no mundo, e ele vale igual em todas as fases.

**Uma tabela, não três.** Tabelas 1:1 por fase (`imovel_lote`, …) normalizariam
os nulos das fases não atingidas, mas custariam três joins, três repositórios e
ciclo de vida próprio num sistema de uso solo com dezenas de imóveis. O
agrupamento lógico que se queria vem do `@Embedded`, sem esse preço. JSONB foi
descartado por perder tipagem, validação e consulta.

**Cuidado herdado:** o Hibernate devolve `null` para um `@Embedded` com todas as
colunas nulas quando ele tem associação `@ManyToOne` — foi o que motivou os
getters manuais de `compra`/`venda` em `ImovelModel`. `DadosConstrucao` tem
`responsavelTecnico`, então precisa do mesmo tratamento.

## ADR-032 — A data da compra é o marco inicial do ciclo (Ago 2026)

`ImovelModel` tinha `dataInicioLote` (obrigatória) e `compra.data` (opcional)
como campos distintos. Neste negócio eles são o mesmo fato: todo imóvel começa
como lote (ADR-020) e entra na carteira quando é comprado. Manter os dois
permitia divergência silenciosa — um imóvel cuja carteira começa numa data e cuja
compra aconteceu em outra —, e o `RelatorioService` usava `dataInicioLote` para
dias em carteira e início da fase LOTE, enquanto a tela mostrava a data da compra.

**Decisão:** `dataInicioLote` é removida. `compra.data` passa a ser obrigatória e
vira o marco inicial da carteira e da fase LOTE. Onde o código lia
`getDataInicioLote()`, passa a ler `getCompra().getData()`.

**Consequência:** a ordem coerente das datas de transição vira
`compra.data ≤ dataInicioConstrucao ≤ dataConclusaoObra`.

## ADR-033 — O cadastro pede só o que é do lote (Ago 2026)

O formulário de imóvel pedia, na criação, campos que só existem em fases
futuras: área construída, custo estimado da obra, previsão de conclusão, datas de
construção e valor de venda pretendido. Como todo imóvel nasce lote, esses campos
apareciam vazios e sem sentido no único momento em que o cadastro acontece — e
convidavam a registrar estimativa como se fosse fato.

**Decisão:** cada propriedade é pedida no momento do fluxo a que pertence.

- **Cadastro (`POST`)**: identificador, localização, dados registrais e
  tributários do lote, área do lote, compra (valor, data, vendedor) e descrição.
- **`PATCH /fase` para `CONSTRUCAO`**: além da data, os dados da construção —
  área construída, custo estimado, previsão, alvará, ART e CNO.
- **`PATCH /fase` para `CASA`**: além da data, habite-se, averbação e as
  características do imóvel.
- **`PATCH /situacao` para `A_VENDA`**: o valor de venda pretendido, que hoje é
  uma troca de situação sem formulário nenhum.

Os campos continuam **editáveis depois** pelo `PUT` de cadastro, que passa a
expor apenas as fases já alcançadas pelo imóvel — corrigir dado lançado errado
continua sendo possível, conforme a regra de ciclo de vida. Nenhum desses campos
vira obrigatório na transição: alvará sai depois do início da obra, CNO idem, e a
metragem final às vezes só fecha no habite-se.

## ADR-034 — Fornecedor vira uma marca em Pessoa (Ago 2026)

Substitui a ADR-022, que definiu `Fornecedor` como domínio próprio composto com
`Pessoa` por uma FK 1:1. A composição estava correta como modelagem — o erro foi
o domínio existir.

Ao revisar o código em agosto de 2026, `FornecedorModel` provou ser um satélite
1:1 de `PessoaModel` com exatamente dois campos próprios (`areaAtuacao`,
`observacoes`) e **nenhum consumidor no sistema**: `despesa.beneficiario` sempre
apontou para `Pessoa` (consequência já registrada na própria ADR-022), e
`RelatorioService.historicoFornecedor` sempre agregou sobre `Pessoa`. Era um CRUD
completo — model, repository, service, mapper, controller, tela e item de menu —
que ninguém lia.

**Decisão:** `Pessoa` ganha `fornecedor` (boolean), `areaAtuacao` e
`observacoes`; o pacote `fornecedor/` e a tela `fornecedores/` são removidos. Ser
fornecedor passa a ser um **papel marcado no cadastro**, coerente com a ADR-021,
que já dizia que os demais papéis (pagador, beneficiário, vendedor, comprador)
vêm do uso e não do cadastro. A tela de Pessoas ganha o filtro "Só fornecedores".

**Por que uma flag e não só o uso:** diferente dos outros papéis, ser fornecedor
não é dedutível de um lançamento — é uma qualificação que o usuário quer declarar
antes de haver despesa, para achar a pessoa na hora de lançar. Os dois campos
próprios do papel são preenchidos apenas quando a marca está ligada, e são
limpos ao desmarcá-la.

**Consequência operacional:** com o Flyway pausado (ADR-013), `ddl-auto=update`
cria as colunas novas mas nunca apaga a tabela `fornecedor`. A migração dos dados
e o `DROP TABLE` ficam no script manual
`backend/src/main/resources/db/manual/2026-08-migrar-fornecedor-para-pessoa.sql`.

## ADR-035 — `EtapaConstrucao` como recorte interno da fase de obra (Ago 2026)

Não substitui a ADR-026, que renomeou `EtapaProjeto` para `CategoriaDespesa`.
Convive com ela, e a distinção é o ponto inteiro desta decisão.

A ADR-026 removeu "etapa" porque o nome era enviesado para obra e se sobrepunha à
**fase do imóvel** como eixo temporal do custo. O que o usuário pediu agora é
outra coisa: saber **quanto custou cada trecho da obra** — fundação, alvenaria,
cobertura, acabamento —, uma pergunta que nem a categoria nem a fase respondem.
Categoria diz *a natureza* do gasto (material, mão de obra); fase diz *em que
momento da vida do imóvel* ele ocorreu; etapa diz *em que parte da construção*.

**Decisão:** `EtapaConstrucao` é um **enum fixo no código**, não um catálogo
administrável — é vocabulário de obra, não dado que o usuário mantém. Vira uma
coluna opcional em `despesa`, e `DespesaService` **recusa** despesa com etapa
preenchida cuja `faseImovel` não seja `CONSTRUCAO`, porque fora da obra o número
não teria sentido.

No relatório, `despesasPorEtapa` é **apresentação, não contabilidade**: agrega as
mesmas despesas que já entram por fase, e não toca `custoTotal`, `jurosPagos` nem
qualquer outro número da regra de custo. Despesa sem etapa fica de fora do quadro
em vez de virar uma chave nula na agregação.

## ADR-036 — Contrato financeiro editável, com histórico protegido (Ago 2026)

Complementa a ADR-025. O contrato só podia ser criado, quitado e ter parcela
baixada — corrigir um valor digitado errado exigia criar outro contrato.

**Decisão:** existe `PUT /api/contratos-financeiros/{id}`, com duas recusas:

- **contrato `QUITADO` não é editável** — a quitação antecipada tem valor próprio
  negociado, e as parcelas originais precisam continuar legíveis como histórico;
- **parcela já paga não pode ser alterada nem removida** — número, vencimento,
  valor e sobretudo `valorJuros` dela já entraram em `jurosPagos` e no
  `custoTotal` do relatório. A comparação inclui `valorJuros` justamente por isso.

A edição governa apenas as parcelas **em aberto**. Como a coleção usa
`orphanRemoval = true`, a implementação remove só as não pagas e nunca recria as
pagas: um `clear()` seguido de re-add apagaria do banco parcelas com baixa
registrada.

Continua valendo da ADR-025: **não** validar a soma das parcelas contra
`valorContratado`, porque juros fazem a soma exceder o principal legitimamente.

## ADR-037 — Compra parcelada do lote: entrada, preço e quitação (Ago 2026)

Complementa a ADR-025 e vale **somente para `PARCELAMENTO_COMPRA`** — o contrato
de compra do lote com o vendedor. `FINANCIAMENTO_CONSTRUCAO` e
`PARCELAMENTO_VENDA` não mudam.

Três lacunas motivaram a decisão. **A entrada não existia** no modelo: não era
parcela do contrato nem despesa, simplesmente sumia. **`compraValor` era
ambíguo**, sem dizer se recebia o preço do lote ou o total do parcelamento — e
lançar o total jogava eventuais juros no custo de uma vez, na data da compra,
contando-os duas vezes quando as parcelas também tinham `valorJuros`. E **o
desconto na quitação antecipada não reduzia o custo**: quem devia 49.000 e quitou
por 45.000 continuava com o lote custando o preço cheio.

### O parcelamento do lote é normalmente sem juros

Esta é a premissa que decide o desenho, e é fácil supor o contrário. No negócio,
o padrão é entrada mais parcelas em que o restante do preço é apenas dividido.
Juros existem, mas são minoritários e dependem do valor da entrada — entrada maior
costuma eliminá-los. Portanto: **o caminho principal não exige nada sobre juros**,
e a alocação de juros só aparece quando os números de fato não fecham. Uma tela
que peça juros para completar o cadastro está desenhada para a exceção.

### Por que o custo NÃO passou a acumular conforme as parcelas são pagas

A proposta inicial era regime de caixa: cada baixa de parcela viraria custo. Foi
descartada, e o motivo precisa ficar registrado porque a ideia volta a parecer
óbvia:

- **Comparabilidade**: dois lotes de 100.000, um à vista e outro em 30.000 +
  20 × 3.500 (mesmo preço, sem juros). Por caixa, no sexto mês um "custou"
  100.000 e o outro 51.000 — mesmo ativo, custo diferente só por causa do
  cronograma, e margem e rentabilidade dos dois deixam de ser comparáveis.
- **Revenda sem obra**: parte dos lotes é comprada e revendida sem construção. Se
  a revenda ocorre antes de terminar de pagar, o lucro aparece inflado no saldo
  devedor — que costuma ser quitado com o dinheiro da própria venda.

O que a proposta queria enxergar — "quanto já saiu do meu bolso" — passou a ser
entregue por um bloco **Desembolso** separado, sem contaminar o custo.

### Decisões

1. **A entrada é a parcela nº 0**, criada já com `dataPagamento` e `valorPago` —
   é fato consumado no momento da compra, não evento futuro. Reaproveita baixa,
   total pago e saldo devedor sem caso especial, e na edição cai sozinha sob a
   guarda de parcela paga da ADR-036. `ParcelaContratoRequestDTO.numero` passou a
   aceitar zero.
2. **O formulário do imóvel não pede o valor do lote na compra parcelada.** Ele
   espelharia um cronograma que ainda não existe naquela tela, e digitar às cegas
   produziria uma divergência que o sistema leria como juros inexistentes. Quem
   grava é `ContratoFinanceiroService.criar`: o `precoAVistaLote` informado (campo
   de entrada de cálculo, não persistido) ou, na falta dele, `entrada + Σ
   parcelas`. Nunca sobrescreve valor já preenchido, e **só na criação** —
   reescrever ao editar o cronograma mudaria em silêncio o custo de um imóvel já
   apurado. Correção depois é pelo `PUT` do imóvel, deliberadamente.
3. **`DadosCompra.parcelada`** é declarado no cadastro, não derivado da existência
   de contrato: é o que diz ao relatório se o valor de compra saiu do bolso na
   data da compra ou está diluído no cronograma.
4. **O ajuste de quitação entra no custo**, comparado contra o **principal** em
   aberto — nunca contra o total das parcelas. As parcelas em aberto carregam
   juros que jamais foram pagos e portanto nunca entraram no custo; comparar com o
   total inteiro os subtrairia de novo. Negativo é desconto e abate o custo;
   positivo é a parte de juros embutida no valor negociado e soma. As parcelas
   originais não são tocadas (ADR-025) — o ajuste é calculado, nunca gravado.
5. **`totalDesembolsado` e `saldoAPagar`** no resultado são posição de caixa e
   **nunca** somam ao `custoTotal`.

### A invariante que prova a fórmula

Num `PARCELAMENTO_COMPRA` quitado, o custo do lote converge exatamente para o
desembolso real, nos dois regimes:

| | Sem juros | Com juros |
|---|---|---|
| Preço / entrada / parcelas | 100.000 / 30.000 / 20 × 3.500 | 100.000 / 30.000 / 24 × 4.000 |
| Em aberto na quitação | 49.000 | 72.000 |
| **Principal** em aberto | 49.000 | 52.500 |
| Valor negociado | 45.000 | 66.000 |
| `ajusteQuitacao` | −4.000 | +13.500 |
| `custoTotal` = desembolso | 96.000 | 120.000 |

Coberto por `RelatorioServiceTest` e `ContratoFinanceiroServiceTest`.

**Pendência conhecida, deliberadamente fora deste escopo:** a mesma matemática
valeria para `FINANCIAMENTO_CONSTRUCAO`, onde a parte de juros embutida na
quitação também se perde hoje. Não é esquecimento — é escopo.

## ADR-038 — Resultado provisório é obra em andamento, não "fase ≠ CASA" (Ago 2026)

Corrige a redação da **ADR-020**, que definiu o resultado provisório como
`situacao = VENDIDO` **e** `fase != CASA`. A intenção estava certa — imóvel
vendido com a obra correndo ainda vai receber despesa, então o lucro exibido não
é o final. A tradução em código é que estava larga demais.

`FaseImovel` tem três valores, e "diferente de CASA" varre dois casos que não são
o mesmo:

- **`CONSTRUCAO` + `VENDIDO`** — obra em andamento, vendida na planta ou no meio.
  Novas despesas de construção ainda chegam: provisório de verdade.
- **`LOTE` + `VENDIDO`** — lote comprado e revendido **sem obra nenhuma**. É um
  dos fluxos normais do negócio (construir é opcional, ver ADR-019): o ciclo
  fechou, nada mais vai chegar, e o resultado é definitivo. Ainda assim a tela
  marcava esse imóvel como provisório **para sempre**, já que a fase nunca mais
  avançaria.

A decisão é usar `vendido && fase == CONSTRUCAO`. A regra fica auto-corretiva:
quem está em `LOTE` nunca começou a obra, quem está em `CASA` terminou, e se um
lote vendido começar a obra depois, a fase avança para `CONSTRUCAO` e o resultado
volta a ser provisório sozinho — sem flag nova nem campo de "obra prevista".

**Alternativa descartada:** deduzir a obra pendente de `custoEstimadoObra` ou
`previsaoConclusao` preenchidos. São dados de planejamento, opcionais e
frequentemente vazios; a fase é o único registro confiável de que a obra começou.

Registrado em `.agents/rules/ciclo-vida-imovel.md`, no comentário de
`RelatorioService.resultadoImovel` e no teste
`loteRevendidoSemObraNaoTemResultadoProvisorio`. O aviso da tela de resultado já
dizia "obra ainda em andamento" e não mudou — o texto estava certo, quem estava
errado era a condição que o disparava.
