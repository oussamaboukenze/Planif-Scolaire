# Guide : lancer et tester Planif-Scolaire

## 1. L'ordre de lancement

1. **Docker Desktop** ouvert, puis à la racine : `docker compose up -d` (Kafka 9092, Redis 6379, Mailpit 1025/8025).
2. Dans IntelliJ, fenêtre **Services** (en bas) : lancer dans l'ordre
   `ConfigServer` (8888) → `Eureka` (8761) → `Referentiel`, `Planning`, `Presences`, `Notifications` → `Gateway` (8080).

## 2. Chaque service : rôle et comment le reconnaître

| Service | Port | Rôle | Test rapide |
| --- | --- | --- | --- |
| config-server | 8888 | Sert la config de `config-repo/` à tous les services | http://localhost:8888/service-planning/default |
| service-registry (Eureka) | 8761 | Annuaire : chaque service s'y enregistre | http://localhost:8761 (tableau de bord) |
| service-referentiel | 8081 | Classes et enseignants | `GET /api/classes` |
| service-planning | 8082 | Cours ; appelle le référentiel (Feign) ; publie sur Kafka | `GET /api/cours` |
| service-presences | 8083 | Feuilles d'appel (créées via Kafka) et absences | `GET /api/appels` |
| notification-service | 8084 | Lit Kafka et envoie les emails (Mailpit) | `GET /api/notifications/cours/{id}` |
| gateway-service | 8080 | Porte d'entrée unique + limite 5 req/s (Redis) | `GET http://localhost:8080/api/classes` |
| Mailpit | 8025 | Boîte mail locale | http://localhost:8025 |

Les services 8081–8084 répondent à `/actuator/health` → `{"status":"UP"}`.

## 3. Tester avec le plugin HTTP Client d'IntelliJ (dossier `http/`)

Lancer les fichiers **dans l'ordre** (ils se passent `classeId`, `enseignantId`, `coursId`, `appelId`) :

| Fichier | Ce qu'il teste |
| --- | --- |
| `00-sante.http` | Tous les services UP, Eureka, Config server |
| `01-referentiel.http` | Créer / lire / modifier une classe et un enseignant, erreurs 400 et 404 |
| `02-planning.http` | Créer un cours, détail enrichi (Feign), erreurs 400 et 404 |
| `03-presences.http` | Feuille d'appel (Kafka), absence refusée 409, REALISE, absence 201, compteur |
| `04-notifications-gateway.http` | Email SENT, routage Gateway, rate limit 429 |

Comment faire :
- Ouvrir le fichier, cliquer **▶▶ (Run All Requests)** en haut, ou la **flèche verte ▶** à gauche d'une seule requête.
- Résultat en bas : ✔ vert = test OK, ✖ rouge = échec. L'onglet **Response** montre le JSON.
- Les blocs `> {% client.test(...) %}` sont les assertions ; `client.global.set` garde un id pour les fichiers suivants.
- Si `03-presences` donne 404 sur la feuille d'appel : Kafka n'a pas encore fini, relancer 1 s après.

## 4. Les autres tests

| Type | Commande (à la racine) | Ce qu'il couvre |
| --- | --- | --- |
| JUnit (35 tests, 8 modules) | `.\mvnw.cmd verify` | Règles métier, HTTP, parcours complet (Kafka embarqué, sans Docker) |
| Smoke (PowerShell) | `.\scripts\smoke.ps1` | Parcours cours → appel → absence → annulation sur les services lancés |
| Postman (26 requêtes) | `npx newman run postman\Planif-Scolaire.postman_collection.json --delay-request 1000` | Tout, y compris email et 429 |

Dans IntelliJ, un test JUnit se lance aussi par clic droit sur la classe de test → **Run**.
