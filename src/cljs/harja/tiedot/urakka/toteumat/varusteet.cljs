(ns harja.tiedot.urakka.toteumat.varusteet
  (:require [reagent.core :refer [atom]]
            [cljs.core.async :refer [<!]]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.urakka :as urakka]
            [harja.tiedot.navigaatio :as nav]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon kartalla-xf]]
            [harja.pvm :as pvm])
  (:require-macros [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(defn hae-toteumat [urakka-id sopimus-id [alkupvm loppupvm] tienumero]
  (k/post! :urakan-varustetoteumat
           {:urakka-id  urakka-id
            :sopimus-id sopimus-id
            :alkupvm    alkupvm
            :loppupvm   loppupvm
            :tienumero tienumero}))

(defonce tienumero (atom nil))

(def nakymassa? (atom false))

(def haetut-toteumat
  (reaction<! [urakka-id (:id @nav/valittu-urakka)
               sopimus-id (first @urakka/valittu-sopimusnumero)
               hoitokausi @urakka/valittu-hoitokausi
               kuukausi @urakka/valittu-hoitokauden-kuukausi
               tienumero @tienumero
               nakymassa? @nakymassa?]
              {:odota 500
               :nil-kun-haku-kaynnissa? true}
              (when nakymassa?
                (hae-toteumat urakka-id sopimus-id (or kuukausi hoitokausi) tienumero))))

(tarkkaile! "Haetut toteumat: " haetut-toteumat)

(def varuste-toimenpide->string {:lisatty    "Lisätty"
                                 :paivitetty "Päivitetty"
                                 :poistettu  "Poistettu"})

(def karttataso-varustetoteuma (atom false))

(def varusteet-kartalla
  (reaction
    (when karttataso-varustetoteuma
      (kartalla-esitettavaan-muotoon
        (map (fn [toteuma]
               (-> toteuma
                   (assoc :tyyppi-kartalla :varustetoteuma)
                   (assoc :selitys-kartalla (str
                                              (varuste-toimenpide->string (:toimenpide toteuma))
                                              ": "
                                              (:tietolaji toteuma)
                                              " (" (pvm/pvm (:alkupvm toteuma)) " )"))))
          @haetut-toteumat)))))
