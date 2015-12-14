(ns harja.views.murupolku
  "Murupolku on sovelluksenlaajuinen navigaatiokomponentti.
  Sen avulla voidaan vaikuttaa sovelluksen tilaan muun muassa
  seuraavia parametrejä käyttäen: väylämuoto, hallintayksikkö,
  urakka, urakan tyyppi, urakoitsija."
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? alasveto-ei-loydoksia livi-pudotusvalikko]]

            [harja.loki :refer [log]]
            [harja.tiedot.urakoitsijat :as urakoitsijat]
            [harja.tiedot.hallintayksikot :as hal]
            [harja.tiedot.navigaatio :as nav]))

(defn koko-maa []
  [:li
   [:a.murupolkuteksti {:href     "#"
                        :style    (when (nil? @nav/valittu-hallintayksikko)
                                    {:text-decoration "none"
                                     :color           "#323232"})
                        :on-click #(do
                                    (.preventDefault %)
                                    (nav/valitse-hallintayksikko nil))}
    "Koko maa"]])

(defn urakoitsija []
  [:div [:span.livi-valikkonimio.urakoitsija-otsikko "Urakoitsija"]
   [livi-pudotusvalikko {:valinta    @nav/valittu-urakoitsija
                         :format-fn  #(if % (:nimi %) "Kaikki")
                         :valitse-fn nav/valitse-urakoitsija!
                         :class      (str "alasveto-urakoitsija" (when (boolean @nav/valittu-urakka) " disabled"))
                         :disabled   (boolean @nav/valittu-urakka)}
    (vec (conj (into [] (case (:arvo @nav/valittu-urakkatyyppi)
                          :hoito @urakoitsijat/urakoitsijat-hoito
                          :paallystys @urakoitsijat/urakoitsijat-paallystys
                          :tiemerkinta @urakoitsijat/urakoitsijat-tiemerkinta
                          :valaistus @urakoitsijat/urakoitsijat-valaistus

                          @urakoitsijat/urakoitsijat-hoito)) ;;defaulttina hoito
               nil))]])

(defn urakkatyyppi []
  [:div [:span.livi-valikkonimio.urakoitsija-otsikko "Urakkatyyppi"]
   [livi-pudotusvalikko {:valinta    @nav/valittu-urakkatyyppi
                         :format-fn  #(if % (:nimi %) "Kaikki")
                         :valitse-fn nav/vaihda-urakkatyyppi!
                         :class      (str "alasveto-urakkatyyppi" (when (boolean @nav/valittu-urakka) " disabled"))
                         :disabled   (boolean @nav/valittu-urakka)}
    nav/+urakkatyypit+]])

(defn murupolku
  "Itse murupolkukomponentti joka sisältää html:n"
  []
  (kuuntelija
    {:valinta-auki (atom nil)}                              ;; nil | :hallintayksikko | :urakka


    (fn []
      (let [valinta-auki (:valinta-auki (reagent/state (reagent/current-component)))]
        [:span {:class (when (empty? @nav/tarvitsen-isoa-karttaa)
                         (cond
                           (= @nav/sivu :hallinta) "hide"
                           (= @nav/sivu :about) "hide"
                           :default ""))}
         [:ol.murupolku
          [koko-maa]
          (let [valittu @nav/valittu-hallintayksikko]
            [:li.dropdown.livi-alasveto {:class (when (= :hallintayksikko @valinta-auki) "open")}

             (let [vu @nav/valittu-urakka
                   va @valinta-auki]
               (if (or (not (nil? vu)) (= va :hallintayksikko))
                 [:a.murupolkuteksti {:href     "#"
                                      :on-click #(do
                                                  (.preventDefault %)
                                                  (nav/valitse-hallintayksikko valittu))}
                  (str (or (:nimi valittu) "- Hallintayksikkö -") " ")]

                 [:span.valittu-hallintayksikko.murupolkuteksti (or (:nimi valittu) "- Hallintayksikkö -") " "]))

             [:button.nappi-murupolkualasveto.dropdown-toggle {:on-click #(swap! valinta-auki
                                                                                 (fn [v]
                                                                                   (if (= v :hallintayksikko)
                                                                                     nil
                                                                                     :hallintayksikko)))}
              [:span.livicon-chevron-down]]

             ;; Alasvetovalikko yksikön nopeaa vaihtamista varten
             [:ul.dropdown-menu.livi-alasvetolista {:role "menu"}
              (for [muu-yksikko (filter #(not= % valittu) @hal/hallintayksikot)]
                ^{:key (str "hy-" (:id muu-yksikko))}
                [:li.harja-alasvetolistaitemi
                 [linkki (:nimi muu-yksikko)
                  #(do (reset! valinta-auki nil)
                       (nav/valitse-hallintayksikko muu-yksikko))]])]])

          (when @nav/valittu-hallintayksikko
            (let [valittu @nav/valittu-urakka]
              [:li.dropdown.livi-alasveto {:class (when (= :urakka @valinta-auki) "open")}
               [:span.valittu-urakka.murupolkuteksti (or (:nimi valittu) "- Urakka -") " "]

               [:button.nappi-murupolkualasveto.dropdown-toggle {:on-click #(swap! valinta-auki
                                                                                   (fn [v]
                                                                                     (if (= v :urakka)
                                                                                       nil
                                                                                       :urakka)))}
                [:span.livicon-chevron-down]]

               ;; Alasvetovalikko urakan nopeaa vaihtamista varten
               [:ul.dropdown-menu.livi-alasvetolista {:role "menu"}

                (let [muut-urakat (filter #(not= % valittu) @nav/suodatettu-urakkalista)]

                  (if (empty? muut-urakat)
                    [alasveto-ei-loydoksia "Tästä hallintayksiköstä ei löydy muita urakoita valituilla hakukriteereillä."]

                    (for [muu-urakka muut-urakat]
                      ^{:key (str "ur-" (:id muu-urakka))}
                      [:li.harja-alasvetolistaitemi [linkki (:nimi muu-urakka) #(nav/valitse-urakka muu-urakka)]])))]]))

          [:span.pull-right.murupolku-suotimet
           [urakoitsija]
           [urakkatyyppi]]]]))

    ;; Jos hallintayksikkö tai urakka valitaan, piilota dropdown
    [:hallintayksikko-valittu :hallintayksikkovalinta-poistettu :urakka-valittu :urakkavalinta-poistettu]
    #(reset! (-> % reagent/state :valinta-auki) nil)

    ;; Jos klikataan komponentin ulkopuolelle, vaihdetaan piilotetaan valintalistat
    :body-klikkaus
    (fn [this {klikkaus :tapahtuma}]
      (when-not (sisalla? this klikkaus)
        (let [valinta-auki (:valinta-auki (reagent/state this))]
          (reset! valinta-auki false))))))