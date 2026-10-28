# service-planning

Projet Spring Boot Maven autonome (Java 17, Spring Boot 3.5.6).
Ouvrir ce dossier ou son `pom.xml` comme projet Maven dans IntelliJ IDEA.
Aucun projet Maven local parent ou module partagé à installer.

Depuis ce dossier :

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Sous Linux/macOS : `sh mvnw clean verify` puis `sh mvnw spring-boot:run`.

La classe principale est `com.planifscolaire.planning.PlanningApplication`.
Configuration : `src/main/resources/application.properties`.
JAR exécutable : `target/service-planning-0.0.1-SNAPSHOT-exec.jar`.
Le JAR sans suffixe permet uniquement aux tests de bout en bout de charger les classes.
Les données H2 sont conservées dans `data/` du dossier de lancement.

Consulter le README à la racine du dépôt pour les API et le démarrage de Kafka.

## Création et détail enrichi (TP4)

Le planning utilise OpenFeign et Eureka pour appeler `service-referentiel`.
Config Server et Eureka doivent être démarrés avant les services métier.

Une création utilise `POST /api/cours` avec `Content-Type: application/json` et
un en-tête `idempotencyKey` unique pour l’opération. Une répétition de clé retourne `400`.
Le détail enrichi est accessible avec `GET /api/cours/{id}/detail`, directement sur
le port 8082 ou via la Gateway sur le port 8080. Il ajoute les objets `classe` et `enseignant`.

Pour une base créée avant l’ajout de la clé, appliquer la migration décrite dans le README racine.
