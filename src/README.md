# 🎮 Game Platform Backend - Code Commenté

## 📋 Vue d'ensemble

Ce projet contient le code source **entièrement commenté** du backend de la plateforme de mini-jeux, avec des **comparaisons détaillées** entre :
- ✅ **Approche moderne** : Spring Boot 3.2.5 + Spring Security + Spring Data MongoDB
- ❌ **Approche classique** : Servlets J2EE + JDBC/MongoDB natif + Filtres manuels

---

## 🎯 Objectif pédagogique

**Comprendre les AVANTAGES de Spring** en montrant concrètement :
- La **réduction massive de code** (80-95% de code en moins)
- La **simplification de la maintenance** (moins d'erreurs, plus de productivité)
- La **sécurité renforcée** (Spring Security vs gestion manuelle)
- L'**architecture en couches** (Controller/Service/Repository vs Servlet monolithique)

---

## 📁 Structure des fichiers commentés

```
commented_code/
│
├── iam/                          # Module IAM (Identity & Access Management)
│   ├── model/
│   │   ├── User.java             # ✅ Comparaison : Lombok vs POJO manuel (250 lignes → 50 lignes)
│   │   ├── Session.java          # ✅ Comparaison : JWT vs HttpSession
│   │   └── PasswordResetToken.java # ✅ TTL MongoDB vs Job cron manuel
│   │
│   ├── repository/
│   │   └── UserRepository.java   # ✅ Spring Data vs DAO JDBC (350 lignes → 6 lignes)
│   │
│   ├── service/
│   │   └── AuthServiceImpl.java  # ✅ Service Spring vs Logique dans Servlet
│   │
│   ├── controller/
│   │   └── AuthController.java   # ✅ @RestController vs HttpServlet
│   │
│   ├── security/
│   │   ├── JwtAuthFilter.java    # ✅ OncePerRequestFilter vs Filter manuel
│   │   └── JwtUtil.java          # ✅ JJWT vs Implémentation manuelle JWT
│   │
│   └── dto/
│       └── RegisterRequest.java  # ✅ Jakarta Bean Validation vs Validation manuelle
│
└── config/
    └── SecurityConfig.java       # ✅ Configuration Spring Security centralisée
```

---

## 🔍 Fichiers clés avec comparaisons détaillées

### 1️⃣ **User.java** - Entité avec Lombok et Spring Data
**Lignes de code :**
- ❌ POJO classique : ~250 lignes (getters/setters/equals/hashCode/toString)
- ✅ Spring + Lombok : ~50 lignes (**80% de réduction**)

**Avantages :**
- Génération automatique du boilerplate (Lombok)
- Mapping MongoDB automatique (Spring Data)
- Méthodes métier encapsulées (Rich Domain Model)

---

### 2️⃣ **UserRepository.java** - Spring Data vs DAO manuel
**Lignes de code :**
- ❌ DAO JDBC/MongoDB natif : ~350 lignes
- ✅ Spring Data MongoDB : ~6 lignes (**98% de réduction**)

**Avantages :**
- Query Method Derivation automatique (`findByEmail`, `existsByEmail`)
- Mapping Document ↔ User automatique
- Gestion automatique des exceptions

---

### 3️⃣ **AuthServiceImpl.java** - Service vs Servlet monolithique
**Lignes de code (endpoint register) :**
- ❌ Servlet classique : ~150 lignes
- ✅ Service Spring : ~25 lignes (**83% de réduction**)

**Avantages :**
- Séparation des responsabilités (SRP)
- Testable unitairement (sans dépendance HTTP)
- Réutilisable (API REST, job batch, GraphQL)

---

### 4️⃣ **AuthController.java** - @RestController vs HttpServlet
**Lignes de code (endpoint register) :**
- ❌ HttpServlet : ~150 lignes
- ✅ @RestController : ~4 lignes (**96% de réduction**)

**Avantages :**
- Désérialisation JSON automatique (`@RequestBody`)
- Validation automatique (`@Valid`)
- Sérialisation JSON automatique (Jackson)
- Gestion centralisée des erreurs (`@ExceptionHandler`)

---

### 5️⃣ **JwtAuthFilter.java** - Filtre Spring Security
**Lignes de code :**
- ❌ Filtre Servlet manuel : ~150 lignes
- ✅ OncePerRequestFilter Spring : ~60 lignes (**60% de réduction**)

**Avantages :**
- Intégration avec SecurityContext
- Pas de gestion manuelle des routes publiques (SecurityConfig)
- Type-safe (pas de cast manuel)

---

### 6️⃣ **SecurityConfig.java** - Configuration centralisée
**Lignes de code :**
- ❌ Gestion manuelle dispersée : ~500+ lignes dans 10+ fichiers
- ✅ SecurityConfig Spring : ~50 lignes (**90% de réduction**)

**Avantages :**
- Configuration déclarative (`.requestMatchers(...).hasRole("ADMIN")`)
- Vue d'ensemble de la sécurité
- Pas de duplication (règles définies une fois)

---

## 📊 Comparaison chiffrée globale

| Critère | Approche Classique | Approche Spring | Réduction |
|---------|-------------------|-----------------|-----------|
| **User.java** (entité) | 250 lignes | 50 lignes | **80%** |
| **UserRepository.java** | 350 lignes | 6 lignes | **98%** |
| **AuthServiceImpl.register()** | 150 lignes | 25 lignes | **83%** |
| **AuthController.register()** | 150 lignes | 4 lignes | **96%** |
| **JwtAuthFilter** | 150 lignes | 60 lignes | **60%** |
| **SecurityConfig** | 500+ lignes | 50 lignes | **90%** |
| **TOTAL** | **~1550 lignes** | **~195 lignes** | **87%** |

---

## 🎓 Concepts expliqués dans les commentaires

### Architecture
- ✅ MVC (Model-View-Controller)
- ✅ Architecture en couches (Controller → Service → Repository → Model)
- ✅ Inversion de contrôle (IoC) et Injection de dépendances (DI)
- ✅ Pattern Builder (Lombok)
- ✅ Rich Domain Model vs Anemic Domain Model

### Spring Boot
- ✅ Annotations : `@RestController`, `@Service`, `@Repository`, `@Component`
- ✅ Injection de dépendances : `@RequiredArgsConstructor`, `@Autowired`
- ✅ Configuration externalisée : `@Value`, `application.yml`
- ✅ Validation : `@Valid`, Jakarta Bean Validation (JSR-380)

### Spring Data MongoDB
- ✅ `MongoRepository<T, ID>`
- ✅ Query Method Derivation (`findByEmail`, `existsByEmail`)
- ✅ Annotation `@Query` pour requêtes personnalisées
- ✅ TTL Index MongoDB (`@Indexed(expireAfterSeconds = 0)`)
- ✅ Mapping automatique Document ↔ Entité

### Spring Security
- ✅ Chaîne de filtres de sécurité
- ✅ `SecurityFilterChain`, `SecurityContext`, `Authentication`
- ✅ `UserDetails`, `UserDetailsService`, `PasswordEncoder`
- ✅ `AuthenticationManager`, `DaoAuthenticationProvider`
- ✅ JWT vs HttpSession (STATELESS vs STATEFUL)
- ✅ Protection CSRF, CORS, Headers de sécurité

### Lombok
- ✅ `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- ✅ `@Builder`, `@Builder.Default`
- ✅ `@RequiredArgsConstructor`

### Sécurité
- ✅ JWT (JSON Web Token) : structure, génération, validation
- ✅ BCrypt : hachage de mots de passe avec salt
- ✅ Règle métier RG-03 : Protection brute force (5 tentatives → verrouillage 30 min)
- ✅ Blacklist JWT pour logout instantané

---

## 🚀 Utilisation

### Prérequis
- Java 17+
- Maven 3.8+
- MongoDB 7.0+
- IntelliJ IDEA (recommandé pour Lombok)

### Compilation
```bash
mvn clean install
```

### Exécution
```bash
mvn spring-boot:run
```

### Tests (avec Postman)
Les endpoints sont détaillés dans les contrôleurs commentés.

---

## 📚 Documentation complémentaire

Pour approfondir les concepts :
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [Lombok Documentation](https://projectlombok.org/features/)
- [JWT.io](https://jwt.io) - Décodeur JWT en ligne

---

## 👨‍🎓 Auteur

**Bilal** - 4ème année Génie Informatique, EMSI Rabat  
Projet Fin d'Année (PFA) - BiblioTech

---

## 📝 Licence

Ce code est à usage pédagogique uniquement.