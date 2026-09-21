package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Flyway pausado (ADR-013): seed via runner, não migration. Garante que sempre exista um perfil
 * com todas as permissões — bootstrap do ADR-046 ("perfil" é só mais um domínio da própria
 * matriz, então precisa nascer com acesso a si mesmo) — e migra usuários pré-existentes sem
 * perfil (o antigo campo `role` solto) pra ele, sem exigir script manual.
 */
@Component
@RequiredArgsConstructor
public class PerfilSeedRunner implements CommandLineRunner {

    private static final String NOME_ADMINISTRADOR = "Administrador";

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void run(String... args) {
        PerfilModel administrador = perfilRepository.findByNome(NOME_ADMINISTRADOR)
                .orElseGet(this::criarPerfilAdministrador);

        usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getPerfil() == null)
                .forEach(usuario -> {
                    usuario.setPerfil(administrador);
                    usuarioRepository.save(usuario);
                });
    }

    private PerfilModel criarPerfilAdministrador() {
        PerfilModel perfil = PerfilModel.builder().nome(NOME_ADMINISTRADOR).build();
        Arrays.stream(DominioSistema.values()).forEach(dominio ->
                Arrays.stream(AcaoPermissao.values()).forEach(acao ->
                        perfil.getPermissoes().add(PerfilPermissaoModel.builder()
                                .perfil(perfil)
                                .dominio(dominio)
                                .acao(acao)
                                .build())));
        return perfilRepository.save(perfil);
    }
}
