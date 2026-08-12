-- Seed do usuário admin inicial (senha: admin123, BCrypt cost 10).
-- Idempotente: não recria se o e-mail já existir.
INSERT INTO usuario (nome, email, senha_hash, role)
VALUES ('Administrador', 'admin@gestao.local',
        '$2a$10$96v2MxwW0./0SzhKYkAHO.4RV5x3C0hOFfiVrbXzkdhtr1OXd9MmK', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
