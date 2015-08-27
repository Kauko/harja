-- Hakee ja laskee tietoja kannasta laskutusyhteenvetoa varten

CREATE OR REPLACE FUNCTION laskutusyhteenveto(
  hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
  ur         INTEGER)
  RETURNS TABLE(nimi VARCHAR, kht_laskutettu NUMERIC, kht_laskutettu_ind_kor NUMERIC, kht_laskutetaan_aikavalilla NUMERIC) AS $$
DECLARE
  kerroin NUMERIC;
BEGIN
  -- Kerroin on ko. indeksin arvo ko. kuukautena ja vuonna
  RETURN QUERY SELECT
                 tpk2.nimi,
                 (SELECT SUM(summa)
                  FROM kokonaishintainen_tyo
                  WHERE toimenpideinstanssi = tpi.id
                        AND maksupvm >= hk_alkupvm
                        AND maksupvm <= hk_loppupvm
                        AND maksupvm < aikavali_alkupvm)
                   AS kht_laskutettu_hoitokaudella_ennen_aikavalia,

                 (SELECT SUM(summa)
                  FROM kokonaishintainen_tyo
                  WHERE toimenpideinstanssi = tpi.id
                        AND maksupvm >= hk_alkupvm
                        AND maksupvm <= hk_loppupvm
                        AND maksupvm >= aikavali_alkupvm
                        AND maksupvm <= aikavali_loppupvm)
                   AS kht_laskutetaan_aikavalilla

               FROM toimenpideinstanssi tpi
                 JOIN toimenpidekoodi tpk3 ON tpk3.id = tpi.toimenpide
                 JOIN toimenpidekoodi tpk2 ON tpk3.emo = tpk2.id
               WHERE tpi.urakka = ur;
END;
$$ LANGUAGE plpgsql;


DROP FUNCTION laskutusyhteenveto(hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
ur INTEGER );

SELECT *
FROM laskutusyhteenveto('2014-10-01', '2015-09-30', '2015-07-01', '2015-07-31', 4);


DROP  TYPE laskyht_rivi;
DROP FUNCTION laskyht(
hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
ur         INTEGER);


CREATE TYPE laskyht_rivi AS (nimi VARCHAR, kht_laskutettu NUMERIC, kht_laskutettu_ind_kor NUMERIC,  kht_laskutetaan NUMERIC);

CREATE OR REPLACE FUNCTION laskyht(
  hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
  ur         INTEGER)
  RETURNS SETOF laskyht_rivi AS $$
DECLARE
  kerroin NUMERIC;
  t RECORD;
  kht_laskutettu NUMERIC;
  kht_laskutettu_ind_kor NUMERIC;
  kht_laskutetaan NUMERIC;
  khti RECORD;
BEGIN
  -- Kerroin on ko. indeksin arvo ko. kuukautena ja vuonna
  FOR t IN SELECT tpk2.nimi as nimi, tpi.id as tpi
               FROM toimenpideinstanssi tpi
                 JOIN toimenpidekoodi tpk3 ON tpk3.id = tpi.toimenpide
                 JOIN toimenpidekoodi tpk2 ON tpk3.emo = tpk2.id
               WHERE tpi.urakka = ur
  LOOP
    kht_laskutettu := 0.0;
    kht_laskutettu_ind_kor := 0.0;


    FOR khti IN SELECT kuukauden_indeksikorotus(to_date(kht.vuosi||'/'||kht.kuukausi||'/1', 'YYYY/MM/DD'), 'MAKU 2010', kht.summa) AS ind,
      kht.summa as kht_summa
                FROM kokonaishintainen_tyo kht
                WHERE toimenpideinstanssi = t.tpi
                      AND maksupvm >= hk_alkupvm
                      AND maksupvm <= hk_loppupvm
                      AND maksupvm < aikavali_alkupvm LOOP
      kht_laskutettu := kht_laskutettu + khti.kht_summa;
      kht_laskutettu_ind_kor := kht_laskutettu_ind_kor + khti.ind;
    END LOOP;

    SELECT SUM(summa) INTO kht_laskutetaan
      FROM kokonaishintainen_tyo
     WHERE toimenpideinstanssi = t.tpi
       AND maksupvm >= hk_alkupvm
       AND maksupvm <= hk_loppupvm
       AND maksupvm >= aikavali_alkupvm
       AND maksupvm <= aikavali_loppupvm;

    RETURN NEXT (t.nimi, kht_laskutettu, kht_laskutettu_ind_kor, kht_laskutetaan);
  END LOOP;

