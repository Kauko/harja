(ns harja.tiedot.ilmoitukset
  (:require [reagent.core :refer [atom]]

            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tiedot.urakka :as u]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]]
            [harja.atom :refer-macros [reaction<!]]
            [harja.asiakas.tapahtumat :as tapahtumat])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

;; FILTTERIT
(defonce ilmoitusnakymassa? (atom false))
(defonce valittu-ilmoitus (atom nil))

(defonce valittu-hallintayksikko (reaction @nav/valittu-hallintayksikko))
(defonce valittu-urakka (reaction @nav/valittu-urakka))
(defonce valitut-tilat (atom {:suljetut true :avoimet true}))
(defonce valittu-aikavali (reaction [(first @u/valittu-hoitokausi) (second @u/valittu-hoitokausi)]))
(defonce valitut-ilmoitusten-tyypit (atom {:kysely true :toimenpidepyynto true :tiedoitus true}))
(defonce hakuehto (atom nil))

(defonce haetut-ilmoitukset (atom []))

;; POLLAUS
(def pollaus-id (atom nil))
(def +sekuntti+ 1000)
(def +minuutti+ (* 60 +sekuntti+))
(def +intervalli+ +minuutti+)

(def ilmoitus-kartalla-xf
  #(assoc %
    :type :ilmoitus
    :alue {:type        :circle
           :coordinates (:sijainti %)
           :color        (if (= (:id %) (:id @valittu-ilmoitus)) "green" "blue")
           :stroke      {:color "black" :width 10}}))

(defonce ilmoitusta-klikattu
         (tapahtumat/kuuntele! :ilmoitus-klikattu
                               (fn [ilmoitus]
                                 (reset! valittu-ilmoitus (dissoc ilmoitus :type :alue)))))

(defonce taso-ilmoitukset (atom false))

(defonce ilmoitukset-kartalla
         (reaction
           @valittu-ilmoitus
           (when @taso-ilmoitukset
             (into [] (map ilmoitus-kartalla-xf) @haetut-ilmoitukset))))

(defonce filttereita-vaihdettu? (reaction
                                  @valittu-hallintayksikko
                                  @valittu-urakka
                                  @valitut-tilat
                                  @valittu-aikavali
                                  @valitut-ilmoitusten-tyypit
                                  @hakuehto
                                  true))

(defn kasaa-parametrit []
  (let [valitut (vec (keep #(when (val %) (key %)) @valitut-ilmoitusten-tyypit)) ;; Jos ei yhtäkään valittuna,
        tyypit (if (empty? valitut) (keep key @valitut-ilmoitusten-tyypit) valitut) ;; lähetetään kaikki tyypit.
        ret {:hallintayksikko (:id @valittu-hallintayksikko)
             :urakka          (:id @valittu-urakka)
             :tilat           @valitut-tilat
             :tyypit          tyypit
             :aikavali        @valittu-aikavali
             :hakuehto        @hakuehto}]
    ret))

(defn jarjesta-ilmoitukset [tulos]
  (sort-by
    :ilmoitettu
    pvm/ennen?
    (mapv
      (fn [ilmo]
        (assoc ilmo :kuittaukset
                    (sort-by :kuitattu pvm/ennen? (:kuittaukset ilmo))))
      tulos)))

(defn hae-ilmoitukset
  []
  (go
    (let [tulos (<! (k/post! :hae-ilmoitukset (kasaa-parametrit)))]
      (when-not (k/virhe? tulos)
        (reset! haetut-ilmoitukset (jarjesta-ilmoitukset tulos)))
      (reset! filttereita-vaihdettu? false)

      tulos)))

(defn lopeta-pollaus
  []
  (when @pollaus-id
    (js/clearInterval @pollaus-id)
    (reset! pollaus-id nil)))

(run! (when @filttereita-vaihdettu? (lopeta-pollaus)))

(defn aloita-pollaus
  []
  (when @pollaus-id (lopeta-pollaus))
  (reset! pollaus-id (js/setInterval hae-ilmoitukset +intervalli+)))
