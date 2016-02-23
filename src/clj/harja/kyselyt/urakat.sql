-- name: hae-kaikki-urakat-aikavalilla
SELECT
  u.id,
  u.nimi,
  u.tyyppi
FROM urakka u
WHERE ((u.loppupvm >= :alku AND u.alkupvm <= :loppu) OR (u.loppupvm IS NULL AND u.alkupvm <= :loppu)) AND
      (:urakoitsija :: INTEGER IS NULL OR :urakoitsija = u.urakoitsija) AND
      (:urakkatyyppi :: urakkatyyppi IS NULL OR u.tyyppi :: TEXT = :urakkatyyppi) AND
      (:hallintayksikko :: INTEGER IS NULL OR :hallintayksikko = u.hallintayksikko);

-- name: hae-kaynnissa-olevat-urakat
SELECT
  u.id,
  u.nimi,
  u.tyyppi
FROM urakka u
WHERE (u.alkupvm IS NULL OR u.alkupvm <= current_date) AND
      (u.loppupvm IS NULL OR u.loppupvm >= current_date);

-- name: hae-hallintayksikon-urakat
SELECT
  u.id,
  u.nimi,
  u.tyyppi
FROM urakka u
  JOIN organisaatio o ON o.id = u.hallintayksikko
WHERE o.id = :hy;

-- name: listaa-urakat-hallintayksikolle
-- Palauttaa listan annetun hallintayksikön (id) urakoista. Sisältää perustiedot ja geometriat.
-- PENDING: joinataan mukaan ylläpidon urakat eri taulusta?
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN hanke h ON u.hanke = h.id
  LEFT JOIN alueurakka au ON h.alueurakkanro = au.alueurakkanro
WHERE hallintayksikko = :hallintayksikko
      AND (('hallintayksikko' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi OR
            'liikennevirasto' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi)
           OR ('urakoitsija' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi AND
               :kayttajan_org_id = urk.id));

-- name: hae-urakan-organisaatio
-- Hakee urakan organisaation urakka-id:llä.
SELECT
  o.nimi,
  o.ytunnus
FROM organisaatio o
  JOIN urakka u ON o.id = u.urakoitsija
WHERE u.id = :urakka;

-- name: hae-urakoita
-- Hakee urakoita tekstihaulla.
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN hanke h ON u.hanke = h.id
  LEFT JOIN alueurakka au ON h.alueurakkanro = au.alueurakkanro
WHERE u.nimi ILIKE :teksti
      OR hal.nimi ILIKE :teksti
      OR urk.nimi ILIKE :teksti;

-- name: hae-organisaation-urakat
-- Hakee organisaation "omat" urakat, joko urakat joissa annettu hallintayksikko on tilaaja
-- tai urakat joissa annettu urakoitsija on urakoitsijana.
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN hanke h ON u.hanke = h.id
  LEFT JOIN alueurakka au ON h.alueurakkanro = au.alueurakkanro
WHERE urk.id = :organisaatio
      OR hal.id = :organisaatio;

-- name: tallenna-urakan-sopimustyyppi!
-- Tallentaa urakalle sopimustyypin
UPDATE urakka
SET sopimustyyppi = :sopimustyyppi :: sopimustyyppi
WHERE id = :urakka;

-- name: tallenna-urakan-tyyppi!
-- Vaihtaa urakan tyypin
UPDATE urakka
SET tyyppi = :urakkatyyppi :: urakkatyyppi
WHERE id = :urakka;

-- name: hae-urakan-sopimustyyppi
-- Hakee urakan sopimustyypin
SELECT sopimustyyppi
FROM urakka
WHERE id = :urakka;

-- name: hae-urakan-tyyppi
-- Hakee urakan tyypin
SELECT tyyppi
FROM urakka
WHERE id = :urakka;

-- name: hae-urakoiden-tunnistetiedot
-- Hakee urakoista ydintiedot tekstihaulla.
SELECT
  u.id,
  u.nimi,
  u.hallintayksikko,
  u.sampoid
FROM urakka u
  JOIN organisaatio hal ON u.hallintayksikko = hal.id
  JOIN organisaatio urk ON u.urakoitsija = urk.id
