-- Pistokoe

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja, tr_alkuetaisyys) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM sopimus WHERE nimi = 'Oulun alueurakka pääsopimus' AND urakka = 1), '2005-10-01 10:00.00', 1 ,2, 3, 4, point(430768.8350704433, 7203153.238678749)::GEOMETRY, 'Ismo', 'pistokoe'::tarkastustyyppi, 'jotain havaintoja siellä oli', NOW(), 1, 3);
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja, tr_alkuetaisyys) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM sopimus WHERE nimi = 'Oulun alueurakka pääsopimus' AND urakka = 1), '2005-10-03 10:00.00', 1 ,2, 3, 4, point(430080.9018158768, 7204538.659816418)::GEOMETRY, 'Matti', 'pistokoe'::tarkastustyyppi, 'havaittiin kaikenlaista', NOW(), 1, 3);

-- Talvihoito

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-12-28 10:00:02', 4 ,364, 8012, null, null, point(429000, 7202314)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'järjestelmän raportoima testitarkastus 1', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2016-01-4 06:02:20', 4 ,364, 5, null, null, point(430750.5220656799, 7198888.689460491)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'järjestelmän raportoima testitarkastus 2', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-11-23 11:00:30', 4 ,364, 8012, null, null, point(430999.34049970115, 7202184.240103625)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'järjestelmän raportoima testitarkastus 3', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-10-23 10:00:02', 4 ,364, 8012, null, null, point(430999.3404997012, 7201565.577905941)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'järjestelmän raportoima testitarkastus 4', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-10-23 10:00:02', 4 ,364, 8012, null, null, point(430999.3404998012, 7205565.577905941)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'OK', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-10-23 10:00:02', 4 ,364, 8012, null, null, point(430999.3404999012, 7207565.577905941)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'ok', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-10-23 10:00:02', 4 ,364, 8012, null, null, point(430999.3404996012, 7209565.577905941)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'Ok', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-10-23 10:00:02', 4 ,364, 8012, null, null, point(430999.3404994012, 7200505.577905941)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, '', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2016-01-02 16:02:00', 4 ,364, 8012, null, null, point(430877.5189858716, 7200994.6888509365)::GEOMETRY, 'Matti', 'talvihoito'::tarkastustyyppi, 'Urakoitsija on kirjannut tämän tarkastuksen Harjaan käsin', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));

INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'järjestelmän raportoima testitarkastus 1'), 'B', 11, 5, 0.1, -14, -6, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'järjestelmän raportoima testitarkastus 2'), 'A', 10, 6, 0.4, -13, -6, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'järjestelmän raportoima testitarkastus 3'), 'A', 9, 6, 0.3, -13, -3, 2);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'järjestelmän raportoima testitarkastus 4'), 'B', 6, 6, 0.5, -15, -5, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'OK'), 'B', 6, 6, 0.5, -15, -5, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'ok'), 'B', 6, 6, 0.5, -15, -5, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Ok'), 'B', 6, 6, 0.5, -15, -5, 1);INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'järjestelmän raportoima testitarkastus 4'), 'B', 6, 6, 0.5, -15, -5, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = ''), 'B', 6, 6, 0.5, -15, -5, 1);
INSERT INTO talvihoitomittaus (tarkastus, talvihoitoluokka, lumimaara, tasaisuus, kitka, lampotila_ilma, lampotila_tie, ajosuunta) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Urakoitsija on kirjannut tämän tarkastuksen Harjaan käsin'), 'A', 10, 5, 1, -16, -3, 1);

-- Tiestö

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-02 16:02:00', 4 ,364, 8012, null, null, point(430877.5189858716, 7200994.6888509365)::GEOMETRY, 'Tarmo Tarkastaja', 'tiesto'::tarkastustyyppi, 'Tiessä oli pieni kuoppa', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-05 16:18:00', 4 ,364, 8012, null, null, point(430999.34049970115, 7202184.240103625)::GEOMETRY, 'Tarmo Tarkastaja', 'tiesto'::tarkastustyyppi, 'Tiessä oli muutamia pieniä kuoppia, ei kuitenkaan mitään vakavaa ongelmaa aiheuta', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));

INSERT INTO liite (nimi) VALUES ('tiestotarkastus_456.jpg');
INSERT INTO liite (nimi) VALUES ('tiesto5667858.jpg');
INSERT INTO tarkastus_liite (tarkastus, liite) VALUES ((SELECT id FROM tarkastus WHERE havainnot ='Tiessä oli pieni kuoppa'), (SELECT id FROM liite WHERE nimi ='tiestotarkastus_456.jpg'));
INSERT INTO tarkastus_liite (tarkastus, liite) VALUES ((SELECT id FROM tarkastus WHERE havainnot ='Tiessä oli pieni kuoppa'), (SELECT id FROM liite WHERE nimi ='tiesto5667858.jpg'));

-- Soratie

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-06 16:18:00', 4 ,364, 8012, null, null, ST_GeomFromText('LINESTRING(443798.31784756 7229301.60995499,443815.8652 7229436.1158,443833.9475 7229570.7144)'), 'Tarmo Tarkastaja', 'soratie'::tarkastustyyppi, 'Soratietarkastus 1', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 1'), 1, 3, 2, 5, 1);
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 1'), 3, 2, null, null, null);
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 1'), 3, 4, 1, 4, 4);

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-05 16:18:00', 5 ,364, 8012, null, null, point(430999.34049970115, 7202184.240103625)::GEOMETRY, 'Tarmo Tarkastaja', 'soratie'::tarkastustyyppi, 'Soratietarkastus 2', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 2'), 2, 5, 3, 3, 1);
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 2'), 2, 5, 4, null, 1);

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-05 16:18:00', 5 ,364, 8011, null, null, point(430999.34049970115, 7202184.240103625)::GEOMETRY, 'Tarmo Tarkastaja', 'soratie'::tarkastustyyppi, 'Soratietarkastus 3', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 3'), 2, 5, 1, 4, 3);

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, tyyppi, havainnot, luotu, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS NULL), '2015-01-08 16:18:00', 5 ,364, 8011, null, null, point(430999.34049970115, 7202184.240103625)::GEOMETRY, 'Tarmo Tarkastaja', 'soratie'::tarkastustyyppi, 'Soratietarkastus 4', NOW(), (SELECT id from kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO soratiemittaus (tarkastus, hoitoluokka, tasaisuus, kiinteys, polyavyys, sivukaltevuus) VALUES ((SELECT id FROM tarkastus WHERE havainnot = 'Soratietarkastus 4'), 2, 5, 5, 1, 3);
