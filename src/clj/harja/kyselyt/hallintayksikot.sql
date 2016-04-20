-- name: listaa-hallintayksikot-kulkumuodolle
-- Hakee hallintayksiköiden perustiedot ja geometriat kulkumuodon mukaan
SELECT id, nimi, alue,
       lpad(cast(elynumero as varchar), 2, '0') as elynumero
  FROM organisaatio
 WHERE tyyppi = 'hallintayksikko'::organisaatiotyyppi AND
       liikennemuoto = :liikennemuoto::liikennemuoto
 ORDER BY elynumero ASC

-- name: hae-organisaation-tunnistetiedot
-- Hakee organisaation perustiedot tekstihaulla.
SELECT o.id, o.nimi, o.tyyppi as organisaatiotyyppi, o.lyhenne
  FROM organisaatio o
 WHERE o.nimi ILIKE :teksti OR upper(o.lyhenne) ILIKE upper(:teksti)
 LIMIT 11;

-- name: hae-organisaatio
-- Hakee organisaation perustiedot id:llä
SELECT o.id, o.nimi, o.tyyppi as organisaatiotyyppi
  FROM organisaatio o
 WHERE o.id = :id;

 -- name: hae-hallintayksikon-geometria
 SELECT alue FROM organisaatio WHERE id = :id;
