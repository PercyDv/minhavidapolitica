package br.com.minhavidapolitica.api.config;

import br.com.minhavidapolitica.api.domain.PerfilUsuario;
import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import br.com.minhavidapolitica.api.repository.UsuarioPainelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador inicial na primeira execução (tabela vazia), usando ADMIN_USERNAME e ADMIN_PASSWORD
 * do .env. Nenhuma senha fica escrita no código nem nas migrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UsuarioPainelRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String username;

    @Value("${app.admin.nome:Administrador}")
    private String nome;

    @Value("${app.admin.password:}")
    private String senha;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        if (senha == null || senha.length() < 8) {
            log.warn("Nenhum usuário cadastrado e ADMIN_PASSWORD ausente ou com menos de 8 caracteres no .env. "
                    + "O painel ficará inacessível até que o administrador inicial seja criado.");
            return;
        }
        repository.save(UsuarioPainel.builder()
                .nome(nome)
                .username(username.trim())
                .senhaHash(passwordEncoder.encode(senha))
                .perfil(PerfilUsuario.ADMIN)
                .ativo(true)
                .build());
        log.info("Administrador inicial '{}' criado.", username);
    }
}
