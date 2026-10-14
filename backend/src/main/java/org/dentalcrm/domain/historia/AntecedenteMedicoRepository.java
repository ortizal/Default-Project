package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AntecedenteMedicoRepository extends JpaRepository<AntecedenteMedico, Long> {

    List<AntecedenteMedico> findByHistoriaIdOrderByTipoAscNombreAsc(Long historiaId);

    long countByHistoriaIdAndTipo(Long historiaId, TipoAntecedente tipo);

    void deleteByHistoriaId(Long historiaId);
}
