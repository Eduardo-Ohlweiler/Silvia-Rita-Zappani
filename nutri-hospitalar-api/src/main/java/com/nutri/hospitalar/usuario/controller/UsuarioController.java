package com.nutri.hospitalar.usuario.controller;

import com.nutri.hospitalar.usuario.dtos.PerfilUpdateDto;
import com.nutri.hospitalar.usuario.dtos.SenhaUpdateDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioCreateDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioResponseDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioSelectDto;
import com.nutri.hospitalar.usuario.dtos.UsuarioUpdateDto;
import com.nutri.hospitalar.usuario.enums.Role;
import com.nutri.hospitalar.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Lista os usuários do tenant efetivo")
    public ResponseEntity<Page<UsuarioResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo) {
        return ResponseEntity.ok(usuarioService.getAll(pageable, nome, email, role, ativo));
    }

    @GetMapping("/global")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Lista usuários de todos os tenants",
            description = """
                    Único ponto do módulo que lê através dos tenants. Existe
                    porque, ao cadastrar um cliente novo, o usuário nasce no
                    tenant recém-criado — e não apareceria na listagem do
                    tenant efetivo. Aceita `tenantId` para filtrar.""")
    public ResponseEntity<Page<UsuarioResponseDto>> getAllGlobal(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo) {
        return ResponseEntity.ok(
                usuarioService.getAllGlobal(pageable, tenantId, nome, email, role, ativo));
    }

    @GetMapping("/select")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Usuários ativos do tenant, para combo")
    public ResponseEntity<List<UsuarioSelectDto>> select(@RequestParam(required = false) String nome) {
        return ResponseEntity.ok(usuarioService.select(nome));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Detalha um usuário do tenant efetivo")
    public ResponseEntity<UsuarioResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.findByIdResponse(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Cria um usuário")
    public ResponseEntity<UsuarioResponseDto> create(@Valid @RequestBody UsuarioCreateDto dto) {
        UsuarioResponseDto criado = usuarioService.create(dto);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Altera um usuário")
    public ResponseEntity<UsuarioResponseDto> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UsuarioUpdateDto dto) {
        return ResponseEntity.ok(usuarioService.update(id, dto));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Ativa ou desativa um usuário")
    public ResponseEntity<UsuarioResponseDto> alterarAtivo(@PathVariable UUID id,
                                                           @RequestParam boolean ativo) {
        return ResponseEntity.ok(usuarioService.alterarAtivo(id, ativo));
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Libera conta bloqueada por tentativas de login")
    public ResponseEntity<UsuarioResponseDto> desbloquear(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.desbloquear(id));
    }

    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<UsuarioResponseDto> getPerfil() {
        return ResponseEntity.ok(usuarioService.getPerfil());
    }

    @PutMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Altera o próprio nome e telefone")
    public ResponseEntity<UsuarioResponseDto> updatePerfil(@Valid @RequestBody PerfilUpdateDto dto) {
        return ResponseEntity.ok(usuarioService.updatePerfil(dto));
    }

    @PatchMapping("/perfil/senha")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Altera a própria senha", description = "Exige a senha atual.")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody SenhaUpdateDto dto) {
        usuarioService.alterarSenha(dto);
        return ResponseEntity.noContent().build();
    }
}
