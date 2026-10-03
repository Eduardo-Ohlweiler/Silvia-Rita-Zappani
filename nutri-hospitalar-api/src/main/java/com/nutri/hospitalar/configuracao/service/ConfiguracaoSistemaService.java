package com.nutri.hospitalar.configuracao.service;

import com.nutri.hospitalar.configuracao.dtos.AparenciaResponseDto;
import com.nutri.hospitalar.configuracao.dtos.AparenciaUpdateDto;
import com.nutri.hospitalar.configuracao.entity.ConfiguracaoSistema;
import com.nutri.hospitalar.configuracao.enums.PaletaSistema;
import com.nutri.hospitalar.configuracao.mapper.ConfiguracaoMapper;
import com.nutri.hospitalar.configuracao.repository.ConfiguracaoSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoSistemaService {

    private final ConfiguracaoSistemaRepository repository;

    /**
     * Sem linha — banco montado à mão, fora da migration — a resposta é a
     * paleta padrão, e não um erro: a tela de login depende disto para abrir,
     * e cor é a última coisa que deve impedir alguém de entrar.
     */
    @Transactional(readOnly = true)
    public AparenciaResponseDto aparencia() {
        return repository.findFirstByOrderByCreatedAtAsc()
                .map(ConfiguracaoMapper::toAparencia)
                .orElseGet(() -> new AparenciaResponseDto(PaletaSistema.PADRAO));
    }

    @Transactional
    public AparenciaResponseDto atualizarAparencia(AparenciaUpdateDto dto) {
        ConfiguracaoSistema configuracao = repository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(ConfiguracaoSistema::new);
        configuracao.setPaleta(dto.paleta());
        log.info("Paleta do sistema alterada para {}", dto.paleta());
        return ConfiguracaoMapper.toAparencia(repository.save(configuracao));
    }
}
