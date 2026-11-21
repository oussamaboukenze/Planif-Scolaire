-- Données de démonstration (ids 1001+), cohérentes avec referentiel (classes/enseignants 1001+).
-- 1001 REALISE (2 absences) · 1002 et 1003 PLANIFIE · 1004 ANNULE
MERGE INTO cours (id, classe_id, enseignant_id, matiere, date_heure, salle, statut, nb_absences_signalees, idempotency_key) KEY(id)
    VALUES (1001, 1001, 1001, 'Microservices', TIMESTAMP '2026-11-23 09:00:00', 'Amphi 1', 'REALISE', 2, 'seed-demo-1001');
MERGE INTO cours (id, classe_id, enseignant_id, matiere, date_heure, salle, statut, nb_absences_signalees, idempotency_key) KEY(id)
    VALUES (1002, 1002, 1002, 'Data Mining', TIMESTAMP '2026-11-24 10:00:00', 'Labo 2', 'PLANIFIE', 0, 'seed-demo-1002');
MERGE INTO cours (id, classe_id, enseignant_id, matiere, date_heure, salle, statut, nb_absences_signalees, idempotency_key) KEY(id)
    VALUES (1003, 1003, 1003, 'Flutter', TIMESTAMP '2026-11-25 14:00:00', 'Salle 12', 'PLANIFIE', 0, 'seed-demo-1003');
MERGE INTO cours (id, classe_id, enseignant_id, matiere, date_heure, salle, statut, nb_absences_signalees, idempotency_key) KEY(id)
    VALUES (1004, 1001, 1004, 'Deep Learning', TIMESTAMP '2026-11-20 09:00:00', 'Salle 14', 'ANNULE', 0, 'seed-demo-1004');
