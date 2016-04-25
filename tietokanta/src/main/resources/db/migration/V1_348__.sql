-- Lisää urakan YHA-tiedoille uusi taulu

CREATE TABLE yhatiedot (
  id                       SERIAL PRIMARY KEY,
  urakka                   INTEGER REFERENCES urakka (id),
  yhatunnus                VARCHAR(255),
  yhaid                    INTEGER,
  yhanimi                  VARCHAR(2048),
  elyt                     VARCHAR(2048) [],
  vuodet                   INTEGER [],
  kohdeluettelo_paivitetty TIMESTAMP,
  luotu                    TIMESTAMP,
  muokattu                 TIMESTAMP
);

ALTER TABLE yhatiedot ADD CONSTRAINT uniikki_yhatunnus UNIQUE (yhatunnus);
ALTER TABLE yhatiedot  ADD CONSTRAINT uniikki_yhaid UNIQUE (yhaid);

-- Uudelleennimeä päällystystaulut

ALTER TABLE paallystyskohde RENAME TO yllapitokohde;
ALTER TABLE paallystyskohdeosa RENAME TO yllapitokohdeosa;

CREATE TYPE yllapitokohdetyyppi AS ENUM ('paallystys', 'paikkaus');

ALTER TABLE yllapitokohde ADD COLUMN tyyppi yllapitokohdetyyppi;

UPDATE yllapitokohde SET tyyppi = (SELECT tyyppi FROM urakka WHERE id = urakka)::TEXT::yllapitokohdetyyppi;