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
            [harja.views.urakka.valinnat :as urakka-valinnat]
            [harja.ui.ikonit :as ikonit]
            [harja.views.urakka.toteumat.yksikkohintaiset-tyot :as yksikkohintaiset-tyot])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn varustetoteuman-tehtavat [toteuma]
  (let [toteumatehtavat (:toteumatehtavat toteuma)]
    [grid/grid
     {:otsikko  "Tehtävät"
      :tyhja    (if (nil? @varustetiedot/haetut-toteumat) [ajax-loader "Haetaan tehtäviä..."] "Tehtäviä  ei löytynyt")
      :tunniste :id}
     [{:otsikko "Tehtävä" :nimi :nimi :tyyppi :string :leveys 1}
      {:otsikko "Tyyppi" :nimi :toteumatyyppi :tyyppi :string :leveys 1 :hae (fn [_] (name (:toteumatyyppi toteuma)))}
      {:otsikko "Määrä" :nimi :maara :tyyppi :string :leveys 1}
      (when (= (:toteumatyyppi toteuma) :yksikkohintainen)
        {:otsikko     "Toteuma" :nimi :linkki-toteumaan :tyyppi :komponentti :leveys 1
        :komponentti (fn [] [:button.nappi-toissijainen.nappi-grid
                             {:on-click #(yksikkohintaiset-tyot/nayta-toteuma-lomakkeessa @nav/valittu-urakka-id (:toteumaid toteuma))}
                             (ikonit/eye-open) " Toteuma"])})]
     toteumatehtavat]))

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
                           (when (:toteumatehtavat toteuma)
                             [varustetoteuman-tehtavat toteuma]))
                         toteumat))}
      [{:tyyppi :vetolaatikon-tila :leveys 5}
       {:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :alkupvm :leveys 10}
       {:otsikko "Tunniste" :nimi :tunniste :tyyppi :string :leveys 15}
       {:otsikko "Tietolaji" :nimi :tietolaji :tyyppi :string :leveys 15 :hae (fn [rivi]
                                                                     (or (varustetiedot/tietolaji->selitys (:tietolaji rivi))
                                                                         (:tietolaji rivi)))}
       {:otsikko "Toimenpide" :nimi :toimenpide :tyyppi :string :leveys 15 :hae (fn [rivi]
                                                                       (varustetiedot/varuste-toimenpide->string (:toimenpide rivi)))}
       {:otsikko "Tie" :nimi :tie :tyyppi :positiivinen-numero :leveys 10}
       {:otsikko "Aosa" :nimi :aosa :tyyppi :positiivinen-numero :leveys 5}
       {:otsikko "Aet" :nimi :aet :tyyppi :positiivinen-numero :leveys 5}
       {:otsikko "Losa" :nimi :losa :tyyppi :positiivinen-numero :leveys 5}
       {:otsikko "Let" :nimi :let :tyyppi :positiivinen-numero :leveys 5}]
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
