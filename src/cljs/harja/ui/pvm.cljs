(ns harja.ui.pvm
  "Päivämäärän valintakomponentti"
  (:require [reagent.core :refer [atom] :as r]
            [cljs-time.core :as t]
            [harja.pvm :as pvm]
            [harja.loki :refer [log]]
            [harja.ui.ikonit :as ikonit]
            [goog.events :as events]
            [goog.events.EventType :as EventType]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.komponentti :as komp]))

(def +paivat+ ["Ma" "Ti" "Ke" "To" "Pe" "La" "Su"])
(def +kuukaudet+ ["Tammi" "Helmi" "Maalis" "Huhti"
                  "Touko" "Kesä" "Heinä" "Elo"
                  "Syys" "Loka" "Marras" "Joulu"])

(defn selvita-kalenterin-suunta [kalenteri-komponentti sijainti-atom]
  (let [pvm-input-solmu (.-parentNode (r/dom-node kalenteri-komponentti))
        r (.getBoundingClientRect pvm-input-solmu)
        etaisyys-alareunaan (- @yleiset/korkeus (.-bottom r))
        etaisyys-oikeaan-reunaan (- @yleiset/leveys (.-right r))
        uusi-suunta (if (< etaisyys-alareunaan 250)
                      (if (< etaisyys-oikeaan-reunaan 100)
                        :ylos-vasen
                        :ylos-oikea)
                      :alas)]
    (reset! sijainti-atom uusi-suunta)))

(defn- pilko-viikoiksi [vuosi kk]
  ;;(.log js/console "vuosi: " vuosi ", kk: " kk)
  (let [kk (inc kk) ;; cljs-time käyttää luonnollisia kk numeroita
        kk-alku (t/date-time vuosi kk 1)
        ;;_ (log "kk-alku on: " kk-alku)
        viikon-alku (loop [pvm (t/date-time vuosi kk 1)]
                      (if (= 1 (t/day-of-week pvm))
                        pvm
                        (recur (t/minus pvm (t/days 1)))))
        kk-loppu (loop [pvm (t/date-time vuosi kk 28)]
                   (if (not= (t/month pvm) kk)
                     ;; mentiin yli seuraavaan kuukauten, palauta edellinen päivä
                     (t/minus pvm (t/days 1))

                     ;; vielä samassa kuussa, kelataan huomiseen
                     (recur (t/plus pvm (t/days 1)))))
        viikon-loppu (loop [pvm kk-loppu]
                       ;; Kelataan päivää sunnuntaihin asti eteenpäin, että saadaan täysi viikko
                       (if (= 7 (t/day-of-week pvm))
                         pvm
                         (recur (t/plus pvm (t/days 1)))))]
    (loop [paivat []
           p viikon-alku]
      (if (t/after? p viikon-loppu)
        (vec (partition 7 7 [] paivat))
        (recur (conj paivat p)
               (t/plus p (t/days 1)))))))


(defn pvm-valintakalenteri
  "Luo uuden päivämäärävalinnan.
Seuraavat optiot ovat mahdollisia:

:pvm      tämänhetkinen päivämäärä (goog.date.Date)
:vuosi    näytettävä vuosi, oletus nykyinen
:kuukausi näytettävä kuukausi (0 - 11)
:valitse  funktio, jota kutsutaan kun päivämäärä valitaan

  ...muita tarpeen mukaan..."
  [optiot]
  (let [sijainti-atom (atom nil)
        nyt (or (:pvm optiot) (t/now))
        nayta (atom [(.getYear nyt) (.getMonth nyt)])
        scroll-kuuntelija (fn [this _]
                            (selvita-kalenterin-suunta this sijainti-atom))]
    (komp/luo
     {:component-will-receive-props
      (fn [this & [_ optiot]]
        (when-let [pvm (:pvm optiot)]
          ;; päivitetään näytä vuosi ja kk
          (reset! nayta [(.getYear pvm) (.getMonth pvm)])))
      :component-did-mount
      (fn [this _]
        (selvita-kalenterin-suunta this sijainti-atom))}

     (komp/dom-kuuntelija js/window
                          EventType/SCROLL scroll-kuuntelija)
     
     (fn [{:keys [pvm valitse style] :as optiot}]
       (let [[vuosi kk] @nayta
             naytettava-kk (t/date-time vuosi (inc kk) 1)
             naytettava-kk-paiva? #(pvm/sama-kuukausi? naytettava-kk %)]
         [:table.pvm-valinta {:style (merge
                                      {:display (if @sijainti-atom "block" "none")}
                                      (case @sijainti-atom
                                        :oikea {:top 0 :left "100%"}
                                        :ylos-oikea {:bottom "100%" :left 0}
                                        :ylos-vasen {:bottom "100%" :right 0}
                                        {:top "100%" :left 0}))} ; Oletusarvo, alas
          [:tbody.pvm-kontrollit
           [:tr
            [:td.pvm-edellinen-kuukausi.klikattava
             {:on-click #(do (.preventDefault %)
                             (swap! nayta
                                    (fn [[vuosi kk]]
                                      (if (= kk 0)
                                        [(dec vuosi) 11]
                                        [vuosi (dec kk)])))
                             nil)}
             (ikonit/chevron-left)]
            [:td {:col-span 5} [:span.pvm-kuukausi (nth +kuukaudet+ kk)] " " [:span.pvm-vuosi vuosi]]
            [:td.pvm-seuraava-kuukausi.klikattava
             {:on-click #(do (.preventDefault %)
                             (swap! nayta
                                    (fn [[vuosi kk]]
                                      (if (= kk 11)
                                        [(inc vuosi) 0]
                                        [vuosi (inc kk)])))
                             nil)}
             (ikonit/chevron-right)]]
           [:tr.pvm-viikonpaivat
            (for [paiva +paivat+]
              ^{:key paiva}
              [:td paiva])]]

          [:tbody.pvm-paivat
           (for [paivat (pilko-viikoiksi vuosi kk)]
             ^{:key (pvm/millisekunteina (first paivat))}
             [:tr
              (for [paiva paivat]
                ^{:key (pvm/millisekunteina paiva)}
                [:td.pvm-paiva.klikattava {:class    (str
                                                      (when (and pvm
                                                                 (= (t/day paiva) (t/day pvm))
                                                                 (= (t/month paiva) (t/month pvm))
                                                                 (= (t/year paiva) (t/year pvm)))
                                                        "pvm-valittu ")
                                                      (if (naytettava-kk-paiva? paiva)
                                                        "pvm-naytettava-kk-paiva" "pvm-muu-kk-paiva"))

                                           :on-click #(do (.stopPropagation %) (valitse paiva) nil)}
                 (t/day paiva)])])]
          [:tbody.pvm-tanaan
           [:tr [:td {:colSpan 7}
                 [:a {:on-click #(do
                                   (.preventDefault %)
                                   (.stopPropagation %)
                                   (valitse (pvm/nyt)))}
                  "Tänään"]]]]])))))


