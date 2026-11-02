# Planif-Scolaire — Rapport d'integration V5 et V6

**Date :** 2 novembre 2026
**Objet :** mise en oeuvre des notifications asynchrones et de la resilience des API.

## Resume

Les phases V5 et V6 ajoutent un service de notification independant, connecte a Kafka, un envoi SMTP consultable dans Mailpit, des protections Retry/Circuit Breaker sur les appels Feign de lecture, et une limitation de debit Redis dans l'API Gateway.

Le decoupage microservice est conserve : chaque application garde son propre module Maven, sa configuration externalisee et sa base H2. `notification-service` ne lit pas les bases de planning ou de presences ; il consomme uniquement leurs evenements Kafka et conserve son propre journal d'envoi.

## V5 — Notification par email

### Flux

1. `service-planning` publie `course-scheduled` ou `course-cancelled` dans Kafka via son outbox.
2. `service-presences` et `notification-service` consomment chacun ces evenements dans leur propre groupe Kafka.
3. `service-presences` cree ou cloture la feuille d'appel.
4. `notification-service` envoie un email SMTP et conserve le resultat dans sa base H2.
5. L'evenement `absence-recorded` declenche aussi une notification.

Le service stocke `PENDING`, `SENT` ou `FAILED`, avec l'identifiant d'evenement, l'identifiant du cours, l'heure d'envoi et l'erreur eventuelle. `eventId` est la cle primaire : un evenement deja marque `SENT` n'envoie pas de second email. Une erreur SMTP declenche trois nouvelles tentatives espacees d'une seconde ; apres epuisement, l'evenement est publie dans `<topic>.DLT`.

Mailpit remplace un serveur email externe en developpement. L'interface est disponible sur `http://localhost:8025`, SMTP sur le port `1025`. Le destinataire par defaut est `demo@planif-scolaire.local`, configurable via `NOTIFICATION_RECIPIENT`.

### API ajoutee

- `GET /api/notifications/cours/{coursId}` : liste les notifications d'un cours.
- `GET /api/notifications/{eventId}` : consulte le resultat d'un evenement.

## V6 — Resilience et limitation de debit

### Appels Feign

- Planning protege les lectures du referentiel pour les classes et enseignants.
- Presences protege la lecture d'un cours dans planning.
- Les erreurs reseau et HTTP 5xx sont retentees au maximum trois fois, avec 250 ms entre tentatives.
- Les erreurs 4xx, notamment `404`, ne sont ni retentees ni comptees comme pannes serveur.
- Un Circuit Breaker s'ouvre apres une fenetre de cinq appels avec au moins trois appels et un taux d'echec de 50 %. Il attend cinq secondes, puis autorise deux appels de verification.
- Le fallback renvoie `503` dans le format `ProblemDetail` deja utilise par les services.

### Gateway et Redis

Toutes les routes API du Gateway utilisent `RequestRateLimiter` et Redis : 5 jetons par seconde, avec un burst maximal de 10. `X-Client-Id` identifie le client ; en son absence, l'adresse IP est utilisee. Une requete depassant le quota recoit `429 Too Many Requests`.

Les erreurs centralisees par `ControllerAdvice`, l'idempotence de creation de cours via `idempotencyKey` et la deduplication des evenements de presences restent en place.

## Fichiers ajoutes

