-- Données de démonstration (ids 1001+), cohérentes avec planning (cours 1001+) et utilisateurs (1001+).
MERGE INTO appel (id, cours_id, classe_id, date, statut, nb_absences) KEY(id) VALUES (1001, 1001, 1001, DATE '2026-11-23', 'OUVERT', 2);
MERGE INTO appel (id, cours_id, classe_id, date, statut, nb_absences) KEY(id) VALUES (1002, 1002, 1002, DATE '2026-11-24', 'OUVERT', 0);
MERGE INTO appel (id, cours_id, classe_id, date, statut, nb_absences) KEY(id) VALUES (1003, 1003, 1003, DATE '2026-11-25', 'OUVERT', 0);
MERGE INTO appel (id, cours_id, classe_id, date, statut, nb_absences) KEY(id) VALUES (1004, 1004, 1001, DATE '2026-11-20', 'CLOTURE', 0);

MERGE INTO absence (id, appel_id, etudiant_id, etudiant_nom, motif, justifiee) KEY(id) VALUES (1001, 1001, 1001, 'Sara El Amrani', 'Maladie', TRUE);
MERGE INTO absence (id, appel_id, etudiant_id, etudiant_nom, motif, justifiee) KEY(id) VALUES (1002, 1001, 1002, 'Omar Bennani', 'Retard', FALSE);
