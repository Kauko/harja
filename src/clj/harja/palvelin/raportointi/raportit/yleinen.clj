(ns harja.palvelin.raportointi.raportit.yleinen
  (:require [yesql.core :refer [defqueries]]
            [taoensso.timbre :as log]
            [harja.pvm :as pvm]
            [clj-time.local :as l]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]))

(defn raportin-otsikko
  [konteksti nimi alkupvm loppupvm]
  (let [kk-vali? (pvm/kyseessa-kk-vali? alkupvm loppupvm)
        kkna-ja-vuonna (pvm/kuukautena-ja-vuonna (l/to-local-date-time alkupvm))]
    (if kk-vali?
      (str konteksti ", " nimi " " kkna-ja-vuonna)
      (str konteksti ", " nimi " ajalta "
           (pvm/pvm alkupvm) " - " (pvm/pvm loppupvm)))))


(def vuosi-ja-kk-fmt (tf/formatter "YYYY/MM"))
(defn vuosi-ja-kk [pvm]
  (tf/unparse vuosi-ja-kk-fmt (tc/from-date pvm)))

(defn kuukaudet [alku loppu]
  (let [alku (tc/from-date alku)
        loppu (tc/from-date loppu)]
    (letfn [(kuukaudet [kk]
              (when (or (t/before? kk loppu)
                        (t/equal? kk loppu))
                (lazy-seq
                  (cons (tf/unparse vuosi-ja-kk-fmt kk)
                        (kuukaudet (t/plus kk (t/months 1)))))))]
      (kuukaudet alku))))

(defn pylvaat
  [otsikko alkupvm loppupvm kuukausittainen-data]
  [:pylvaat {:otsikko (str otsikko " " (pvm/pvm alkupvm) "-" (pvm/pvm loppupvm))}
   (into []
         (map (juxt identity #(or (kuukausittainen-data %) 0)))
         (kuukaudet alkupvm loppupvm))])

(defn ei-osumia-aikavalilla-teksti
  [nimi alkupvm loppupvm]
  [:otsikko-kuin-pylvaissa (str "Ei " nimi " aikana " (pvm/pvm alkupvm) "-" (pvm/pvm loppupvm))])