- `notification-service/pom.xml` et `notification-service/src/main/java/com/planifscolaire/notification/NotificationApplication.java` : nouveau module Spring Boot independant.
- `notification-service/src/main/java/com/planifscolaire/notification/domain/NotificationDelivery.java` et `NotificationStatus.java` : journal d'envoi persistant et etats.
- `notification-service/src/main/java/com/planifscolaire/notification/repository/NotificationRepository.java` : acces a la base propre au service.
- `notification-service/src/main/java/com/planifscolaire/notification/event/` : DTO d'evenements et consommateurs Kafka.
- `notification-service/src/main/java/com/planifscolaire/notification/service/` : envoi email, deduplication et consultation des statuts.
- `notification-service/src/main/java/com/planifscolaire/notification/api/NotificationController.java` : endpoints de suivi.
- `notification-service/src/main/java/com/planifscolaire/notification/configuration/KafkaErrorConfiguration.java` : retries et publication en DLT.
- `notification-service/src/test/java/com/planifscolaire/notification/service/NotificationServiceTest.java` : tests d'envoi, de deduplication et d'echec SMTP.
- `config-repo/notification-service.properties` : port, base, Kafka, SMTP, Eureka et destinataire.
- `gateway-service/src/main/java/com/planifscolaire/gateway/configuration/RateLimitConfiguration.java` : resolution de cle par client ou IP.
- `service-planning/src/main/java/com/planifscolaire/planning/service/ResilientReferentielLookup.java` et `service-presences/src/main/java/com/planifscolaire/presences/service/ResilientPlanningLookup.java` : wrappers resilience des appels Feign.
- Deux exceptions `RemoteServiceUnavailableException` traduisent les echecs distants en 503 dans chaque service.

## Fichiers modifies

- `pom.xml` : ajout de `notification-service` au reactor Maven.
- `compose.yaml` : ajout de Mailpit et Redis, sans remplacer Kafka.
- `service-planning/pom.xml` et `service-presences/pom.xml` : dependances Resilience4j et AOP.
- `service-planning/src/main/java/com/planifscolaire/planning/service/CoursService.java` et `service-presences/src/main/java/com/planifscolaire/presences/service/PresenceService.java` : delegation des lectures distantes aux wrappers resilients.
- `gateway-service/pom.xml` : client Redis reactif.
- `config-repo/service-planning.properties` et `config-repo/service-presences.properties` : parametres Retry/Circuit Breaker.
- `config-repo/gateway-service.properties` : routes notifications, filtres de quota et adresse Redis.
- `postman/Planif-Scolaire.postman_collection.json` : collection portee a 26 requetes ; ajout du controle email et du scenario `429`.
- `README.md` : architecture, prerequis, lancement, ports, API et validation V5/V6.

## Lancement et demonstration

1. Demarrer les dependances : `docker compose up -d`.
2. Demarrer Config Server et Eureka.
3. Demarrer les services referentiel, planning, presences et notification, puis le Gateway.
4. Importer `postman/Planif-Scolaire.postman_collection.json` et lancer les 26 requetes dans l'ordre.
5. Verifier l'email dans Mailpit et le statut `SENT` dans l'API Notifications. Le dernier scenario envoie un burst avec un client unique et verifie le `429`.

Ports : Gateway `8080`, Referentiel `8081`, Planning `8082`, Presences `8083`, Notification `8084`, Eureka `8761`, Config Server `8888`, Mailpit UI `8025`, SMTP `1025`, Redis `6379`, Kafka `9092`.

## Validation realisee

- `./mvnw.cmd clean verify` : reactor complet valide ; 35 tests passes, dont le parcours `SchoolFlowTest` (HTTP, Feign et Kafka).
- Tests cibles : notification 3/3, planning HTTP 7/7, planning metier 12/12, presences 8/8.
- `docker compose config -q` : configuration Compose valide.
- Le fichier Postman a ete parse comme JSON ; 26 requetes sont presentes.

La collection Postman de 26 requetes n'a pas ete executee en entier. Les tests notification utilisent un `JavaMailSender` mocke ; l'envoi reel via Mailpit et le `429` via le Gateway ne sont donc pas annonces comme verifies ici. Le lancement local de `notification-service` n'a pas atteint son endpoint health pendant cette session.

## Limite de livraison email

Kafka et le journal `eventId` donnent un traitement deduplique au niveau applicatif, mais SMTP et la base ne partagent pas de transaction distribuee. Un arret exactement entre l'envoi SMTP et la sauvegarde `SENT` peut donc exceptionnellement provoquer un email en double lors d'une nouvelle livraison. Le flux est au moins une fois, pas une garantie SMTP exactement une fois.