WHERE (u.nimi ILIKE :teksti
       OR u.sampoid ILIKE :teksti)
      AND (('hallintayksikko' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi OR
            'liikennevirasto' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi)
           OR ('urakoitsija' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi AND
               :kayttajan_org_id = urk.id))
LIMIT 11;

-- name: hae-urakka
-- Hakee urakan perustiedot id:llä APIa varten.
SELECT
  u.id,
  u.nimi,
  u.tyyppi,
  u.alkupvm,
  u.loppupvm,
  h.alueurakkanro AS alueurakkanumero,
  urk.nimi        AS urakoitsija_nimi,
  urk.ytunnus     AS urakoitsija_ytunnus
FROM urakka u
  LEFT JOIN hanke h ON h.id = u.hanke
  JOIN organisaatio urk ON u.urakoitsija = urk.id
WHERE u.id = :id;

-- name: hae-urakat-ytunnuksella
SELECT
  u.id,
  u.nimi,
  u.tyyppi,
  u.alkupvm,
  u.loppupvm,
  h.alueurakkanro AS alueurakkanumero,
  urk.nimi        AS urakoitsija_nimi,
  urk.ytunnus     AS urakoitsija_ytunnus
FROM urakka u
  JOIN hanke h ON h.id = u.hanke
  JOIN organisaatio urk ON u.urakoitsija = urk.id
                           AND urk.ytunnus = :ytunnus;

-- name: hae-urakan-sopimukset
-- Hakee urakan sopimukset urakan id:llä.
SELECT
  s.id,
  s.nimi,
  s.alkupvm,
  s.loppupvm
FROM sopimus s
WHERE s.urakka = :urakka;

-- name: onko-olemassa
-- Tarkistaa onko id:n mukaista urakkaa olemassa tietokannassa
SELECT EXISTS(SELECT id
              FROM urakka
              WHERE id = :id);

-- name: paivita-hankkeen-tiedot-urakalle!
-- Päivittää hankkeen sampo id:n avulla urakalle
UPDATE urakka
SET hanke = (SELECT id
             FROM hanke
             WHERE sampoid = :hanke_sampo_id)
WHERE hanke_sampoid = :hanke_sampo_id;

-- name: luo-urakka<!
-- Luo uuden urakan.
INSERT INTO urakka (nimi, alkupvm, loppupvm, hanke_sampoid, sampoid, tyyppi, hallintayksikko)
VALUES (:nimi, :alkupvm, :loppupvm, :hanke_sampoid, :sampoid, :urakkatyyppi :: urakkatyyppi, :hallintayksikko);

-- name: paivita-urakka!
-- Paivittaa urakan
UPDATE urakka
SET nimi = :nimi, alkupvm = :alkupvm, loppupvm = :loppupvm, hanke_sampoid = :hanke_sampoid,
  tyyppi = :urakkatyyppi :: urakkatyyppi, hallintayksikko = :hallintayksikko
WHERE id = :id;

-- name: paivita-tyyppi-hankkeen-urakoille!
-- Paivittaa annetun tyypin kaikille hankkeen urakoille
UPDATE urakka
SET tyyppi = :urakkatyyppi :: urakkatyyppi
WHERE hanke = (SELECT id
               FROM hanke
               WHERE sampoid = :hanke_sampoid);

-- name: hae-id-sampoidlla
-- Hakee urakan id:n sampo id:llä
SELECT urakka.id
FROM urakka
WHERE sampoid = :sampoid;

-- name: aseta-urakoitsija-sopimuksen-kautta!
-- Asettaa urakalle urakoitsijan sopimuksen Sampo id:n avulla
UPDATE urakka
SET urakoitsija = (
  SELECT id
  FROM organisaatio
  WHERE sampoid = (
    SELECT urakoitsija_sampoid
    FROM sopimus
    WHERE sampoid = :sopimus_sampoid))
WHERE sampoid = (
  SELECT urakka_sampoid
  FROM sopimus
  WHERE sampoid = :sopimus_sampoid AND
        paasopimus IS NULL);

-- name: aseta-urakoitsija-urakoille-yhteyshenkilon-kautta!
-- Asettaa urakoille urakoitsijan yhteyshenkilön Sampo id:n avulla
UPDATE urakka
SET urakoitsija = (
  SELECT id
  FROM organisaatio
  WHERE sampoid = :urakoitsija_sampoid)
