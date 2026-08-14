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
Campos: identificador, endereço, área (m²), descrição.

**Ciclo de vida em dois eixos independentes** (ADR-020):
- `fase`: LOTE → CONSTRUCAO → CASA, só avança. Todo imóvel começa como lote;
  construir é opcional.
- `situacao`: ADQUIRIDO ⇄ A_VENDA → VENDIDO. A venda pode ocorrer em qualquer
  fase, inclusive na planta, e não congela a fase.

**Compra e venda** (ADR-024): valor, data e contraparte de cada uma, mais o
valor pretendido de venda. **Datas de transição** de fase e **projeção da obra**
(custo estimado e previsão de conclusão) gravadas no imóvel.

Ações: criar, listar, editar, inativar (soft delete), avançar fase, mudar
situação. `/api/imoveis`

### RF02 — Pessoas 🔄 Em reformulação
Substitui "Aportantes" (ADR-021). Pessoas físicas ou jurídicas que se relacionam
com despesas e negociações. Campos: nome, tipo (Física/Jurídica), documento
(CPF/CNPJ, único), e-mail, telefone. O papel — pagadora, beneficiária, vendedora,
compradora — vem do uso, não do cadastro. Ações: criar, listar, editar, inativar.
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

### RF08 — Fornecedores 🆕
Domínio próprio composto com Pessoa (ADR-022): toda pessoa pode ser promovida a
fornecedora sem recriar cadastro. Campos próprios: área de atuação e observações.
Ações: criar, listar, editar, inativar. `/api/fornecedores`

### RF09 — Contratos Financeiros 🆕
Um imóvel encadeia vários contratos ao longo da vida (ADR-025): parcelamento da
compra com o vendedor, financiamento de construção com o banco, parcelamento da
venda. Campos: tipo, contraparte, número, valor contratado, data, situação
(Ativo/Quitado) e, na quitação antecipada, data e valor negociados.

Cronograma de parcelas: número, vencimento, valor, valor de juros, data e valor
de pagamento (parcela sem data de pagamento está em aberto).
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
- **RNF03 — Responsividade** ⏳ Pendente: Angular + Angular Material,
  mobile-friendly para lançamento de despesas no canteiro de obras.
- **RNF04 — Precisão Monetária** ✅: `BigDecimal` + `NUMERIC(14,2)`. Única
  exceção documentada: indicadores percentuais com expoente fracionário.
