package br.tec.dev2b.whats.instancia.repository;

import br.tec.dev2b.whats.instancia.model.Instancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstanciaRepository extends JpaRepository<Instancia, UUID> {

    List<Instancia> findByEmpresaId(UUID empresaId);

    List<Instancia> findByUsuarioId(UUID usuarioId);

    Optional<Instancia> findByInstanceName(String instanceName);

    boolean existsByInstanceName(String instanceName);

    boolean existsByEmpresaId(UUID empresaId);
}
