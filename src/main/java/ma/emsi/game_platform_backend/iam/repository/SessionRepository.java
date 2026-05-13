package ma.emsi.game_platform_backend.iam.repository;


import ma.emsi.game_platform_backend.iam.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des sessions JWT (blacklist/révocation).
 *
 * SANS Spring : classe SessionDAO avec MongoCollection<Document> sessions;
 * méthodes findByJwtToken(), findByUserId() écrites manuellement avec
 * Filters.eq("jwtToken", token) et mapping manuel.
 *
 * AVEC Spring Data : méthodes générées par Query Derivation.
 */
@Repository
public interface SessionRepository extends MongoRepository<Session, String> {

    Optional<Session> findByJwtToken(String jwtToken);

    List<Session> findByUserId(String userId);

    /** Vérifie si un token est dans la blacklist (révoqué). */
    boolean existsByJwtTokenAndIsRevokedTrue(String jwtToken);
}