(ns harja.views.urakka.toteumat.varusteet
  "Urakan 'Toteumat' välilehden 'Varusteet' osio"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<! >! chan timeout]]
            [harja.atom :refer [paivita!] :refer-macros [reaction<!]]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :refer [ajax-loader]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.tiedot.urakka.toteumat.varusteet :as varustetiedot]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.views.kartta :as kartta]
            [harja.ui.komponentti :as komp]
            [harja.pvm :as pvm]
            [harja.tiedot.navigaatio :as nav]
            [harja.views.urakka.valinnat :as urakka-valinnat])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn varustetoteuman-tehtavat [toteumatehtavat]
  [grid/grid
   {:otsikko      "Tehtävät"
    :tyhja        (if (nil? @varustetiedot/haetut-toteumat) [ajax-loader "Haetaan tehtäviä..."] "Tehtäviä  ei löytynyt")
    :tunniste     :id}
   [{:otsikko "Tehtävä" :nimi :nimi :tyyppi :string :leveys 2}
    {:otsikko "Määrä" :nimi :maara :tyyppi :string :leveys 3}]
   toteumatehtavat])

(defn toteumataulukko []
  (let [toteumat @varustetiedot/haetut-toteumat]
    [:span
     [grid/grid
      {:otsikko      "Varustetoteumat"
       :tyhja        (if (nil? @varustetiedot/haetut-toteumat) [ajax-loader "Haetaan toteumia..."] "Toteumia ei löytynyt")
       :tunniste     :id
       :vetolaatikot (zipmap
                       (range)
                       (map
                         (fn [toteuma]
                           [varustetoteuman-tehtavat (:toteumatehtavat toteuma)])
                         toteumat))}
      [{:tyyppi :vetolaatikon-tila :leveys "5%"}
       {:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :alkupvm :leveys "10%"}
       {:otsikko "Tunniste" :nimi :tunniste :tyyppi :string :leveys "15%"}
       {:otsikko "Tietolaji" :nimi :tietolaji :tyyppi :string :hae (fn [rivi]
                                                                     (or (varustetiedot/tietolaji->selitys (:tietolaji rivi))
                                                                         (:tietolaji rivi))) :leveys "15%"}
       {:otsikko "Toimenpide" :nimi :toimenpide :tyyppi :string :hae (fn [rivi]
                                                                       (varustetiedot/varuste-toimenpide->string (:toimenpide rivi)))
        :leveys "10%"}
       {:otsikko "Tie" :nimi :tie :tyyppi :positiivinen-numero :leveys "10%"}
       {:otsikko "Aosa" :nimi :aosa :tyyppi :positiivinen-numero :leveys "5%"}
       {:otsikko "Aet" :nimi :aet :tyyppi :positiivinen-numero :leveys "5%"}
       {:otsikko "Losa" :nimi :losa :tyyppi :positiivinen-numero :leveys "5%"}
       {:otsikko "Let" :nimi :let :tyyppi :positiivinen-numero :leveys "5%"}]
      (take 500 toteumat)]
     (when (> (count toteumat) 500)
       [:div.alert-warning "Toteumia löytyi yli 500. Tarkenna hakurajausta."])]))

(defn valinnat []
  [:span
   [urakka-valinnat/urakan-sopimus]
   [urakka-valinnat/urakan-hoitokausi-ja-kuukausi @nav/valittu-urakka]
   [urakka-valinnat/tienumero varustetiedot/tienumero]])

(defn varusteet []
  (komp/luo
    (komp/lippu varustetiedot/nakymassa? varustetiedot/karttataso-varustetoteuma)

    (fn []
      [:span
       [kartta/kartan-paikka]
       [valinnat]
       [toteumataulukko]])))
