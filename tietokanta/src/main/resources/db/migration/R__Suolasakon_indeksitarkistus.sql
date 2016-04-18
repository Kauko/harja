-- Suolasakon indeksitarkistuksen laskenta. Ottaa huomioon peruslukuun vertaamisen
-- From: Leppänen Anne <Anne.Leppanen@liikennevirasto.fi>
-- Date: Tuesday 17 November 2015 at 17:25
-- Perusluku, johon KAIKKIA indeksitarkistuksia verrataan on urakan kilpailuttamisvuotta
-- edeltävän vuoden joulukuun ja kilpailuttamisvuoden tammi- ja helmikuiden keskiarvo.
-- Esim.2016 vuoden urakoiden perusluku on joulukuun 2015 ja 2016 tammi-helmikuiden
-- keskiarvo. Perusluku on koko urakan ajan pysyvä.

-- esim. indeksi pisteluvut: loka, marras, joulu, tammi, helmi, maalis
--                             98,    102,   110,   115,   104,    101
--                keskiarvo: 105
-- esim sakkosumma: 1 234 € * (105/perus) = 1295,70 €
-- palautustyyppi indeksitarkistettu_suolasakko_rivi AS (summa NUMERIC, korotettuna NUMERIC, korotus NUMERIC);

DROP FUNCTION IF EXISTS laske_suolasakon_indeksitarkistus(integer, varchar, numeric);

CREATE OR REPLACE FUNCTION laske_suolasakon_indeksitarkistus(talvikauden_alkuvuosi integer, indeksinimi varchar, summa NUMERIC, ur INTEGER)
  RETURNS indeksitarkistettu_suolasakko_rivi AS $$
DECLARE
  kerroin numeric;
  alkuv integer;
  loppuv integer;
  perusluku numeric;
BEGIN
  alkuv := talvikauden_alkuvuosi;
  loppuv := talvikauden_alkuvuosi + 1;
  perusluku := hoitourakan_indeksilaskennan_perusluku(ur);

  IF perusluku IS NULL
  THEN
    RAISE NOTICE 'Suolasakon indeksitarkistusta ei voitu laskea koska urakan id=% peruslukua ei voitu laskea.', ur;
    RETURN (summa, NULL :: NUMERIC, NULL :: NUMERIC);
  END IF;

  -- Kerroin on talvikauden alkuvuoden loka,marras,joulu kuukausien sekä
  -- seuraavan vuoden tammi,helmi,maalis kuukausien prosenttiarvojen
  -- keskiarvo kertoimena.
  SELECT
    INTO kerroin
    AVG(arvo)/perusluku
  FROM indeksi
  WHERE nimi = indeksinimi
        AND ((vuosi = alkuv  AND kuukausi = 10) OR
             (vuosi = alkuv  AND kuukausi = 11) OR
             (vuosi = alkuv  AND kuukausi = 12) OR
             (vuosi = loppuv AND kuukausi = 1) OR
             (vuosi = loppuv AND kuukausi = 2) OR
             (vuosi = loppuv AND kuukausi = 3));
  -- Jos yhtään indeksilukuja ei ole, kerroin on NULL, jolloin myös
  -- tämä lasku palauttaa NULL.
  RETURN (summa, summa * kerroin, summa * kerroin - summa);
END;
$$ LANGUAGE plpgsql;
