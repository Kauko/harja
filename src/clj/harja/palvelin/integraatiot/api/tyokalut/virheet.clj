(ns harja.palvelin.integraatiot.api.tyokalut.virheet)

(def +invalidi-json+ ::invalidi-json)
(def +viallinen-kutsu+ ::viallinen-kutsu)
(def +sisainen-kasittelyvirhe+ ::sisainen-kasittelyvirhe)

;; Virhekoodit
(def +invalidi-json-koodi+ "invalidi-json")
(def +sisainen-kasittelyvirhe-koodi+ "sisainen-kasittelyvirhe")
(def +ulkoinen-kasittelyvirhe-koodi+ "ulkoinen-kasittelyvirhe")
(def +virheellinen-liite-koodi+ "virheellinen-liite")
(def +tuntematon-urakka-koodi+ "tuntematon-urakka")
(def +tuntematon-sopimus-koodi+ "tuntematon-sopimus")

; Virhetyypit
(def +virheellinen-liite+ "virheellinen-liite")
(def +tuntematon-silta+ "tuntematon-silta")
(def +tuntematon-materiaali+ "tuntematon-materiaali")
(def +tuntematon-kayttaja-koodi+ "tuntematon-kayttaja")
(def +tyhja-vastaus+ "tyhja-vastaus")
(def +kayttajalla-puutteelliset-oikeudet+ "kayttajalla-puutteelliset-oikeudet")
(def +puutteelliset-parametrit+ "puutteelliset-parametrit")
