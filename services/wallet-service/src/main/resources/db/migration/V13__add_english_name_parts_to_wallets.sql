-- V13: Split the wallet display name into structured English-name parts.
--
-- Background: NAFATH returns demographic data with separate English fields
-- (englishFirstName, englishSecondName, englishThirdName, englishLastName).
-- The mock now keeps englishFirstName="OMAR" and merges a per-session pool
-- pick into englishThirdName, producing a stable "<first> <third>" merged
-- name that flows downstream.
--
-- We store both halves on the wallet so future APIs can render the name
-- whichever way they want without re-deriving anything. masked_name continues
-- to hold the merged "<first> <third>" string and stays the canonical value
-- read by all current endpoints.

ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS english_first_name VARCHAR(60),
    ADD COLUMN IF NOT EXISTS english_third_name VARCHAR(60);

-- Backfill existing rows by splitting masked_name on the first whitespace.
-- Wallets created before V12 have masked_name = NULL → both new columns stay
-- NULL too (no random invention).
UPDATE wallets
   SET english_first_name = split_part(masked_name, ' ', 1),
       english_third_name = NULLIF(
           trim(substring(masked_name FROM position(' ' IN masked_name) + 1)),
           ''
       )
 WHERE masked_name IS NOT NULL
   AND english_first_name IS NULL;
