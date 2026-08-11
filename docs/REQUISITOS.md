# Requisitos

## Requisitos Funcionais (RF)

### RF01 — Cadastro e Gestão de Imóveis/Lotes ✅ Implementado
Gerenciar as propriedades (lotes e imóveis) que recebem os aportes financeiros.
- Campos: identificador único, tipo (Lote ou Imóvel), endereço/localização,
  área (m²), valor de aquisição inicial, status (Planejamento, Construção,
  Finalizado), descrição
- Ações: criar, visualizar, editar, inativar (soft delete)
- Endpoint: `/api/imoveis`

### RF02 — Cadastro de Aportantes ✅ Implementado
Mapear as pessoas físicas ou jurídicas que aportam recursos ao projeto.
- Campos: nome, CPF/CNPJ (documento), e-mail, telefone, tipo de participação
  (ex: Sócio, Investidor, Proprietário)
- Ações: criar, listar, editar, inativar (soft delete)
- Endpoint: `/api/aportantes`

### RF03 — Gestão de Etapas do Projeto ✅ Implementado
Catálogo global de fases de construção para organizar despesas e relatórios.
- Exemplos: Aquisição do Terreno, Documentação/Alvará, Projetos
  (Arquitetônico/Estrutural), Fundação, Alvenaria, Acabamento, Mão de Obra
- Catálogo global: etapas cadastradas uma vez e reutilizadas em qualquer imóvel
- Ações: criar, listar, editar, excluir
- Endpoint: `/api/etapas-projeto`

### RF04 — Lançamento e Vínculo de Custos (Despesas) ✅ Implementado
Cada gasto é registrado e vinculado a um imóvel, uma etapa e um ou mais aportantes.
- Campos: valor, data do pagamento, descrição, comprovante (URL), etapa
  vinculada, imóvel correspondente
- Rateio: selecionar quem pagou (um aportante) ou dividir o custo entre
  múltiplos aportantes via `DespesaPagamento`
- Validação crítica: soma dos pagamentos não pode exceder o valor da despesa
- Endpoint: `/api/despesas`

### RF05 — Módulo de Relatórios e Gráficos ✅ Implementado
- Filtros: por imóvel/lote, por período (início/fim), por etapa, por aportante
- Visualização: custo total acumulado; gráfico de pizza/barras por etapa;
  extrato de aportes por aportante
- Exportação: PDF ou planilha (Excel/CSV)
- Endpoints em `/api/relatorios`: `custo-por-imovel`, `custo-por-m2`,
  `extrato-aportantes`, `orcado-vs-realizado` — com filtros opcionais
  (`imovelId`, `etapaProjetoId`, `aportanteId`, `dataInicio`, `dataFim`) e
  exportação CSV via `?format=csv` (delimitador `;`, compatível com Excel pt-BR).
  A visualização de gráficos (pizza/barras) será renderizada no frontend Angular
  (Etapa 9) a partir dos dados agregados destes endpoints.

### RF06 — Upload de Imagens do Imóvel ✅ Implementado
- Galeria de fotos do imóvel via `POST /api/imoveis/{id}/fotos` (upload `multipart/form-data`)
- Listagem de fotos com carimbo de data/hora em `GET /api/imoveis/{id}/fotos`
- Remoção de fotos em `DELETE /api/imoveis/{id}/fotos/{fotoId}`
- Upload de comprovante de despesa via `POST /api/despesas/{id}/comprovante`
- Serviço de storage local (`shared/storage/LocalStorageService`), extensível para S3/Cloud Storage
- Download de arquivos via `GET /api/arquivos/download/{subpasta}/{nomeArquivo}`

## Requisitos Não Funcionais (RNF)

- **RNF01 — Segurança e Autenticação** ⏳ Pendente: controle de acesso
  (Login/Senha) com JWT no Spring Boot. Atualmente usa HTTP Basic temporário.
- **RNF02 — Persistência de Dados** ✅: PostgreSQL com integridade referencial
  (ON DELETE RESTRICT em relações financeiras). Flyway para migrações.
- **RNF03 — Responsividade** ⏳ Pendente: interface Angular + Angular Material
  responsiva em desktop e mobile, permitindo lançar nota fiscal do canteiro de
  obras pelo celular.
- **RNF04 — Precisão Monetária** ✅: `BigDecimal` no Java e `NUMERIC(14,2)` no
  PostgreSQL para evitar erros de arredondamento.
