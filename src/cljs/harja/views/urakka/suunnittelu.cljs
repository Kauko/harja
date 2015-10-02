(ns harja.views.urakka.suunnittelu
  "Päätason sivu Hallinta, josta kaikkeen ylläpitötyöhön pääsee käsiksi."
  (:require [reagent.core :refer [atom] :as r]
            [bootstrap :as bs]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.tiedot.urakka.kokonaishintaiset-tyot :as kok-hint-tyot]
            [harja.tiedot.urakka.yksikkohintaiset-tyot :as yks-hint-tyot]

            [harja.views.urakka.valinnat :as valinnat]
            [harja.views.urakka.yksikkohintaiset-tyot :as yksikkohintaiset-tyot]
            [harja.views.urakka.kokonaishintaiset-tyot :as kokonaishintaiset-tyot]
            [harja.views.urakka.suunnittelu.muut-tyot :as muut-tyot]
            [harja.views.urakka.materiaalit :as mat]

            [harja.pvm :as pvm]
            [harja.loki :refer [log]]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? livi-pudotusvalikko]])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

; TODO Siirrä tietoihin
(defn valitun-hoitokauden-yks-hint-kustannukset [urakka]
  (reaction (transduce (map #(* (:maara %) (:yksikkohinta %)))
                       + 0
                       (get (u/ryhmittele-hoitokausittain (into []
                                                                (filter (fn [t]
                                                                          (= (:sopimus t) (first @u/valittu-sopimusnumero))))
                                                                @u/urakan-yks-hint-tyot)
                                                          (u/hoitokaudet urakka)) @u/valittu-hoitokausi))))

(defn suunnittelu [ur]
  ;; suunnittelu-välilehtien yhteiset valinnat hoitokaudelle ja sopimusnumerolle
  (let [valitun-hoitokauden-yks-hint-kustannukset (valitun-hoitokauden-yks-hint-kustannukset ur)]

    (r/create-class
      {:reagent-render
       (fn [ur]

         [:span.suunnittelu
          (case @u/suunnittelun-valittu-valilehti
            :kokonaishintaiset [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide ur]
            :yksikkohintaiset [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide+muut ur]
            :muut [valinnat/urakan-sopimus ur]
            :materiaalit [valinnat/urakan-sopimus-ja-hoitokausi ur])

          ;; suunnittelun välilehdet
          [bs/tabs {:style :tabs :classes "tabs-taso2":active u/suunnittelun-valittu-valilehti}

           "Kokonaishintaiset työt"
           :kokonaishintaiset
           ^{:key "kokonaishintaiset-tyot"}
           [kokonaishintaiset-tyot/kokonaishintaiset-tyot ur valitun-hoitokauden-yks-hint-kustannukset]

           "Yksikköhintaiset työt"
           :yksikkohintaiset
           ^{:key "yksikkohintaiset-tyot"}
           [yksikkohintaiset-tyot/yksikkohintaiset-tyot-view ur valitun-hoitokauden-yks-hint-kustannukset]

           "Muutos- ja lisätyöt"
           :muut
           ^{:key "muut-tyot"}
           [muut-tyot/muut-tyot]

           "Materiaalit"
           :materiaalit
           ^{:key "materiaalit"}
           [mat/materiaalit ur]
           ]])

       })))


