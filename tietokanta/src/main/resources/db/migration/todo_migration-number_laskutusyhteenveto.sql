-- Hakee ja laskee tietoja kannasta laskutusyhteenvetoa varten
DROP FUNCTION laskutusyhteenveto(hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
ur INTEGER );
DROP TYPE laskutusyhteenveto_rivi;


CREATE TYPE laskutusyhteenveto_rivi AS (nimi           VARCHAR,
                                        kht_laskutettu NUMERIC, kht_laskutettu_ind_kor NUMERIC, kht_laskutetaan NUMERIC, kht_laskutetaan_ind_kor NUMERIC,
                                        yht_laskutettu NUMERIC, yht_laskutettu_ind_kor NUMERIC, yht_laskutetaan NUMERIC, yht_laskutetaan_ind_kor NUMERIC);

CREATE OR REPLACE FUNCTION laskutusyhteenveto(
  hk_alkupvm DATE, hk_loppupvm DATE, aikavali_alkupvm DATE, aikavali_loppupvm DATE,
  ur         INTEGER)
  RETURNS SETOF laskutusyhteenveto_rivi AS $$
DECLARE
  kerroin                 NUMERIC;
  t                       RECORD;
  kht_laskutettu          NUMERIC;
  kht_laskutettu_ind_kor  NUMERIC;
  kht_laskutetaan         NUMERIC;
  kht_laskutetaan_ind_kor NUMERIC;
  khti                    RECORD;
  aikavalin_kht           RECORD;

  yht_laskutettu          NUMERIC;
  yht_laskutettu_ind_kor  NUMERIC;
  yht_laskutetaan         NUMERIC;
  yht_laskutetaan_ind_kor NUMERIC;
  yhti                    RECORD;

BEGIN
  -- Kerroin on ko. indeksin arvo ko. kuukautena ja vuonna
  FOR t IN SELECT
             tpk2.nimi AS nimi,
             tpi.id    AS tpi,
             tpk3.id   AS tpk3_id
           FROM toimenpideinstanssi tpi
             JOIN toimenpidekoodi tpk3 ON tpk3.id = tpi.toimenpide
             JOIN toimenpidekoodi tpk2 ON tpk3.emo = tpk2.id
           WHERE tpi.urakka = ur
  LOOP
    kht_laskutettu := 0.0;
    kht_laskutettu_ind_kor := 0.0;

    yht_laskutettu := 0.0;
    yht_laskutettu_ind_kor := 0.0;


    FOR khti IN SELECT
                  kuukauden_indeksikorotus(to_date(kht.vuosi || '/' || kht.kuukausi || '/1', 'YYYY/MM/DD'), 'MAKU 2010',
                                           kht.summa) AS ind,
                  kht.summa                           AS kht_summa
                FROM kokonaishintainen_tyo kht
                WHERE toimenpideinstanssi = t.tpi
                      AND maksupvm >= hk_alkupvm
                      AND maksupvm <= hk_loppupvm
                      AND maksupvm < aikavali_alkupvm LOOP
      kht_laskutettu := kht_laskutettu + khti.kht_summa;
      kht_laskutettu_ind_kor := kht_laskutettu_ind_kor + khti.ind;
    END LOOP;

    SELECT
      summa,
      vuosi,
      kuukausi
    INTO aikavalin_kht
    FROM kokonaishintainen_tyo
    WHERE toimenpideinstanssi = t.tpi
          AND maksupvm >= hk_alkupvm
          AND maksupvm <= hk_loppupvm
          AND maksupvm >= aikavali_alkupvm
          AND maksupvm <= aikavali_loppupvm;

    SELECT kuukauden_indeksikorotus(to_date(aikavalin_kht.vuosi || '/' || aikavalin_kht.kuukausi || '/1', 'YYYY/MM/DD'),
                                    'MAKU 2010', aikavalin_kht.summa)
    INTO kht_laskutetaan_ind_kor
    FROM kokonaishintainen_tyo
    WHERE toimenpideinstanssi = t.tpi
          AND maksupvm >= hk_alkupvm
          AND maksupvm <= hk_loppupvm
          AND maksupvm >= aikavali_alkupvm
          AND maksupvm <= aikavali_loppupvm;

    FOR yhti IN SELECT
                  SUM(tt.maara * yht.yksikkohinta) AS yht_summa,
                  tot.alkanut                      AS tot_alkanut
                FROM toteuma_tehtava tt
                  JOIN toteuma tot ON tt.toteuma = tot.id
                  JOIN toimenpidekoodi tpk4 ON tt.toimenpidekoodi = tpk4.id
                  JOIN toimenpidekoodi tpk3 ON tpk4.emo = tpk3.id
                  JOIN yksikkohintainen_tyo yht ON (tt.toimenpidekoodi = yht.tehtava
                                                    AND yht.alkupvm <= tot.alkanut AND yht.loppupvm >= tot.paattynyt
                                                    AND tpk3.id = t.tpk3_id)
                WHERE yht.urakka = ur
                      AND tot.urakka = ur
                      AND tot.alkanut >= hk_alkupvm AND tot.alkanut <= hk_loppupvm
                      AND tot.alkanut <= aikavali_alkupvm AND tot.paattynyt <= aikavali_alkupvm
                GROUP BY tot.alkanut
    LOOP
      yht_laskutettu := yht_laskutettu + yhti.yht_summa;
      yht_laskutettu_ind_kor := yht_laskutettu_ind_kor +
                                kuukauden_indeksikorotus(yhti.tot_alkanut :: DATE, 'MAKU 2010', yhti.yht_summa);


    END LOOP;

    RETURN NEXT (t.nimi, kht_laskutettu, kht_laskutettu_ind_kor, aikavalin_kht.summa, kht_laskutetaan_ind_kor,
                 yht_laskutettu, yht_laskutettu_ind_kor, 1.0, 1.0);
  END LOOP;

END;
$$ LANGUAGE plpgsql;


-- ^ yllä tuotantokoodi ^
-- Alla helppereitä

SELECT *
FROM laskutusyhteenveto('2014-10-01', '2015-09-30', '2015-07-01', '2015-07-31', 4);


x


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