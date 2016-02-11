(ns harja.kyselyt.paivystajatekstiviestit
  (:require [yesql.core :refer [defqueries]]))

(defqueries "harja/kyselyt/paivystajatekstiviestit.sql")

(defn kirjaa-uusi-viesti [db yhteyshenkilo-id ilmoitus-id]
  (:viestinumero (harja.kyselyt.paivystajatekstiviestit/kirjaa-uusi-paivystajatekstiviesti<! db yhteyshenkilo-id ilmoitus-id)))

(defn hae-ilmoitus [db yhteyshenkilo-id viestinumero]
  (:ilmoitus (first (harja.kyselyt.paivystajatekstiviestit/hae-ilmoitus-id db yhteyshenkilo-id viestinumero))))