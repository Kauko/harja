(ns harja.tiedot.urakka.muut-tyot
  "Tämä nimiavaruus hallinnoi urakan yksikköhintaisia töitä."
  (:require [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn hae-urakan-muutoshintaiset-tyot [urakka-id]
  (k/post! :muutoshintaiset-tyot urakka-id))


(defn tallenna-muutoshintaiset-tyot
  "Tallentaa muutoshintaiset työt, palauttaa kanavan, josta vastauksen voi lukea."
  [urakka-id tyot]
  (let [hyotykuorma {:urakka-id urakka-id
                     :tyot tyot}]
    (k/post! :tallenna-muutoshintaiset-tyot
             hyotykuorma)))