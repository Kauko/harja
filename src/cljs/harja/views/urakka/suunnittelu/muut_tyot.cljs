(ns harja.views.urakka.suunnittelu.muut-tyot
  "Urakan 'Muut työt' välilehti, sis. Muutos-, lisä- ja äkilliset hoitotyöt"
  (:require [reagent.core :refer [atom]]
            [harja.domain.roolit :as roolit]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      alasveto-ei-loydoksia livi-pudotusvalikko radiovalinta vihje]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu.muut-tyot :as muut-tyot]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]

            [harja.loki :refer [log logt tarkkaile!]]
            [harja.fmt :as fmt]
            [cljs.core.async :refer [<!]]
            [harja.views.urakka.valinnat :as valinnat])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))


(defn tallenna-tyot [tyot atomi]
  (go (let [ur @nav/valittu-urakka
            sopimusnumero (first @u/valittu-sopimusnumero)
            tyot (map #(assoc % :alkupvm (:alkupvm ur)
                                :loppupvm (:loppupvm ur)
                                :sopimus sopimusnumero) tyot)
            res (<! (muut-tyot/tallenna-muutoshintaiset-tyot (:id @nav/valittu-urakka)
                                                             (into [] tyot)))]
        (reset! atomi res)
        res)))

(defn muut-tyot [ur]
  (let [toimenpideinstanssit @u/urakan-toimenpideinstanssit
        g (grid/grid-ohjaus)
        jo-valitut-tehtavat (atom nil)]
    (komp/luo
      (fn []
        (let [tehtavat-tasoineen @u/urakan-muutoshintaiset-toimenpiteet-ja-tehtavat
              tehtavat (map #(nth % 3) tehtavat-tasoineen)
              valittu-tpi-id (:tpi_id @u/valittu-toimenpideinstanssi)
              valitut-tyot (reaction
                             (filter #(and
                                       (= (:sopimus %) (first @u/valittu-sopimusnumero))
                                       (= (:toimenpideinstanssi %) valittu-tpi-id))
                                     @u/muutoshintaiset-tyot))
              valitun-tpin-tehtavat-tasoineen (urakan-toimenpiteet/toimenpideinstanssin-tehtavat
                                                valittu-tpi-id
                                                toimenpideinstanssit tehtavat-tasoineen)]
          [:div.row.muut-tyot
           [valinnat/urakan-sopimus ur]
           [valinnat/urakan-toimenpide+muut ur]
           (if-not (empty? valitun-tpin-tehtavat-tasoineen)
             [grid/grid
             {:otsikko      "Urakkasopimuksen mukaiset muutos- ja lisätyöhinnat"
              :luokat ["col-md-10"]
              :tyhja        (if (nil? @u/muutoshintaiset-tyot)
                              [ajax-loader "Muutoshintaisia töitä haetaan..."]
                              "Ei muutoshintaisia töitä")
              :tallenna     (roolit/jos-rooli-urakassa roolit/urakanvalvoja
                                                       (:id @nav/valittu-urakka)
                                                       #(tallenna-tyot
                                                         % u/muutoshintaiset-tyot)
                                                       :ei-mahdollinen)
              :ohjaus       g
              :muutos       #(reset! jo-valitut-tehtavat (into #{} (map (fn [rivi]
                                                                          (:tehtava rivi))
                                                                        (vals (grid/hae-muokkaustila %)))))
              :voi-poistaa? #(roolit/roolissa? roolit/jarjestelmavastuuhenkilo)}

             [{:otsikko       "Tehtävä" :nimi :tehtavanimi
               :jos-tyhja "Ei valittavia tehtäviä"
               :valinta-arvo  #(:nimi (nth % 3))
               :valinta-nayta #(if % (:nimi (nth % 3)) "- Valitse tehtävä -")
               :tyyppi        :valinta
               :valinnat-fn   #(filter (fn [t]
                                         (not ((disj @jo-valitut-tehtavat (:tehtava %))
                                                (:id (nth t 3))))) valitun-tpin-tehtavat-tasoineen)
               :muokattava?   #(neg? (:id %))
               :aseta         #(assoc %1 :tehtavanimi %2
                                         :tehtava (:id (urakan-toimenpiteet/tehtava-nimella %2 tehtavat))
                                         :yksikko (:yksikko (urakan-toimenpiteet/tehtava-nimella %2 tehtavat)))
               :leveys        "45%"}
              {:otsikko "Yksikkö" :nimi :yksikko :tyyppi :string :muokattava? (constantly false) :leveys "10%"}
              {:otsikko "Muutoshinta / yksikkö" :nimi :yksikkohinta :tasaa :oikea
               :validoi [[:ei-tyhja "Anna muutoshinta"]]
               :tyyppi  :positiivinen-numero :fmt fmt/euro-opt :leveys "20%"}]

              @valitut-tyot]
             [vihje "Ei tehtäviä valitulla toimenpiteellä tässä urakassa" "col-xs-12"])

           [vihje yleiset/+tehtavien-hinta-vaihtoehtoinen+ "col-xs-12"]])))))

