(ns harja.views.urakka
  "Urakan näkymät: sisältää urakan perustiedot ja tabirakenteen"
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.bootstrap :as bs]
            [harja.asiakas.tapahtumat :as t]

            [harja.views.urakka.yleiset :as urakka-yleiset]
            [harja.views.urakka.suunnittelu :as suunnittelu]
            [harja.views.urakka.toteumat :as toteumat]
            [harja.views.urakka.laskutus :as laskutus]
            [harja.views.urakka.paallystyksen-kohdeluettelo :as paallystyksen-kohdeluettelo]
            [harja.views.urakka.paikkauksen-kohdeluettelo :as paikkauksen-kohdeluettelo]
            [harja.views.urakka.aikataulu :as aikataulu]
            [harja.views.urakka.valitavoitteet :as valitavoitteet]
            [harja.tiedot.urakka.suunnittelu.kokonaishintaiset-tyot :as kok-hint-tyot]
            [harja.tiedot.urakka.suunnittelu.yksikkohintaiset-tyot :as yks-hint-tyot]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.views.urakka.laadunseuranta :as laadunseuranta]
            [harja.views.urakka.turvallisuuspoikkeamat :as turvallisuuspoikkeamat]
            [harja.tiedot.navigaatio :as nav]
            [harja.domain.oikeudet :as oikeudet])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn valilehti-mahdollinen? [valilehti {:keys [tyyppi sopimustyyppi id] :as urakka}]
  (case valilehti
    :yleiset true
    ;; voidaan siistiä tekemällä välitasoja kuten oikeudet-suunnittelu ja oikeudet-toteumat. Nyt otetaan first
    :suunnittelu (and (oikeudet/urakat-suunnittelu id) (not= sopimustyyppi :kokonaisurakka))
    :toteumat (and (oikeudet/urakat-toteumat id) (not= sopimustyyppi :kokonaisurakka))
    :aikataulu (and (oikeudet/urakat-aikataulu id) (= tyyppi :paallystys))
    :kohdeluettelo-paallystys (and (oikeudet/urakat-kohdeluettelo id) (= tyyppi :paallystys))
    :kohdeluettelo-paikkaus (and (oikeudet/urakat-kohdeluettelo id) (= tyyppi :paikkaus))
    :laadunseuranta (oikeudet/urakat-laadunseuranta id)
    :valitavoitteet (oikeudet/urakat-valitavoitteet id)
    :turvallisuuspoikkeamat (and (oikeudet/urakat-turvallisuus id) (= tyyppi :hoito))
    :laskutus (and (oikeudet/urakat-laskutus id)
                   (not= tyyppi :paallystys)
                   (not= tyyppi :tiemerkinta))))

(defn urakka
  "Urakkanäkymä"
  []
  (let [ur @nav/valittu-urakka
        _ (when-not (valilehti-mahdollinen? (nav/valittu-valilehti :urakat) ur)
            (nav/aseta-valittu-valilehti! :urakat :yleiset))
        hae-urakan-tyot (fn [ur]
                          (go (reset! u/urakan-kok-hint-tyot (<! (kok-hint-tyot/hae-urakan-kokonaishintaiset-tyot ur))))
                          (go (reset! u/urakan-yks-hint-tyot
                                      (s/prosessoi-tyorivit ur
                                                            (<! (yks-hint-tyot/hae-urakan-yksikkohintaiset-tyot (:id ur)))))))]

    ;; Luetaan toimenpideinstanssi, jotta se ei menetä arvoaan kun vaihdetaan välilehtiä
    @u/valittu-toimenpideinstanssi

    (hae-urakan-tyot ur)
    [bs/tabs {:style :tabs :classes "tabs-taso1"
              :active (nav/valittu-valilehti-atom :urakat)}
     "Yleiset"
     :yleiset
     (when (oikeudet/urakat-yleiset (:id ur))
       ^{:key "yleiset"}
       [urakka-yleiset/yleiset ur])

     "Suunnittelu"
     :suunnittelu
     (when (valilehti-mahdollinen? :suunnittelu ur)
       ^{:key "suunnittelu"}
       [suunnittelu/suunnittelu ur])

     "Toteumat"
     :toteumat
     (when (valilehti-mahdollinen? :toteumat ur)
       ^{:key "toteumat"}
       [toteumat/toteumat ur])


     "Aikataulu"
     :aikataulu
     (when (valilehti-mahdollinen? :aikataulu ur)
       ^{:key "aikataulu"}
       [aikataulu/aikataulu])

     "Kohdeluettelo"
     :kohdeluettelo-paallystys
     (when (valilehti-mahdollinen? :kohdeluettelo-paallystys ur)
       ^{:key "kohdeluettelo"}
       [paallystyksen-kohdeluettelo/kohdeluettelo ur])

     "Kohdeluettelo"
     :kohdeluettelo-paikkaus
     (when (valilehti-mahdollinen? :kohdeluettelo-paikkaus ur)
       ^{:key "kohdeluettelo"}
       [paikkauksen-kohdeluettelo/kohdeluettelo ur])

     "Laadunseuranta"
     :laadunseuranta
     (when (valilehti-mahdollinen? :laadunseuranta ur)
       ^{:key "laadunseuranta"}
       [laadunseuranta/laadunseuranta])

     "Välitavoitteet"
     :valitavoitteet
     (when (valilehti-mahdollinen? :valitavoitteet ur)
       ^{:key "valitavoitteet"}
       [valitavoitteet/valitavoitteet ur])

     "Turvallisuus"
     :turvallisuuspoikkeamat
     (when (valilehti-mahdollinen? :turvallisuuspoikkeamat ur)
       ^{:key "turvallisuuspoikkeamat"}
       [turvallisuuspoikkeamat/turvallisuuspoikkeamat])

     "Laskutus"
     :laskutus
     (when (valilehti-mahdollinen? :laskutus ur)
     ^{:key "laskutus"}
     [laskutus/laskutus])]))
