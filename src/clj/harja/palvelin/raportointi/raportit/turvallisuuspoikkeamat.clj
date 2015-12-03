(ns harja.palvelin.raportointi.raportit.turvallisuuspoikkeamat
  (:require [clojure.string :as str]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :as log]
            [harja.palvelin.raportointi.raportit.yleinen :refer [raportin-otsikko]]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [harja.pvm :as pvm]))

(defqueries "harja/palvelin/raportointi/raportit/turvallisuuspoikkeamat.sql")

(def vuosi-ja-kk-fmt (tf/formatter "YYYY/MM"))
(defn- vuosi-ja-kk [pvm]
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

(def turvallisuuspoikkeama-tyyppi
  {"turvallisuuspoikkeama" "Turvallisuuspoikkeama"
   "tyoturvallisuuspoikkeama" "Työturvallisuuspoikkeama"
   "prosessipoikkeama" "Prosessipoikkeama"})

(defn rivi [& asiat]
  (into [] (keep identity asiat)))

(defn suorita [db user {:keys [urakka-id hallintayksikko-id urakoittain?
                               alkupvm loppupvm toimenpide-id] :as parametrit}]
  (log/info "PARAMS: " parametrit)
  (let [konteksti (cond urakka-id :urakka
                        hallintayksikko-id :hallintayksikko
                        :default :koko-maa)
        turpot (into []
                     (comp 
                      (map #(konv/array->vec % :tyyppi))
                      (map konv/alaviiva->rakenne))
                     (hae-turvallisuuspoikkeamat db 
                                                 (if urakka-id true false) urakka-id
                                                 (if hallintayksikko-id true false) hallintayksikko-id
                                                 alkupvm loppupvm))
        turpo-maarat-kuukausittain (frequencies (map (comp vuosi-ja-kk :tapahtunut) turpot))
        raportin-nimi "Turvallisuusraportti"
        otsikko (raportin-otsikko
                  (case konteksti
                    :urakka  (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                    :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio db hallintayksikko-id)))
                    :koko-maa "KOKO MAA")
                  raportin-nimi alkupvm loppupvm)]
    [:raportti {:nimi raportin-nimi}
     [:taulukko {:otsikko otsikko :viimeinen-rivi-yhteenveto? true}
      (into []
            (concat (when urakoittain?
                      [{:otsikko "Urakka"}])
                    [{:otsikko "Tyyppi"}
                     {:otsikko "Määrä"}]))
      
      (concat (mapcat (fn [[urakka turpot]]
                        (let [turpo-maarat-per-tyyppi (frequencies (mapcat :tyyppi turpot))]
                          [(rivi (:nimi urakka) "Turvallisuuspoikkeama" (or (turpo-maarat-per-tyyppi "turvallisuuspoikkeama") 0))
                           (rivi (:nimi urakka) "Prosessipoikkeama" (or (turpo-maarat-per-tyyppi "prosessipoikkeama") 0))
                           (rivi (:nimi urakka) "Työturvallisuuspoikkeama" (or (turpo-maarat-per-tyyppi "tyoturvallisuuspoikkeama") 0))]))
                      (if urakoittain?
                        (group-by :urakka turpot)
                        [[nil turpot]]))
              [(rivi (when urakoittain? "") "Yhteensä: " (count turpot))])]
     
     (when (and (not= (vuosi-ja-kk alkupvm) (vuosi-ja-kk loppupvm))
                (> (count turpot) 0))
       [:pylvaat {:otsikko "Turvallisuuspoikkeamat kuukausittain"}
        (into []
              (map (juxt identity #(or (turpo-maarat-kuukausittain %) 0)))
              (kuukaudet alkupvm loppupvm))])
     [:taulukko {:otsikko (str "Turvallisuuspoikkeamat listana: " (count turpot) " kpl")
                 :viimeinen-rivi-yhteenveto? true}
      (into []
            (concat (when urakoittain?
                      [{:otsikko "Urakka"}])
                    [{:otsikko "Pvm"}
                     {:otsikko "Tyyppi"}
                     {:otsikko "Ammatti"}
                     {:otsikko "Työtehtävä"}
                     {:otsikko "Sairaala\u00advuorokaudet"}
                     {:otsikko "Sairaus\u00adpoissaolot\u00adpäivät"}]))
      
      (conj (mapv #(rivi (if urakoittain? (:nimi (:urakka %)) nil)
                         (pvm/pvm-aika (:tapahtunut %))
                         (str/join ", " (map turvallisuuspoikkeama-tyyppi (:tyyppi %)))
                         (:tyontekijanammatti %) (:tyotehtava %)
                         (:sairaalavuorokaudet %) (:sairauspoissaolopaivat %))
                  
                  turpot)
            (rivi (if urakoittain? "" nil) "" "" "" "Yhteensä: "
                  (reduce + 0 (keep :sairaalavuorokaudet turpot))
                  (reduce + 0 (keep :sairauspoissaolopaivat turpot))))]]))
