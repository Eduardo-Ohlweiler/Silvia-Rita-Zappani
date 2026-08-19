package com.nutri.hospitalar.pediatria.repository;

import com.nutri.hospitalar.pediatria.entity.PercentilOms;
import com.nutri.hospitalar.pessoa.enums.Sexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tabela de referência, sem tenant e <b>somente leitura</b>: é semeada pela
 * migration {@code 015} e nenhum service escreve nela.
 *
 * <p>A busca é por igualdade exata de mês inteiro, como na planilha. Não há
 * interpolação: idade de 8,5 meses ou de 61 meses simplesmente não encontra
 * linha, e o calculador devolve a ausência com o motivo.
 */
@Repository
public interface PercentilOmsRepository extends JpaRepository<PercentilOms, UUID> {

    Optional<PercentilOms> findBySexoAndIdadeMeses(Sexo sexo, Integer idadeMeses);

    /**
     * A janela de meses que a curva de crescimento vai desenhar.
     *
     * <p>Ordenada pela idade: o gráfico plota faixa contínua e um ponto fora de
     * ordem desenharia a área virada do avesso.
     */
    List<PercentilOms> findBySexoAndIdadeMesesBetweenOrderByIdadeMesesAsc(
            Sexo sexo, Integer idadeMin, Integer idadeMax);
}
