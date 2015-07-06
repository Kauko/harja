-- name: hae-kaikki-havainnot
-- Hakee listaukseen kaikki urakan havainnot annetulle aikavälille
SELECT
  h.id,
  h.aika,
  h.kohde,
  h.tekija,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  h.kasittelyaika                    AS paatos_kasittelyaika,
  h.paatos                           AS paatos_paatos,
  h.kasittelytapa                    AS paatos_kasittelytapa,
  (SELECT k.kommentti
   FROM kommentti k
   WHERE k.id IN (SELECT hk.kommentti
                  FROM havainto_kommentti hk
                  WHERE hk.havainto = h.id)
   ORDER BY luotu ASC
   OFFSET 0
   LIMIT 1)                          AS kuvaus
FROM havainto h
  JOIN kayttaja k ON h.luoja = k.id
WHERE h.urakka = :urakka
      AND (aika >= :alku AND aika <= :loppu);

-- name: hae-selvitysta-odottavat-havainnot
-- Hakee listaukseen kaikki urakan havainnot, jotka odottavat urakoitsijalta selvitystä.
SELECT
  h.id,
  h.aika,
  h.kohde,
  h.tekija,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  h.kasittelyaika                    AS paatos_kasittelyaika,
  h.paatos                           AS paatos_paatos,
  h.kasittelytapa                    AS paatos_kasittelytapa,
  (SELECT k.kommentti
   FROM kommentti k
   WHERE k.id IN (SELECT hk.kommentti
                  FROM havainto_kommentti hk
                  WHERE hk.havainto = h.id)
   ORDER BY luotu ASC
   OFFSET 0
   LIMIT 1)                          AS kuvaus
FROM havainto h
  JOIN kayttaja k ON h.luoja = k.id
WHERE h.urakka = :urakka
      AND (aika >= :alku AND aika <= :loppu)
      AND selvitys_pyydetty = TRUE AND selvitys_annettu = FALSE;

-- name: hae-kasitellyt-havainnot
-- Hakee listaukseen kaikki urakan havainnot, jotka on käsitelty.
SELECT
  h.id,
  h.aika,
  h.kohde,
  h.tekija,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  h.kasittelyaika                    AS paatos_kasittelyaika,
  h.paatos                           AS paatos_paatos,
  h.kasittelytapa                    AS paatos_kasittelytapa,
  (SELECT k.kommentti
   FROM kommentti k
   WHERE k.id IN (SELECT hk.kommentti
                  FROM havainto_kommentti hk
                  WHERE hk.havainto = h.id)
   ORDER BY luotu ASC
   OFFSET 0
   LIMIT 1)                          AS kuvaus
FROM havainto h
  JOIN kayttaja k ON h.luoja = k.id
WHERE h.urakka = :urakka
      AND (aika >= :alku AND aika <= :loppu)
      AND paatos IS NOT NULL;

-- name: hae-omat-havainnot
-- Hakee listaukseen kaikki urakan havainnot, joiden luoja tai kommentoija on annettu henkilö.
SELECT
  h.id,
  h.aika,
  h.kohde,
  h.tekija,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  h.kasittelyaika                    AS paatos_kasittelyaika,
  h.paatos                           AS paatos_paatos,
  h.kasittelytapa                    AS paatos_kasittelytapa,
  (SELECT k.kommentti
   FROM kommentti k
   WHERE k.id IN (SELECT hk.kommentti
                  FROM havainto_kommentti hk
                  WHERE hk.havainto = h.id)
   ORDER BY luotu ASC
   OFFSET 0
   LIMIT 1)                          AS kuvaus
FROM havainto h
  JOIN kayttaja k ON h.luoja = k.id
WHERE h.urakka = :urakka
      AND (aika >= :alku AND aika <= :loppu)
      AND (h.luoja = :kayttaja OR
           h.id IN (SELECT hk.havainto
                    FROM havainto_kommentti hk JOIN kommentti k ON hk.kommentti = k.id
                    WHERE k.luoja = :kayttaja));


-- name: hae-havainnon-tiedot
-- Hakee havainnon tiedot muokkausnäkymiin.
SELECT
  h.id,
  h.aika,
  h.kohde,
  h.tekija,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  h.kasittelyaika                    AS paatos_kasittelyaika,
  h.paatos                           AS paatos_paatos,
  h.kasittelytapa                    AS paatos_kasittelytapa,
  h.perustelu                        AS paatos_perustelu,
  h.muu_kasittelytapa                AS paatos_muukasittelytapa
FROM havainto h
  JOIN kayttaja k ON h.luoja = k.id
WHERE h.urakka = :urakka
      AND h.id = :id;

