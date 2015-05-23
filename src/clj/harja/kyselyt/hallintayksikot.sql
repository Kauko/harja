-- name: listaa-hallintayksikot-kulkumuodolle
-- Hakee hallintayksiköiden perustiedot ja geometriat kulkumuodon mukaan
SELECT id, nimi, alue
  FROM organisaatio
 WHERE tyyppi = 'hallintayksikko'::organisaatiotyyppi AND
       liikennemuoto = :liikennemuoto::liikennemuoto

-- name: hae-organisaation-tunnistetiedot
-- Hakee organisaation perustiedot tekstihaulla.
SELECT o.id, o.nimi, o.tyyppi
  FROM organisaatio o
 WHERE o.nimi ILIKE :teksti