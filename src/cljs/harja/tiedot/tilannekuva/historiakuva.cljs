(ns harja.tiedot.tilannekuva.historiakuva
  (:require [reagent.core :refer [atom]]

            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]]
            [harja.atom :refer-macros [reaction<!]]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [cljs-time.core :as t])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

;; Mitä haetaan?
(defonce hae-toimenpidepyynnot? (atom true))
(defonce hae-kyselyt? (atom true))
(defonce hae-tiedoitukset? (atom true))
(defonce hae-turvallisuuspoikkeamat? (atom true))
(defonce hae-tarkastukset? (atom true))
(defonce hae-havainnot? (atom true))
(defonce hae-onnettomuudet? (atom true))
(defonce hae-paikkaustyot? (atom true))
(defonce hae-paallystystyot? (atom true))
(defonce haettavat-toteumatyypit (atom {:kokonaishintaiset true :yksikkohintaiset true}))


;; Millä ehdoilla haetaan?
(defonce valittu-aikasuodatin (atom :lyhyt))
(defonce lyhyen-suodattimen-asetukset (atom {:pvm nil :kellonaika "12:00" :plusmiinus 12}))
(defonce pitkan-suodattimen-asetukset (atom {:alku nil :loppu nil}))

(defonce nakymassa? (atom false))
(defonce taso-historiakuva (atom false))

(defonce filtterit-muuttui?
         (reaction @hae-toimenpidepyynnot?
                   @hae-kyselyt?
                   @hae-tiedoitukset?
                   @hae-turvallisuuspoikkeamat?
                   @hae-tarkastukset?
                   @hae-havainnot?
                   @hae-onnettomuudet?
                   @hae-paikkaustyot?
                   @hae-paallystystyot?
                   @haettavat-toteumatyypit
                   @nav/valittu-hallintayksikko
                   @nav/valittu-urakka
                   @valittu-aikasuodatin
                   @lyhyen-suodattimen-asetukset
                   @pitkan-suodattimen-asetukset))

(def haetut-asiat (atom nil))

(defn oletusalue [asia]
  {:type        :circle
   :coordinates (:sijainti asia)
   :color       "green"
   :radius      5000
   :stroke      {:color "black" :width 10}})

(defmulti kartalla-xf :tyyppi)
(defmethod kartalla-xf :ilmoitus [ilmoitus]
  (assoc ilmoitus
    :type :ilmoitus
    :alue (oletusalue ilmoitus)))

(defmethod kartalla-xf :havainto [havainto]
  (assoc havainto
    :type :havainto
    :alue (oletusalue havainto)))

(defmethod kartalla-xf :tarkastus [tarkastus]
  (assoc tarkastus
    :type :tarkastus
    :alue (oletusalue tarkastus)))

(defmethod kartalla-xf :toteuma [toteuma]
  (assoc toteuma
    :type :toteuma
    :alue (oletusalue toteuma)))

(defmethod kartalla-xf :turvallisuuspoikkeama [tp]
  (assoc tp
    :type :turvallisuuspoikkeama
    :alue (oletusalue tp)))

(defmethod kartalla-xf :paallystystyo [pt]
  (assoc pt
    :type :paallystystyo
    :alue (oletusalue pt)))

(defmethod kartalla-xf :paikkaustyo [pt]
  (assoc pt
    :type :paikkaustyo
    :alue (oletusalue pt)))

(def historiakuvan-asiat-kartalla
  (reaction
    @haetut-asiat
    (when @taso-tilannekuva
      (into [] (map kartalla-xf) @haetut-asiat))))

