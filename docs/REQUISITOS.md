# Requisitos

Requisitos de negócio (o quê), tecnologia-agnósticos. Requisito novo que é só
manifestação de tela/UX de um RF/RNF já existente aqui (validação de
formulário, fluxo, mensagem) entra em `docs/FRONTEND.md`, não vira entrada
nova neste arquivo — evita duplicar "o quê" (aqui) com "como no frontend"
(lá) conforme a Etapa 9 avança.

O objetivo do sistema (ADR-019) é acompanhar o **resultado financeiro de cada
imóvel do começo ao fim**: compra do lote, custos ao longo de todo o ciclo de
vida e venda. A numeração dos requisitos foi preservada no reescopo de Ago 2026
para não invalidar referências; vários tiveram o conteúdo reescrito.

## Requisitos Funcionais (RF)

### RF01 — Imóveis e ciclo de vida 🔄 Em reformulação
Campos gerais: identificador, descrição e endereço completo — logradouro,
número, bairro, cidade, UF, CEP e observação do endereço.

**Propriedades por fase** (ADR-031), agrupadas em `@Embedded` no próprio imóvel:
- **Lote**: matrícula, cartório e data do registro; inscrição municipal (IPTU);
  área do lote (m²).
- **Construção**: área construída (m²), data de início, previsão de conclusão e
  custo estimado; alvará (número, emissão, validade); ART/RRT e responsável
  técnico; CNO.
- **Casa**: data de conclusão da obra; habite-se (número e data); data de
  averbação na matrícula; quartos, suítes, banheiros e vagas.

Cada uma é pedida no fluxo a que pertence, nunca toda no cadastro (ADR-033): o
cadastro só aceita dados do lote, a transição de fase leva os dados da fase de
destino, e o valor de venda pretendido entra ao colocar à venda.

**Ciclo de vida em dois eixos independentes** (ADR-020):
- `fase`: LOTE → CONSTRUCAO → CASA, só avança. Todo imóvel começa como lote;
  construir é opcional.
- `situacao`: ADQUIRIDO ⇄ A_VENDA → VENDIDO. A venda pode ocorrer em qualquer
  fase, inclusive na planta, e não congela a fase.

**Compra e venda** (ADR-024): valor, data e contraparte de cada uma, mais o
valor pretendido de venda. A **data da compra é obrigatória** e é o marco
inicial da carteira e da fase LOTE — não existe `dataInicioLote` separada
(ADR-032). As demais datas de transição são informadas pelo usuário na ação
correspondente.

**Forma de pagamento da compra** (ADR-037): à vista ou parcelada. Na compra
parcelada o cadastro **não pede o valor do lote** — ele vem do contrato de
`PARCELAMENTO_COMPRA`, aberto na sequência, como `entrada + parcelas` ou como o
preço à vista declarado ali. O custo do lote é reconhecido na compra e não se
move conforme as parcelas são pagas; quanto já saiu do caixa aparece no bloco
Desembolso do resultado.

Ações: criar, listar, editar, inativar (soft delete), avançar fase, mudar
situação. `/api/imoveis`

### RF02 — Pessoas 🔄 Em reformulação
Substitui "Aportantes" (ADR-021). Pessoas físicas ou jurídicas que se relacionam
com despesas e negociações. Campos: nome, tipo (Física/Jurídica), documento
(CPF/CNPJ, único), e-mail, telefone. Os papéis de pagadora, beneficiária,
vendedora e compradora vêm do uso, não do cadastro.

**Fornecedora é a exceção, e é uma marca no cadastro** (ADR-034, substitui o
domínio próprio da ADR-022): a flag `fornecedor`, mais `areaAtuacao` e
`observacoes`, preenchidos só quando ela está ligada. Diferente dos outros
papéis, esse não é dedutível de lançamento nenhum — precisa ser declarado antes
de existir despesa, para a pessoa ser encontrada na hora de lançar. A listagem
tem filtro "Só fornecedores". Ações: criar, listar, editar, inativar.
`/api/pessoas`

### RF03 — Categorias de Despesa 🔄 Em reformulação
Substitui "Etapas do Projeto" (ADR-026). Catálogo global reutilizável que
responde pela **natureza do gasto**: Aquisição, ITBI/Escritura, Documentação,
IPTU, Material, Mão de obra, Custos de financiamento, Corretagem, Impostos sobre
a venda. Ações: criar, listar, editar, excluir. `/api/categorias-despesa`

### RF04 — Despesas 🔄 Em reformulação
Valor, data de pagamento, descrição, categoria, pagador (obrigatório),
beneficiário (opcional), fase em que foi incorrida e vínculo opcional com um
contrato financeiro. **Sem rateio** (ADR-023): um pagador por despesa.

Despesa da fase Construção aceita ainda a **etapa da obra** (ADR-035) — fundação,
alvenaria, cobertura, acabamento etc., lista fixa —, que alimenta o quadro de
custo por etapa no resultado do imóvel. Fora dessa fase a etapa é recusada.

O vínculo com imóvel é **opcional**: despesa sem imóvel é gasto geral (contador,
combustível, ferramentas) e não entra no custo de nenhum imóvel. Mão de obra é
despesa avulsa, uma por diária ou medição. Soft delete (ADR-028).
`/api/despesas`

