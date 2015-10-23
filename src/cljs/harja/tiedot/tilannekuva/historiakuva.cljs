(ns harja.tiedot.tilannekuva.historiakuva
  (:require [reagent.core :refer [atom]]

            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]]
            [harja.atom :refer-macros [reaction<!] :refer [paivita-periodisesti]]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [cljs-time.core :as t]
            [clojure.string :as str]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]

            [clojure.set :refer [rename-keys]])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

;; Mitä haetaan?
(defonce hae-toimenpidepyynnot? (atom true))
(defonce hae-kyselyt? (atom true))
(defonce hae-tiedoitukset? (atom true))
(defonce hae-turvallisuuspoikkeamat? (atom true))
(defonce hae-tarkastukset? (atom true))
(defonce hae-havainnot? (atom true))
(defonce hae-paikkaustyot? (atom true))
(defonce hae-paallystystyot? (atom true))

;; Millä ehdoilla haetaan?
(defonce valittu-aikasuodatin (atom :lyhyt))
(defonce lyhyen-suodattimen-asetukset (atom {:pvm (pvm/nyt) :kellonaika "12:00" :plusmiinus 12}))
(defonce pitkan-suodattimen-asetukset (atom {:alku  (first (pvm/kuukauden-aikavali (pvm/nyt)))
                                             :loppu (second (pvm/kuukauden-aikavali (pvm/nyt)))}))

(defonce nakymassa? (atom false))
(defonce karttataso-historiakuva (atom false))

;; Haetaan/päivitetään toimenpidekoodit kun tullaan näkymään
(defonce toimenpidekoodit (reaction<! [nakymassa? @nakymassa?
                                       urakka @nav/valittu-urakka
                                       tyyppi @nav/valittu-urakkatyyppi]
                                      (when nakymassa?
                                        (go (let [res (<! (k/post! :hae-toimenpidekoodit-historiakuvaan
                                                                   {:urakka        (:id urakka)
                                                                    :urakan-tyyppi (:arvo tyyppi)}))]
                                              (when-not (k/virhe? res) res))))))

(defonce valitut-toteumatyypit (atom {}))


