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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    // CORRIGIDO: Agora aceita MultipartFile
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDto> salvar(@ModelAttribute @Valid UsuarioDto usuarioDto) {
        UsuarioDto usuario = usuarioService.salvar(usuarioDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{cdUsuario}")
    public ResponseEntity<Object> buscar(@PathVariable Integer cdUsuario) {
        try{
            UsuarioDto usuario = usuarioService.buscarPorId(cdUsuario);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // CORRIGIDO: Agora aceita MultipartFile
    @PutMapping(value = "/{cdUsuario}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> atualizar(@PathVariable Integer cdUsuario, @ModelAttribute @Valid UsuarioDto usuarioDto) {
        try{
            UsuarioDto usuario = usuarioService.atualizar(cdUsuario, usuarioDto);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{cdUsuario}")
    public ResponseEntity<String> inativar(@PathVariable Integer cdUsuario) {
        try{
            usuarioService.inativar(cdUsuario);
            return ResponseEntity.ok("Usuario inativado com sucesso");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // NOVO: Endpoint para buscar a imagem do usuário
    @GetMapping("/{cdUsuario}/imagem")
    public ResponseEntity<byte[]> buscarImagem(@PathVariable Integer cdUsuario) {
        try {
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
}