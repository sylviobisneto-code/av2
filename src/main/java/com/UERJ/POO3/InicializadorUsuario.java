package com.UERJ.POO3;

import com.UERJ.POO3.modules.seguranca.domain.Papel;
import com.UERJ.POO3.modules.seguranca.domain.Usuario;
import com.UERJ.POO3.modules.seguranca.repository.PapelRepository;
import com.UERJ.POO3.modules.seguranca.repository.UsuarioRepository;
import com.UERJ.POO3.modules.seguranca.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InicializadorUsuario {

    @Bean
    CommandLineRunner criarUsuarioInicial(
            UsuarioRepository usuarioRepository,
            PapelRepository papelRepository,
            UsuarioService usuarioService) {

        return args -> {

            if (usuarioRepository.findByUsername("sylvio_admin").isEmpty()) {

                papelRepository.findByNome("ROLE_ADMIN")
                        .orElseGet(() ->
                                papelRepository.save(new Papel("ROLE_ADMIN"))
                        );

                Usuario usuario = new Usuario();

                usuario.setUsername("sylvio_admin");
                usuario.setPassword("senha123");
                usuario.setEmail("sylvio@petshop.com");
                usuario.setAtivo(true);

                usuarioService.cadastrarUsuario(usuario, "ROLE_ADMIN");

                System.out.println("======================================");
                System.out.println("USUARIO CRIADO COM SUCESSO!");
                System.out.println("Login: sylvio_admin");
                System.out.println("Senha: senha123");
                System.out.println("Papel: ROLE_ADMIN");
                System.out.println("======================================");

            } else {
                System.out.println("Usuario sylvio_admin ja existe.");
            }
        };
    }
}