package com.nutri.hospitalar.uti.repository;

import com.nutri.hospitalar.uti.entity.RegistroDiarioUti;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entidade de negócio: tudo passa pelo tenant.
 */
@Repository
public interface RegistroDiarioUtiRepository extends JpaRepository<RegistroDiarioUti, UUID> {

    Optional<RegistroDiarioUti> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Já existe registro deste paciente neste dia?
     *
     * <p>A {@code UNIQUE} do banco é a garantia; esta consulta existe para o
     * service poder recusar com uma mensagem que diz <b>qual</b> dia, em vez de
     * deixar a violação de constraint chegar crua ao usuário.
     */
    Optional<RegistroDiarioUti> findByTenantIdAndPessoaIdAndData(
            UUID tenantId, UUID pessoaId, LocalDate data);

    /** Quantos dias estão presos a esta avaliação — para o 409 dizer o número. */
    long countByTenantIdAndAvaliacaoId(UUID tenantId, UUID avaliacaoId);

    /**
     * A avaliação mais recente daquele paciente <b>até</b> aquela data.
     *
     * <p>Até, e não a mais recente de todas: um dia de três meses atrás não deve
     * ser comparado com a prescrição de ontem.
     */
    @Query(value = """
            SELECT a.id FROM avaliacao_uti a
            WHERE a.tenant_id = CAST(:tenantId AS uuid)
              AND a.paciente_id = CAST(:pessoaId AS uuid)
              AND a.data_avaliacao <= CAST(:data AS date)
            ORDER BY a.data_avaliacao DESC, a.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findIdDaAvaliacaoVigente(@Param("tenantId") UUID tenantId,
                                            @Param("pessoaId") UUID pessoaId,
                                            @Param("data") LocalDate data);

    /**
     * <b>A lista de trabalho do dia.</b> Quem está em acompanhamento, e quem
     * ainda não tem registro de hoje.
     *
     * <p>Um paciente entra aqui quando teve registro na janela recente <b>e</b>
     * a avaliação vigente dele <b>não foi encerrada</b>. O encerramento é a
     * saída: sem ele, quem recebeu alta seria cobrado todos os dias e a lista
     * viraria um cemitério — pior que lista nenhuma, porque quem usa aprende a
     * ignorá-la.
     *
     * <p>A janela é rede, não regra clínica: pega quem ninguém encerrou. Por
     * isso a coluna {@code diasSemRegistro} vai junto — a tela mostra o número
     * e quem lê decide, em vez de o sistema arbitrar um limite que ninguém
     * publicou.
     *
     * <p>{@code ultimaAvaliacao} é a mais recente <b>até hoje</b>, pela mesma
     * razão de {@link #findIdDaAvaliacaoVigente}: uma avaliação futura não
     * governa o dia de hoje.
     */
    @Query(value = """
            WITH recente AS (
                SELECT r.pessoa_id, max(r.data) AS ultimo_dia
                FROM registro_diario_uti r
                WHERE r.tenant_id = CAST(:tenantId AS uuid)
                  AND r.data >= CAST(:desde AS date)
                  AND r.data <= CAST(:hoje AS date)
                GROUP BY r.pessoa_id
            )
            SELECT p.id                                    AS pessoa_id,
                   p.nome                                  AS pessoa_nome,
                   rec.ultimo_dia                          AS ultimo_dia,
                   (CAST(:hoje AS date) - rec.ultimo_dia)  AS dias_sem_registro,
                   av.id                                   AS avaliacao_id,
                   av.data_avaliacao                       AS data_avaliacao,
                   (CAST(:hoje AS date) - av.data_avaliacao) AS dias_de_terapia
            FROM recente rec
            JOIN pessoa p ON p.id = rec.pessoa_id
            LEFT JOIN LATERAL (
                SELECT a.id, a.data_avaliacao
                FROM avaliacao_uti a
                WHERE a.tenant_id = CAST(:tenantId AS uuid)
                  AND a.paciente_id = rec.pessoa_id
                  AND a.data_avaliacao <= CAST(:hoje AS date)
                ORDER BY a.data_avaliacao DESC, a.created_at DESC
                LIMIT 1
            ) av ON true
            WHERE NOT EXISTS (
                SELECT 1 FROM avaliacao_uti e
                WHERE e.id = av.id AND e.encerrado_em IS NOT NULL
            )
            ORDER BY rec.ultimo_dia, p.nome
            """, nativeQuery = true)
    List<PacienteEmAcompanhamento> findEmAcompanhamento(@Param("tenantId") UUID tenantId,
                                                        @Param("desde") LocalDate desde,
                                                        @Param("hoje") LocalDate hoje);

    /**
     * Adesão média por paciente na janela, numa consulta só.
     *
     * <p>Repete a regra de {@code AcompanhamentoCalculator.percentualRecebido}:
     * <b>o prescrito da avaliação vence o digitado</b>, e sem avaliação cai no
     * digitado. Repetir a regra em SQL é o preço de não fazer N+1 aqui — e é a
     * razão de este javadoc existir: se a regra mudar lá, muda aqui.
     *
     * <p>Dia sem volume recebido não entra na média. Ausência não é zero: um dia
     * não preenchido afundaria a média de quem só esqueceu de digitar.
     */
    @Query(value = """
            SELECT r.pessoa_id AS pessoa_id,
                   avg(r.vol_recebido_24h * 100.0
                       / NULLIF(COALESCE(av.volume_total_ml, r.vol_prescrito_24h), 0)) AS adesao_media,
                   count(*) AS dias
            FROM registro_diario_uti r
            LEFT JOIN avaliacao_uti av ON av.id = r.avaliacao_id
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND r.data >= CAST(:desde AS date)
              AND r.data <= CAST(:hoje AS date)
              AND r.vol_recebido_24h IS NOT NULL
              AND COALESCE(av.volume_total_ml, r.vol_prescrito_24h) > 0
            GROUP BY r.pessoa_id
            """, nativeQuery = true)
    List<AdesaoDoPaciente> findAdesaoMediaNaJanela(@Param("tenantId") UUID tenantId,
                                                   @Param("desde") LocalDate desde,
                                                   @Param("hoje") LocalDate hoje);

    interface AdesaoDoPaciente {
        UUID getPessoaId();
        BigDecimal getAdesaoMedia();
        Integer getDias();
    }

    /**
     * Quantos dias foram registrados na janela — <b>todos</b>, e não só os que
     * tinham volume recebido.
     *
     * <p>Existe porque somar o {@code dias} da consulta de adesão contaria
     * outra coisa: lá o dia sem volume fica de fora de propósito, para não
     * afundar a média de quem só esqueceu de digitar. Usar aquele número como
     * "dias registrados" faria a tela dizer menos trabalho do que houve.
     */
    @Query(value = """
            SELECT count(*) FROM registro_diario_uti r
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND r.data >= CAST(:desde AS date)
              AND r.data <= CAST(:ate AS date)
            """, nativeQuery = true)
    long contarDiasNaJanela(@Param("tenantId") UUID tenantId,
                            @Param("desde") LocalDate desde,
                            @Param("ate") LocalDate ate);

    /** Quem já tem registro nesta data — os que saem da lista de pendentes. */
    @Query(value = """
            SELECT DISTINCT r.pessoa_id FROM registro_diario_uti r
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND r.data = CAST(:data AS date)
            """, nativeQuery = true)
    List<UUID> findPessoasComRegistroEm(@Param("tenantId") UUID tenantId,
                                        @Param("data") LocalDate data);

    /**
     * Projeção da lista de trabalho. Interface e não record: é assim que o
     * Spring Data mapeia native query sem entidade.
     */
    interface PacienteEmAcompanhamento {
        UUID getPessoaId();
        String getPessoaNome();
        LocalDate getUltimoDia();
        Integer getDiasSemRegistro();
        UUID getAvaliacaoId();
        LocalDate getDataAvaliacao();
        Integer getDiasDeTerapia();
    }

    /**
     * Os dias que alimentam os painéis — sem paginação e em <b>ordem
     * cronológica crescente</b>, que é o eixo X dos gráficos. A lista da tela
     * ordena ao contrário porque lá o dia de hoje é o que importa primeiro.
     */
    @Query(value = """
            SELECT r.* FROM registro_diario_uti r
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            ORDER BY r.data
            """, nativeQuery = true)
    List<RegistroDiarioUti> findParaPainel(@Param("tenantId") UUID tenantId,
                                           @Param("pessoaId") UUID pessoaId,
                                           @Param("de") LocalDate de,
                                           @Param("ate") LocalDate ate);

    @Query(value = """
            SELECT r.* FROM registro_diario_uti r
            JOIN pessoa p ON p.id = r.pessoa_id
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:pessoaNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pessoaNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            ORDER BY r.data DESC, p.nome
            """,
            countQuery = """
            SELECT count(*) FROM registro_diario_uti r
            JOIN pessoa p ON p.id = r.pessoa_id
            WHERE r.tenant_id = CAST(:tenantId AS uuid)
              AND (CAST(:pessoaId AS uuid) IS NULL OR r.pessoa_id = CAST(:pessoaId AS uuid))
              AND (CAST(:pessoaNome AS text) IS NULL
                   OR unaccent(lower(p.nome)) LIKE unaccent(lower('%' || CAST(:pessoaNome AS text) || '%')))
              AND (CAST(:de  AS date) IS NULL OR r.data >= CAST(:de  AS date))
              AND (CAST(:ate AS date) IS NULL OR r.data <= CAST(:ate AS date))
            """,
            nativeQuery = true)
    Page<RegistroDiarioUti> findAllWithFilters(Pageable pageable,
                                               @Param("tenantId") UUID tenantId,
                                               @Param("pessoaId") UUID pessoaId,
                                               @Param("pessoaNome") String pessoaNome,
                                               @Param("de") LocalDate de,
                                               @Param("ate") LocalDate ate);
}
