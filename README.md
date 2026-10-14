# Planif-Scolaire

Version initiale — présentation et conception du projet.

## Description

Planif-Scolaire permet de planifier les cours d'un établissement et de suivre les absences. Chaque séance associe une classe, une matière, un enseignant, un créneau et une salle. Sa création génère une feuille d'appel. Après la réalisation du cours, les absences saisies mettent à jour son compteur de suivi. L'annulation d'une séance clôture la feuille d'appel.

Le cours suit le cycle `PLANIFIE → REALISE` ou `PLANIFIE → ANNULE`.

## Diagramme de classes UML

```mermaid
classDiagram
    direction LR

    namespace Referentiel {
        class Classe {
            -Long id
            -String nom
            -String niveau
            -int effectifMax
        }
        class Enseignant {
            -Long id
            -String nom
            -String matiere
            -int chargeMaxHeures
        }
    }

    namespace Planning {
        class Cours {
            -Long id
            -Long classeId
            -Long enseignantId
            -String matiere
            -LocalDateTime dateHeure
            -String salle
            -StatutCours statut
            -int nbAbsencesSignalees
        }
        class StatutCours {
            <<enumeration>>
            PLANIFIE
            REALISE
            ANNULE
        }
    }

    namespace Presences {
        class Appel {
            -Long id
            -Long coursId
            -Long classeId
            -LocalDate date
            -StatutAppel statut
            -int nbAbsences
        }
        class Absence {
            -Long id
            -Long appelId
            -String etudiantNom
            -String motif
            -boolean justifiee
        }
        class StatutAppel {
            <<enumeration>>
            OUVERT
            CLOTURE
        }
    }

    Cours ..> Classe : concerne une classe
    Cours ..> Enseignant : mobilise un enseignant
    Appel ..> Cours : verifie la seance
    Appel ..> Classe : suivi par classe
    Cours ..> Appel : genere ou cloture
    Absence ..> Cours : actualise le compteur

    Appel "1" <-- "0..*" Absence : appelId
    Cours --> StatutCours : statut
    Appel --> StatutAppel : statut
```

Les groupes `Referentiel`, `Planning` et `Presences` correspondent aux trois services métier. Les flèches en pointillés représentent leurs dépendances, sans préciser le protocole de communication. Les références entre services sont conservées sous forme d'identifiants.

Un cours appartient à une seule classe et mobilise un seul enseignant. Une classe ou un enseignant peut être associé à plusieurs cours. Une séance possède au maximum une feuille d'appel, créée après la planification. Chaque absence appartient à un seul appel.

## Architecture

| Application | Port | Rôle |
| --- | --- | --- |
| `service-referentiel` | 8081 | Classes et enseignants |
| `service-planning` | 8082 | Séances de cours et suivi des absences |
| `service-presences` | 8083 | Feuilles d'appel et absences |
| `service-registry` | 8761 | Registre Eureka |
| `gateway` | 8080 | API Gateway |

```mermaid
flowchart TB
    Client["Client"]
    Gateway["gateway<br/>API Gateway<br/>8080"]
    Eureka["service-registry<br/>Eureka<br/>8761"]
    Referentiel["service-referentiel<br/>8081"]
    Planning["service-planning<br/>8082"]
    Presences["service-presences<br/>8083"]

    Client --> Gateway
    Gateway --> Referentiel
    Gateway --> Planning
    Gateway --> Presences

    Gateway -.->|Enregistrement et decouverte| Eureka
    Referentiel -.->|Enregistrement| Eureka
    Planning -.->|Enregistrement| Eureka
    Presences -.->|Enregistrement| Eureka
```

Les services métier et la Gateway s'enregistrent auprès de `service-registry`. La Gateway utilise ce registre pour trouver les instances disponibles.
