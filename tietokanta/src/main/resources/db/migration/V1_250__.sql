-- Kuvaus: tierekisteriosoitteen haku yhdellä pisteellä, korjaus

DROP MATERIALIZED VIEW tieverkko_paloina;

CREATE MATERIALIZED VIEW tieverkko_paloina AS SELECT osoite3,tie,ajorata,osa,tiepiiri,(ST_Dump(geom)).geom AS geom, tr_pituus FROM (SELECT osoite3, tie, ajorata, osa, tiepiiri, ST_LineMerge(ST_SnapToGrid(geometria,0.0001)) AS geom, tr_pituus FROM tieverkko) AS f;

CREATE INDEX tieverkko_paloina_geom_index ON tieverkko_paloina USING GIST (geom);
CREATE INDEX tieverkko_paloina_tieosa_index ON tieverkko_paloina (tie,osa);

CREATE OR REPLACE FUNCTION tierekisteriosoite_pisteelle(
  piste geometry, treshold INTEGER)
  RETURNS tr_osoite
AS $$
DECLARE
   alkuosa RECORD;
   alkuet NUMERIC;
   palojenpit NUMERIC;
BEGIN
   SELECT osoite3, tie, ajorata, osa, tiepiiri, geom
      FROM tieverkko_paloina
      WHERE ST_DWithin(geom, piste, treshold)
      ORDER BY ST_Length(ST_ShortestLine(geom, piste)) ASC
      LIMIT 1
   INTO alkuosa;

   IF alkuosa IS NULL THEN
     RAISE EXCEPTION 'pisteelle ei löydy tietä';
   END IF;
   
   SELECT ST_Length(ST_Line_Substring(alkuosa.geom, 0, ST_Line_Locate_Point(alkuosa.geom, piste))) INTO alkuet;
   
   RETURN ROW(alkuosa.tie, alkuosa.osa, alkuet::INTEGER, 0, 0, NULL::geometry);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_osan_etaisyys(
  piste geometry, tienro INTEGER, treshold INTEGER)
  RETURNS INTEGER
AS $$
DECLARE
   alkuosa RECORD;
   alkuet NUMERIC;
BEGIN
   SELECT osoite3, tie, ajorata, osa, tiepiiri, geom
      FROM tieverkko_paloina
      WHERE ST_DWithin(geom, piste, treshold)
        AND tie=tienro
      ORDER BY ST_Length(ST_ShortestLine(geom, piste)) ASC
      LIMIT 1
   INTO alkuosa;
   
   SELECT ST_Length(ST_Line_Substring(alkuosa.geom, 0, ST_Line_Locate_Point(alkuosa.geom, piste))) INTO alkuet;
   
   RETURN alkuet::INTEGER;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tierekisteriosoite_pisteille(
  alkupiste geometry, loppupiste geometry, treshold INTEGER)
  RETURNS tr_osoite
AS $$
DECLARE
  reitti geometry;
  apiste geometry;
  bpiste geometry;
  aosa INTEGER;
  bosa INTEGER;
  tienosavali RECORD;
  ap NUMERIC;
  bp NUMERIC;
  alkuet INTEGER;
  loppuet INTEGER;
  tmp geometry;
BEGIN
   -- valitaan se tie ja tienosaväli jota lähellä alku- ja loppupisteet ovat yhdessä lähimpänä
  SELECT a.tie, 
         a.osa AS aosa, 
         b.osa AS bosa,
         a.ajorata AS ajorataa,
         b.ajorata AS ajoratab
    FROM tieverkko_paloina a, 
         tieverkko_paloina b 
   WHERE ST_DWithin(a.geom, alkupiste, treshold) 
     AND ST_DWithin(b.geom, loppupiste, treshold) 
     AND a.tie=b.tie
