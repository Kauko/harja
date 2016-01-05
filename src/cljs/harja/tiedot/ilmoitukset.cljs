(ns harja.tiedot.ilmoitukset
  (:require [reagent.core :refer [atom]]
            [harja.domain.ilmoitusapurit :refer [+ilmoitustyypit+ ilmoitustyypin-nimi +ilmoitustilat+]]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tiedot.urakka :as u]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]]
            [harja.atom :refer [paivita-periodisesti] :refer-macros [reaction<!]]
            [harja.asiakas.tapahtumat :as tapahtumat]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
            [harja.geo :as geo])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

;; FILTTERIT
(defonce ilmoitusnakymassa? (atom false))
(defonce valittu-ilmoitus (atom nil))

(defonce valinnat (reaction {:hallintayksikko (:id @nav/valittu-hallintayksikko)
                             :urakka          (:id @nav/valittu-urakka)
                             :urakoitsija     (:id @nav/valittu-urakoitsija)
                             :urakkatyyppi    (:arvo @nav/valittu-urakkatyyppi)
                             :hoitokausi      @u/valittu-hoitokausi
                             :aikavali        (or @u/valittu-hoitokausi [nil nil])
                             :tyypit          +ilmoitustyypit+
                             :tilat           +ilmoitustilat+
                             :hakuehto        ""}))

(defonce ilmoitushaku (atom 0))

(defn hae-ilmoitukset []
  (go (swap! ilmoitushaku inc)))

(defn jarjesta-ilmoitukset [tulos]
  (sort-by
    :ilmoitettu
    pvm/ennen?
    (mapv
      (fn [ilmo]
        (assoc ilmo :kuittaukset
                    (sort-by :kuitattu pvm/ennen? (:kuittaukset ilmo))))
      tulos)))

(defonce haetut-ilmoitukset
         (reaction<! [valinnat @valinnat
                      haku @ilmoitushaku]
                     {:odota 100}
                     (go
                       (if (zero? haku)
                         []
                         (let [tulos (<! (k/post! :hae-ilmoitukset
                                                  (-> valinnat
                                                      ;; jos tyyppiä/tilaa ei valittu, ota kaikki
                                                      (update-in [:tyypit]
                                                                 #(if (empty? %) +ilmoitustyypit+ %))
                                                      (update-in [:tilat]
                                                                 #(if (empty? %) +ilmoitustilat+ %)))))]
                           (when-not (k/virhe? tulos)
                             (when @valittu-ilmoitus ;; Jos on valittuna ilmoitus joka ei ole haetuissa, perutaan valinta
                               (when-not (some #{(:ilmoitusid @valittu-ilmoitus)} (map :ilmoitusid tulos))
                                 (reset! valittu-ilmoitus nil)))
                             (jarjesta-ilmoitukset tulos)))))))



(defonce karttataso-ilmoitukset (atom false))

(defonce ilmoitukset-kartalla
         (reaction
           @valittu-ilmoitus
           (when @karttataso-ilmoitukset
             (kartalla-esitettavaan-muotoon
               (map
                 #(assoc % :tyyppi-kartalla (get % :ilmoitustyyppi))
                 @haetut-ilmoitukset)
               @valittu-ilmoitus))))
