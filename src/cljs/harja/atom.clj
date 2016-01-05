(ns harja.atom
  "Apureita atomeille."
  (:refer-clojure :exclude [run!])
  (:require [reagent.ratom :refer [run!]]))


(defmacro reaction<!
  "Asynkroninen reaktio (esim. serveriltä haku). Haun tulee palauttaa kanava (tai nil).
  Kun kanava palauttaa dataa, se asetetaan atomin tilaksi. Reaktio laukaistaan uudelleen
  ennen kuin kanavasta on luettu, hylätään arvo. Jos haku palauttaa nil, asetetaan atomin arvoksi nil.

  Ottaa sisään let-bindings vektorin, [arvo1 @riippuvuusatomi1 ...] jossa kaikki atomit, joihin
  reaktion halutaan riippuvan, tulee määritellä. Arvoja voi käyttää bodyssa hyödyksi.
  Reaktiossa on oletuksena 20ms odotus, eli ennen kuin reaktio ajetaan, odotetaan 20ms jos riippuvuudet
  muuttuvat uudelleen. Kun riippuvuudet eivät ole muuttuneet odotusaikana, ajetaan reaktio 
  viimeisimmillä arvoilla.

  Bodyn alkuun voi laittaa optionaalisen asetukset mäpin, jossa voi olla seuraavat avaimet:
  :odota   millisekuntimäärä parametrien arvojen muuttumisen odottamiselle (oletus 20ms)

  Reaktion tulee palauttaa kanava, josta tuloksen voi lukea."
  [let-bindings & body]
  (let [asetukset (when (map? (first body))
                    (first body))
        body (if asetukset
               (rest body)
               body)
        bindings (mapv vec (partition 2 let-bindings))
        nimi-symbolit (vec (take (count bindings) (repeatedly #(gensym "ARG"))))
        nimet (mapv first bindings)]
    `(let [odota# ~(or (get asetukset :odota) 20)
           arvo# (reagent.core/atom nil)
           parametrit-ch# (cljs.core.async/chan)
           paivita# (reagent.core/atom 0)]
       (cljs.core.async.macros/go
         (loop [haku-ch# nil ;; käynnissä olevan haun kanava
                parametrit# (cljs.core.async/<! parametrit-ch#)]

           ;; Jos parametrit on, katsotaan muuttuvatko ne kurista ajan sisällä
           (let [timeout-ch# (when parametrit# (cljs.core.async/timeout odota#))
                 kanavat# (if timeout-ch#
                            [parametrit-ch# timeout-ch#]
                            [parametrit-ch#])
                 kanavat# (if haku-ch#
                            (conj kanavat# haku-ch#)
                            kanavat#)
                 [vastaus# kanava#] (cljs.core.async/alts! kanavat#)]

             (if (= kanava# parametrit-ch#)
               ;; Saatiin parametrit, loopataan uudelleen
               (recur haku-ch# vastaus#)

               (let [[haku# parametrit#]
                     (if (= kanava# haku-ch#)
                       ;; Serveri vastaus tuli läpi => asetetaan tila
                       (do (reset! arvo# vastaus#)
                           [nil parametrit#])
                       
                       ;; Timeout, tehdään kutsu
                       (if (= kanava# timeout-ch#)
                         (let [[~@nimet] parametrit#
                               chan# (do ~@body)]
                           (if (nil? chan#)
                             (do (reset! arvo# nil)
                                 [nil (cljs.core.async/<! parametrit-ch#)])
                             [chan# nil]))
                         (harja.loki/log "tuntematon kanava!!!")))]
                 (recur haku# parametrit#))))))
       

       (reagent.ratom/run!
        (let [~@(interleave nimi-symbolit (map second bindings))]
          @paivita#
          (cljs.core.async.macros/go
            (cljs.core.async/>! parametrit-ch#
                                [~@nimi-symbolit]))))

       (swap! harja.atom/+reaktiot+ assoc arvo# {:paivita #(swap! paivita# inc)})
       arvo#)))
           
            
