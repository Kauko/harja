-- name: hae-urakan-tarkastukset
-- Hakee urakan tarkastukset aikavälin perusteella
SELECT t.id, sopimus, t.aika,
  t.tr_numero, t.tr_alkuosa, t.tr_alkuetaisyys, t.tr_loppuosa, t.tr_loppuetaisyys,
  t.sijainti,
  tarkastaja, mittaaja,
  tyyppi, -- tähän myös havainnon kuvaus
  tekija
  FROM tarkastus t
  LEFT JOIN havainto ON t.havainto = havainto.id
 WHERE t.urakka = :urakka
   AND (t.aika >= :alku AND t.aika <= :loppu)
   AND (:rajaa_tienumerolla = false OR t.tr_numero = :tienumero)
   AND (:rajaa_tyypilla = false OR t.tyyppi = :tyyppi::tarkastustyyppi);

-- name: hae-tarkastus
-- Hakee yhden urakan tarkastuksen tiedot id:llä.
SELECT t.id, t.sopimus, t.aika,
       t.tr_numero, t.tr_alkuosa, t.tr_alkuetaisyys, t.tr_loppuosa, t.tr_loppuetaisyys,
       t.sijainti,
       t.tarkastaja, t.mittaaja,
       t.tyyppi, t.havainnot,
       stm.hoitoluokka as soratiemittaus_hoitoluokka,
       stm.tasaisuus as soratiemittaus_tasaisuus,
       stm.kiinteys as soratiemittaus_kiinteys,
       stm.polyavyys as soratiemittaus_polyavyys,
       stm.sivukaltevuus as soratiemittaus_sivukaltevuus,
       thm.talvihoitoluokka as talvihoitomittaus_hoitoluokka,
       thm.lumimaara as talvihoitomittaus_lumimaara,
       thm.tasaisuus as talvihoitomittaus_tasaisuus,
       thm.kitka as talvihoitomittaus_kitka,
       thm.lampotila as talvihoitomittaus_lampotila,
       thm.ajosuunta as talvihoitomittaus_ajosuunta
  FROM tarkastus t
       LEFT JOIN soratiemittaus stm ON (t.tyyppi = 'soratie'::tarkastustyyppi AND stm.tarkastus=t.id)
       LEFT JOIN talvihoitomittaus thm ON (t.tyyppi = 'talvihoito'::tarkastustyyppi AND thm.tarkastus=id)
 WHERE t.urakka = :urakka AND t.id = :id
 
-- name: luo-tarkastus<!
-- Luo uuden tarkastuksen
INSERT
  INTO tarkastus
       (urakka, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys,
        sijainti, tarkastaja, mittaaja, tyyppi, luoja, ulkoinen_id, havainnot)
VALUES (:urakka, :aika, :tr_numero, :tr_alkuosa, :tr_alkuetaisyys, :tr_loppuosa, :tr_loppuetaisyys,
        :sijainti, :tarkastaja, :mittaaja, :tyyppi::tarkastustyyppi, :luoja, :ulkoinen_id, :havainnot)

-- name: paivita-tarkastus!
-- Päivittää tarkastuksen tiedot
UPDATE tarkastus
   SET aika = :aika,
       tr_numero = :tr_numero,
       tr_alkuosa = :tr_alkuosa,
       tr_alkuetaisyys = :tr_alkuetaisyys,
       tr_loppuosa = :tr_loppuosa,
       tr_loppuetaisyys = :tr_loppuetaisyys,
       sijainti = :sijainti,
       tarkastaja = :tarkastaja,
       mittaaja = :mittaaja,
       tyyppi = :tyyppi::tarkastustyyppi,
       muokkaaja = :muokkaaja,
       muokattu = current_timestamp,
       havainnot = :havainnot
 WHERE urakka = :urakka AND id = :id
 
-- name: luo-talvihoitomittaus<!
-- Luo uuden talvihoitomittauksen annetulle tarkastukselle.
INSERT
  INTO talvihoitomittaus
       (talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila, ajosuunta, tarkastus)
VALUES (:talvihoitoluokka, :lumimaara, :tasaisuus, :kitka, :lampotila, :ajosuunta, :tarkastus)

-- name: paivita-talvihoitomittaus!
-- Päivittää tarkastuksen aiemmin luodun talvihoitomittauksen.
UPDATE talvihoitomittaus
   SET talvihoitoluokka = :talvihoitoluokka,
       lumimaara = :lumimaara,
       tasaisuus = :tasaisuus,
       kitka = :kitka,
       lampotila = :lampotila,
       ajosuunta = :ajosuunta
 WHERE tarkastus = :tarkastus
       
-- name: luo-soratiemittaus<!
-- Luo uuden soratiemittauksen annetulle tarkastukselle.
INSERT
  INTO soratiemittaus
       (hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus, tarkastus)
VALUES (:hoitoluokka, :tasaisuus, :kiinteys, :polyavyys, :sivukaltevuus, :tarkastus)

-- name: paivita-soratiemittaus!
-- Päivittää tarkastuksen aiemmin luodun soratiemittauksen
UPDATE soratiemittaus
   SET hoitoluokka = :hoitoluokka,
       tasaisuus = :tasaisuus,
       kiinteys = :kiinteys,
       polyavyys = :polyavyys,
       sivukaltevuus = :sivukaltevuus
 WHERE tarkastus = :tarkastus

-- name: hae-tarkastus-ulkoisella-idlla-ja-tyypilla
-- Hakee tarkastuksen ja sen havainnon id:t ulkoisella id:lla ja luojalla.
SELECT id
  FROM tarkastus
 WHERE ulkoinen_id = :id
   AND tyyppi = :tyyppi :: tarkastustyyppi
   AND luoja = :luoja

-- name: luo-liite<!
-- Luo tarkastukselle liite
INSERT INTO tarkastus_liite (tarkastus, liite) VALUES (:tarkastus, :liite)