### RF05 — Relatórios 🔄 Em reformulação
- **Resultado por imóvel** (principal): custo de compra, despesas quebradas por
  fase, juros pagos, custo total, valor de venda, lucro, margem, tempo em cada
  fase, dias em carteira, rentabilidade anualizada e posição dos contratos.
  Marca o resultado como provisório quando o imóvel foi vendido com obra pendente.
- **Histórico por fornecedor**: quanto já foi pago a cada beneficiário.
- **Extrato por pessoa**: total pago por pessoa (só quem tem valor > 0).
- **Carteira**: total investido, total vendido, lucro realizado, imóveis por fase
  e situação, parcelas a vencer e saldo devedor.

Filtros: imóvel, período, categoria, pessoa. Exportação CSV via `?format=csv`
(delimitador `;`, compatível com Excel pt-BR). `/api/relatorios`

### RF06 — Anexos 🔄 Em reformulação
- Galeria de fotos do imóvel (`/api/imoveis/{id}/fotos`) ✅
- **Documentos do imóvel** (`/api/imoveis/{id}/documentos`): matrícula,
  escritura, contrato, IPTU, alvará, projeto, ART, habite-se, com o tipo e a fase
  a que pertencem.
- **Anexos da despesa** (`/api/despesas/{id}/anexos`): múltiplos e tipados —
  comprovante, nota fiscal, recibo, contrato — listáveis separadamente por tipo
  (ADR-027).

Storage local (`shared/storage/LocalStorageService`), extensível para S3/Cloud.

### RF07 — Configuração de Tema (Painel Admin) ⏳ Pendente
Admin escolhe, entre paletas pré-definidas, qual tema visual vale para todos os
usuários (não é preferência individual). Suporta light e dark mode. Paletas
curadas: Nocturne (default, dark-only), Azul corporativo, Terracota industrial,
Verde financeiro, Grafite + âmbar (hex e detalhes em `docs/FRONTEND.md`).
Persistência no backend (config chave-valor), endpoint restrito a `ROLE_ADMIN`.
Decisão completa: ADR-016/ADR-018 em `docs/DECISOES.md`.

### RF08 — Fornecedores ✅ Absorvido pelo RF02
Deixou de ser domínio próprio (ADR-034): virou a marca `fornecedor` no cadastro
de Pessoa, com área de atuação e observações. Não há `/api/fornecedores` nem tela
de Fornecedores — a gestão acontece em Pessoas, com filtro "Só fornecedores".

### RF09 — Contratos Financeiros 🆕
Um imóvel encadeia vários contratos ao longo da vida (ADR-025): parcelamento da
compra com o vendedor, financiamento de construção com o banco, parcelamento da
venda. Campos: tipo, contraparte, número, valor contratado, data, situação
(Ativo/Quitado) e, na quitação antecipada, data e valor negociados.

Cronograma de parcelas: número, vencimento, valor, valor de juros, data e valor
de pagamento (parcela sem data de pagamento está em aberto).

O contrato é **editável** (ADR-036), com duas travas: contrato quitado não muda,
e parcela já paga não pode ser alterada nem removida. Há gerador de cronograma
(quantidade, valor e primeiro vencimento) e anexo de documentos ao contrato.

No `PARCELAMENTO_COMPRA` (ADR-037) o contrato ainda recebe a **entrada**, gravada
como parcela nº 0 já baixada, e uma linha que reconcilia o preço do lote com
`entrada + parcelas`: sem diferença não há juros — o caso normal —, e com
diferença positiva ela pode ser distribuída como juros nas parcelas. A quitação
antecipada ajusta o custo do lote pela diferença entre o valor negociado e o
principal em aberto.
`/api/contratos-financeiros`

### RF10 — Dashboard da Carteira 🆕
Visão consolidada: total investido, total vendido, lucro realizado, imóveis por
fase e por situação, parcelas a vencer nos próximos 30 dias, saldo devedor total
e gastos gerais do período. Alimentado por um endpoint único de relatório — o
cálculo financeiro não é refeito no frontend.

## Requisitos Não Funcionais (RNF)

- **RNF01 — Autenticação** ✅: JWT stateless (jjwt). Login em `/api/auth/login`
  emite token; `JwtAuthenticationFilter` valida `Authorization: Bearer`.
  Senha armazenada como hash BCrypt. Role viaja no token; regras `.hasRole(...)`
  por domínio pendentes (fase 2).
- **RNF02 — Persistência** ✅: PostgreSQL, integridade referencial
  (`ON DELETE RESTRICT` em relações financeiras). Flyway pausado (ADR-013/029).
- **RNF03 — Responsividade** ✅ (no fluxo de despesa): o lançamento de despesa —
  listagem, formulário e anexo — é usável no celular. Abaixo de 600px a tabela
  vira cartão por linha e o formulário vai para uma coluna; abaixo de 768px o
  menu lateral vira gaveta sobreposta. No desktop o menu recolhe para o modo só
  de ícones, e a escolha persiste. As demais telas continuam desktop-first: são
  consulta, não uso no canteiro.
- **RNF04 — Precisão Monetária** ✅: `BigDecimal` + `NUMERIC(14,2)`. Única
  exceção documentada: indicadores percentuais com expoente fracionário.
