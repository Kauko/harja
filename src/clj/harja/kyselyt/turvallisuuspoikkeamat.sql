-- name: hae-urakan-turvallisuuspoikkeamat
SELECT t.id, t.urakka, t.tapahtunut, t.paattynyt, t.kasitelty, t.tyontekijanammatti,
       t.tyotehtava, t.kuvaus, t.vammat, t.sairauspoissaolopaivat, t.sairaalavuorokaudet, t.sijainti,
       t.tr_numero, t.tr_alkuetaisyys, t.tr_loppuetaisyys, t.tr_alkuosa, t.tr_loppuosa, t.tyyppi,
       k.id              AS korjaavatoimenpide_id,
       k.kuvaus          AS korjaavatoimenpide_kuvaus,
       k.suoritettu      AS korjaavatoimenpide_suoritettu,
       k.vastaavahenkilo AS korjaavatoimenpide_vastaavahenkilo
  FROM turvallisuuspoikkeama t
       LEFT JOIN korjaavatoimenpide k ON t.id = k.turvallisuuspoikkeama AND k.poistettu IS NOT TRUE
 WHERE t.urakka = :urakka
       AND t.tapahtunut :: DATE BETWEEN :alku AND :loppu
 ORDER BY t.tapahtunut DESC;

-- name: hae-hallintayksikon-turvallisuuspoikkeamat
-- Hakee turvallisuuspoikkeamat, jotka ovat annetun hallintayksikön urakoissa raportoituja
SELECT t.id, t.urakka, t.tapahtunut, t.paattynyt, t.kasitelty, t.tyontekijanammatti,
       t.tyotehtava, t.kuvaus, t.vammat, t.sairauspoissaolopaivat, t.sairaalavuorokaudet, t.sijainti,
       t.tr_numero, t.tr_alkuetaisyys, t.tr_loppuetaisyys, t.tr_alkuosa, t.tr_loppuosa, t.tyyppi,
       k.id AS korjaavatoimenpide_id,
       k.kuvaus AS korjaavatoimenpide_kuvaus,
       k.suoritettu AS korjaavatoimenpide_suoritettu,
       k.vastaavahenkilo AS korjaavatoimenpide_vastaavahenkilo
  FROM turvallisuuspoikkeama t
      LEFT JOIN korjaavatoimenpide k ON t.id = k.turvallisuuspoikkeama AND k.poistettu IS NOT TRUE
 WHERE t.urakka IN (SELECT id FROM urakka WHERE hallintayksikko = :hallintayksikko)
       AND t.tapahtunut :: DATE BETWEEN :alku AND :loppu
 ORDER BY t.tapahtunut DESC;

-- name: hae-turvallisuuspoikkeamat
-- Hakee kaikki turvallisuuspoikkeamat aikavälillä ilman aluerajausta
SELECT t.id, t.urakka, t.tapahtunut, t.paattynyt, t.kasitelty, t.tyontekijanammatti,
       t.tyotehtava, t.kuvaus, t.vammat, t.sairauspoissaolopaivat, t.sairaalavuorokaudet, t.sijainti,
       t.tr_numero, t.tr_alkuetaisyys, t.tr_loppuetaisyys, t.tr_alkuosa, t.tr_loppuosa, t.tyyppi,
       k.id AS korjaavatoimenpide_id,
       k.kuvaus AS korjaavatoimenpide_kuvaus,
       k.suoritettu AS korjaavatoimenpide_suoritettu,
       k.vastaavahenkilo AS korjaavatoimenpide_vastaavahenkilo
  FROM turvallisuuspoikkeama t
      LEFT JOIN korjaavatoimenpide k ON t.id = k.turvallisuuspoikkeama AND k.poistettu IS NOT TRUE
 WHERE t.tapahtunut :: DATE BETWEEN :alku AND :loppu
 ORDER BY t.tapahtunut DESC;

