package com.UERJ.POO3.modules.seguranca.service;

import com.UERJ.POO3.modules.seguranca.domain.Papel;
import com.UERJ.POO3.modules.seguranca.domain.Usuario;
import com.UERJ.POO3.modules.seguranca.repository.PapelRepository;
import com.UERJ.POO3.modules.seguranca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PapelRepository papelRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioTeste;
    private Papel papelTeste;

    // Configuração inicial executada antes de cada teste
    @BeforeEach
    void setUp() {
        usuarioTeste = new Usuario();
        usuarioTeste.setUsername("sylvio_admin");
        usuarioTeste.setPassword("senha123");
        usuarioTeste.setEmail("sylvio@petshop.com");

        papelTeste = new Papel("ROLE_ADMIN");
    }

    @Test
    void deveCadastrarUsuarioComSucessoECriptografarSenha() {
        // Arrange (Preparação)
        when(usuarioRepository.findByUsername(usuarioTeste.getUsername())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(usuarioTeste.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hash_criptografado_seguro");
        when(papelRepository.findByNome("ROLE_ADMIN")).thenReturn(Optional.of(papelTeste));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioTeste);

        // Act (Ação)
        Usuario usuarioSalvo = usuarioService.cadastrarUsuario(usuarioTeste, "ROLE_ADMIN");

        // Assert (Verificação)
        assertNotNull(usuarioSalvo);
        assertEquals("hash_criptografado_seguro", usuarioSalvo.getPassword());
        assertTrue(usuarioSalvo.getPapeis().contains(papelTeste));

        // Verifica se o método save foi chamado exatamente 1 vez
        verify(usuarioRepository, times(1)).save(usuarioTeste);
    }

    @Test
    void deveLancarExcecaoAoCadastrarUsernameDuplicado() {
        // Arrange
        when(usuarioRepository.findByUsername(usuarioTeste.getUsername())).thenReturn(Optional.of(usuarioTeste));

        // Act & Assert
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.cadastrarUsuario(usuarioTeste, "ROLE_ADMIN");
        });

        assertEquals("Username já está em uso.", excecao.getMessage());

        // Garante que o sistema NUNCA chamou o save() do banco de dados neste cenário
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveGerarTokenEEnviarEmailDeRecuperacao() {
        // Arrange
        when(usuarioRepository.findByEmail(usuarioTeste.getEmail())).thenReturn(Optional.of(usuarioTeste));

        // Act
        String tokenGerado = usuarioService.gerarTokenRecuperacaoSenha(usuarioTeste.getEmail());

        // Assert
        assertNotNull(tokenGerado);
        assertNotNull(usuarioTeste.getResetToken());
        assertNotNull(usuarioTeste.getTokenExpiracao());

        // Verifica se o e-mail foi disparado
        verify(emailService, times(1)).enviarEmailRecuperacao(usuarioTeste.getEmail(), tokenGerado);
        verify(usuarioRepository, times(1)).save(usuarioTeste);
    }

    @Test
    void deveRedefinirSenhaComTokenValido() {
        // Arrange
        String token = "token-uuid-valido";
        usuarioTeste.setResetToken(token);
        usuarioTeste.setTokenExpiracao(LocalDateTime.now().plusMinutes(30)); // Token válido por mais 30 min

        when(usuarioRepository.findByResetToken(token)).thenReturn(Optional.of(usuarioTeste));
        when(passwordEncoder.encode("novaSenhaSegura")).thenReturn("novo_hash_seguro");

        // Act
        usuarioService.redefinirSenha(token, "novaSenhaSegura");

        // Assert
        assertEquals("novo_hash_seguro", usuarioTeste.getPassword());
        assertNull(usuarioTeste.getResetToken()); // Token deve ser limpo após o uso
        assertNull(usuarioTeste.getTokenExpiracao());
        verify(usuarioRepository, times(1)).save(usuarioTeste);
    }

    @Test
    void deveLancarExcecaoAoTentarRedefinirSenhaComTokenExpirado() {
        // Arrange
        String token = "token-uuid-expirado";
        usuarioTeste.setResetToken(token);
        usuarioTeste.setTokenExpiracao(LocalDateTime.now().minusMinutes(10)); // Expirou há 10 minutos

        when(usuarioRepository.findByResetToken(token)).thenReturn(Optional.of(usuarioTeste));

        // Act & Assert
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.redefinirSenha(token, "novaSenhaTeste");
        });

        assertEquals("O token de recuperação expirou.", excecao.getMessage());
        verify(passwordEncoder, never()).encode(anyString()); // Garante que não tentou alterar a senha
    }
}