ORDER BY ST_Length(ST_ShortestLine(alkupiste, a.geom)) +
          ST_Length(ST_ShortestLine(loppupiste, b.geom))
   LIMIT 1
    INTO tienosavali;
  
  -- sortataan alku- ja loppupiste ja tienosavälit siten että alkuosa on ensimmäisenä osoitteessa
  IF tienosavali.aosa > tienosavali.bosa THEN
    aosa := tienosavali.bosa;
    bosa := tienosavali.aosa;
    apiste := loppupiste;
    bpiste := alkupiste;
  ELSE
    aosa := tienosavali.aosa;
    bosa := tienosavali.bosa;
    apiste := alkupiste;
    bpiste := loppupiste;
  END IF;

  IF aosa=bosa THEN
    SELECT ST_Line_Substring(geom, LEAST(ST_Line_Locate_Point(geom, apiste), ST_Line_Locate_Point(geom, bpiste)),
				    GREATEST(ST_Line_Locate_Point(geom, apiste),ST_Line_Locate_Point(geom, bpiste)))
      FROM tieverkko_paloina tv
     WHERE tv.tie = tienosavali.tie
       AND tv.osa = aosa
       AND tv.ajorata = tienosavali.ajorataa
    INTO reitti;
  ELSE     
  -- kootaan osien geometriat yhdeksi viivaksi
  SELECT ST_LineMerge(ST_Union((CASE 
				 WHEN tv.osa=aosa 
				    THEN ST_Line_Substring(tv.geom, ST_Line_Locate_Point(tv.geom, apiste), 1)
				 WHEN tv.osa=bosa 
				    THEN ST_Line_Substring(tv.geom, 0, ST_Line_Locate_Point(tv.geom, bpiste))
				 ELSE tv.geom 
				 END)
				ORDER BY tv.osa)) 
    FROM tieverkko_paloina tv
   WHERE tv.tie=tienosavali.tie
     AND tv.osa>=aosa
     AND tv.osa<=bosa
    INTO reitti;
  END IF;
  
  IF reitti IS NULL THEN
     RAISE EXCEPTION 'pisteillä ei yhteistä tietä';
  END IF;

  alkuet := tr_osan_etaisyys(alkupiste, tienosavali.tie, treshold);
  loppuet := tr_osan_etaisyys(loppupiste, tienosavali.tie, treshold);
  
  RETURN ROW(tienosavali.tie, 
             aosa, 
             alkuet, 
             bosa, 
             loppuet,
             reitti);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tierekisteriosoitteelle_viiva(
  tie_ INTEGER, aosa_ INTEGER, aet_ INTEGER, losa_ INTEGER, let_ INTEGER)
  RETURNS geometry
AS $$
DECLARE
   rval geometry;
BEGIN
   IF aosa_ = losa_ THEN
	SELECT ST_Line_Substring(geom, aet_/tr_pituus::FLOAT, let_/tr_pituus::FLOAT)
	FROM tieverkko_paloina
	WHERE tie=tie_
	  AND osa=aosa_
	INTO rval;
   ELSE
	SELECT ST_LineMerge(ST_Union((CASE WHEN osa=aosa_ THEN ST_Line_Substring(geom, LEAST(1, aet_/ST_Length(geom)), 1)
				      WHEN osa=losa_ THEN ST_Line_Substring(geom, 0, LEAST(1,let_/ST_Length(geom)))
				      ELSE geom END) ORDER BY osa)) FROM tieverkko_paloina
	WHERE tie = tie_
	AND osa >= aosa_
	AND osa <= losa_
	INTO rval;
   END IF;

   IF rval IS NULL THEN
     RAISE EXCEPTION 'Virheellinen tierekisteriosoite';
   END IF;

   RETURN rval;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tierekisteriosoitteelle_piste(
  tie_ INTEGER, aosa_ INTEGER, aet_ INTEGER)
  RETURNS geometry
AS $$
DECLARE
   result geometry;
   suhde float;
BEGIN
   SELECT ST_Line_Interpolate_Point(geom, LEAST(1, (aet_::float)/tr_pituus))
    FROM tieverkko_paloina WHERE tie=tie_ AND osa=aosa_ LIMIT 1 INTO result;

    RETURN result;
END;
$$ LANGUAGE plpgsql;
