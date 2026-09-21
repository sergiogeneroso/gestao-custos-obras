package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.perfil.dto.MinhasPermissoesResponseDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilPermissaoDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilRequestDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PerfilPermissaoRepository perfilPermissaoRepository;
    @Spy
    private PerfilMapper perfilMapper = new PerfilMapper();
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PerfilService perfilService;

    @Test
    void criarRecusaNomeJaExistente() {
        when(perfilRepository.existsByNome("Operador")).thenReturn(true);

        assertThatThrownBy(() -> perfilService.criar(dto("Operador")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um perfil");
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void atualizarPermiteManterOProprioNome() {
        PerfilModel existente = perfil(10L, "Operador");
        when(perfilRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        when(perfilRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        PerfilResponseDTO resultado = perfilService.atualizar(10L, dto("Operador"));

        assertThat(resultado.nome()).isEqualTo("Operador");
        verify(perfilRepository, never()).existsByNome(any());
    }

    @Test
    void atualizarRecusaNomeDeOutroPerfilJaExistente() {
        PerfilModel existente = perfil(10L, "Operador");
        when(perfilRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        when(perfilRepository.existsByNome("Administrador")).thenReturn(true);

        assertThatThrownBy(() -> perfilService.atualizar(10L, dto("Administrador")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe outro perfil");
        verify(perfilRepository, never()).save(any());
    }

    // Cobre .agents/rules/auditoria.md: criar audita CRIACAO com estadoAnterior nulo.
    @Test
    void criarAuditaCriacaoComEstadoAnteriorNulo() {
        when(perfilRepository.existsByNome("Operador")).thenReturn(false);
        when(perfilRepository.save(any())).thenAnswer(chamada -> {
            PerfilModel salvo = chamada.getArgument(0);
            salvo.setId(50L);
            return salvo;
        });

        perfilService.criar(dto("Operador"));

        verify(auditoriaService).registrar(eq("Perfil"), eq(50L), eq(OperacaoAuditoria.CRIACAO), eq(null), any());
    }

    @Test
    void excluirAplicaExclusaoLogicaComMotivo() {
        PerfilModel existente = perfil(10L, "Operador");
        when(perfilRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        when(perfilRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(UsuarioModel.builder().id(1L).build());

        perfilService.excluir(10L, "Perfil não é mais usado");

        assertThat(existente.getExclusao().getAtivo()).isFalse();
        assertThat(existente.getExclusao().getMotivoExclusao()).isEqualTo("Perfil não é mais usado");
        verify(auditoriaService).registrar(eq("Perfil"), eq(10L), eq(OperacaoAuditoria.EXCLUSAO), any(), any());
    }

    // Fail-closed: usuário sem perfil atribuído nunca tem permissão nenhuma (ADR-046).
    @Test
    void possuiPermissaoEhFalsoParaUsuarioSemPerfil() {
        UsuarioModel usuario = UsuarioModel.builder().id(1L).perfil(null).build();

        boolean resultado = perfilService.possuiPermissao(usuario, DominioSistema.DESPESA, AcaoPermissao.ACESSAR);

        assertThat(resultado).isFalse();
        verify(perfilPermissaoRepository, never()).possuiPermissao(any(), any(), any());
    }

    @Test
    void possuiPermissaoConsultaAMatrizDoPerfilDoUsuario() {
        PerfilModel perfil = perfil(10L, "Operador");
        UsuarioModel usuario = UsuarioModel.builder().id(1L).perfil(perfil).build();
        when(perfilPermissaoRepository.possuiPermissao(10L, DominioSistema.DESPESA, AcaoPermissao.ALTERAR)).thenReturn(true);

        boolean resultado = perfilService.possuiPermissao(usuario, DominioSistema.DESPESA, AcaoPermissao.ALTERAR);

        assertThat(resultado).isTrue();
    }

    // minhasPermissoes agrupa por domínio pra alimentar a UI (menu/botões) de quem loga.
    @Test
    void minhasPermissoesAgrupaAsAcoesPorDominio() {
        PerfilModel perfil = perfil(10L, "Operador");
        UsuarioModel usuario = UsuarioModel.builder().id(1L).perfil(perfil).build();
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(usuario);
        when(perfilPermissaoRepository.findByPerfilId(10L)).thenReturn(List.of(
                PerfilPermissaoModel.builder().perfil(perfil).dominio(DominioSistema.DESPESA).acao(AcaoPermissao.ACESSAR).build(),
                PerfilPermissaoModel.builder().perfil(perfil).dominio(DominioSistema.DESPESA).acao(AcaoPermissao.INCLUIR).build(),
                PerfilPermissaoModel.builder().perfil(perfil).dominio(DominioSistema.PESSOA).acao(AcaoPermissao.ACESSAR).build()
        ));

        MinhasPermissoesResponseDTO resultado = perfilService.minhasPermissoes();

        assertThat(resultado.perfilNome()).isEqualTo("Operador");
        assertThat(resultado.permissoes().get(DominioSistema.DESPESA)).containsExactlyInAnyOrder(AcaoPermissao.ACESSAR, AcaoPermissao.INCLUIR);
        assertThat(resultado.permissoes().get(DominioSistema.PESSOA)).containsExactly(AcaoPermissao.ACESSAR);
    }

    @Test
    void minhasPermissoesEhVazioParaUsuarioSemPerfil() {
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(UsuarioModel.builder().id(1L).perfil(null).build());

        MinhasPermissoesResponseDTO resultado = perfilService.minhasPermissoes();

        assertThat(resultado.permissoes()).isEmpty();
    }

    private PerfilModel perfil(Long id, String nome) {
        return PerfilModel.builder().id(id).nome(nome).build();
    }

    private PerfilRequestDTO dto(String nome) {
        return new PerfilRequestDTO(nome, List.of(new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.ACESSAR)));
    }
}
