(ns harja.palvelin.palvelut.yksikkohintaiset-tyot
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [taoensso.timbre :as log]
            [clojure.set :refer [intersection difference]]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [harja.kyselyt.yksikkohintaiset-tyot :as q]
            [harja.domain.oikeudet :as oikeudet]))

(declare hae-urakan-yksikkohintaiset-tyot tallenna-urakan-yksikkohintaiset-tyot)

(defrecord Yksikkohintaiset-tyot []
  component/Lifecycle
  (start [this]
   (doto (:http-palvelin this)
     (julkaise-palvelu
       :yksikkohintaiset-tyot (fn [user urakka-id]
         (hae-urakan-yksikkohintaiset-tyot (:db this) user urakka-id)))
     (julkaise-palvelu
       :tallenna-urakan-yksikkohintaiset-tyot (fn [user tiedot]
         (tallenna-urakan-yksikkohintaiset-tyot (:db this) user tiedot))))
   this)

  (stop [this]
    (poista-palvelu (:http-palvelin this) :yksikkohintaiset-tyot)
    (poista-palvelu (:http-palvelin this) :tallenna-urakan-yksikkohintaiset-tyot)
    this))


(defn hae-urakan-yksikkohintaiset-tyot
  "Palvelu, joka palauttaa urakan yksikkohintaiset työt."
  [db user urakka-id]
  (oikeudet/lue oikeudet/urakat-suunnittelu-yksikkohintaisettyot user urakka-id)
  (into []
        (map #(assoc %
                     :maara (if (:maara %) (double (:maara %)))
                     :yksikkohinta (if (:yksikkohinta %) (double (:yksikkohinta %)))))
        (q/listaa-urakan-yksikkohintaiset-tyot db urakka-id)))

(defn tallenna-urakan-yksikkohintaiset-tyot
  "Palvelu joka tallentaa urakan yksikkohintaiset tyot."
  [db user {:keys [urakka-id sopimusnumero tyot]}]
  (oikeudet/kirjoita oikeudet/urakat-suunnittelu-yksikkohintaisettyot user urakka-id)
  (assert (vector? tyot) "tyot tulee olla vektori")
  (jdbc/with-db-transaction [c db]
        (let [nykyiset-arvot (hae-urakan-yksikkohintaiset-tyot c user urakka-id)
              valitut-pvmt (into #{} (map (juxt :alkupvm :loppupvm) tyot))
              tyo-avain (fn [rivi]
                          [(:alkupvm rivi) (:loppupvm rivi) (:tehtava rivi)])
              tyot-kannassa (into #{} (map tyo-avain
                                           (filter #(and
                                                      (= (:sopimus %) sopimusnumero)
                                                      (valitut-pvmt [(:alkupvm %) (:loppupvm %)]))
                                                   nykyiset-arvot)))
              uniikit-tehtavat (into #{} (map #(:tehtava %) tyot)) ]
          (doseq [tyo tyot]
            (log/debug "TALLENNA TYÖ: " (pr-str tyo))
            (if (not (tyot-kannassa (tyo-avain tyo)))
              ;; insert
              (do
                (log/debug "--> LISÄTÄÄN UUSI!")
                (q/lisaa-urakan-yksikkohintainen-tyo<!
                 c (:maara tyo) (:yksikko tyo) (:yksikkohinta tyo)
                 urakka-id sopimusnumero (:tehtava tyo)
                 (java.sql.Date. (.getTime (:alkupvm tyo)))
                 (java.sql.Date. (.getTime (:loppupvm tyo)))))
              ;;update
              (do (log/debug " --> päivitetään vanha")
                  (q/paivita-urakan-yksikkohintainen-tyo!
                   c (:maara tyo) (:yksikko tyo) (:yksikkohinta tyo)
                   urakka-id sopimusnumero (:tehtava tyo)
                   (java.sql.Date. (.getTime (:alkupvm tyo)))
                   (java.sql.Date. (.getTime (:loppupvm tyo)))))))
          (log/debug "Merkitään kustannussuunnitelmat likaiseksi tehtäville: " uniikit-tehtavat)
          (q/merkitse-kustannussuunnitelmat-likaisiksi! c urakka-id uniikit-tehtavat))
      (hae-urakan-yksikkohintaiset-tyot c user urakka-id)))
