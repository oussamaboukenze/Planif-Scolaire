# Planif-Scolaire — interface web

Interface d'administration de l'établissement : emploi du temps de la semaine, feuilles d'appel,
classes, enseignants, étudiants, emails envoyés et état des microservices.

HTML, CSS et JavaScript, sans framework ni `npm install`. Un petit serveur Node sert la page et
relaie les appels vers la **Gateway** : le navigateur ne parle qu'à `http://localhost:4200`, donc
aucune configuration CORS n'est nécessaire côté Spring.

## Lancer

1. Démarrer Docker (`docker compose up -d`) et les 8 applications Spring Boot.
2. À la racine du projet : `node frontend/server.mjs` (Node 18 ou plus).
3. Ouvrir http://localhost:4200.

Variables optionnelles : `FRONT_PORT` (défaut 4200), `GATEWAY_URL` (défaut http://localhost:8080).

## Ce que montre chaque page

| Page | Ce qu'on y fait | Services appelés |
| --- | --- | --- |
| Aperçu | Prochains cours, chiffres clés, état des 8 applications | tous |
| Emploi du temps | Semaine en grille, planifier un cours, marquer réalisé ou annuler, détail enrichi | planning (+ referentiel, presences via Feign) |
| Feuilles d'appel | Feuilles créées par Kafka, absences, saisie d'une absence | presences (+ planning, utilisateurs via Feign) |
| Classes et enseignants | CRUD complet | referentiel |
| Étudiants | CRUD complet | utilisateurs |
| Emails envoyés | Statut des emails d'un cours (lien vers Mailpit) | notification |
| Services | Santé de chaque application, instances Eureka, routes de la Gateway | accès direct à chaque service |

Le bouton **Journal des requêtes** liste chaque appel HTTP avec sa méthode, son statut, sa durée et
le microservice qui l'a traité. Pratique pour expliquer le parcours Gateway → service pendant une démo.

La Gateway limite à 5 requêtes par seconde : en cas de 429, l'interface réessaie automatiquement.
