package org.dentalcrm.domain.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    // Los lookups de login/unicidad deben cruzar tenants: el username y el
    // email son únicos a nivel global. Las consultas nativas no reciben el
    // filtro de tenant que Hibernate aplica a las entidades anotadas.
    @Query(value = """
            SELECT * FROM usuarios u
            WHERE lower(u.username) = lower(:usuario)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Usuario> buscarGlobalPorUsername(@Param("usuario") String usuario);

    @Query(value = """
            SELECT * FROM usuarios u
            WHERE lower(u.email) = lower(:email)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Usuario> buscarGlobalPorEmail(@Param("email") String email);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM usuarios u
                WHERE lower(u.username) = lower(:usuario)
            )
            """, nativeQuery = true)
    boolean existeGlobalUsername(@Param("usuario") String usuario);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM usuarios u
                WHERE lower(u.email) = lower(:email)
            )
            """, nativeQuery = true)
    boolean existeGlobalEmail(@Param("email") String email);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:q = '' OR lower(u.username) LIKE lower(concat('%', :q, '%'))
                         OR lower(u.email) LIKE lower(concat('%', :q, '%'))
                         OR lower(u.nombres) LIKE lower(concat('%', :q, '%'))
                         OR lower(u.apellidos) LIKE lower(concat('%', :q, '%')))
            """)
    Page<Usuario> buscar(@Param("q") String q, Pageable pageable);
}