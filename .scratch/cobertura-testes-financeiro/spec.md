# Cobertura de testes do financeiro — spec

Resultado de uma sessão de grilling (set/2026) com todas as decisões
confirmadas pelo usuário. Pela ADR-041, a implementação mecânica destas
decisões dispensa plan mode. **Qualquer regra de comportamento que não esteja
aqui exige parar e perguntar ao usuário.**

## Decisões

1. **Critério de "coberto"**: cada regra (bullet) de
   `.agents/rules/regras-negocio-financeiras.md`, `contratos-financeiros.md`,
   `ciclo-vida-imovel.md` e `auditoria.md` tem ao menos um teste nomeado pela
   regra. Regra estática/não testável fica marcada como tal na guia. Sem
   JaCoCo, sem meta de %.
2. **Testes existentes**: corrigir só problema de substância (teste que não
   prova o que diz, valor esperado recomputado, teste que só testa framework —
   este é apagado, ex. `frontend/src/app/app.spec.ts`). Divergência só de
   estilo fica.
3. **Integração com banco real** só para os filtros `AtivoTrue`/queries JPQL
   que afetam custo e para o caminho do relatório. Sem MockMvc (descartado
   antes, ver `docs/PROXIMOS-PASSOS.md`).
4. **Banco de teste**: banco `gestao_custos_obras_test` no Postgres local,
   perfil `test` (`application-test.properties` em `src/test/resources`), só a
   URL muda; mesmo usuário/senha do dev; `ddl-auto=create-drop`; isolamento
   por transação com rollback (padrão `@DataJpaTest`). Criado uma vez por
   `backend/src/main/resources/db/manual/criar-banco-teste.sql`. Postgres
   desligado ou banco inexistente → **teste falha** (nunca pular).
   `contextLoads` e `BuscasPaginadasTest` passam a usar esse perfil — nunca
   mais o banco de desenvolvimento.
5. **Testcontainers adiado** até o projeto usar Docker: ADR-045 em
   `docs/DECISOES.md` (porquê + gatilho), item em `docs/PROXIMOS-PASSOS.md`
   (passos: dependência, `@ServiceConnection`, apagar script e perfil) e
   comentário de uma linha no `application-test.properties` apontando a ADR.
6. **`ContratoFinanceiroService.pagarParcela` recusa** (RegraDeNegocioException)
   três casos: parcela já paga; contrato `QUITADO`; contrato `CANCELADO`.
   Registrar em `.agents/rules/contratos-financeiros.md` e ADR-044.
7. **Dois bugs**, teste primeiro (vermelho), depois correção, sem ADR:
   - `DespesaService.buscarContratoOpcional` usa `findById` sem filtro de
     ativo → contrato excluído deve ser "não encontrado"
   - checagem de orçamento duplicado (`OrcamentoCategoriaRepository
     .existsBy…`/`findByImovelIdAndCategoriaDespesaId`) sem filtro de ativo →
     orçamento excluído não bloqueia um novo
8. **Auditoria**: um teste por método de mutação nos domínios contrato,
   despesa, imóvel e orçamento, verificando `registrar` com a operação certa
   e o estado anterior capturado **antes** da mutação.
9. **Frontend**: extrair a matemática do `contrato-form-dialog.ts`
   (`totalCronograma`, `diferencaJuros`, `distribuirJuros`, `gerarParcelas`,
   `somarMeses`, `proximoNumero`) para funções puras em
   `features/contratos/cronograma.ts`, **calculando em centavos inteiros**, e
   testar. Também testar `saldoAEstornar` (detalhe do contrato). Tela fora.
10. **Fora do escopo**: `PessoaService`, `CategoriaDespesaService`. Entra só a
    trava de caminho inválido (`..`) do `LocalStorageService`.
11. **Se um teste revelar comportamento atual que contradiz uma rule**
    (relatório, imóvel, carteira…): **não corrigir** — parar e relatar ao
    usuário (regra nova → plan mode, ADR-041).

## Entrega

Etapas em `issues/`, em ordem, uma por commit (mensagem no padrão do repo,
ex. `test(contrato): ...`), sem pedir aprovação entre etapas. Guias a seguir:
`.agents/rules/testes-*.md`. Rodar `cd backend && ./mvnw test` (e
`cd frontend && npx ng test --watch=false` na etapa de frontend) antes de cada
commit.
