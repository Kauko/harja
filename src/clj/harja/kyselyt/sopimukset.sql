-- name: luo-sopimus<!
-- Luo uuden sopimukset.
INSERT INTO sopimus (sampoid, nimi, alkupvm, loppupvm, urakka_sampoid, urakoitsija_sampoid, paasopimus)
VALUES (:sampoid, :nimi, :alkupvm, :loppupvm, :urakka_sampoid, :urakoitsija_sampoid, :paasopimus);

-- name: paivita-sopimus!
-- Paivittaa sopimukset.
UPDATE sopimus
SET nimi              = :nimi,
  alkupvm             = :alkupvm,
  loppupvm            = :loppupvm,
  urakka_sampoid      = :urakka_sampoid,
  urakoitsija_sampoid = :urakoitsija_sampoid
WHERE id = :id;

-- name: hae-id-sampoidlla
-- Hakee sopimuksen id:n sampo id:llä
SELECT id
FROM sopimus
WHERE sampoid = :sampoid;

-- name: hae-paasopimuksen-id-urakan-sampoidlla
-- Hakee pääsopimuksen id:n urakan sampo id:n avulla
SELECT id
FROM sopimus
WHERE urakka_sampoid = :urakka_sampoid AND
      paasopimus IS NULL;

-- name: paivita-urakka-sampoidlla!
-- Päivittää sopimukselle urakan id:n urakan sampo id:llä
UPDATE sopimus
SET urakka = (SELECT id
              FROM urakka
              WHERE sampoid = :urakka_sampo_id)
WHERE urakka_sampoid = :urakka_sampo_id;