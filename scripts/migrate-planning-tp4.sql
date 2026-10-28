-- À exécuter une seule fois sur une base H2 existante, planning arrêté et base sauvegardée.
ALTER TABLE cours ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
UPDATE cours SET idempotency_key = CONCAT('legacy-', CAST(id AS VARCHAR))
WHERE idempotency_key IS NULL;
ALTER TABLE cours ALTER COLUMN idempotency_key SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cours_idempotency_key ON cours(idempotency_key);
