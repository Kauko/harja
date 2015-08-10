(ns harja.tiedot.urakka.kohdeluettelo.paikkaus
  "Tämä nimiavaruus hallinnoi urakan paikkaustietoja."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(defonce yhteenvetonakymassa? (atom false))
(defonce paikkausilmoitukset-nakymassa? (atom false))