-- name: hae-turvallisuuspoikkeama
-- Hakee yksittäisen urakan turvallisuuspoikkeaman
SELECT
  t.id,
  t.urakka,
  t.tapahtunut,
  t.paattynyt,
  t.kasitelty,
  t.tyontekijanammatti,
  t.tyotehtava,
  t.kuvaus,
  t.vammat,
  t.sairauspoissaolopaivat,
  t.sairaalavuorokaudet,
  t.vahingoittuneet_ruumiinosat as vahingoittuneetruumiinosat,
  t.sairaspoissaolo_jatkuu as sairaspoissaolojatkuu,
  t.sijainti,
  t.tr_numero,
  t.tr_alkuetaisyys,
  t.tr_loppuetaisyys,
  t.tr_alkuosa,
  t.tr_loppuosa,
  t.vakavuusaste,
  t.vahinkoluokittelu,
  t.tyyppi,

  k.id                   AS korjaavatoimenpide_id,
  k.kuvaus               AS korjaavatoimenpide_kuvaus,
  k.suoritettu           AS korjaavatoimenpide_suoritettu,
  k.vastaavahenkilo      AS korjaavatoimenpide_vastaavahenkilo,

  kom.id                 AS kommentti_id,
  kom.tekija             AS kommentti_tekija,
  kom.kommentti          AS kommentti_kommentti,
  kom.luotu              AS kommentti_aika,
  (SELECT CONCAT(etunimi, ' ', sukunimi)
   FROM kayttaja
   WHERE id = kom.luoja) AS kommentti_tekijanimi,

  koml.id                AS kommentti_liite_id,
  koml.tyyppi            AS kommentti_liite_tyyppi,
  koml.koko              AS kommentti_liite_koko,
  koml.nimi              AS kommentti_liite_nimi,
  koml.liite_oid         AS kommentti_liite_oid,

  l.id                   AS liite_id,
  l.tyyppi               AS liite_tyyppi,
  l.koko                 AS liite_koko,
  l.nimi                 AS liite_nimi,
  l.liite_oid            AS liite_oid,
  l.pikkukuva            AS liite_pikkukuva

FROM turvallisuuspoikkeama t
  LEFT JOIN korjaavatoimenpide k
    ON t.id = k.turvallisuuspoikkeama
       AND k.poistettu IS NOT TRUE

  LEFT JOIN turvallisuuspoikkeama_liite tl
    ON t.id = tl.turvallisuuspoikkeama
  LEFT JOIN liite l
    ON l.id = tl.liite

  LEFT JOIN turvallisuuspoikkeama_kommentti tpk
    ON t.id = tpk.turvallisuuspoikkeama
  LEFT JOIN kommentti kom
    ON tpk.kommentti = kom.id
       AND kom.poistettu IS NOT TRUE

  LEFT JOIN liite koml ON kom.liite = koml.id

WHERE t.id = :id AND t.urakka = :urakka

-- name: onko-olemassa-ulkoisella-idlla
-- Tarkistaa löytyykö turvallisuuspoikkeamaa ulkoisella id:llä
SELECT exists(
    SELECT tp.id
    FROM turvallisuuspoikkeama tp
    WHERE tp.ulkoinen_id = :ulkoinen_id AND luoja = :luoja);

-- name: liita-kommentti<!
INSERT INTO turvallisuuspoikkeama_kommentti (turvallisuuspoikkeama, kommentti)
VALUES (:turvallisuuspoikkeama, :kommentti);

-- name: liita-liite<!
INSERT INTO turvallisuuspoikkeama_liite (turvallisuuspoikkeama, liite)
VALUES (:turvallisuuspoikkeama, :liite);

--name: paivita-korjaava-toimenpide<!
UPDATE korjaavatoimenpide
SET
  kuvaus          = :kuvaus,
  suoritettu      = :suoritettu,
  vastaavahenkilo = :vastaava,
  poistettu       = :poistettu
WHERE id = :id AND turvallisuuspoikkeama = :tp;

--name: luo-korjaava-toimenpide<!
INSERT INTO korjaavatoimenpide
(turvallisuuspoikkeama, kuvaus, suoritettu, vastaavahenkilo, poistettu)
VALUES
  (:tp, :kuvaus, :suoritettu, :vastaava, FALSE);

--name: paivita-turvallisuuspoikkeama<!
-- Kysely piti katkaista kahtia, koska Yesql <0.5 tukee vain positional parametreja, joita
-- Clojuressa voi olla max 20.
UPDATE turvallisuuspoikkeama
SET
  urakka                      = :urakka,
  tapahtunut                  = :tapahtunut,
  paattynyt                   = :paattynyt,
  kasitelty                   = :kasitelty,
  tyontekijanammatti          = :ammatti,
  tyotehtava                  = :tehtava,
  kuvaus                      = :kuvaus,
  vammat                      = :vammat :: turvallisuuspoikkeama_aiheutuneet_vammat[],
  sairauspoissaolopaivat      = :poissa,
  sairaalavuorokaudet         = :sairaalassa,
  tyyppi                      = :tyyppi :: turvallisuuspoikkeama_luokittelu [],
  vahingoittuneet_ruumiinosat = :vahingoittuneet_ruumiinosat :: turvallisuuspoikkeama_vahingoittunut_ruumiinosa [],
  sairaspoissaolo_jatkuu      = :sairaspoissaolo_jatkuu,
  muokkaaja                   = :kayttaja,
  muokattu                    = NOW(),
  vahinkoluokittelu           = :vahinkoluokittelu :: turvallisuuspoikkeama_vahinkoluokittelu [],
  vakavuusaste                = :vakavuusaste :: turvallisuuspoikkeama_vakavuusaste
