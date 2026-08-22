package com.nutri.hospitalar.uti.repository;

import com.nutri.hospitalar.pessoa.enums.Sexo;
import com.nutri.hospitalar.uti.entity.PercentilCb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Tabela de referência, sem tenant e <b>somente leitura</b>: semeada pela
 * migration {@code 020}, nenhum service escreve nela.
 *
 * <p>A busca é por <b>faixa fechada nas duas pontas</b>, e isso é o conserto do
 * defeito 19: na planilha o {@code PROCV} devolve a linha anterior no limite, e
 * aos 59 anos lê o P50 da faixa 30–39,9 em vez do da 50–59,9. Aqui 59 cai em
 * {@code [50; 59.9]} porque a comparação é {@code <=} nas duas pontas.
 *
 * <p>Idade fora de toda faixa devolve vazio — e o calculador informa a ausência
 * com o motivo, em vez de escolher uma faixa qualquer.
 */
@Repository
public interface PercentilCbRepository extends JpaRepository<PercentilCb, UUID> {

    @Query("""
            SELECT p FROM PercentilCb p
            WHERE p.sexo = :sexo
              AND p.idadeMin <= :idadeAnos
              AND p.idadeMax >= :idadeAnos
            """)
    Optional<PercentilCb> findFaixaDe(@Param("sexo") Sexo sexo,
                                      @Param("idadeAnos") BigDecimal idadeAnos);
}
