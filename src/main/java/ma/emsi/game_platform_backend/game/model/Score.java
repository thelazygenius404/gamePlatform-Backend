package ma.emsi.game_platform_backend.game.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  DOCUMENT SCORE - INDEX COMPOSÉS POUR LEADERBOARDS & RICH DOMAIN MODEL
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : L'INDEX "LEADERBOARD" (Tri Pré-calculé)                     │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Imaginons un jeu avec 10 millions de scores. Sans index, si le Repository
 * demande `findTop10ByGameIdOrderByValueDesc`, MongoDB doit :
 * 1. Trouver les 10 millions de scores du jeu.
 * 2. Les charger en mémoire vive (RAM).
 * 3. Les trier (Sort) pour trouver les 10 meilleurs.
 * -> Résultat : L'application crash (Out Of Memory) ou la requête prend 5 secondes.
 *
 * L'index `{'gameId': 1, 'value': -1}` crée un arbre de données pré-trié :
 * • `gameId: 1`  : Regroupe d'abord par jeu (ordre alphabétique de l'ID).
 * • `value: -1`  : À l'intérieur de ce jeu, trie les scores du PLUS GRAND au
 *                  plus petit (-1 = Descending).
 *
 * -> Résultat : MongoDB lit les 10 premières lignes de l'index et s'arrête.
 *               La requête prend 0.001 seconde, même avec 10 millions de scores.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : L'INDEX "USER HISTORY" (Filtrage Multiple)                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * L'index `{'userId': 1, 'gameId': 1}` optimise la méthode du Repository :
 * `findTopByUserIdAndGameId...` (Le Record Personnel).
 * Il est toujours plus performant de créer UN index composé sur deux colonnes
 * plutôt que DEUX index simples que MongoDB devra essayer de croiser.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "scores")
@CompoundIndexes({
        // Index vital pour les Leaderboards (Tri décroissant sur la valeur)
        @CompoundIndex(name = "idx_leaderboard", def = "{'gameId': 1, 'value': -1}"),

        // Index vital pour retrouver le profil/historique d'un joueur sur un jeu
        @CompoundIndex(name = "idx_user_game", def = "{'userId': 1, 'gameId': 1}")
})
public class Score {

    @Id
    private String id;

    /**
     * Identifiant du joueur (référence à l'entité User/Player gérée ailleurs)
     */
    private String userId;

    /**
     * Identifiant du jeu concerné
     */
    private String gameId;

    /**
     * Le score brut réalisé dans le jeu (ex: 15000)
     */
    private int value;

    /**
     * L'expérience ou la monnaie virtuelle gagnée grâce à ce score.
     */
    private int pointsEarned;

    /**
     * Temps passé sur la partie en secondes (utile pour des stats ou anti-cheat)
     */
    private int duration;

    /**
     * Date et heure de la réalisation du score
     */
    private LocalDateTime playedAt;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CONCEPT CLÉ 3 : LE RICH DOMAIN MODEL (Modèle de Domaine Riche)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Au lieu de faire le calcul dans le Service (ce qu'on appelle un Modèle
     * Anémique), l'entité porte elle-même sa propre logique métier.
     *
     * C'est une excellente pratique de Programmation Orientée Objet (POO).
     * Le service se contentera d'appeler `score.calculatePointsEarned(game.getMultiplier())`.
     *
     * @param multiplier Le multiplicateur spécifique au jeu (ex: Jeux Premium = x1.5)
     * @return Les points calculés à enregistrer dans le profil du joueur.
     */
    public int calculatePointsEarned(double multiplier) {
        return (int) Math.round(value * multiplier);
    }
}