(ns harja.tiedot.urakka.laadunseuranta.sanktiot
  (:require [reagent.core :refer [atom]]
            [cljs.core.async :refer [<!]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [harja.pvm :as pvm]

            [harja.tiedot.urakka :as urakka]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.istunto :as istunto]
            [harja.tiedot.urakka.laadunseuranta :as laadunseuranta])
  (:require-macros [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(def nakymassa? (atom false))
(def +uusi-sanktio+
  (reaction {:suorasanktio true
             :laatupoikkeama
                           {
                            :tekijanimi @istunto/kayttajan-nimi
                            :paatos     {:paatos "sanktio"}
                            }}))

(defonce valittu-sanktio (atom nil))

(defn hae-urakan-sanktiot
  "Hakee urakan sanktiot annetulle hoitokaudelle."
  [urakka-id [alku loppu]]
  (k/post! :hae-urakan-sanktiot {:urakka-id urakka-id
                                 :alku      alku
                                 :loppu     loppu}))

(defonce haetut-sanktiot
  (reaction<! [urakka (:id @nav/valittu-urakka)
               hoitokausi @urakka/valittu-hoitokausi
               _ @nakymassa?]
              {:nil-kun-haku-kaynnissa? true}
              (when @nakymassa?
                (hae-urakan-sanktiot urakka hoitokausi))))

(defn kasaa-tallennuksen-parametrit
  [s]
  {:sanktio  (dissoc s :laatupoikkeama)
   :laatupoikkeama (if-not (get-in s [:laatupoikkeama :urakka])
               (:laatupoikkeama (assoc-in s [:laatupoikkeama :urakka] (:id @nav/valittu-urakka)))
               (:laatupoikkeama s))})

(defn tallenna-sanktio
  [sanktio]
  (k/post! :tallenna-suorasanktio (kasaa-tallennuksen-parametrit sanktio)))

(defn sanktion-tallennus-onnistui
  [palautettu-id sanktio]
  (when (and
          palautettu-id
          (pvm/valissa?
            (get-in sanktio [:laatupoikkeama :aika])
            (first @urakka/valittu-hoitokausi)
            (second @urakka/valittu-hoitokausi)))
    (if (some #(= (:id %) palautettu-id) @haetut-sanktiot)
     (reset! haetut-sanktiot
             (into [] (map (fn [vanha] (if (= palautettu-id (:id vanha)) (assoc sanktio :id palautettu-id) vanha)) @haetut-sanktiot)))

     (reset! haetut-sanktiot
             (into [] (concat @haetut-sanktiot [(assoc sanktio :id palautettu-id)]))))))


(defonce sanktiotyypit
  (reaction<! [laadunseurannassa? @laadunseuranta/laadunseurannassa?]
              (when laadunseurannassa?
                (k/get! :hae-sanktiotyypit))))

(defn lajin-sanktiotyypit
  [laji]
  (filter #((:laji %) laji) @sanktiotyypit))
