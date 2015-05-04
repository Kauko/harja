(ns harja.tiedot.urakka.toteumat
  "Tämä nimiavaruus hallinnoi urakan toteumien tietoja."
  (:require [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.ui.protokollat :refer [Haku hae]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn hae-tehtavat [urakka-id]
  (k/post! :hae-urakan-tehtavat urakka-id))

(defn hae-materiaalit [urakka-id]
  (k/post! :hae-urakan-materiaalit urakka-id))

(defn hae-urakan-toteumat [urakka-id sopimus-id [alkupvm loppupvm]]
  (k/post! :urakan-toteumat
           {:urakka-id urakka-id
            :sopimus-id sopimus-id
            :alkupvm alkupvm
            :loppupvm loppupvm}))

(defn hae-urakan-toteuma-paivat [urakka-id sopimus-id [alkupvm loppupvm]]
  (k/post! :urakan-toteuma-paivat
           {:urakka-id urakka-id
            :sopimus-id sopimus-id
            :alkupvm alkupvm
            :loppupvm loppupvm}))

(defn tallenna-toteuma [toteuma]
  (k/post! :tallenna-urakan-toteuma toteuma))
           
           
