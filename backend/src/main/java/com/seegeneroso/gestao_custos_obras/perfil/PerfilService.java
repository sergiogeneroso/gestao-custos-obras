package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.perfil.dto.MinhasPermissoesResponseDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilRequestDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final PerfilPermissaoRepository perfilPermissaoRepository;
    private final PerfilMapper perfilMapper;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PerfilResponseDTO criar(PerfilRequestDTO dto) {
        if (perfilRepository.existsByNome(dto.nome())) {
            throw new RegraDeNegocioException("Já existe um perfil com o nome: " + dto.nome());
        }
        PerfilModel entity = perfilMapper.toEntity(dto);
        PerfilModel salvo = perfilRepository.save(entity);
        PerfilResponseDTO responseDto = perfilMapper.toResponseDTO(salvo);
        auditoriaService.registrar("Perfil", salvo.getId(), OperacaoAuditoria.CRIACAO, null, responseDto);
        return responseDto;
    }

    @Transactional(readOnly = true)
    public List<PerfilResponseDTO> listarTodos() {
        return perfilRepository.findByAtivoTrue().stream()
                .map(perfilMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO buscarPorId(Long id) {
        PerfilModel entity = perfilRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com id: " + id));
        return perfilMapper.toResponseDTO(entity);
    }

    @Transactional
    public PerfilResponseDTO atualizar(Long id, PerfilRequestDTO dto) {
        PerfilModel entity = perfilRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com id: " + id));
        PerfilResponseDTO estadoAnterior = perfilMapper.toResponseDTO(entity);

        if (!entity.getNome().equalsIgnoreCase(dto.nome()) && perfilRepository.existsByNome(dto.nome())) {
            throw new RegraDeNegocioException("Já existe outro perfil com o nome: " + dto.nome());
        }

        perfilMapper.updateEntityFromDto(dto, entity);
        PerfilModel atualizado = perfilRepository.save(entity);
        PerfilResponseDTO estadoNovo = perfilMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("Perfil", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    @Transactional
    public void excluir(Long id, String motivo) {
        PerfilModel entity = perfilRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado com id: " + id));
        PerfilResponseDTO estadoAnterior = perfilMapper.toResponseDTO(entity);
        entity.getExclusao().excluir(motivo, usuarioAutenticadoService.usuarioAtual());
        PerfilModel excluido = perfilRepository.save(entity);
        auditoriaService.registrar("Perfil", id, OperacaoAuditoria.EXCLUSAO, estadoAnterior, perfilMapper.toResponseDTO(excluido));
    }

    @Transactional(readOnly = true)
    public MinhasPermissoesResponseDTO minhasPermissoes() {
        return permissoesDoUsuario(usuarioAutenticadoService.usuarioAtual());
    }

    @Transactional(readOnly = true)
    public MinhasPermissoesResponseDTO permissoesDoUsuario(UsuarioModel usuario) {
        PerfilModel perfil = usuario.getPerfil();
        if (perfil == null || !Boolean.TRUE.equals(perfil.getExclusao().getAtivo())) {
            return new MinhasPermissoesResponseDTO(null, Map.of());
        }
        Map<DominioSistema, Set<AcaoPermissao>> matriz = new EnumMap<>(DominioSistema.class);
        perfilPermissaoRepository.findByPerfilId(perfil.getId()).forEach(p ->
                matriz.computeIfAbsent(p.getDominio(), d -> EnumSet.noneOf(AcaoPermissao.class)).add(p.getAcao()));
        return new MinhasPermissoesResponseDTO(perfil.getNome(), matriz);
    }

    // Usado pelo PermissaoInterceptor a cada requisição — usuário sem perfil nunca tem permissão.
    @Transactional(readOnly = true)
    public boolean possuiPermissao(UsuarioModel usuario, DominioSistema dominio, AcaoPermissao acao) {
        if (usuario.getPerfil() == null) {
            return false;
        }
        return perfilPermissaoRepository.possuiPermissao(usuario.getPerfil().getId(), dominio, acao);
    }
}
