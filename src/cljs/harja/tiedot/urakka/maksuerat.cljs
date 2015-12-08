(ns harja.tiedot.urakka.maksuerat
  "Tämä nimiavaruus hallinnoi urakan maksueria."
  (:require [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defn hae-urakan-maksuerat [urakka-id]
  (k/post! :hae-urakan-maksuerat urakka-id))

(defn laheta-maksuerat [maksueranumerot]
  (k/post! :laheta-maksuerat-sampoon (into [] maksueranumerot)))

(defonce nakymassa? (atom false))

(defonce maksuerat
  (reaction<! [urakan-id (:id @nav/valittu-urakka)
               nakymassa? @nakymassa?]
              (when (and urakan-id nakymassa?)
                (hae-urakan-maksuerat urakan-id))))
