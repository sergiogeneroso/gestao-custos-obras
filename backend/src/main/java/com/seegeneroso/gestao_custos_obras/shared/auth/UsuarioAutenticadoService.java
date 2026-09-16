package com.seegeneroso.gestao_custos_obras.shared.auth;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.auth.UsuarioRepository;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;

    // O principal é o e-mail (JwtAuthenticationFilter monta o Authentication com o e-mail do
    // token), nunca nulo num endpoint autenticado — SecurityConfig exige Authorization em tudo
    // fora /api/auth/login.
    public UsuarioModel usuarioAtual() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não encontrado: " + email));
    }
}
