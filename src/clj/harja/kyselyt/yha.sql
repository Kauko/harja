-- name: poista-urakan-yha-tiedot!
-- Poistaa urakan yha-tiedot
DELETE FROM yhatiedot
WHERE urakka = :urakka;

-- name: lisaa-urakalle-yha-tiedot<!
-- Lisää urakalle YHA-tiedot
INSERT INTO yhatiedot
(urakka, yhatunnus, yhaid, yhanimi, elyt, vuodet, kohdeluettelo_paivitetty, luotu, linkittaja, muokattu)
VALUES (:urakka, :yhatunnus, :yhaid, :yhanimi, :elyt :: TEXT [], :vuodet :: INTEGER [], NULL, NOW(), :kayttaja, NOW());

-- name: paivita-yhatietojen-kohdeluettelon-paivitysaika<!
-- Päivittää urakan YHA-tietoihin kohdeluettelon uudeksi päivitysajaksi nykyhetken
UPDATE yhatiedot
SET
  kohdeluettelo_paivitetty = NOW(),
  muokattu                 = NOW()
WHERE urakka = :urakka;

-- name: hae-urakan-yhatiedot
SELECT
  yhatunnus,
  yhaid,
  yhanimi,
  elyt,
  vuodet,
  kohdeluettelo_paivitetty as "kohdeluettelo-paivitetty",
  sidonta_lukittu as "sidonta-lukittu"
FROM yhatiedot
WHERE urakka = :urakka;

-- name: poista-urakan-yllapitokohteet!
DELETE FROM yllapitokohde
WHERE urakka = :urakka;

-- name: poista-urakan-yllapitokohdeosat!
DELETE FROM yllapitokohdeosa
WHERE yllapitokohde IN
      (SELECT id
       FROM yllapitokohde
       WHERE urakka = :urakka);

-- name: hae-yllapitokohteen-kohdeosat
SELECT
  id,
  tr_numero AS "tr-numero",
  tr_alkuosa AS "tr-alkuosa",
  tr_alkuetaisyys AS "tr-alkuetaisyys",
  tr_loppuosa AS "tr-loppuosa",
  tr_loppuetaisyys AS "tr-loppuetaisyys",
  tr_ajorata AS "tr-ajorata",
  tr_kaista AS "tr-kaista"
from yllapitokohdeosa WHERE yllapitokohde = :id;

-- name: hae-urakoiden-sidontatiedot
SELECT
  yt.yhaid,
  u.nimi AS "sidottu-urakkaan"
FROM yhatiedot yt
  JOIN urakka u ON yt.urakka = u.id
WHERE yt.yhaid IN (:yhaidt);

-- name: luo-yllapitokohde<!
INSERT INTO yllapitokohde
(urakka, sopimus, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys,
  tr_ajorata, tr_kaista,
 yhatunnus, yhaid, tyyppi, yllapitoluokka, keskimaarainen_vuorokausiliikenne, nykyinen_paallyste,
sopimuksen_mukaiset_tyot, arvonvahennykset, bitumi_indeksi, kaasuindeksi)
VALUES (
  :urakka,
  (SELECT id FROM sopimus WHERE paasopimus IS NULL AND urakka = :urakka),
  :tr_numero,
  :tr_alkuosa,
  :tr_alkuetaisyys,
  :tr_loppuosa,
  :tr_loppuetaisyys,
  :tr_ajorata,
  :tr_kaista,
  :yhatunnus,
  :yhaid,
  :tyyppi::yllapitokohdetyyppi,
  :yllapitoluokka,
  :keskimaarainen_vuorokausiliikenne,
  :nykyinen_paallyste,
  0,
  0,
  0,
  0);

-- name: luo-yllapitokohdeosa<!
-- Luo uuden yllapitokohdeosan
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys,
                              tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti,
                              yhaid)
VALUES (
  :yllapitokohde,
  :nimi,
  :tr_numero,
  :tr_alkuosa,
  :tr_alkuetaisyys,
  :tr_loppuosa,
  :tr_loppuetaisyys,
  :tr_ajorata,
  :tr_kaista,
  :sijainti,
  :yhaid);

-- name: hae-yllapitokohde-idlla
-- single?: true
SELECT *
FROM yllapitokohde
WHERE yhatunnus = :yhatunnus;

-- name: hae-urakan-yha-id
-- single?: true
SELECT yhaid FROM yhatiedot WHERE urakka = :urakkaid;

-- name: hae-urakan-kohteiden-yha-idt
SELECT yhaid FROM yllapitokohde WHERE urakka = :urakkaid;

-- name: merkitse-urakan-yllapitokohteet-paivitetyksi<!
UPDATE yhatiedot SET
  kohdeluettelo_paivitetty = NOW()
WHERE urakka = :urakka;

-- name: luo-paallystysilmoitus<!
INSERT INTO paallystysilmoitus
(paallystyskohde, ilmoitustiedot, luotu, luoja)
    VALUES (:paallystyskohde, :ilmoitustiedot::JSONB, NOW(), :luoja);

-- name: lukitse-urakan-yha-sidonta<!
UPDATE yhatiedot SET sidonta_lukittu = true WHERE urakka = :urakka;

-- name: poista-urakan-paallystysilmoitukset!
DELETE FROM paallystysilmoitus WHERE paallystyskohde IN (SELECT id FROM yllapitokohde WHERE urakka = :urakka);

-- name: poista-urakan-paikkausilmoitukset!
DELETE FROM paikkausilmoitus WHERE paikkauskohde IN (SELECT id FROM yllapitokohde WHERE urakka = :urakka);

-- name: paivita-paallystys-tai-paikkausurakan-geometria
SELECT paivita_paallystys_tai_paikkausurakan_geometria(:urakka :: INTEGER);