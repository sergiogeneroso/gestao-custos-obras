---
paths:
  - "backend/src/test/**"
---

# Testes do backend (Java)

## Stack

JUnit 5 + Mockito + AssertJ, todos via `spring-boot-starter-test`. Asserções
com `assertThat` do AssertJ. Teste unitário puro: `@SpringBootTest` fica só no
`GestaoCustosObrasApplicationTests` (subir o contexto é lento e não prova
regra nenhuma).

O teste mora no mesmo pacote da classe testada, em `src/test/java`, com o
sufixo `Test` (`imovel/ImovelServiceTest.java`,
`imovel/dto/ImovelRequestDTOTest.java`).

## Por tipo de classe

### Service (onde mora a regra)

- `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` no Service
- `@Mock` para Repository, `StorageService`, `AuditoriaService` e Services de
  outro domínio
- **Mapper entra como `@Spy`** (`@Spy private ImovelMapper imovelMapper = new
  ImovelMapper();`) quando a regra roda sobre o estado que o mapper aplica —
  com mapper mockado o teste não exercita nada
- Um método privado `mockar(...)` concentra os `when(...)` repetidos
- Verifique o **resultado** (DTO devolvido, estado do Model, exceção). Use
  `verify(...)` só para efeito colateral que é a própria regra: auditoria
  gravada, `save` que **não** pode acontecer (`never()`), arquivo removido

### DTO com validação própria

Métodos `isXxxValido()` do record são testados direto, sem Bean Validation:
monte o DTO por um método `dto(...)` que só expõe os campos em jogo. Ver
`ImovelRequestDTOTest`.

### Utilitário estático (`shared/`)

Chamada direta, sem mock, agrupando casos válidos e inválidos da mesma regra
no mesmo teste. Ver `DocumentosTest`.

### Controller e Repository

Controller sem teste próprio enquanto for só delegação — sem MockMvc no
projeto (descartado, ver `docs/PROXIMOS-PASSOS.md`). Query derivada também
fica sem teste.

Repository com `@Query` escrita à mão (JPQL) e regra que depende dela ganham
teste de integração contra o banco de teste local
(`gestao_custos_obras_test`, perfil `test` — ver ADR-045): `@SpringBootTest`
+ `@ActiveProfiles("test")` + `@Transactional` para isolamento por rollback,
injeta o Repository por `@Autowired` (sem mock) e chama o método direto.
`@Transactional` sem `readOnly` sempre que o teste grava dado de setup (caso
comum, ex. `DespesaRepositoryTest`) — com `readOnly = true` o Postgres recusa
o insert porque a conexão vai como somente leitura. Reserve
`readOnly = true` para o teste que só lê, sem inserir nada (ver
`BuscasPaginadasTest`).

## Armadilhas do projeto

- **`BigDecimal`**: sempre `isEqualByComparingTo("100050")`, nunca
  `isEqualTo` — `100050` e `100050.00` diferem em `equals`
- **Exceção de regra**: `assertThatThrownBy(() -> ...)
  .isInstanceOf(RegraDeNegocioException.class)` e, quando a mensagem é o que
  o usuário vê, `.hasMessageContaining(...)`
- **Exclusão lógica**: os repositories filtram por `AtivoTrue`
  (`findByIdAndAtivoTrue`); mocke o método com o filtro, que é o que o Service
  chama de verdade
- Entidades têm `@Builder`: monte com builder dentro dos construtores de
  dados, preenchendo os embeddables (`DadosLote`, `DadosCompra`, ...) para não
  estourar `NullPointerException` fora da regra testada
