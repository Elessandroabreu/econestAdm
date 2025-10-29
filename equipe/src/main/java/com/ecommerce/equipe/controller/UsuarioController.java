package com.ecommerce.equipe.controller;

import com.ecommerce.equipe.dto.UsuarioDto;
import com.ecommerce.equipe.model.UsuarioModel;
import com.ecommerce.equipe.repository.UsuarioRepository;
import com.ecommerce.equipe.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDto> salvar(@ModelAttribute @Valid UsuarioDto usuarioDto) {
        UsuarioDto usuario = usuarioService.salvar(usuarioDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    // APENAS ADMIN pode listar todos os usuários
    @GetMapping
    public ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // Usuário autenticado pode ver seu próprio perfil, Admin pode ver qualquer perfil
    @GetMapping("/{cdUsuario}")
    public ResponseEntity<Object> buscar(@PathVariable Integer cdUsuario) {
        try {
            // Verificar se o usuário está tentando acessar seu próprio perfil ou se é admin
            if (!isOwnerOrAdmin(cdUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não tem permissão para acessar este perfil");
            }

            UsuarioDto usuario = usuarioService.buscarPorId(cdUsuario);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Usuário autenticado pode atualizar seu próprio perfil, Admin pode atualizar qualquer perfil
    @PutMapping(value = "/{cdUsuario}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> atualizar(@PathVariable Integer cdUsuario,
                                            @ModelAttribute @Valid UsuarioDto usuarioDto) {
        try {
            // Verificar se o usuário está tentando atualizar seu próprio perfil ou se é admin
            if (!isOwnerOrAdmin(cdUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não tem permissão para atualizar este perfil");
            }

            UsuarioDto usuario = usuarioService.atualizar(cdUsuario, usuarioDto);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // APENAS ADMIN pode inativar usuários
    @DeleteMapping("/{cdUsuario}")
    public ResponseEntity<String> inativar(@PathVariable Integer cdUsuario) {
        try {
            usuarioService.inativar(cdUsuario);
            return ResponseEntity.ok("Usuario inativado com sucesso");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Usuário autenticado pode ver sua própria imagem, Admin pode ver qualquer imagem
    @GetMapping("/{cdUsuario}/imagem")
    public ResponseEntity<byte[]> buscarImagem(@PathVariable Integer cdUsuario) {
        try {
            // Verificar se o usuário está tentando acessar sua própria imagem ou se é admin
            if (!isOwnerOrAdmin(cdUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UsuarioModel usuario = usuarioRepository.findById(cdUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            byte[] imagem = usuario.getImgUsuario();

            if (imagem == null || imagem.length == 0) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imagem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Verifica se o usuário logado é o dono do recurso ou é admin
     */
    private boolean isOwnerOrAdmin(Integer cdUsuario) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Verificar se é ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));

        if (isAdmin) {
            return true;
        }

        // Verificar se é o próprio usuário
        String email = authentication.getName();
        UsuarioModel usuarioLogado = usuarioRepository.findByNmEmail(email)
                .orElse(null);

        if (usuarioLogado == null) {
            return false;
        }

        return usuarioLogado.getCdUsuario().equals(cdUsuario);
    }
}