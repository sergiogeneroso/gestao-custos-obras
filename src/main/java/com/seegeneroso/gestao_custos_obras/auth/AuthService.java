package com.seegeneroso.gestao_custos_obras.auth;

import com.seegeneroso.gestao_custos_obras.auth.dto.LoginRequestDTO;
import com.seegeneroso.gestao_custos_obras.auth.dto.LoginResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.config.JwtService;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDTO login(LoginRequestDTO dto) {
        UsuarioModel usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole());
        return new LoginResponseDTO(token, usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