(defonce valitse-uudet-tpkt
         (run!
           (let [nimet (into #{} (map :nimi @toimenpidekoodit))]
             (swap! valitut-toteumatyypit
                    (fn [nyt-valitut]
                      (let [olemasaolevat (into #{} (keys nyt-valitut))
                            kaikki (vec (clojure.set/union olemasaolevat nimet))]
                        (zipmap kaikki
                                (map #(get nyt-valitut % true)
                                     kaikki))))))))

(defonce naytettavat-toteumatyypit (reaction
                                     (group-by :emo @toimenpidekoodit)))

(def haetut-asiat (atom nil))

(def historiakuvan-asiat-kartalla
  (reaction
    @haetut-asiat
    (when @karttataso-historiakuva
      (kartalla-esitettavaan-muotoon @haetut-asiat))))

(defn kasaa-parametrit []
  {:hallintayksikko @nav/valittu-hallintayksikko-id
   :urakka          (:id @nav/valittu-urakka)
   :alue            @nav/kartalla-nakyva-alue
   :alku            (if (= @valittu-aikasuodatin :lyhyt)
                      (t/minus (pvm/->pvm-aika (str (pvm/pvm (:pvm @lyhyen-suodattimen-asetukset))
                                                    " "
                                                    (:kellonaika @lyhyen-suodattimen-asetukset)))
                               (t/hours (:plusmiinus @lyhyen-suodattimen-asetukset)))

                      (:alku @pitkan-suodattimen-asetukset))

   :loppu           (if (= @valittu-aikasuodatin :lyhyt)
                      (t/plus (pvm/->pvm-aika (str (pvm/pvm (:pvm @lyhyen-suodattimen-asetukset))
                                                   " "
                                                   (:kellonaika @lyhyen-suodattimen-asetukset)))
                              (t/hours (:plusmiinus @lyhyen-suodattimen-asetukset)))

                      (:loppu @pitkan-suodattimen-asetukset))})

(defn hae-asiat []
  (log "Hae historia! " (pr-str @valittu-aikasuodatin))
  (go
    (let [yhdista (fn [& tulokset]
                    (apply (comp vec concat) (remove k/virhe? tulokset)))
          yhteiset-parametrit (kasaa-parametrit)
          haettavat-toimenpidekoodit (mapv
                                       :id
                                       (filter
                                         (fn [{:keys [nimi]}]
                                           (get @valitut-toteumatyypit nimi))
                                         @toimenpidekoodit))
          tulos (yhdista
                  (when @hae-turvallisuuspoikkeamat? (mapv
                                                       #(assoc % :tyyppi-kartalla :turvallisuuspoikkeama)
                                                       (<! (k/post! :hae-turvallisuuspoikkeamat (rename-keys
                                                                                                  yhteiset-parametrit
                                                                                                  {:urakka :urakka-id})))))
                  (when @hae-tarkastukset?
                    (mapv
                      #(assoc % :tyyppi-kartalla :tarkastus)
                      (<! (k/post! :hae-urakan-tarkastukset (rename-keys
                                                             yhteiset-parametrit
                                                             {:urakka :urakka-id
                                                              :alku   :alkupvm
                                                              :loppu  :loppupvm})))))
                  (when @hae-havainnot? (mapv
                                          #(assoc % :tyyppi-kartalla :havainto)
                                          (<! (k/post! :hae-urakan-havainnot (rename-keys
                                                                               yhteiset-parametrit
                                                                               {:urakka :urakka-id})))))
                  (when @hae-paikkaustyot? (remove
                                             #(empty? (:kohdeosat %))
                                             (mapv
                                               #(assoc % :tyyppi-kartalla :paikkaustoteuma)
                                               (<! (k/post! :urakan-paikkaustoteumat (rename-keys
                                                                                       yhteiset-parametrit
                                                                                       {:urakka :urakka-id}))))))
                  (when @hae-paallystystyot? (remove
                                               #(empty? (:kohdeosat %))
                                               (mapv
                                                 #(assoc % :tyyppi-kartalla :paallystyskohde)
                                                 (<! (k/post! :urakan-paallystyskohteet (rename-keys
                                                                                          yhteiset-parametrit
                                                                                          {:urakka :urakka-id}))))))
                  (when (or @hae-toimenpidepyynnot? @hae-tiedoitukset? @hae-kyselyt?)
                    (mapv
                      #(assoc % :tyyppi-kartalla (get % :ilmoitustyyppi))
                      (<! (k/post! :hae-ilmoitukset (assoc
                                                     yhteiset-parametrit
                                                     :aikavali [(:alku yhteiset-parametrit)
                                                                (:loppu yhteiset-parametrit)]
                                                     :tilat #{:avoimet}
                                                     :tyypit (remove nil? [(when @hae-toimenpidepyynnot? :toimenpidepyynto)
                                                                           (when @hae-kyselyt? :kysely)
                                                                           (when @hae-tiedoitukset? :tiedoitus)]))))))
                  (when-not (empty? haettavat-toimenpidekoodit)
                    (mapv
                      #(assoc % :tyyppi-kartalla :toteuma)
                      (<! (k/post! :hae-toteumat-historiakuvaan (assoc
                                                                 yhteiset-parametrit
                                                                 :toimenpidekoodit
                                                                 haettavat-toimenpidekoodit))))))]
      (reset! haetut-asiat tulos))))

(def +bufferi+ 1000) ;1s

(def asioiden-haku (reaction<!
                     [_ @hae-toimenpidepyynnot?
                      _ @hae-kyselyt?
                      _ @hae-tiedoitukset?
                      _ @hae-turvallisuuspoikkeamat?
                      _ @hae-tarkastukset?
                      _ @hae-havainnot?
                      _ @hae-paikkaustyot?
                      _ @hae-paallystystyot?
                      _ @toimenpidekoodit
                      _ @valitut-toteumatyypit
                      valittu-aikasuodatin @valittu-aikasuodatin
                      lyhyen-suodattimen-asetukset @lyhyen-suodattimen-asetukset
                      pitkan-suodattimen-asetukset @pitkan-suodattimen-asetukset
                      nakymassa @nakymassa?
                      _ @nav/kartalla-nakyva-alue
                      _ @nav/valittu-urakka
                      _ @nav/valittu-hallintayksikko-id]
                     {:odota +bufferi+}
                     (when (and nakymassa?
                                (or
                                  (and
                                    (= valittu-aikasuodatin :lyhyt)
                                    (not (some nil? (vals lyhyen-suodattimen-asetukset))))
                                  (and
                                    (= valittu-aikasuodatin :pitka)
                                    (not (some nil? (vals pitkan-suodattimen-asetukset))))))
                       (hae-asiat))))
