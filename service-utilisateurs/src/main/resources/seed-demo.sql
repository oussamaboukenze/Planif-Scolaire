-- Données de démonstration (ids 1001+).
MERGE INTO utilisateur (id, nom, email, role, actif) KEY(id) VALUES (1001, 'Sara El Amrani', 'sara.elamrani@emsi.ma', 'ETUDIANT', TRUE);
MERGE INTO utilisateur (id, nom, email, role, actif) KEY(id) VALUES (1002, 'Omar Bennani', 'omar.bennani@emsi.ma', 'ETUDIANT', TRUE);
MERGE INTO utilisateur (id, nom, email, role, actif) KEY(id) VALUES (1003, 'Hiba Tazi', 'hiba.tazi@emsi.ma', 'ETUDIANT', TRUE);
MERGE INTO utilisateur (id, nom, email, role, actif) KEY(id) VALUES (1004, 'Yassine Ait Lahcen', 'yassine.aitlahcen@emsi.ma', 'ETUDIANT', FALSE);
MERGE INTO utilisateur (id, nom, email, role, actif) KEY(id) VALUES (1005, 'Scolarite EMSI', 'scolarite@emsi.ma', 'ADMIN', TRUE);
