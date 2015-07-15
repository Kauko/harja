(ns harja.palvelin.palvelut.integraatioloki
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [harja.kyselyt.integraatioloki :as q]
            [harja.kyselyt.konversio :as konversio]
            [harja.domain.roolit :as roolit]))

(declare hae-jarjestelmien-integraatiot)
(declare hae-integraatiotapahtumat)

(defrecord Integraatioloki []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-jarjestelmien-integraatiot (fn [kayttaja _]
                                                        (hae-jarjestelmien-integraatiot (:db this) kayttaja)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-integraatiotapahtumat (fn [kayttaja {:keys [jarjestelma integraatio alkaen paattyen]}]
                                                   (hae-integraatiotapahtumat (:db this) kayttaja jarjestelma integraatio alkaen paattyen)))
    this)

  (stop [this]
    (poista-palvelu (:http-palvelin this) :hae-jarjestelmien-integraatiot)
    (poista-palvelu (:http-palvelin this) :hae-integraatiotapahtumat)
    this))


(def tapahtuma-xf
  (comp
    (map #(assoc % :onnistunut (boolean (Boolean/valueOf (:onnistunut %)))))))

(defn hae-integraatiot [db]
  (let [integraatiot (q/hae-jarjestelmien-integraatiot db)
        uniikit-integraatiot (mapv (fn [kartta]
                                     (assoc kartta :integraatiot
                                                   (mapv #(:integraatio %)
                                                         (into []
                                                               (filter #(= (:jarjestelma %) (:jarjestelma kartta))) integraatiot))))
                                   (set (map #(dissoc % :integraatio) integraatiot)))]
    (log/debug "Integraatiot:" uniikit-integraatiot)
    uniikit-integraatiot))

(defn hae-jarjestelmien-integraatiot
  "Palvelu, joka palauttaa kaikki eri järjestelmien integraatiot."
  [db kayttaja]
  (roolit/vaadi-rooli kayttaja roolit/jarjestelmavastuuhenkilo)
  (log/debug "Haetaan järjestelmien integraatiot.")
  (hae-integraatiot db))

(defn hae-integraatiotapahtumat
  "Palvelu, joka palauttaa järjestelmän integraation tapahtumat tietyltä aikaväliltä."
  [db kayttaja jarjestelma integraatio alkaen paattyen]
  (roolit/vaadi-rooli kayttaja roolit/jarjestelmavastuuhenkilo)
  (log/debug "Haetaan tapahtumat järjestelmän:" jarjestelma ", integraatiolle:" integraatio ", alkaen:" alkaen ", paattyen:" paattyen)
  (let [tapahtumat
        (into []
              tapahtuma-xf
              (q/hae-jarjestelman-integraatiotapahtumat-aikavalilla db jarjestelma integraatio (konversio/sql-date alkaen) (konversio/sql-date paattyen)))]
    (log/debug "Tapahtumat:" tapahtumat)
    (let [tapahtumat-viesteineen
          (mapv #(assoc % :viestit (q/hae-integraatiotapahtuman-viestit db (:id %))) tapahtumat)]
      (log/debug "Tapahtumat viesteineen:" tapahtumat-viesteineen)
      tapahtumat-viesteineen)))
