(ns harja.palvelin.raportointi.raportit.suolasakko
  "Suolasakkoraportti"
  (:require [taoensso.timbre :as log]
            [harja.domain.roolit :as roolit]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [harja.kyselyt.konversio :as konv]
            [harja.pvm :as pvm]
            [harja.palvelin.raportointi.raportit.yleinen :refer [raportin-otsikko]]
            [jeesql.core :refer [defqueries]]
            [harja.fmt :as fmt]))

(defqueries "harja/kyselyt/suolasakkoraportti.sql"
  {:positional? true})

(defn muodosta-suolasakkoraportti-urakalle [db user {:keys [urakka-id alkupvm loppupvm]}]
  (log/debug "Haetaan tiedot suolasakon raportille urakka-kontekstissa: " urakka-id alkupvm loppupvm)
  (let [parametrit [db
                    urakka-id
                    (konv/sql-timestamp alkupvm)
                    (konv/sql-timestamp loppupvm)
                    (+ (.getYear (konv/sql-timestamp alkupvm)) 1900)
                    (+ (.getYear (konv/sql-timestamp loppupvm)) 1900)]
        raportin-tiedot (into [] (apply hae-tiedot-urakan-suolasakkoraportille parametrit))]
    raportin-tiedot))


(defn muodosta-suolasakkoraportti-hallintayksikolle [db user {:keys [hallintayksikko-id alkupvm loppupvm urakkatyyppi]}]
  (log/debug "Haetaan tiedot suolasakon raportille hallintayksikkö-kontekstissa: " hallintayksikko-id alkupvm loppupvm urakkatyyppi)
  (let [parametrit [db
                    hallintayksikko-id
                    (when urakkatyyppi (name urakkatyyppi))
                    (konv/sql-timestamp alkupvm)
                    (konv/sql-timestamp loppupvm)
                    (+ (.getYear (konv/sql-timestamp alkupvm)) 1900)
                    (+ (.getYear (konv/sql-timestamp loppupvm)) 1900)]
        raportin-tiedot (into [] (apply hae-tiedot-hallintayksikon-suolasakkoraportille parametrit))]
    raportin-tiedot))

(defn muodosta-suolasakkoraportti-koko-maalle [db user {:keys [alkupvm loppupvm urakkatyyppi]}]
  (log/debug "Haetaan tiedot suolasakon raportille koko maa -kontekstissa: " alkupvm loppupvm urakkatyyppi)
  (let [parametrit [db
                    (when urakkatyyppi (name urakkatyyppi))
                    (konv/sql-timestamp alkupvm)
                    (konv/sql-timestamp loppupvm)
                    (+ (.getYear (konv/sql-timestamp alkupvm)) 1900)
                    (+ (.getYear (konv/sql-timestamp loppupvm)) 1900)]
        raportin-tiedot (into [] (apply hae-tiedot-koko-maan-suolasakkoraportille parametrit))]
    raportin-tiedot))

