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
