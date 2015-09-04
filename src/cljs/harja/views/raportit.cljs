(ns harja.views.raportit
  "Harjan raporttien pääsivu."
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.komponentti :as komp]
            [harja.ui.valinnat :as valinnat]
            [harja.ui.lomake :as lomake]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.pvm :as pvm]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.yleiset :refer [livi-pudotusvalikko]]
            [harja.fmt :as fmt])
  (:require-macros [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(defonce valittu-raporttityyppi (atom nil))
(tarkkaile! "[RAPORTTI] Valittu-raporttityyppi" valittu-raporttityyppi)

(def +raporttityypit+
  ; HUOM: Hardcoodattu testidata vectori mappeja
  [{:nimi :laskutusyhteenveto
    :otsikko "Yks.hint. töiden toteumat -raportti"
    :konteksti #{:urakka}
    :parametrit
    [{:otsikko  "Hoitokausi"
      :nimi     :hoitokausi
      :tyyppi   :valinta
      :validoi [[:ei-tyhja "Anna arvo"]]
      :valinnat :valitun-urakan-hoitokaudet}
     {:otsikko  "Kuukausi"
      :nimi     :kuukausi
      :tyyppi   :valinta
      :validoi [[:ei-tyhja "Anna arvo"]]
      :valinnat :valitun-aikavalin-kuukaudet}]
    :suorita (constantly nil)}])

(defn tee-lomakekentta [kentta lomakkeen-tiedot]
  (if (= :valinta (:tyyppi kentta))
    (case (:valinnat kentta)
      :valitun-urakan-hoitokaudet
      (assoc kentta :valinnat @u/valitun-urakan-hoitokaudet
                    :valinta-nayta fmt/pvm-vali-opt)
      :valitun-aikavalin-kuukaudet
      (assoc kentta :valinnat (if-let [hk (:hoitokausi lomakkeen-tiedot)]
                                (pvm/hoitokauden-kuukausivalit hk) ; FIXME Näytä kuukaudet tekstinä "Tammikuu, Helmikuu jne. Ehkä myös Koko hoitokausi?"
                                [])
                    :valinta-nayta (comp fmt/pvm-opt first)))

    kentta))

(defn raporttinakyma []
  [:div "Tänne tulee myöhemmin raporttinäkymä..."])

(def lomake-tiedot (atom nil))
(def lomake-virheet (atom nil))
(tarkkaile! "[RAPORTTI] Lomake-virheet: " lomake-virheet)

(defn luo-raportti []
  {} ; TODO Luo raportti
  )

(defonce valittu-raportti
         (reaction (let [valittu-raporttityyppi @valittu-raporttityyppi
                         lomake-virheet @lomake-virheet]
                     (log "Lomake-virheet: " (pr-str lomake-virheet))
                     (when (and valittu-raporttityyppi
                                (not (nil? lomake-virheet))
                                (empty? lomake-virheet))
                       (luo-raportti)))))

(tarkkaile! "[RAPORTTI] Valittu-raportti" valittu-raportti)

(defn raporttivalinnat
  []
  (komp/luo
    (fn []
      [:div.raportit
       [:div.label-ja-alasveto
        [:span.alasvedon-otsikko "Valitse raportti"]
        [livi-pudotusvalikko {:valinta    @valittu-raporttityyppi
                              ;;\u2014 on väliviivan unikoodi
                              :format-fn  #(if % (:otsikko %) "Valitse")
                              :valitse-fn #(reset! valittu-raporttityyppi %)
                              :class      "valitse-raportti-alasveto"}
         +raporttityypit+]]

       (when @valittu-raporttityyppi
         [lomake/lomake
          {:luokka   :horizontal
           :virheet  lomake-virheet
           :muokkaa! (fn [uusi]
                       (reset! lomake-tiedot uusi))}
          (let [lomake-tiedot @lomake-tiedot
                kentat (into []
                             (concat
                               [{:otsikko "Kohde" :nimi :kohteen-nimi :hae #(:nimi @nav/valittu-urakka) :muokattava? (constantly false)}]
                               (map
                                 (fn [kentta]
                                   (tee-lomakekentta kentta lomake-tiedot))
                                 (:parametrit @valittu-raporttityyppi))))]
            kentat)

          @lomake-tiedot])])))

(defn raportit []
  (komp/luo
    (fn [] ; FIXME Urakan oltava valittuna, muuten ei toimi. Valitse hallintayksikkö -komponentti voisi olla myös täällä geneerisenä komponenttina.
      (if @valittu-raportti
        [raporttinakyma]
        [raporttivalinnat]))))