-- name: hae-havainnon-kommentit
-- Hakee annetun havainnon kaikki kommentit (joita ei ole poistettu) sekä
-- kommentin mahdollisen liitteen tiedot. Kommentteja on vaikea hakea
-- array aggregoimalla itse havainnon hakukyselyssä.
SELECT
  k.id,
  k.tekija,
  k.kommentti,
  k.luoja,
  k.luotu                              AS aika,
  CONCAT(ka.etunimi, ' ', ka.sukunimi) AS tekijanimi,
  l.id                                 AS liite_id,
  l.tyyppi                             AS liite_tyyppi,
  l.koko                               AS liite_koko,
  l.nimi                               AS liite_nimi,
  l.liite_oid                          AS liite_oid
FROM kommentti k
  JOIN kayttaja ka ON k.luoja = ka.id
  LEFT JOIN liite l ON l.id = k.liite
WHERE k.poistettu = FALSE
      AND k.id IN (SELECT hk.kommentti
                   FROM havainto_kommentti hk
                   WHERE hk.havainto = :id)
ORDER BY k.luotu ASC;


-- name: paivita-havainnon-perustiedot!
-- Päivittää aiemmin luodun havainnon perustiedot
UPDATE havainto
SET aika            = :aika,
  tekija            = :tekija :: osapuoli,
  kohde             = :kohde,
  selvitys_pyydetty = :selvitys,
  muokkaaja         = :muokkaaja,
  kuvaus            = :kuvaus,
  muokattu          = current_timestamp
WHERE id = :id;

-- name: luo-havainto<!
-- Luo uuden havainnon annetuille perustiedoille. Luontivaiheessa ei
-- voi antaa päätöstietoja.
INSERT
INTO havainto
     (urakka, aika, tekija, kohde, selvitys_pyydetty, luoja, luotu, kuvaus, sijainti, tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, ulkoinen_id)
VALUES (:urakka, :aika, :tekija :: osapuoli, :kohde, :selvitys, :luoja, current_timestamp, :kuvaus,
        POINT(:x_koordinaatti, :y_koordinaatti), :tr_numero, :tr_alkuosa, :tr_loppuosa, :tr_alkuetaisyys,
        :tr_loppuetaisyys, :ulkoinen_id);

-- name: kirjaa-havainnon-paatos!
-- Kirjaa havainnolle päätöksen.
UPDATE havainto
SET kasittelyaika   = :kasittelyaika,
  paatos            = :paatos :: havainnon_paatostyyppi,
  perustelu         = :perustelu,
  kasittelytapa     = :kasittelytapa :: havainnon_kasittelytapa,
  muu_kasittelytapa = :muukasittelytapa,
  muokkaaja         = :muokkaaja,
  muokattu          = current_timestamp
WHERE id = :id;

-- name: liita-kommentti<!
-- Liittää havaintoon uuden kommentin
INSERT INTO havainto_kommentti (havainto, kommentti) VALUES (:havainto, :kommentti);

-- name: liita-havainto<!
-- Liittää havaintoon uuden liitteen
INSERT INTO havainto_liite (havainto, liite) VALUES (:havainto, :liite);

-- name: onko-olemassa-ulkoisella-idlla
-- Tarkistaa löytyykö havaintoa ulkoisella id:llä
SELECT exists(
    SELECT havainto.id
    FROM havainto
    WHERE ulkoinen_id = :ulkoinen_id);

-- name: poista-havainto-ulkoisella-idlla!
-- Poistaa havainnon sekä siihen liittyvät kommentit ja liitteet ulkoisella id:llä
WITH havainto_idt AS (
    SELECT id
    FROM havainto
    WHERE ulkoinen_id = :ulkoinen_id),
    liitteet AS (
      SELECT liite
      FROM havainto_liite
      WHERE havainto IN (
        SELECT id
        FROM havainto_idt)),
    kommentit AS (
      SELECT kommentti
      FROM havainto_kommentti
      WHERE havainto IN (
        SELECT id
        FROM havainto_idt)),
    havainto_liitteet_poisto AS (
    DELETE FROM havainto_liite
    WHERE havainto IN (SELECT id
                       FROM havainto_idt)),
    havainto_kommentit_poisto AS (
    DELETE FROM havainto_kommentti
    WHERE havainto IN (SELECT id
                       FROM havainto_idt)),
    kommentien_poisto AS (
    DELETE FROM kommentti
    WHERE id IN (SELECT id
                 FROM kommentit)),
    liitteiden_poisto AS (
    DELETE FROM liite
    WHERE id IN (SELECT id
                 FROM liitteet))
DELETE FROM havainto
WHERE id IN (SELECT id
             FROM havainto_idt);

