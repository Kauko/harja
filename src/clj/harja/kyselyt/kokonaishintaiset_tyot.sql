-- name: listaa-kokonaishintaiset-tyot
-- Hakee kaikki urakan kokonaishintaiset-tyot
SELECT
  kt.id,
  kt.vuosi,
  kt.kuukausi,
  kt.summa,
  kt.maksupvm,
  kt.toimenpideinstanssi,
  kt.sopimus,
  tpi.id         AS tpi_id,
  tpi.nimi       AS tpi_nimi,
  tpi.toimenpide AS toimenpide
FROM kokonaishintainen_tyo kt
  LEFT JOIN toimenpideinstanssi tpi ON kt.toimenpideinstanssi = tpi.id
WHERE tpi.urakka = :urakka
ORDER BY vuosi, kuukausi;

-- name: hae-urakan-sopimuksen-kokonaishintaiset-tehtavat
-- Urakan sopimuksen kokonaishintaiset tehtävät
SELECT
  id,
  nimi,
  yksikko
FROM toimenpidekoodi
WHERE
  NOT poistettu AND
  id IN
  (SELECT DISTINCT (id)
   FROM toimenpidekoodi
   WHERE emo IN
         (SELECT DISTINCT (tpi.toimenpide)
          FROM kokonaishintainen_tyo kht
            INNER JOIN toimenpideinstanssi tpi ON tpi.id = kht.toimenpideinstanssi
          WHERE tpi.urakka = :urakkaid AND
          WHERE tpi.urakka = :urakkaid AND
                kht.sopimus = :sopimusid))
  AND kokonaishintainen
ORDER BY id;

-- name: paivita-kokonaishintainen-tyo!
-- Päivittää kokonaishintaisen tyon summan ja maksupvm:n, tunnisteena tpi, sop, vu ja kk

UPDATE kokonaishintainen_tyo
SET summa = :summa, maksupvm = :maksupvm
WHERE toimenpideinstanssi = :toimenpideinstanssi AND sopimus = :sopimus
      AND vuosi = :vuosi AND kuukausi = :kuukausi;


-- name: lisaa-kokonaishintainen-tyo<!
-- Lisää kokonaishintaisen tyon
INSERT INTO kokonaishintainen_tyo
(summa, maksupvm, toimenpideinstanssi, sopimus, vuosi, kuukausi)
VALUES (:summa, :maksupvm, :toimenpideinstanssi, :sopimus, :vuosi, :kuukausi);

-- name: merkitse-kustannussuunnitelmat-likaisiksi!
-- Merkitsee kokonaishintaisia töitä vastaavat kustannussuunnitelmat likaisiksi: lähtetetään seuraavassa päivittäisessä lähetyksessä
UPDATE kustannussuunnitelma
SET likainen = TRUE
WHERE maksuera IN (SELECT m.numero
                   FROM maksuera m
                     JOIN toimenpideinstanssi tpi ON tpi.id = m.toimenpideinstanssi
                   WHERE m.tyyppi = 'kokonaishintainen' AND tpi.id IN (:toimenpideinstanssit));
