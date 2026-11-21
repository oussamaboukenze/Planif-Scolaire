-- Données de démonstration (ids 1001+ pour ne pas gêner les ids générés automatiquement).
-- MERGE ... KEY(id) : rejouable à chaque démarrage, remet les lignes de démo à leur état initial.
MERGE INTO classe (id, nom, niveau, effectif_max) KEY(id) VALUES (1001, '5IIR-A', '5IIR', 30);
MERGE INTO classe (id, nom, niveau, effectif_max) KEY(id) VALUES (1002, '5IIR-B', '5IIR', 28);
MERGE INTO classe (id, nom, niveau, effectif_max) KEY(id) VALUES (1003, '4IIR-A', '4IIR', 32);
MERGE INTO classe (id, nom, niveau, effectif_max) KEY(id) VALUES (1004, '3IIR-C', '3IIR', 35);

MERGE INTO enseignant (id, nom, email, matiere, charge_max_heures) KEY(id) VALUES (1001, 'Karim Benali', 'karim.benali@emsi.ma', 'Microservices', 20);
MERGE INTO enseignant (id, nom, email, matiere, charge_max_heures) KEY(id) VALUES (1002, 'Salma Idrissi', 'salma.idrissi@emsi.ma', 'Data Mining', 18);
MERGE INTO enseignant (id, nom, email, matiere, charge_max_heures) KEY(id) VALUES (1003, 'Youssef Alaoui', 'youssef.alaoui@emsi.ma', 'Flutter', 22);
MERGE INTO enseignant (id, nom, email, matiere, charge_max_heures) KEY(id) VALUES (1004, 'Nadia Chraibi', 'nadia.chraibi@emsi.ma', 'Deep Learning', 16);
