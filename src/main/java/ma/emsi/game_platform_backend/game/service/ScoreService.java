package ma.emsi.game_platform_backend.game.service;

import ma.emsi.game_platform_backend.game.dto.LeaderboardDTO;
import ma.emsi.game_platform_backend.game.dto.ScoreDTO;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  SCORE SERVICE - ORCHESTRATION MÉTIER & PATTERN DTO
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : LE RÔLE DU SERVICE (Orchestration)                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Pourquoi ne pas appeler directement `scoreRepository.save()` dans le Controller ?
 * Parce qu'une action métier comme "Soumettre un score" implique souvent plusieurs
 * opérations complexes (règles de gestion) :
 *
 * 1. Vérifier que le jeu existe (appel au GameRepository).
 * 2. Vérifier que le joueur existe et n'est pas banni (UserRepository).
 * 3. Récupérer le "multiplier" du jeu pour calculer l'XP gagnée.
 * 4. Sauvegarder le score (ScoreRepository).
 * 5. Mettre à jour les statistiques globales du jeu (totalPlays, averageScore).
 * 6. Mettre à jour le solde de points/XP du joueur.
 *
 * Le Service est le "Chef d'Orchestre" qui coordonne tous ces Repositories au
 * sein d'une même Transaction (garantissant que si une étape échoue, tout est annulé).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : LE PATTERN DTO (Data Transfer Object)                       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Remarquez que les méthodes retournent des `...DTO` et NON les entités MongoDB
 * (`Score` ou `Game`). Pourquoi ?
 *
 * 1. Sécurité : On ne veut jamais exposer la structure interne de notre BDD
 *    ni des champs sensibles (ex: mots de passe, tokens) à l'API publique.
 * 2. Performance : Un DTO permet de ne renvoyer que les données strictement
 *    nécessaires pour l'écran Front-End ciblé (économie de bande passante).
 * 3. Agrégation : Un `LeaderboardDTO` va contenir le "Score" + le "Pseudo du
 *    joueur" + "l'Avatar du joueur", nécessitant de fusionner les données de
 *    la collection `scores` et de la collection `users`.
 */
public interface ScoreService {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * SOUMISSION D'UN SCORE (Opération Transactionnelle)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * L'implémentation de cette méthode devra idéalement être annotée avec
     * `@Transactional`. C'est l'opération métier la plus critique de la plateforme.
     *
     * Étapes attendues dans l'implémentation :
     * 1. Vérifier si `value` > 0 et `duration` > 0 (Anti-triche basique).
     * 2. Récupérer le `Game` pour appliquer le multiplicateur.
     * 3. Créer et sauvegarder l'entité `Score`.
     * 4. Mettre à jour l'XP du `User`.
     *
     * @param userId L'ID du joueur qui a fait la partie.
     * @param gameId L'ID du jeu joué.
     * @param value Le score brut obtenu dans le jeu.
     * @param duration Le temps passé sur la partie (utile pour détecter la triche).
     * @return Le DTO contenant le score final et les points/XP gagnés.
     */
    ScoreDTO submitScore(String userId, String gameId, int value, int duration);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LE CLASSEMENT GÉNÉRAL (Enrichissement de données)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * L'implémentation va appeler `scoreRepository.findTop10ByGameId...`.
     * MAIS l'entité Score ne contient que le `userId` (ex: "65a1b2c3...").
     *
     * Pour que le Front-End puisse afficher un classement propre, cette méthode
     * métier doit faire l'enrichissement : elle prend la liste des `userId` du Top 10,
     * va chercher leurs Pseudos/Avatars dans le UserRepository, et assemble le
     * tout dans un beau `LeaderboardDTO`.
     *
     * @param gameId L'identifiant du jeu.
     * @return Un objet structuré contenant la liste des meilleurs joueurs (pseudos + scores).
     */
    LeaderboardDTO getLeaderboard(String gameId);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LE RECORD PERSONNEL (Consultation)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Permet d'afficher à l'utilisateur son meilleur score absolu sur un jeu
     * donné avant même qu'il ne lance la partie (pour le motiver à se battre
     * lui-même).
     *
     * Si l'utilisateur n'a jamais joué, l'implémentation peut renvoyer un
     * Custom Exception (ex: `ScoreNotFoundException`) ou renvoyer `null` / un
     * DTO vide selon votre convention d'API.
     *
     * @param userId L'identifiant de l'utilisateur connecté.
     * @param gameId L'identifiant du jeu affiché à l'écran.
     * @return Le DTO formaté du meilleur score du joueur.
     */
    ScoreDTO getPersonalBest(String userId, String gameId);
}