(defn suorita [db user {:keys [urakka-id alkupvm loppupvm hallintayksikko-id urakkatyyppi] :as parametrit}]
  (log/debug "Ajat:" (pr-str alkupvm loppupvm))
  (let [[konteksti raportin-data]
        (cond
          (and urakka-id alkupvm loppupvm)
          [:urakka (muodosta-suolasakkoraportti-urakalle db user {:urakka-id urakka-id
                                                                  :alkupvm   alkupvm
                                                                  :loppupvm  loppupvm})]

          (and hallintayksikko-id alkupvm loppupvm)
          [:hallintayksikko (muodosta-suolasakkoraportti-hallintayksikolle db user {:hallintayksikko-id hallintayksikko-id
                                                                                    :alkupvm alkupvm
                                                                                    :loppupvm loppupvm
                                                                                    :urakkatyyppi urakkatyyppi})]
          (and alkupvm loppupvm)
          [:koko-maa (muodosta-suolasakkoraportti-koko-maalle db user {:alkupvm alkupvm
                                                                       :loppupvm loppupvm
                                                                       :urakkatyyppi urakkatyyppi})]

          :default
          nil)

        suolasakot-hallintayksikoittain (sort (group-by :hallintayksikko_elynumero
                                                        raportin-data))
        raportin-nimi "Suolasakkoraportti"
        otsikko (raportin-otsikko
                  (case konteksti
                    :urakka  (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                    :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio db hallintayksikko-id)))
                    :koko-maa "KOKO MAA")
                  raportin-nimi alkupvm loppupvm)
        laske-sakko (fn [rivi]
                      (when (and (:ylitys rivi)
                                 (> (:ylitys rivi) 0)
                                 (:sakko_maara_per_tonni rivi))
                        (* (:ylitys rivi)
                           (:sakko_maara_per_tonni rivi))))
        laske-indeksikorotettu-sakko (fn [rivi]
                                       (when (and (:ylitys rivi)
                                                  (> (:ylitys rivi) 0)
                                                  (:kerroin rivi) (:ylitys rivi) (:sakko_maara_per_tonni rivi))
                                         (* (:kerroin rivi)
                                            (* (:ylitys rivi)
                                               (:sakko_maara_per_tonni rivi)))))]
    [:raportti {:orientaatio :landscape
                :nimi        raportin-nimi}
     [:taulukko {:otsikko                    otsikko
                 :viimeinen-rivi-yhteenveto? true
                 :oikealle-tasattavat-kentat (set (range 1 14))}
      [{:leveys 10 :otsikko "Urakka"}
       {:otsikko "Keski\u00ADlämpö\u00ADtila" :leveys 5}
       {:otsikko "Pitkän aikavälin keski\u00ADlämpö\u00ADtila" :leveys 5}
       {:otsikko "Talvi\u00ADsuolan max-määrä (t)" :leveys 5}
       {:otsikko "Sakko\u00ADraja (t)" :leveys 5}
       {:otsikko "Kerroin" :leveys 4}
       {:otsikko "Kohtuul\u00ADlis\u00ADtarkis\u00ADtettu sakko\u00ADraja (t)" :leveys 5}
       {:otsikko "Käytetty suola\u00ADmäärä (t)" :leveys 5}
       {:otsikko "Suola\u00ADerotus (t)" :leveys 5}
       {:otsikko "Sakko \u20AC / tonni" :leveys 4}
       {:otsikko "Sakko €" :leveys 6}
       {:otsikko "Indeksi €" :leveys 5}
       {:otsikko "Indeksi\u00ADkorotettu sakko €" :leveys 6}]

      (keep identity
            (conj
              (into []
                    (apply concat
                           (for [[elynum hyn-suolasakot] suolasakot-hallintayksikoittain
                                 :let [elynimi (:hallintayksikko_nimi (first hyn-suolasakot))]]
                             (concat
                               (for [rivi hyn-suolasakot]
                                 (let [sakko (laske-sakko rivi)
                                       indeksikorotettu-sakko (laske-indeksikorotettu-sakko rivi)]
                                   [(:urakka_nimi rivi)
                                    (str (:keskilampotila rivi) " °C")
                                    (str (:pitkakeskilampotila rivi) " °C")
                                    (:sakko_talvisuolaraja rivi)
                                    (when (:sakko_talvisuolaraja rivi)
                                      (format "%.2f" (* (:sakko_talvisuolaraja rivi) 1.05)))
                                    (if (:kerroin rivi)
                                      (format "%.4f" (:kerroin rivi))
                                      "Indeksi puuttuu!")
                                    (when (:kohtuullistarkistettu_sakkoraja rivi)
                                      (format "%.2f" (:kohtuullistarkistettu_sakkoraja rivi)))
                                    (:suola_kaytetty rivi)
                                    (when (and (:suola_kaytetty rivi) (:kohtuullistarkistettu_sakkoraja rivi))
                                      (- (:suola_kaytetty rivi) (:kohtuullistarkistettu_sakkoraja rivi)))
                                    (fmt/euro-opt false (:sakko_maara_per_tonni rivi))
                                    (fmt/euro-opt false (laske-sakko rivi))
                                    (when (and sakko indeksikorotettu-sakko)
                                      (fmt/euro-opt false (- indeksikorotettu-sakko sakko)))
                                    (when (and (:kerroin rivi) sakko)
                                      (fmt/euro-opt false (* (:kerroin rivi) sakko)))]))
                               ;; jos koko maan rapsa, näytä kunkin Hallintayksikön summarivi
                               (when (and (= :koko-maa konteksti) elynum)
                                 [{:lihavoi? true
                                   :rivi
                                   [(str elynum " "  elynimi)
                                    nil
                                    nil
                                    (reduce + (keep :sakko_talvisuolaraja hyn-suolasakot))
                                    nil
                                    nil
                                    (reduce + (keep :kohtuullistarkistettu_sakkoraja hyn-suolasakot))
                                    (reduce + (keep :suola_kaytetty hyn-suolasakot))
                                    (-
                                      (reduce + (keep :suola_kaytetty hyn-suolasakot))
                                      (reduce + (keep :kohtuullistarkistettu_sakkoraja hyn-suolasakot)))
                                    nil
                                    (fmt/euro-opt false
                                                  (reduce + (keep
                                                              (fn [rivi]
                                                                (laske-sakko rivi))
                                                              hyn-suolasakot)))
                                    nil
                                    (fmt/euro-opt false
                                                  (reduce + (keep
                                                              (fn [rivi]
                                                                (laske-indeksikorotettu-sakko rivi))
                                                              hyn-suolasakot)))]}])))))
              (when (not (empty? raportin-data))
                ["Yhteensä"
                 nil
                 nil
                 (reduce + (keep :sakko_talvisuolaraja raportin-data))
                 nil
                 nil
                 (reduce + (keep :kohtuullistarkistettu_sakkoraja raportin-data))
                 (reduce + (keep :suola_kaytetty raportin-data))
                 (-
                   (reduce + (keep :suola_kaytetty raportin-data))
                   (reduce + (keep :kohtuullistarkistettu_sakkoraja raportin-data)))
                 nil
                 (fmt/euro-opt false
                               (reduce + (keep
                                           (fn [rivi]
                                             (laske-sakko rivi))
                                           raportin-data)))
                 nil
                 (fmt/euro-opt false
                               (reduce + (keep
                                           (fn [rivi]
                                             (laske-indeksikorotettu-sakko rivi))
                                           raportin-data)))])))]]))
