(ns harja.views.hallinta
  "Päätason sivu Hallinta, josta kaikkeen ylläpitötyöhön pääsee käsiksi."
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.bootstrap :as bs]

            [harja.domain.roolit :as roolit]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.views.toimenpidekoodit :as tp]
            [harja.views.indeksit :as i]
            [harja.views.hallinta.lampotilat :as lampotilat]
            [harja.views.hallinta.kayttajat :as kayttajat]
            [harja.views.hallinta.integraatioloki :as integraatioloki]
            [harja.ui.grid :as g]
            ))

(def +vain-jvhn-kaytossa+ "Tämä osio on vain järjestelmän vastuuhenkilön käytössä.")

(defn hallinta []
  [bs/tabs {:style :tabs :classes "tabs-taso1"
            :active (nav/valittu-valilehti-atom :hallinta)}

   "Käyttäjät"
   :kayttajat
   ^{:key "kayttajat"}
   (when (oikeudet/voi-lukea? oikeudet/hallinta-kayttajat)
     [kayttajat/kayttajat])

   "Indeksit"
   :indeksit
   (when (oikeudet/voi-lukea? roolit/jarjestelmavastuuhenkilo)
     ^{:key "indeksit"}
     [i/indeksit-elementti])

   "Tehtävät"
   :tehtavat
   (when (oikeudet/voi-lukea? roolit/jarjestelmavastuuhenkilo)
     ^{:key "tehtävät"}
     [tp/toimenpidekoodit])

   "Lämpötilat"
   :lampotilat
   (when (oikeudet/voi-lukea? roolit/jarjestelmavastuuhenkilo)
     ^{:key "lämpötilat"}
     [lampotilat/lampotilat])

   "Integraatioloki"
   :integraatioloki
   (when (oikeudet/voi-lukea? roolit/jarjestelmavastuuhenkilo)
     ^{:key "integraatioloki"}
     [integraatioloki/integraatioloki])])
