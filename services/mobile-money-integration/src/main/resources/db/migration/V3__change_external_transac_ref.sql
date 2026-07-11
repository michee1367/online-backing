-- 1. Suppression de l'index existant pour éviter les conflits pendant la modification
DROP INDEX IF EXISTS mobile_money.idx_mm_external_ref;

-- 2. Modification du type de la colonne avec transtypage (USING)
-- ⚠️ Note : Cela nécessite que vos références existantes soient convertibles en UUID.
-- Si la table est vide ou ne contient que des chaînes au format UUID, cela passera tout seul.
ALTER TABLE mobile_money.transactions 
    ALTER COLUMN external_ref TYPE VARCHAR(255);

-- 3. Recréation de l'index sur la colonne désormais typée UUID
CREATE INDEX idx_mm_external_ref ON mobile_money.transactions (external_ref);
