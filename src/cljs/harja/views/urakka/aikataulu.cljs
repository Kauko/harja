(ns harja.views.urakka.aikataulu
  "Ylläpidon urakoiden aikataulunäkymä"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.urakka.aikataulu :as tiedot]
            [harja.ui.grid :as grid]
            [harja.ui.kentat :refer [tee-kentta]]
            [cljs.core.async :refer [<!]]
            [harja.domain.roolit :as roolit]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))


(defn aikataulu
  []
  (komp/luo
    (komp/lippu tiedot/aikataulu-nakymassa?)
      (fn []
        (let [ur @nav/valittu-urakka
              urakka-id (:id ur)
              sopimus-id (first @u/valittu-sopimusnumero)
              aikataulut @tiedot/aikataulurivit
              paallystysurakoitsijana? #(roolit/rooli-urakassa? roolit/paallystysaikataulun-kirjaus urakka-id)
              tiemerkintaurakoitsijana? #(roolit/rooli-urakassa? roolit/urakan-tiemerkitsija urakka-id)]
          (log "aikataulut: " (pr-str aikataulut))
          [:div.aikataulu
          [grid/grid
           {:otsikko      "Kohteiden aikataulu"
            :voi-poistaa? (constantly false)
            :piilota-toiminnot? true
            :tallenna     (roolit/jos-rooli-urakassa roolit/paallystysaikataulun-kirjaus
                                                     urakka-id
                                                     #(tiedot/tallenna-paallystyskohteiden-aikataulu urakka-id
                                                                                                     sopimus-id
                                                                                                     %)
                                                     :ei-mahdollinen)}

           [{:otsikko "Kohde\u00AD ID" :leveys "5%" :nimi :kohdenumero :tyyppi :string :pituus-max 128 :muokattava? (constantly false)}
            {:otsikko "Kohteen nimi" :leveys "10%" :nimi :nimi :tyyppi :string :pituus-max 128 :muokattava? (constantly false)}

            {:otsikko "TR-osoite" :leveys "10%" :nimi :tr-osoite :tyyppi :string :muokattava? (constantly false)}
            {:otsikko "Pääll. aloitus\u00AD" :leveys "8%" :nimi :aikataulu_paallystys_alku :tyyppi :pvm-aika :fmt pvm/pvm-aika-opt
             :muokattava? paallystysurakoitsijana?}
            {:otsikko "Pääll. valmis" :leveys "8%" :nimi :aikataulu_paallystys_loppu :tyyppi :pvm-aika :fmt pvm/pvm-aika-opt :muokattava? paallystysurakoitsijana?}
            {:otsikko     "Valmis tie\u00ADmerkin\u00ADtään" :leveys "7%" :nimi :valmis_tiemerkintaan :tyyppi :komponentti :muokattava? paallystysurakoitsijana?
             :komponentti (fn [rivi]
                            (if (not (:valmis_tiemerkintaan rivi))
                              [:button.nappi-ensisijainen.nappi-grid
                               {:type     "button"
                                :on-click #(log "Painettu")} "Valmis"]
                              [:span (pvm/pvm-aika-opt (:valmis_tiemerkintaan rivi))]))}
            {:otsikko "TM valmis" :leveys "8%" :nimi :aikataulu_tiemerkinta_loppu :tyyppi :pvm :fmt pvm/pvm-opt :muokattava? tiemerkintaurakoitsijana?}
            {:otsikko "Kohde valmis" :leveys "7%" :nimi :aikataulu_kohde_valmis :tyyppi :pvm :fmt pvm/pvm-opt :muokattava? paallystysurakoitsijana?}]
           @tiedot/aikataulurivit]]))))