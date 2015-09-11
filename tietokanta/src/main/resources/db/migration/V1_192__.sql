-- Kuvaus: hoitoluokkataulu
ALTER TABLE tieverkko DROP COLUMN IF EXISTS hoitoluokka;

CREATE TABLE hoitoluokka (
   ajorata INTEGER,
   aosa INTEGER,
   tie INTEGER,
   piirinro INTEGER,
   let INTEGER,
   losa INTEGER,
   aet INTEGER,
   osa INTEGER,
   hoitoluokka INTEGER,
   geometria geometry
);

CREATE INDEX hoitoluokka_geom_index ON hoitoluokka USING GIST ( geometria ); 
