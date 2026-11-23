# Planif-Scolaire — documentation détaillée

> Planification des cours et suivi des absences avec une architecture **microservices Spring Boot / Spring Cloud**.

![Java 17](https://img.shields.io/badge/Java-17-orange) ![Spring Boot 3.5.6](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen) ![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-blue) ![Kafka](https://img.shields.io/badge/Kafka-KRaft-black) ![H2](https://img.shields.io/badge/DB-H2%20par%20service-lightgrey)

## Sommaire

1. [En bref](#1-en-bref)
2. [Architecture globale](#2-architecture-globale)
3. [Évolution du projet V0 → V6](#3-évolution-du-projet-v0--v6)
4. [Modèle de données](#4-modèle-de-données)
5. [Cycles de vie](#5-cycles-de-vie)
6. [Scénarios de communication](#6-scénarios-de-communication)
7. [Résilience et limitation de débit](#7-résilience-et-limitation-de-débit)
8. [Configuration centralisée](#8-configuration-centralisée)
9. [Lancement](#9-lancement)
10. [API REST](#10-api-rest)
11. [Données de démonstration](#11-données-de-démonstration)
12. [Tests](#12-tests)
13. [Interface web](#13-interface-web)
14. [Structure du dépôt](#14-structure-du-dépôt)

---

## 1. En bref

Chaque séance associe une **classe**, une **matière**, un **enseignant**, un **créneau** et une **salle**.

```mermaid
flowchart LR
    A["📅 Planifier un cours"] --> B["📝 Feuille d'appel<br/>créée automatiquement"]
    B --> C["✅ Cours réalisé"]
    C --> D["🙋 Saisie des absences"]
    D --> E["🔢 Compteur d'absences<br/>du cours mis à jour"]
    A -.-> F["❌ Cours annulé"]
    F -.-> G["🔒 Feuille d'appel clôturée"]
    A --> M["📧 Email à l'enseignant"]
    F --> M
```

| Port | Application | Rôle |
| ---: | --- | --- |
| 8080 | `gateway-service` | Point d'entrée unique : routage, découverte, limitation de débit |
| 8081 | `service-referentiel` | Classes et enseignants |
| 8082 | `service-planning` | Séances de cours |
| 8083 | `service-presences` | Feuilles d'appel et absences |
| 8084 | `notification-service` | Emails déclenchés par Kafka |
| 8085 | `service-utilisateurs` | Étudiants, enseignants, admins |
| 8761 | `service-registry` | Annuaire Eureka |
| 8888 | `config-server` | Configuration centralisée (`config-repo`) |
| 4200 | `frontend` | Interface web d'administration |
| 9092 · 6379 · 1025/8025 | Kafka · Redis · Mailpit | Infrastructure (Docker Compose) |

---

## 2. Architecture globale

```mermaid
flowchart TB
    subgraph clients["Clients"]
        UI["🖥️ Frontend :4200"]
        PM["🧪 Postman / .http"]
    end

    GW["🚪 gateway-service :8080<br/>routes + RateLimiter"]
    RD[("Redis")]

    subgraph metier["Services métier — une base H2 chacun"]
        REF["📚 referentiel :8081"]
        PLA["🗓️ planning :8082"]
        PRE["📝 presences :8083"]
        USR["👤 utilisateurs :8085"]
        NOT["📧 notification :8084"]
    end

    KF[("🟧 Kafka")]
    MP["📬 Mailpit :8025"]

    subgraph socle["Socle Spring Cloud"]
        EU["📒 Eureka :8761"]
        CS["⚙️ config-server :8888"]
    end

    UI --> GW
    PM --> GW
    GW --- RD
    GW --> metier

    PLA -->|Feign| REF
    PLA -->|Feign| PRE
    PRE -->|Feign| PLA
    PRE -->|Feign| USR

    PLA ==>|course-*| KF
    PRE ==>|absence-recorded| KF
    KF ==> NOT
    NOT -->|SMTP| MP

    metier -.->|Eureka + config| socle
```

**Légende**

| Trait | Signification |
| --- | --- |
| `──▶` Feign | Appel **synchrone** HTTP via OpenFeign, résolu par Eureka (`lb://`) |
| `══▶` topic | Message **asynchrone** Kafka (publié via le pattern Outbox) ; les consommateurs sont détaillés en 6.4 |
| `- - ▶` | Tous les services (Gateway compris) s'enregistrent dans Eureka et chargent leur configuration au démarrage |

### Synchrone ou asynchrone ?

| Besoin | Mode | Pourquoi |
| --- | --- | --- |
| Vérifier qu'une classe ou un enseignant existe avant de planifier | Feign (synchrone) | Le résultat conditionne la réponse HTTP |
| Vérifier qu'un étudiant est actif avant une absence | Feign (synchrone) | Idem |
| Créer la feuille d'appel après la planification | Kafka (asynchrone) | Le planning ne doit pas attendre les présences |
| Mettre à jour le compteur d'absences du cours | Kafka (asynchrone) | Découplage, tolérance aux pannes |
| Envoyer un email | Kafka (asynchrone) | Un SMTP lent ne bloque pas l'API |

---

## 3. Évolution du projet V0 → V6

```mermaid
timeline
    title Jalons du projet
    V0 : Conception : UML, découpage en services
    V1 : Services métier : REST, JPA, MapStruct, validation, ProblemDetail
    V2 : Gateway + Eureka : routage statique et dynamique
    V3 : Config Server : configuration globale + spécifique
    V4 : OpenFeign : vue enrichie, idempotencyKey
    V5 : Kafka : Outbox, consommateurs idempotents, DLT, emails
    V6 : Résilience : Retry, Circuit Breaker, RateLimiter Redis
```

| Version | Ce qui a été ajouté | Où le voir dans le code |
| --- | --- | --- |
| V0 | Diagramme UML, une base par service | Section 4 |
| V1 | CRUD, `@Valid`, `@RestControllerAdvice` | `*/controller`, `*/exception` |
| V2 | `gateway-service`, `service-registry` | `config-repo/gateway-service.properties` |
| V3 | `config-server`, `config-repo/application.properties` | `config-repo/` |
| V4 | `ReferentielClient`, `PresencesClient`, `UtilisateursClient`, `/detail` | `*/client` |
| V5 | `OutboxPublisher`, `ProcessedEvent`, `notification-service` | `*/event` |
| V6 | `@CircuitBreaker`, `@Retry`, `RequestRateLimiter` | `Resilient*Lookup`, Gateway |

---

## 4. Modèle de données

Chaque service possède **sa propre base** (*Database per Service*). Les liens entre services sont de simples **identifiants**, jamais des clés étrangères.

```mermaid
erDiagram
    CLASSE {
        Long id PK
        String nom
        String niveau
        int effectifMax
    }
    ENSEIGNANT {
        Long id PK
        String nom
        String matiere
        String email
        int chargeMaxHeures
    }
    UTILISATEUR {
        Long id PK
        String nom
        String email UK
        Role role
        boolean actif
    }
    COURS {
        Long id PK
        Long classeId
        Long enseignantId
        String matiere
        LocalDateTime dateHeure
        String salle
        StatutCours statut
        int nbAbsencesSignalees
        String idempotencyKey UK
    }
    APPEL {
        Long id PK
        Long coursId
        Long classeId
        LocalDate date
        StatutAppel statut
        int nbAbsences
    }
    ABSENCE {
        Long id PK
        Long appelId FK
        Long etudiantId
        String etudiantNom
        String motif
        boolean justifiee
    }

    CLASSE ||..o{ COURS : "classeId"
    ENSEIGNANT ||..o{ COURS : "enseignantId"
    COURS ||..o| APPEL : "coursId"
    APPEL ||--o{ ABSENCE : "appelId"
    UTILISATEUR ||..o{ ABSENCE : "etudiantId"
```

Les traits **pleins** sont de vraies relations JPA dans une même base. Les traits **pointillés** sont des références par identifiant entre deux services.

| Base H2 | Service | Tables |
| --- | --- | --- |
| `./data/service-referentiel` | referentiel | `classe`, `enseignant` |
| `./data/service-utilisateurs` | utilisateurs | `utilisateur` |
| `./data/service-planning` | planning | `cours`, `outbox_event`, `processed_event`, `operation_lock` |
| `./data/service-presences` | presences | `appel`, `absence`, `outbox_event`, `processed_event`, `operation_lock` |
| `./data/notification-service` | notification | `notification_delivery` (`PENDING` / `SENT` / `FAILED`) |

### Couches d'un service métier

```mermaid
flowchart LR
    R["HTTP"] --> C["Controller<br/>@Valid DTO"]
    C --> S["Service<br/>règles métier<br/>@Transactional"]
    S --> M["Mapper<br/>MapStruct"]
    S --> Rp["Repository<br/>Spring Data JPA"]
    Rp --> DB[("H2")]
    S --> F["Client Feign"]
    S --> O["Outbox → Kafka"]
    C -. erreur .-> A["@RestControllerAdvice<br/>ProblemDetail"]
```

---

## 5. Cycles de vie

```mermaid
stateDiagram-v2
    direction LR
    state "Cours" as cours {
        [*] --> PLANIFIE : POST /api/cours
        PLANIFIE --> REALISE : PATCH statut
        PLANIFIE --> ANNULE : PATCH statut
        REALISE --> [*]
        ANNULE --> [*]
    }
```

```mermaid
stateDiagram-v2
    direction LR
    state "Feuille d'appel" as appel {
        [*] --> OUVERT : événement course-scheduled
        OUVERT --> CLOTURE : événement course-cancelled
        OUVERT --> OUVERT : absence ajoutée (cours REALISE)
    }
```

```mermaid
stateDiagram-v2
    direction LR
    state "Email" as mail {
        [*] --> PENDING : événement reçu
        PENDING --> SENT : SMTP OK
        PENDING --> FAILED : SMTP KO
        FAILED --> DLT : 3 tentatives échouées
    }
```

---

## 6. Scénarios de communication

### 6.1 Planifier un cours

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant GW as Gateway
    participant PL as Planning
    participant RF as Référentiel
    participant K as Kafka
    participant PR as Présences
    participant N as Notification
    participant MP as Mailpit

    U->>GW: POST /api/cours (idempotencyKey)
    GW->>PL: route /api/cours/**
    PL->>RF: GET classe + enseignant (Feign, Retry, CB)
    RF-->>PL: 200 (email enseignant)
    PL->>PL: save Cours + OutboxEvent (même transaction)
    PL-->>U: 201 Created + Location
    Note over PL,K: @Scheduled OutboxPublisher
    PL->>K: course-scheduled
    par
        K->>PR: crée l'appel (OUVERT)
    and
        K->>N: notification
        N->>MP: email à l'enseignant
    end
```

### 6.2 Saisir une absence

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant PR as Présences
    participant PL as Planning
    participant US as Utilisateurs
    participant K as Kafka
    participant N as Notification

    U->>PR: POST /api/appels/{id}/absences
    PR->>PL: GET cours (doit être REALISE)
    PR->>US: GET utilisateur (ETUDIANT actif ?)
    alt étudiant invalide
        PR-->>U: 409 Conflict
    else OK
        PR->>PR: save Absence + OutboxEvent
        PR-->>U: 201 Created
        PR->>K: absence-recorded
        K->>PL: nbAbsencesSignalees + 1
        K->>N: email
    end
```

### 6.3 Vue enrichie d'un cours

```mermaid
sequenceDiagram
    actor U as Client
    participant PL as Planning
    participant RF as Référentiel
    participant PR as Présences

    U->>PL: GET /api/cours/{id}/detail
    PL->>RF: classe + enseignant
    PL->>PR: absences du cours
    alt Présences en panne
        PR--xPL: erreur
        PL->>PL: fallback → absences = []
    end
    PL-->>U: cours + classe + enseignant + absences
```

### 6.4 Topics Kafka

| Topic | Producteur | Consommateurs | Effet |
| --- | --- | --- | --- |
| `course-scheduled` | planning | presences, notification | Crée la feuille d'appel, envoie un email |
| `course-cancelled` | planning | presences, notification | Clôture la feuille d'appel, envoie un email |
| `absence-recorded` | presences | planning, notification | Incrémente le compteur, envoie un email |
| `<topic>.DLT` | Kafka (après 3 échecs) | — | Garde les messages en échec |

**Fiabilité :** pattern **Outbox** côté producteur (l'événement est écrit en base dans la même transaction que la donnée), table **ProcessedEvent** côté consommateur (un `eventId` déjà traité est ignoré).

---

## 7. Résilience et limitation de débit

```mermaid
flowchart LR
    A["Appel Feign"] --> R{"Retry<br/>max 3, 250 ms"}
    R -->|succès| OK["✅ Réponse"]
    R -->|5xx / réseau| CB{"Circuit<br/>Breaker"}
    R -->|4xx| E4["❌ Pas de retry<br/>404 / 422"]
    CB -->|fermé| R
    CB -->|ouvert| FB["⚠️ Fallback<br/>503 ProblemDetail<br/>ou liste vide"]
```

| Instance | Où | Fallback |
| --- | --- | --- |
| `referentiel*` | planning → référentiel | `503` |
| `presencesAbsences` | planning → présences | `absences = []` |
| `planning*` | présences → planning | `503` |
| `utilisateursEtudiant` | présences → utilisateurs | `503` |

```mermaid
flowchart LR
    C["Client"] -->|X-Client-Id ou IP| G["Gateway<br/>RequestRateLimiter"]
    G <-->|token bucket| R[("Redis")]
    G -->|"≤ 5 req/s, burst 10"| S["Service"]
    G -->|au-delà| T["429 Too Many Requests"]
```

---

## 8. Configuration centralisée

```mermaid
flowchart TB
    subgraph repo["config-repo/"]
        G["application.properties<br/>(commun : Eureka, Actuator)"]
        P1["service-planning.properties"]
        P2["service-presences.properties"]
        P3["gateway-service.properties"]
        P4["…"]
    end
    CS["config-server :8888<br/>profil native"] --> repo
    S1["service-planning"] -->|"GET /service-planning/default"| CS
    S2["service-presences"] --> CS
    S3["gateway-service"] --> CS
```

Vérification : `GET http://localhost:8888/service-planning/default` retourne les deux sources (spécifique + globale).

| Variable | Rôle |
| --- | --- |
| `CONFIG_REPO_LOCATION` | Dossier `config-repo` si lancé depuis un autre dossier |
| `CONFIG_SERVER_PROFILE=git` | Lire la configuration depuis Git |
| `CONFIG_SERVER_URL` | Cibler un autre serveur de configuration |
| `EUREKA_URL` | Adresse Eureka |
| `PLANNING_URL`, `PRESENCES_URL`, `UTILISATEURS_URL` | URL fixe pour Feign (vide = Eureka) |
| `NOTIFICATION_RECIPIENT` | Destinataire si l'enseignant n'a pas d'email |
| `APP_SEED_LOCATION` | Script de données de démonstration |

---

## 9. Lancement

**Prérequis :** JDK 17, Docker Desktop. Ouvrir le `pom.xml` racine dans IntelliJ IDEA (chaque module garde son propre `pom.xml` et son wrapper Maven).

```mermaid
flowchart LR
    D["1️⃣ docker compose up -d<br/>Kafka · Redis · Mailpit"] --> C["2️⃣ ConfigServer<br/>:8888"]
    C --> E["3️⃣ ServiceRegistry<br/>:8761"]
    E --> M["4️⃣ Référentiel · Utilisateurs<br/>Planning · Présences<br/>Notification"]
    M --> G["5️⃣ Gateway<br/>:8080"]
    G --> F["6️⃣ node frontend/server.mjs<br/>:4200"]
```

```powershell
docker compose up -d
# puis dans chaque dossier de service, ou via les configurations IntelliJ :
.\mvnw.cmd spring-boot:run
```

Sur macOS ou Linux : `./mvnw spring-boot:run`.

> ⚠️ Kafka doit être prêt avant planning, présences et notifications. Redis doit être prêt avant le Gateway. Pour une base H2 du planning créée avant le TP4, exécuter `scripts/migrate-planning-tp4.sql` après une sauvegarde du fichier `.mv.db`.

| Interface | URL |
| --- | --- |
| Tableau Eureka | http://localhost:8761 |
| Mailpit | http://localhost:8025 |
| Frontend | http://localhost:4200 |

---

## 10. API REST

Toutes les routes passent par la Gateway (`http://localhost:8080`) ; chaque service reste aussi accessible sur son port.

```mermaid
flowchart LR
    GW["Gateway :8080"]
    GW -->|"/api/classes/**<br/>/api/enseignants/**"| R["Référentiel"]
    GW -->|"/api/cours/**"| P["Planning"]
    GW -->|"/api/appels/**"| A["Présences"]
    GW -->|"/api/notifications/**"| N["Notification"]
    GW -->|"/api/users/**"| U["Utilisateurs"]
    GW -.->|"/service-xxx/** (discovery locator)"| E["Eureka"]
```

| Service | Méthode et chemin | Résultat |
| --- | --- | --- |
| Référentiel | `GET` `POST` `/api/classes` · `GET` `PUT` `DELETE` `/api/classes/{id}` | CRUD classes (`DELETE` → `204`) |
| Référentiel | `GET` `POST` `/api/enseignants` · `GET` `PUT` `DELETE` `/api/enseignants/{id}` | CRUD enseignants (`email` optionnel) |
| Utilisateurs | `GET` `POST` `/api/users` · `GET` `PUT` `DELETE` `/api/users/{id}` | CRUD utilisateurs (`409` si email déjà utilisé) |
| Planning | `GET /api/cours` · `GET /api/cours/{id}` | Liste ou détail |
| Planning | `POST /api/cours` | Création, en-tête `idempotencyKey` obligatoire |
| Planning | `GET /api/cours/{id}/detail` | Cours + classe + enseignant + absences |
| Planning | `PATCH /api/cours/{id}/statut` | `REALISE` ou `ANNULE` |
| Présences | `GET /api/appels` · `GET /api/appels/{id}` · `GET /api/appels/cours/{coursId}` | Feuilles d'appel |
| Présences | `GET` `POST` `/api/appels/{id}/absences` | Absences (`etudiantId` vérifié, ou `etudiantNom`) |
| Notifications | `GET /api/notifications/cours/{coursId}` · `GET /api/notifications/{eventId}` | Statut des emails |

### Codes de réponse

| Code | Quand |
| :---: | --- |
| `200` | Lecture ou modification |
| `201` | Création, avec en-tête `Location` |
| `204` | Suppression |
| `400` | DTO invalide, ou `idempotencyKey` déjà utilisée |
| `404` | Ressource introuvable (ou référence distante absente, côté planning) |
| `409` | Conflit métier (doublon, étudiant inactif…) |
| `422` | Référence distante absente, côté présences |
| `429` | Quota Gateway dépassé |
| `502` · `503` | Erreur ou indisponibilité d'un service distant |

Les erreurs suivent le format `ProblemDetail` : `status`, `title`, `detail`.

---

## 11. Données de démonstration

Au démarrage, chaque service charge `src/main/resources/seed-demo.sql` (ids **1001+**, rechargés à chaque démarrage).

| Service | Données |
| --- | --- |
| Référentiel | Classes 1001–1004 (5IIR-A, 5IIR-B, 4IIR-A, 3IIR-C) · Enseignants 1001–1004 avec email |
| Utilisateurs | 1001–1003 étudiants actifs · 1004 étudiant inactif · 1005 admin |
| Planning | Cours 1001 `REALISE` · 1002 et 1003 `PLANIFIE` · 1004 `ANNULE` |
| Présences | Appels 1001–1004 (1004 `CLOTURE`) · 2 absences sur l'appel 1001 |

Tests prêts à l'emploi : [collection DEMO](../postman/Planif-Scolaire-DEMO.postman_collection.json) (25 requêtes) et `http/06-donnees-demo.http`.

---

## 12. Tests

### Tests automatisés

```powershell
.\mvnw.cmd clean verify
```

Ils couvrent les règles métier, les validations HTTP, les ressources inexistantes et le parcours cours → appel → absence (bases en mémoire + Kafka embarqué, sans Docker). `notification-service` teste l'envoi, l'idempotence et la persistance d'un échec SMTP. `integration-tests` est un module de tests, pas une application.

### Postman

Importer [la collection](../postman/Planif-Scolaire.postman_collection.json) et lancer ses **39 requêtes dans l'ordre** (Collection Runner, 1 itération, délai **1000 ms**).

| Domaine | Cas valide | Validation `400` | Inexistant `404` |
| --- | --- | --- | --- |
| Classes | Création, lecture, modification, suppression | Nom vide, effectif nul | ID absent |
| Enseignants | Création, lecture | Nom, matière vides, charge nulle | ID absent |
| Utilisateurs | CRUD complet | — | ID absent, `409` email en double |
| Cours | Création, lecture, réalisation, détail | Champs absents | ID absent |
| Présences | Lecture d'appel, saisie par nom ou `etudiantId` | Nom et motif absents | Appel absent |
| Gateway | Routes statiques et dynamiques | — | `429` après un burst |
| Notifications | Statut `SENT` de l'email | — | — |

L'appel est créé de façon asynchrone : la collection réessaie jusqu'à 30 fois. Chaque exécution utilise de nouveaux noms et une nouvelle salle.

---

## 13. Interface web

`frontend/` est une interface d'administration qui passe uniquement par la Gateway. Lancement : `node frontend/server.mjs`, puis http://localhost:4200 ([détails](../frontend/README.md)).

| | |
| :---: | :---: |
| ![Aperçu](../captures/01-apercu.jpg) **Aperçu** | ![Emploi du temps](../captures/02-emploi-du-temps.jpg) **Emploi du temps** |
| ![Détail](../captures/03-detail-cours-vue-enrichie.jpg) **Vue enrichie d'un cours** | ![Planifier](../captures/04-planifier-un-cours.jpg) **Planifier un cours** |
| ![Appels](../captures/05-feuilles-appel-absences.jpg) **Feuilles d'appel** | ![Référentiel](../captures/06-classes-enseignants.jpg) **Classes et enseignants** |
| ![Étudiants](../captures/07-etudiants.jpg) **Étudiants** | ![Emails](../captures/08-emails-envoyes.jpg) **Emails envoyés** |
| ![Santé](../captures/09-services-sante.jpg) **Santé des services** | ![Eureka](../captures/10-services-eureka-routes.jpg) **Eureka et routes** |

---

## 14. Structure du dépôt

```text
Planif-Scolaire/
├── config-repo/            # configuration externalisée (globale + par service)
├── config-server/          # Spring Cloud Config        :8888
├── service-registry/       # Eureka                     :8761
├── gateway-service/        # Spring Cloud Gateway       :8080
├── service-referentiel/    # classes, enseignants       :8081
├── service-planning/       # cours, Outbox              :8082
├── service-presences/      # appels, absences, Outbox   :8083
├── notification-service/   # emails Kafka → SMTP        :8084
├── service-utilisateurs/   # utilisateurs               :8085
├── integration-tests/      # parcours de bout en bout
├── frontend/               # interface web              :4200
├── postman/                # collections Postman
├── http/                   # requêtes IntelliJ HTTP Client
├── captures/               # captures de l'interface web
├── scripts/                # migrations SQL
├── docs/                   # rapport V5–V6
└── compose.yaml            # Kafka, Redis, Mailpit
```

**Livraison :** le tag Git `v1.0` identifie le jalon des services métier ; le commit initial de conception reste dans l'historique.
