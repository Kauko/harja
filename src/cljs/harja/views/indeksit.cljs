(ns harja.views.indeksit
  "Indeksien hallinta."
  (:require [reagent.core :refer [atom] :as reagent]
            [cljs.core.async :refer [<! >! chan close!]]

            [harja.tiedot.indeksit :as i]
            [harja.ui.yleiset :as yleiset]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [harja.ui.grid :as grid]
            [harja.fmt :as fmt])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(declare tallenna-indeksi)

(defn indeksi-grid [indeksin-nimi]
  (let [indeksit @i/indeksit
        rivit (reverse (sort-by :vuosi
                                (map #(assoc (second %) :kannassa? true)
                                     (filter (fn [[[nimi _] _]]
                                               (= nimi indeksin-nimi)
                                               ) indeksit))))
        varatut-vuodet (into #{} (map :vuosi rivit))
        formatter #(fmt/desimaaliluku-opt % 1)]
    [grid/grid
     {:otsikko      indeksin-nimi
      :tyhja        (if (nil? indeksit) [yleiset/ajax-loader "Indeksejä haetaan..."] "Ei indeksitietoja")
      :tallenna     #(tallenna-indeksi indeksin-nimi rivit %)
      :tunniste     :vuosi
      :voi-poistaa? #(not (:kannassa? %))}
     [{:otsikko       "Vuosi" :nimi :vuosi :tyyppi :valinta :leveys "17%"
       :valinta-arvo  identity
       :valinta-nayta #(if (nil? %) "- valitse -" %)

       :valinnat      (vec (filter #(not (varatut-vuodet %)) (range 2010 2045)))

       :validoi       [[:ei-tyhja "Anna indeksin vuosi"] [:uniikki "Sama vuosi vain kerran per indeksi."]]
       :muokattava?   #(not (:kannassa? %))}

      {:otsikko "tammi" :nimi 1 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "helmi" :nimi 2 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "maalis" :nimi 3 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "huhti" :nimi 4 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "touko" :nimi 5 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "kesä" :nimi 6 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "heinä" :nimi 7 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "elo" :nimi 8 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "syys" :nimi 9 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "loka" :nimi 10 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "marras" :nimi 11 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}
      {:otsikko "joulu" :nimi 12 :tyyppi :positiivinen-numero :desimaalien-maara 1 :fmt formatter :leveys "7%"}]
     rivit]))

(defn indeksit-elementti []
  (i/hae-indeksit)
  [:span.indeksit
   [indeksi-grid "MAKU 2005"]                               ;; nimellä haetaan indeksi kannasta
   [:hr]
   [indeksi-grid "MAKU 2010"]])

(defn tallenna-indeksi [nimi indeksivuodet uudet-indeksivuodet]
  (go (let [tallennettavat
            (into []
                  ;; Kaikki tiedon mankelointi ennen lähetystä tähän
                  (comp (filter #(not (:poistettu %))))
                  uudet-indeksivuodet)
            res (<! (i/tallenna-indeksi nimi tallennettavat))]
        (reset! i/indeksit res)
        true)))