(defn kasaa-parametrit []
  {:hallintayksikko @nav/valittu-hallintayksikko-id
   :urakka          (:id @nav/valittu-urakka)
   :alue            @nav/kartalla-nakyva-alue
   :alku            (if (= @valittu-aikasuodatin :lyhyt)
                      (t/minus (pvm/->pvm-aika (str (pvm/pvm (:pvm @lyhyen-suodattimen-asetukset))
                                                    (:kellonaika @lyhyen-suodattimen-asetukset)))
                               (t/hours (:plusmiinus @lyhyen-suodattimen-asetukset)))

                      (:alku @pitkan-suodattimen-asetukset))

   :loppu           (if (= @valittu-aikasuodatin :lyhyt)
                      (t/plus (pvm/->pvm-aika (str (pvm/pvm (:pvm @lyhyen-suodattimen-asetukset))
                                                   (:kellonaika @lyhyen-suodattimen-asetukset)))
                              (t/hours (:plusmiinus @lyhyen-suodattimen-asetukset)))

                      (:loppu @pitkan-suodattimen-asetukset))})

(defn hae-asiat []
  (go
    (let [yhdista (fn [& tulokset]
                    (concat (remove k/virhe? tulokset)))
          tulos (yhdista
                  #_(when @hae-toimenpidepyynnot? (<! (k/post! :hae-toimenpidepyynnot (kasaa-parametrit))))
                  #_(when @hae-tiedoitukset? (<! (k/post! :hae-tiedoitukset (kasaa-parametrit))))
                  #_(when @hae-kyselyt? (<! (k/post! :hae-kyselyt (kasaa-parametrit))))
                  #_(when @hae-turvallisuuspoikkeamat? (<! (k/post! :hae-turvallisuuspoikkeamat (kasaa-parametrit))))
                  #_(when @hae-tarkastukset? (<! (k/post! :hae-urakan-tarkastukset (kasaa-parametrit))))
                  #_(when @hae-onnettomuudet? (<! (k/post! :hae-urakan-onnettomuudet (kasaa-parametrit))))
                  #_(when @hae-havainnot? (<! (k/post! :hae-urakan-havainnot (kasaa-parametrit))))
                  #_(when @hae-paikkaustyot? (<! (k/post! :hae-paikkaustyot (kasaa-parametrit))))
                  #_(when @hae-paallystystyot? (<! (k/post! :hae-paallystystyot (kasaa-parametrit))))
                  #_(when (some (fn [_ k] k) @haettavat-toteumatyypit) (<! (k/post! :hae-kaikki-toteumat (kasaa-parametrit)))))]
      (reset! haetut-asiat tulos))))

;; Käytetään timeouttia (kerran) ja intervallia (toistuva)
;; Timeouttia käytetään siihen, että kun käyttäjä muuttaa suodattimia,
;; odotetaan jonkin aikaa kunnes tehdään ensimmäinen haku, ja sen jälkeen
;; jatketaan intervallilla, jolloin päivitysten aika on pidempi.
(def pollaus-id (atom nil))
(def pollauksen-aloitus-id (atom nil))
(def +sekuntti+ 1000)
(def +minuutti+ (* 60 +sekuntti+))
(def +intervalli+ (* 10 +sekuntti+))

(defn lopeta-pollaus
  []
  (when @pollaus-id
    (js/clearInterval @pollaus-id)
    (reset! pollaus-id nil)))

(defn peru-pollauksen-aloitus []
  (when @pollauksen-aloitus-id
    (js/clearTimeout @pollauksen-aloitus-id)
    (reset! pollauksen-aloitus-id nil)))

(defn tasoita-pollauksen-tahti []
  (reset! pollaus-id (js/setInterval hae-asiat +intervalli+)))

(defn aloita-pollaus
  []
  (when @pollauksen-aloitus-id (peru-pollauksen-aloitus))
  (when @pollaus-id (lopeta-pollaus))
  (reset! pollauksen-aloitus-id (js/setTimeout
                                  (do (hae-asiat) (tasoita-pollauksen-tahti))
                                  (* 2 +sekuntti+))))

(run! (if @nakymassa? (aloita-pollaus) (lopeta-pollaus)))
(run! (when @filtterit-muuttui? (aloita-pollaus)))