WHERE sampoid IN (
  SELECT urakka_sampoid
  FROM sopimus
  WHERE urakoitsija_sampoid = :urakoitsija_sampoid AND
        paasopimus IS NULL);

-- name: hae-yksittainen-urakka
-- Hakee yhden urakan id:n avulla
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN hanke h ON u.hanke = h.id
  LEFT JOIN alueurakka au ON h.alueurakkanro = au.alueurakkanro
WHERE u.id = :urakka_id;

-- name: hae-urakan-urakoitsija
-- Hakee valitun urakan urakoitsijan id:n
SELECT urakoitsija
FROM urakka
WHERE id = :urakka_id

-- name: paivita-urakka-alaueiden-nakyma
-- Päivittää urakka-alueiden materialisoidun näkymän
SELECT paivita_urakoiden_alueet();

-- name: hae-urakan-alueurakkanumero
-- Hakee urakan alueurakkanumeron
SELECT alueurakkanro
FROM hanke
WHERE id = (SELECT hanke
            FROM urakka
            WHERE id = :id);

-- name: hae-aktiivisten-hoitourakoiden-alueurakkanumerot
-- Hakee käynnissäolevien hoitourakoiden alueurakkanumerot
SELECT
  u.id,
  u.hanke,
  u.nimi,
  h.alueurakkanro
FROM urakka u
  LEFT JOIN hanke h ON u.hanke = h.id
WHERE u.id IN (SELECT id
               FROM urakka
               WHERE (tyyppi = 'hoito' AND
                      u.hanke IS NOT NULL AND
                      (SELECT EXTRACT(YEAR FROM u.alkupvm)) <= :vuosi AND
                      :vuosi <= (SELECT EXTRACT(YEAR FROM u.loppupvm))));

-- name: hae-hallintayksikon-kaynnissa-olevat-urakat
-- Palauttaa nimen ja id:n hallintayksikön käynnissä olevista urakoista
SELECT
  id,
  nimi
FROM urakka
WHERE hallintayksikko = :hal
      AND (alkupvm IS NULL OR alkupvm <= current_date)
      AND (loppupvm IS NULL OR loppupvm >= current_date);

-- name: onko-urakalla-tehtavaa
SELECT EXISTS(
    SELECT tpk.id
    FROM toimenpidekoodi tpk
      INNER JOIN toimenpideinstanssi tpi
        ON tpi.toimenpide = tpk.emo
    WHERE
      tpi.urakka = :urakkaid AND
      tpk.id = :tehtavaid);

-- name: hae-urakka-sijainnilla
-- Hakee sijainnin ja urakan tyypin perusteella urakan. Urakan täytyy myös olla käynnissä.
SELECT u.id
FROM urakoiden_alueet ua
  JOIN urakka u ON ua.id = u.id
WHERE ua.tyyppi = :urakkatyyppi :: urakkatyyppi
      AND (st_contains(ua.alue, ST_MakePoint(:x, :y)))
      AND (u.alkupvm IS NULL OR u.alkupvm <= current_timestamp)
      AND (u.loppupvm IS NULL OR u.loppupvm > current_timestamp)
ORDER BY id ASC;

-- name: luo-alueurakka<!
INSERT INTO alueurakka (alueurakkanro, alue, elynumero)
VALUES (:alueurakkanro, ST_GeomFromText(:alue) :: GEOMETRY, :elynumero);

-- name: paivita-alueurakka!
UPDATE alueurakka
SET alueurakkanro = :alueurakkanro,
  alue            = ST_GeomFromText(:alue) :: GEOMETRY,
  elynumero       = :elynumero;

-- name: hae-alueurakka-numerolla
SELECT *
FROM alueurakka
WHERE alueurakkanro = :alueurakkanro;

-- name: tuhoa-alueurakkadata!
DELETE FROM alueurakka;

-- name: hae-urakan-geometria
SELECT
  u.alue          AS urakka_alue,
  alueurakka.alue AS alueurakka_alue
FROM urakka u
  JOIN hanke ON u.hanke = hanke.id
  JOIN alueurakka ON hanke.alueurakkanro = alueurakka.alueurakkanro
WHERE u.id = :id;