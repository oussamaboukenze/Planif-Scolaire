# Interface de test Planif-Scolaire

Interface web qui teste le backend **sans le modifier** : un petit serveur Node (`server.mjs`, aucune dépendance)
sert la page et relaie les appels vers chaque service. Le navigateur ne parle qu'à `localhost:3000`,
donc aucun CORS n'est nécessaire côté Spring.

## Lancer

1. Docker (`docker compose up -d`) et les 7 services démarrés (voir `http/GUIDE-TESTS.md`).
2. À la racine du projet :

```powershell
node test-ui/server.mjs
```

3. Ouvrir http://localhost:3000, cliquer **Lancer tous les tests** (≈ 30 s à cause de Kafka).

Les pastilles en haut indiquent l'état de chaque service (vert = UP). Cliquer sur une ligne de test affiche
les requêtes envoyées et les réponses reçues. La **console manuelle** à droite envoie n'importe quelle requête ;
`{classeId}`, `{coursId}`, `{appelId}`, `{uuid}` sont remplacés automatiquement.

## Les tests automatiques (47)

### Santé
| ID | Test | Attendu |
| --- | --- | --- |
| S1–S4 | `/actuator/health` de referentiel, planning, presences, notifications | `UP` |
| S5 | Eureka liste les 5 services | tous présents |
| S6 | Config server sert `service-planning` | 200 |

### Référentiel (8081)
| ID | Test | Attendu |
| --- | --- | --- |
| R1 | Créer une classe | 201 + `Location` |
| R2 | Lire la classe | 200, effectifMax 30 |
| R3 | Modifier la classe | 200, effectifMax 32 |
| R4 | Lister les classes | contient la classe |
| R5 | Classe invalide (nom vide, effectif 0) | 400 + `detail` |
| R6 | Classe inexistante | 404 |
| R7 | Créer un enseignant | 201 + `Location` |
| R8 | Lire l'enseignant | 200 |
| R9 | Enseignant invalide | 400 |
| R10 | Enseignant inexistant | 404 |

### Planning (8082)
| ID | Test | Attendu |
| --- | --- | --- |
| P1 | Créer un cours avec `idempotencyKey` | 201, `PLANIFIE` |
| P2 | Lire le cours | 200 |
| P3 | Détail enrichi (Feign → référentiel) | `classe` et `enseignant` remplis |
| P4 | Cours invalide (corps vide) | 400 |
| P5 | Sans en-tête `idempotencyKey` | 400 |
| P6 | Même `idempotencyKey` réutilisée | 400 « Opération déjà traitée » |
| P7 | Même créneau, nouvelle clé | 409 |
| P8 | Classe inexistante dans le référentiel | 404 |
| P9 | Cours inexistant | 404 |

### Présences (8083) — via Kafka
| ID | Test | Attendu |
| --- | --- | --- |
| A1 | Feuille d'appel créée automatiquement | 200, `OUVERT` (attente ≤ 30 s) |
| A2 | Absence avant réalisation du cours | 409 |
| A3 | Passer le cours à `REALISE` | 200 |
| A4 | Saisir une absence | 201 + `Location` |
| A5 | Même absence une 2e fois | 409 |
| A6 | Lister les absences | 1 absence |
| A7 | Compteur du cours mis à jour (Kafka → planning) | `nbAbsencesSignalees = 1` |
| A8 | Absence invalide | 400 |
| A9 | Appel inexistant | 404 |
| A10 | Absence sur appel inexistant | 404 |
| A11 | Annuler un 2e cours | feuille d'appel `CLOTURE` |

### Notifications (8084 + Mailpit)
| ID | Test | Attendu |
| --- | --- | --- |
| N1 | Email « cours planifié » | `COURSE_SCHEDULED` = `SENT` |
| N2 | Email « absence » | `ABSENCE_RECORDED` = `SENT` |
| N3 | Email « annulation » | `COURSE_CANCELLED` = `SENT` |
| N4 | Lire une notification par `eventId` | 200 |
| N5 | `eventId` inconnu | 404 |
| N6 | Email reçu dans Mailpit | au moins 1 message |

### Gateway (8080 + Redis)
| ID | Test | Attendu |
| --- | --- | --- |
| G1–G4 | Routes `/api/classes`, `/api/cours`, `/api/appels`, `/api/notifications` via 8080 | 200 |
| G5 | 30 requêtes simultanées, même `X-Client-Id` | au moins un 429 |

## Tests manuels (résilience) — à faire à la main

Ces tests demandent d'arrêter un composant ; l'interface sert à observer le résultat.

| Test | Comment | Attendu |
| --- | --- | --- |
| Circuit breaker planning → référentiel | Arrêter `Referentiel` dans IntelliJ, puis console : `GET planning /api/cours/{coursId}/detail` plusieurs fois | 503 (ProblemDetail) après les 3 tentatives ; redémarrer → 200 de nouveau |
| Circuit breaker présences → planning | Arrêter `Planning`, puis `POST presences /api/appels/{appelId}/absences` | 503 |
| Email en échec | `docker compose stop mailpit`, créer un cours | notification `FAILED` (puis topic `.DLT`) ; `docker compose start mailpit` |
| Kafka arrêté | `docker compose stop kafka`, créer un cours | la création échoue ou la feuille d'appel n'apparaît pas ; redémarrer Kafka |
| Pastilles | Arrêter un service | sa pastille passe au rouge après « Rafraîchir l'état » |