WHERE id = :id;

--name: aseta-turvallisuuspoikkeaman-sijainti!
-- Kysely piti katkaista kahtia, koska Yesql <0.5 tukee vain positional parametreja, joita
-- Clojuressa voi olla max 20. Ei aseta muokkaajaa ja muokattua, koska:
-- * kyselyä kutsutaan heti paivita1:sen jälkeen, joka jo asettaa ne
-- * kyselyä kutsutaan heti luonnin jälkeen
UPDATE turvallisuuspoikkeama
SET
  sijainti         = :sijainti,
  tr_numero        = :numero,
  tr_alkuetaisyys  = :aet,
  tr_loppuetaisyys = :let,
  tr_alkuosa       = :aos,
  tr_loppuosa      = :los
WHERE id = :id;

--name: paivita-turvallisuuspoikkeama-ulkoisella-idlla<!

UPDATE turvallisuuspoikkeama
SET urakka               = :urakka,
  tapahtunut             = :tapahtunut,
  paattynyt              = :paattynyt,
  kasitelty              = :kasitelty,
  tyontekijanammatti     = :ammatti,
  tyotehtava             = :tehtava,
  kuvaus                 = :kuvaus,
  vammat                 = :vammat :: turvallisuuspoikkeama_aiheutuneet_vammat[],
  sairauspoissaolopaivat = :poissa,
  sairaalavuorokaudet    = :sairaalassa,
  tyyppi                 = :tyyppi :: turvallisuuspoikkeama_luokittelu [],
  muokkaaja              = :kayttaja,
  vahinkoluokittelu      = :vahinkoluokittelu :: turvallisuuspoikkeama_vahinkoluokittelu[],
  vakavuusaste           = :vakavuusaste :: turvallisuuspoikkeama_vakavuusaste,
  muokattu               = NOW()
WHERE ulkoinen_id = :id AND
      luoja = :luoja;

--name: aseta-turvallisuuspoikkeaman-sijainti-ulkoisella-idlla<!
UPDATE turvallisuuspoikkeama
SET
  sijainti         = POINT(:x_koordinaatti, :y_koordinaatti) :: GEOMETRY,
  tr_numero        = :numero,
  tr_alkuetaisyys  = :aet,
  tr_loppuetaisyys = :let,
  tr_alkuosa       = :aos,
  tr_loppuosa      = :los
WHERE ulkoinen_id = :id AND
      luoja = :luoja;

--name: aseta-ulkoinen-id<!
UPDATE turvallisuuspoikkeama
SET ulkoinen_id = :ulk
WHERE id = :id;

--name: luo-turvallisuuspoikkeama<!
-- Kysely piti katkaista kahtia, koska Yesql <0.5 tukee vain positional parametreja, joita
-- Clojuressa voi olla max 20.
INSERT INTO turvallisuuspoikkeama
(urakka, tapahtunut, paattynyt, kasitelty, tyontekijanammatti, tyotehtava, kuvaus, vammat,
 sairauspoissaolopaivat, sairaalavuorokaudet, tyyppi, luoja, luotu, vahinkoluokittelu, vakavuusaste, vahingoittuneet_ruumiinosat,
 sairaspoissaolo_jatkuu)
VALUES
  (:urakka, :tapahtunut, :paattynyt, :kasitelty, :ammatti, :tehtava, :kuvaus, :vammat :: turvallisuuspoikkeama_aiheutuneet_vammat[], :poissaolot, :sairaalassa,
   :tyyppi :: turvallisuuspoikkeama_luokittelu [], :kayttaja, NOW(), :vahinkoluokittelu :: turvallisuuspoikkeama_vahinkoluokittelu[],
   :vakavuusaste :: turvallisuuspoikkeama_vakavuusaste, :vahingoittunut_ruumiinosa :: turvallisuuspoikkeama_vahingoittunut_ruumiinosa[],
   :sairaspoissaolo_jatkuu);
