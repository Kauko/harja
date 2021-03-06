(ns harja.views.urakka.laadunseuranta
  (:require [reagent.core :refer [atom]]
            [harja.ui.bootstrap :as bs]

            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.laadunseuranta :as urakka-laadunseuranta]
            [harja.views.urakka.laadunseuranta.tarkastukset :as tarkastukset]
            [harja.views.urakka.laadunseuranta.laatupoikkeamat :as laatupoikkeamat]
            [harja.views.urakka.laadunseuranta.sanktiot :as sanktiot]
            [harja.ui.komponentti :as komp]
            [harja.views.urakka.siltatarkastukset :as siltatarkastukset]))

(defn laadunseuranta []
  (let [ur @nav/valittu-urakka]
    (komp/luo
      (komp/lippu urakka-laadunseuranta/laadunseurannassa?)
      (fn []
        [bs/tabs
         {:style :tabs :classes "tabs-taso2"
          :active (nav/valittu-valilehti-atom :laadunseuranta)}

         "Tarkastukset" :tarkastukset
         [tarkastukset/tarkastukset]

         "Laatupoikkeamat" :laatupoikkeamat
         [laatupoikkeamat/laatupoikkeamat]

         "Sanktiot" :sanktiot
         [sanktiot/sanktiot]

         "Siltatarkastukset" :siltatarkastukset
         (when (= :hoito (:tyyppi ur))
           ^{:key "siltatarkastukset"}
           [siltatarkastukset/siltatarkastukset])]))))

