-- Päällystyskohteet

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L03', 'Leppäjärven ramppi', 400, true, 100, 4543.95, 0);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 308, 'Mt 2855 Viisari - Renko', 9000, false, 200, 565, 100);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L010', 'Tie 357', 500, true, 3457, 5, 6);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 310, 'Oulaisten ohitusramppi', 500, false, 3457, 5, 6);
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 1', 19521, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((426888 7212758,427081 7212739),(434777 7215499,436899 7217174,438212 7219910,438676 7220554,440102 7221432,441584 7222729,442255 7223162,443128 7223398,443750 7223713,448682 7225293,451886 7226708,456379 7228018,459945 7229222,461039 7229509))'));

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1501', 'Vt13 Hartikkala - Pelkola');
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 0, 239, 222, 4828, 2, 'PAB-B 16/80 MPKJ', 'MULTILINESTRING((569679.576280243 6770940.38019019,569685.927911525 6770936.54598464,569694.917866912 6770931.11985125,569702.825096966 6770926.21423598,569717.059540404 6770917.1194623,569768.068529403 6770886.26298144,569772.905655448 6770883.31437137,569800.582449535 6770866.39038579,569825.147450518 6770851.51214363,569826.659573521 6770850.53304547,569830.600979599 6770848.01502418,569849.985812847 6770836.16460137,569852.934422925 6770834.4297395,569853.208975388 6770834.26715203,569869.210775049 6770824.97723093),(570700.430827977 6770091.60690774,570705.758098659 6770086.85732879,570844.195073798 6769973.54994409,570872.894391503 6769951.86928975))');
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 222, 239, 820, 4828, 2, 'PAB-B 16/80 MPKJ', 'MULTILINESTRING((569869.210775049 6770824.97723093,569872.124508548 6770823.28564906,569918.85619648 6770796.53077951,569962.043715846 6770770.06058692,569985.755877074 6770755.92441063,570007.509246432 6770743.31881966,570092.85754233 6770692.64036563,570152.581934252 6770655.3316041,570259.021934027 6770569.17811313,570337.970358406 6770487.07441987,570348.766508366 6770474.12501744),(570872.894391503 6769951.86928975,570913.557984301 6769921.15032602,571038.051744102 6769821.61642248,571042.671491299 6769817.89894639,571157.696466689 6769725.89230659,571340.874536167 6769579.60903354))');
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 820, 239, 870, 4828, 2, 'PAB-B 16/80 MPKJ', 'MULTILINESTRING((570348.766508366 6770474.12501744,570380.784445928 6770435.72121979),(571340.874536167 6769579.60903354,571371.85691699 6769554.86696651,571373.690046018 6769553.31672776,571379.770800303 6769548.19579434))');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1502', 'Vt 13 Kähärilä - Liikka');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1503', 'Mt 387 Mattila - Hanhi-Kemppi');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1504', 'Mt 408 Pallo - Kivisalmi');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1505', 'Kt 62 Sotkulampi - Rajapatsas');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1506', 'Kt 62 Haloniemi - Syyspohja');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1507', 'Mt 387 Raippo - Koskenkylä');

-- Päällystysilmoitukset

INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Leppäjärven ramppi'), 'aloitettu'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Tie 357'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot, paatos_tekninen_osa, paatos_taloudellinen_osa, perustelu_tekninen_osa, perustelu_taloudellinen_osa, kasittelyaika_tekninen_osa, kasittelyaika_taloudellinen_osa) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}', 'hylatty'::paallystysilmoituksen_paatostyyppi, 'hylatty'::paallystysilmoituksen_paatostyyppi, 'Ei tässä ole mitään järkeä', 'Ei tässä ole mitään järkeä', '2005-12-20 00:00:00+02', '2005-12-20 00:00:00+02');