END;
$$ LANGUAGE plpgsql;


SELECT *
FROM laskyht('2014-10-01', '2015-09-30', '2015-07-01', '2015-07-31', 4);


SELECT kuukauden_indeksikorotus('2015-07-01', 'MAKU 2010', 123);

DROP FUNCTION laskyht(hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
ur INTEGER );


DATE(kht.vuosi, kht.kuukausi, 1)
;

SELECT to_date('2014/01/31', 'YYYY/MM/DD');
SELECT TO_DATE(2011 || '/' || 5 || '/' || 1, 'YYYY/MM/DD');

-- name: hae-laskutusyhteenvedon-tiedot
-- Hakee laskutusyhteenvetoon tarvittavat tiedot

SELECT
  tpk2.nimi,
  (SELECT SUM(summa)
   FROM kokonaishintainen_tyo
   WHERE toimenpideinstanssi = tpi.id
         AND maksupvm >= :hk_alkupvm
         AND maksupvm <= :hk_loppupvm
         AND maksupvm < :aikavali_alkupvm)
    AS kht_laskutettu_hoitokaudella_ennen_aikavalia,
  (SELECT SUM(summa)
   FROM kokonaishintainen_tyo
   WHERE toimenpideinstanssi = tpi.id
         AND maksupvm >= :hk_alkupvm
         AND maksupvm <= :hk_loppupvm
         AND maksupvm >= :aikavali_alkupvm
         AND maksupvm <= :aikavali_loppupvm)
    AS kht_laskutetaan_aikavalilla,

  (SELECT SUM(tt.maara * yht.yksikkohinta)
   FROM toteuma_tehtava tt
     JOIN toteuma t ON tt.toteuma = t.id
     JOIN yksikkohintainen_tyo yht ON (tt.toimenpidekoodi = yht.tehtava
                                       AND yht.alkupvm <= t.alkanut AND yht.loppupvm >= t.paattynyt
                                       AND tpk3.id = (SELECT emo
                                                      FROM toimenpidekoodi tt_tpk
                                                      WHERE tt_tpk.id = tt.toimenpidekoodi))
   WHERE yht.urakka = :urakka
         AND t.urakka = :urakka
         AND t.alkanut >= :hk_alkupvm AND t.alkanut <= :hk_loppupvm
         AND t.alkanut <= :aikavali_alkupvm AND t.paattynyt <= :aikavali_alkupvm)
    AS yht_laskutettu_hoitokaudella_ennen_aikavalia,

  (SELECT SUM(tt.maara * yht.yksikkohinta)
   FROM toteuma_tehtava tt
     JOIN toteuma t ON tt.toteuma = t.id
     JOIN yksikkohintainen_tyo yht ON (tt.toimenpidekoodi = yht.tehtava
                                       AND yht.alkupvm <= t.alkanut AND yht.loppupvm >= t.paattynyt
                                       AND tpk3.id = (SELECT emo
                                                      FROM toimenpidekoodi tt_tpk
                                                      WHERE tt_tpk.id = tt.toimenpidekoodi))
   WHERE yht.urakka = :urakka
         AND t.urakka = :urakka
         AND t.alkanut >= :hk_alkupvm AND t.alkanut <= :hk_loppupvm
         AND t.alkanut >= :aikavali_alkupvm AND t.alkanut <= :aikavali_loppupvm
         AND t.paattynyt >= :aikavali_alkupvm AND t.paattynyt <= :aikavali_loppupvm)
    AS yht_laskutetaan_aikavalilla

FROM toimenpideinstanssi tpi
  JOIN toimenpidekoodi tpk3 ON tpk3.id = tpi.toimenpide
  JOIN toimenpidekoodi tpk2 ON tpk3.emo = tpk2.id
WHERE tpi.urakka = :urakka;