(ns harja.views.urakka.toteumat
  "Urakan 'Toteumat' välilehti:"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.bootstrap :as bs]
            [harja.ui.yleiset :refer [ajax-loader linkki raksiboksi
                                      livi-pudotusvalikko]]
            [harja.tiedot.urakka :as u]
            [harja.views.urakka.toteumat.yksikkohintaiset-tyot :as yks-hint-tyot]
            [harja.views.urakka.toteumat.kokonaishintaiset-tyot :as kokonaishintaiset-tyot]
            [harja.views.urakka.toteumat.muut-tyot :as muut-tyot]
            [harja.views.urakka.toteumat.erilliskustannukset :as erilliskustannukset]
            [harja.views.urakka.toteumat.materiaalit :refer [materiaalit-nakyma]]
            [harja.views.urakka.toteumat.varusteet :as varusteet]
            [harja.views.urakka.toteumat.suola :refer [suolatoteumat]]

            [harja.ui.lomake :refer [lomake]]
            [harja.loki :refer [log logt]]
            [cljs.core.async :refer [<! >! chan]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))


(defn toteumat
  "Toteumien pääkomponentti"
  []
  (let [ur @nav/valittu-urakka]
    (komp/luo
      (komp/sisaan-ulos #(do
                          (reset! nav/kartan-edellinen-koko @nav/kartan-koko)
                          (nav/vaihda-kartan-koko! :S))
                        #(nav/vaihda-kartan-koko! @nav/kartan-edellinen-koko))
      (fn []
        [bs/tabs {:style :tabs :classes "tabs-taso2" :active u/toteumat-valilehti}

         "Kokonaishintaiset työt" :kokonaishintaiset-tyot
         [kokonaishintaiset-tyot/kokonaishintaiset-toteumat]

         "Yksikköhintaiset työt" :yksikkohintaiset-tyot
         [yks-hint-tyot/yksikkohintaisten-toteumat]

         "Muutos- ja lisätyöt" :muut-tyot
         [muut-tyot/muut-tyot-toteumat]

         "Suola" :suola
         (when (= :hoito (:tyyppi ur))
           [suolatoteumat])
         
         "Materiaalit" :materiaalit
         [materiaalit-nakyma ur]

         "Erilliskustannukset" :erilliskustannukset
         [erilliskustannukset/erilliskustannusten-toteumat]

         "Varusteet" :varusteet
         (when (= :hoito (:tyyppi ur))
           [varusteet/varusteet])]))))

