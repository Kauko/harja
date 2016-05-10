-----------------------------------------
-- Oulun alueurakka 2014-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 1', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 1000, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 1'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 2', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 666.666, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 2'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Liikenneympäristön hoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 666', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 100, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 666'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 667', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 10, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 667'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Muu tuote

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 4', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 4'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Ryhmä C

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 5', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('C'::sanktiolaji, 123, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 5'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Muistutus Talvihoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 6', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 6'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Muistutus Muu

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 7', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 7'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-----------------------------------------
-- Pudasjärven alueurakka 2007-2012
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 100', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 10000, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 100'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 200', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 6660, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 200'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Liikenneympäristön hoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 66600', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1000, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 66600'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 66700', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 100, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 66700'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Muu tuote

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 400', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 10, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 400'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Ryhmä C

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 500', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('C'::sanktiolaji, 1230, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 500'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Muistutus Talvihoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 600', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 600'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Muistutus Muu

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 700', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 700'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-----------------------------------------
-- Vantaan alueurakka 2014-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Vantaan alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 999', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 2.5, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 999'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Vantaan alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 9990', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 2, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 9990'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-----------------------------------------
-- Espoon alueurakka 2014-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Espoon alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 6767', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 1, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 6767'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Espoon alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 3424', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1.5, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 3424'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);