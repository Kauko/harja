(ns harja.tiedot.tierekisteri
  "Tierekisteri-UI-komponenttiin liittyvät asiat, joita ei voinut laittaa viewiin circular dependencyn takia"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.ui.yleiset :as yleiset])
  (:require-macros
    [reagent.ratom :refer [reaction run!]]
    [cljs.core.async.macros :refer [go]]))

(def karttataso-tr-alkuosoite (atom true))

(def valittu-alkupiste (atom nil))
(def tr-alkupiste-kartalla (reaction
                             (when (and @karttataso-tr-alkuosoite @valittu-alkupiste)
                               [{:alue {:type        :icon
                                        :direction   0
                                        :coordinates (:coordinates @valittu-alkupiste)
                                        :img         (yleiset/karttakuva "images/karttaikonit/kartta-tr-piste-harmaa")}}])))

(tarkkaile! "TR-alkuosoite kartalla: " tr-alkupiste-kartalla)
