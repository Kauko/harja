CREATE UNIQUE INDEX uniikki_hallintayksikon_nimi on organisaatio (nimi) WHERE tyyppi='hallintayksikko'::organisaatiotyyppi;