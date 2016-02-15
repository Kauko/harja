-- name: kirjaa-uusi-paivystajatekstiviesti<!
INSERT INTO paivystajatekstiviesti (viestinumero, ilmoitus, yhteyshenkilo) VALUES
  ((SELECT hae_seuraava_vapaa_viestinumero(:yhteyshenkiloid :: INTEGER)),
   (SELECT id
    FROM ilmoitus
    WHERE ilmoitusid = :ilmoitusid),
   :yhteyshenkiloid);

-- name: hae-ilmoitus-idt
SELECT
  i.id,
  i.ilmoitusid
FROM paivystajatekstiviesti p
  INNER JOIN ilmoitus i ON i.id = p.ilmoitus
WHERE p.yhteyshenkilo = :yhteyshenkilo AND
      p.viestinumero = :viestinumero
LIMIT 1;