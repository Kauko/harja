(ns harja.views.urakka.paallystyskohteet
  "Päällystyskohteet"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.lomake :as lomake]
            [harja.ui.kentat :as kentat]
            [harja.ui.yleiset :refer [ajax-loader linkki raksiboksi
                                      livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]
            [harja.ui.liitteet :as liitteet]
            [harja.tiedot.urakka.paallystys :refer [paallystyskohderivit paallystys-tai-paikkauskohteet-nakymassa paivita-kohde!] :as paallystys]
            [harja.views.urakka.valinnat :as urakka-valinnat]
            [harja.domain.paallystys.pot :as paallystys-pot]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as tiedot-urakka]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.ui.napit :as napit]
            [clojure.string :as str]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<!]]
            [harja.tiedot.urakka :as u]

            [harja.tyokalut.vkm :as vkm]
            [harja.views.kartta :as kartta]
            [harja.geo :as geo]
            [harja.ui.tierekisteri :as tierekisteri])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))


(defn laske-sarakkeen-summa [sarake]
  (reduce + (mapv
              (fn [rivi] (sarake rivi))
              @paallystyskohderivit)))

(defn paallystyskohdeosa-virheet [tr-virheet]
  [:div.tr-virheet
   (for [virhe (into #{} (vals @tr-virheet))]
     ^{:key (hash virhe)}
     [:div.tr-virhe (ikonit/warning-sign)
      virhe])])

(defn paallystyskohdeosat [_]
  (let [tr-osoite (fn [rivi]
                    (let [arvot (map rivi [:tr_numero :tr_alkuosa :tr_alkuetaisyys :tr_loppuosa :tr_loppuetaisyys])]
                      (when (every? #(not (str/blank? %)) arvot)
                        ;; Tierekisteriosoite on täytetty (ei tyhjiä kenttiä)
                        (zipmap [:numero :alkuosa :alkuetaisyys :loppuosa :loppuetaisyys]
                                arvot))))

        ;; onnistuneesti haetut TR-sijainnit
        tr-sijainnit (atom {})

        ;; virheelliset TR sijainnit 
        tr-virheet (atom {})

        resetoi-tr-tiedot (fn []
                          (reset! tr-sijainnit {})
                          (reset! tr-virheet {}))]
    (komp/luo
      (komp/ulos #(kartta/poista-popup!))
      (fn [{:keys [kohdeosat id] :as rivi}]
        [:div
         [grid/grid
          {:otsikko       "Tierekisterikohteet"
           :tyhja         (if (empty? kohdeosat) "Tierekisterikohteita ei löydy")
           :rivi-klikattu (fn [rivi]
                            (log "KLIKKASIT: " (pr-str rivi))
                            (when-let [viiva (some-> rivi :sijainti)]
                              (nav/vaihda-kartan-koko! :L)
                              (kartta/keskita-kartta-alueeseen! (geo/extent viiva))))
           :tallenna      #(go (let [urakka-id (:id @nav/valittu-urakka)
                                     [sopimus-id _] @u/valittu-sopimusnumero
                                     sijainnit @tr-sijainnit
                                     osat (into []
                                                (map (fn [osa]
                                                       (log "OSA: " (pr-str osa) " => SIJAINTI: " (pr-str (sijainnit (tr-osoite osa))))
                                                       (assoc osa :sijainti (sijainnit (tr-osoite osa)))))
                                                %)
                                     vastaus (<! (paallystys/tallenna-paallystyskohdeosat urakka-id sopimus-id (:id rivi) osat))]
                                 (log "PÄÄ päällystyskohdeosat tallennettu: " (pr-str vastaus))
                                 (resetoi-tr-tiedot)
                                 (paivita-kohde! id assoc :kohdeosat vastaus)))
           :luokat        ["paallystyskohdeosat-haitari"]
           :peruuta       #(resetoi-tr-tiedot)
           :muutos        (fn [g]
                            (log "VIRHEET:" (pr-str (grid/hae-virheet g)))
                            (let [haetut (into #{} (keys @tr-sijainnit))]
                              ;; jos on tullut uusi TR osoite, haetaan sille sijainti
                              (doseq [[id rivi] (grid/hae-muokkaustila g)]
                                (if (:poistettu rivi)
                                  (swap! tr-virheet dissoc id)
                                  (let [osoite (tr-osoite rivi)]
                                    (when (and osoite (not (haetut osoite)))
                                      (go
                                        (log "Haetaan TR osoitteen sijainti: " (pr-str osoite))
                                        (let [sijainti (<! (vkm/tieosoite->viiva osoite))]
                                          (log "SIJAINTI: " (pr-str sijainti))
                                          (when (= (get (grid/hae-muokkaustila g) id) rivi) ;; ettei rivi ole uudestaan muuttunut
                                            (if-let [virhe (and (vkm/virhe? sijainti)
                                                                "Virheellinen TR-osoite")]
                                              (do (swap! tr-virheet assoc id virhe)
                                                  (doseq [kentta [:tr_numero :tr_alkuosa :tr_alkuetaisyys :tr_loppuosa :tr_loppuetaisyys]]
                                                    (grid/aseta-virhe! g id kentta "Tarkista tie")))
                                              (do (swap! tr-virheet dissoc id)
                                                  (doseq [kentta [:tr_numero :tr_alkuosa :tr_alkuetaisyys :tr_loppuosa :tr_loppuetaisyys]]
                                                    (grid/poista-virhe! g id kentta))
                                                  (log "sain sijainnin " (clj->js sijainti))
                                                  (swap! tr-sijainnit assoc osoite sijainti))))))))))))}
          [{:otsikko "Nimi" :nimi :nimi :tyyppi :string :leveys "20%" :validoi [[:ei-tyhja "Anna nimi"]]}
           {:otsikko "Tienumero" :nimi :tr_numero :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Anna tienumero"]]}
           {:otsikko "Aosa" :nimi :tr_alkuosa :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Anna alkuosa"]]}
           {:otsikko "Aet" :nimi :tr_alkuetaisyys :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Anna alkuetäisyys"]]}
           {:otsikko "Losa" :nimi :tr_loppuosa :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Anna loppuosa"]]}
           {:otsikko "Let" :nimi :tr_loppuetaisyys :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Anna loppuetäisyys"]]}
           {:otsikko "Pit" :nimi :pit :muokattava? (constantly false) :tyyppi :string
            :hae     (fn [rivi]
                       (str (tierekisteri/laske-tien-pituus {:aet (:tr_alkuetaisyys rivi)
                                                             :let (:tr_loppuetaisyys rivi)})))
            :leveys  "10%"}
           {:otsikko "Kvl" :nimi :kvl :tyyppi :numero :leveys "10%" :validoi [[:ei-tyhja "Anna kvl"]]}
           {:otsikko       "Nykyinen päällyste"
            :nimi          :nykyinen_paallyste
            :fmt           #(paallystys-pot/hae-paallyste-koodilla %)
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinnat      paallystys-pot/+paallystetyypit+
            :validoi       [[:ei-tyhja "Anna päällystetyyppi"]]
            :valinta-nayta :nimi
            :leveys        "20%"}
           {:otsikko "Toimenpide" :nimi :toimenpide :tyyppi :string :leveys "20%" :validoi [[:ei-tyhja "Anna toimenpide"]]}]
          kohdeosat]

         [paallystyskohdeosa-virheet tr-virheet]]))))

(defn paallystyskohteet []
  (let [paallystyskohteet (reaction (let [kohteet @paallystyskohderivit]
                                      (filter #(false? (:muu_tyo %))
                                              kohteet)))
        muut-tyot (reaction (let [kohteet @paallystyskohderivit]
                              (filter #(true? (:muu_tyo %)) kohteet)))
        yhteensa (reaction (let [_ @paallystyskohderivit
                                 sopimuksen-mukaiset-tyot-yhteensa (laske-sarakkeen-summa :sopimuksen_mukaiset_tyot)
                                 toteutunut-hinta-yhteensa (laske-sarakkeen-summa :toteutunut_hinta)
                                 muutoshinta-yhteensa (laske-sarakkeen-summa :muutoshinta)
                                 arvonvahennykset-yhteensa (laske-sarakkeen-summa :arvonvahennykset)
                                 bitumi-indeksi-yhteensa (laske-sarakkeen-summa :bitumi_indeksi)
                                 kaasuindeksi-yhteensa (laske-sarakkeen-summa :kaasuindeksi)
                                 kokonaishinta (+ sopimuksen-mukaiset-tyot-yhteensa
                                                  toteutunut-hinta-yhteensa
                                                  muutoshinta-yhteensa
                                                  arvonvahennykset-yhteensa
                                                  bitumi-indeksi-yhteensa
                                                  kaasuindeksi-yhteensa)]
                             [{:id                       0
                               :sopimuksen_mukaiset_tyot sopimuksen-mukaiset-tyot-yhteensa
                               :muutoshinta              muutoshinta-yhteensa
                               :toteutunut_hinta         toteutunut-hinta-yhteensa
                               :arvonvahennykset         arvonvahennykset-yhteensa
                               :bitumi_indeksi           bitumi-indeksi-yhteensa
                               :kaasuindeksi             kaasuindeksi-yhteensa
                               :kokonaishinta            kokonaishinta}]))]

    (komp/luo
      (komp/ulos #(kartta/poista-popup!))
      (komp/lippu paallystys-tai-paikkauskohteet-nakymassa)
      (fn []
        (let [haitari-leveys 5
              id-leveys 10
              kohde-leveys 20
              tarjoushinta-leveys 10
              muutoshinta-leveys 10
              toteutunut-hinta-leveys 10
              arvonvahennykset-leveys 10
              bitumi-indeksi-leveys 10
              kaasuindeksi-leveys 10
              yhteensa-leveys 15
              paallystysnakyma? (= :paallystys (:tyyppi @nav/valittu-urakka))]
          [:div.paallystyskohteet
           [kartta/kartan-paikka]
           [:h3 "Päällystyskohteet"]
           [grid/grid
            {:otsikko                  "Kohteet"
             :tyhja                    (if (nil? @paallystyskohderivit) [ajax-loader "Haetaan kohteita..."] "Ei kohteita")
             :vetolaatikot             (into {} (map (juxt :id (fn [rivi] [paallystyskohdeosat rivi])) @paallystyskohderivit))
             :tallenna                 #(go (let [urakka-id (:id @nav/valittu-urakka)
                                                  [sopimus-id _] @u/valittu-sopimusnumero
                                                  payload (mapv (fn [rivi] (assoc rivi :muu_tyo false)) %)
                                                  _ (log "PÄÄ Lähetetään päällystyskohteet: " (pr-str payload))
                                                  vastaus (<! (paallystys/tallenna-paallystyskohteet urakka-id sopimus-id payload))]
                                              (log "PÄÄ päällystyskohteet tallennettu: " (pr-str vastaus))
                                              (reset! paallystyskohderivit vastaus)))
             :esta-poistaminen?        (fn [rivi] (or (not (nil? (:paallystysilmoitus_id rivi)))
                                                      (not (nil? (:paikkausilmoitus_id rivi)))))
             :esta-poistaminen-tooltip (fn [rivi] "Kohteelle on kirjattu ilmoitus, kohdetta ei voi poistaa.")}
            [{:tyyppi :vetolaatikon-tila :leveys haitari-leveys}
             {:otsikko     "YHA-ID" :nimi :kohdenumero :tyyppi :string :leveys id-leveys
              :validoi     [[:ei-tyhja "Anna kohdenumero"] [:uniikki "Sama kohdenumero voi esiintyä vain kerran."]]
              :muokattava? (fn [rivi] (true? (and (:id rivi) (neg? (:id rivi)))))}
             {:otsikko "Kohde" :nimi :nimi :tyyppi :string :leveys kohde-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             (when paallystysnakyma?
               {:otsikko "Tarjous\u00ADhinta" :nimi :sopimuksen_mukaiset_tyot :fmt fmt/euro-opt :tyyppi :numero :leveys tarjoushinta-leveys :validoi [[:ei-tyhja "Anna arvo"]]})
             (when paallystysnakyma?
               {:otsikko "Muutok\u00ADset" :nimi :muutoshinta :muokattava? (constantly false) :fmt fmt/euro-opt :tyyppi :numero :leveys muutoshinta-leveys})
             (when-not paallystysnakyma?
               {:otsikko "Toteutunut hinta" :nimi :toteutunut_hinta :muokattava? (constantly false) :fmt fmt/euro-opt :tyyppi :numero :leveys toteutunut-hinta-leveys})
             {:otsikko "Arvon\u00ADväh." :nimi :arvonvahennykset :fmt fmt/euro-opt :tyyppi :numero :leveys arvonvahennykset-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Bitumi\u00ADindeksi" :nimi :bitumi_indeksi :fmt fmt/euro-opt :tyyppi :numero :leveys bitumi-indeksi-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Kaasu\u00ADindeksi" :nimi :kaasuindeksi :fmt fmt/euro-opt :tyyppi :numero :leveys kaasuindeksi-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Kokonais\u00ADhinta (indeksit mukana)" :muokattava? (constantly false) :nimi :kokonaishinta :fmt fmt/euro-opt :tyyppi :numero :leveys yhteensa-leveys
              :hae     (fn [rivi] (+ (:sopimuksen_mukaiset_tyot rivi)
                                     (:muutoshinta rivi)
                                     (:toteutunut_hinta rivi)
                                     (:arvonvahennykset rivi)
                                     (:bitumi_indeksi rivi)
                                     (:kaasuindeksi rivi)))}]
            @paallystyskohteet]

           [grid/grid
            {:otsikko                  "Muut kohteet"       ; NOTE: Muut kohteet ovat alkuperäiseen sopimukseen kuulumattomia töitä.
             :tyhja                    (if (nil? @muut-tyot) [ajax-loader "Haetaan muita töitä..."] "Ei muita töitä")
             :vetolaatikot             (into {} (map (juxt :id (fn [rivi] [paallystyskohdeosat rivi])) @paallystyskohderivit))
             :tallenna                 #(go (let [urakka-id (:id @nav/valittu-urakka)
                                                  [sopimus-id _] @u/valittu-sopimusnumero
                                                  payload (mapv (fn [rivi] (assoc rivi :muu_tyo true)) %)
                                                  _ (log "PÄÄ Lähetetään päällystyskohteet: " (pr-str payload))
                                                  vastaus (<! (paallystys/tallenna-paallystyskohteet urakka-id sopimus-id payload))]
                                              (log "PÄÄ päällystyskohteet tallennettu: " (pr-str vastaus))
                                              (reset! paallystyskohderivit vastaus)))
             :esta-poistaminen?        (fn [rivi] (or (not (nil? (:paallystysilmoitus_id rivi)))
                                                      (not (nil? (:paikkausilmoitus_id rivi)))))
             :esta-poistaminen-tooltip (fn [rivi] "Kohteelle on kirjattu ilmoitus, kohdetta ei voi poistaa.")}
            [{:tyyppi :vetolaatikon-tila :leveys haitari-leveys}
             {:otsikko "Harja-ID" :nimi :kohdenumero :tyyppi :string :leveys id-leveys  :validoi [[:ei-tyhja "Anna kohdenumero"] [:uniikki "Sama kohdenumero voi esiintyä vain kerran."]]}
             {:otsikko "Kohde" :nimi :nimi :tyyppi :string :leveys kohde-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             (when paallystysnakyma?
               {:otsikko "Tarjous\u00ADhinta" :nimi :sopimuksen_mukaiset_tyot :fmt fmt/euro-opt :tyyppi :numero :leveys tarjoushinta-leveys :validoi [[:ei-tyhja "Anna arvo"]]})
             (when paallystysnakyma?
               {:otsikko "Muutok\u00ADset" :nimi :muutoshinta :muokattava? (constantly false) :fmt fmt/euro-opt :tyyppi :numero :leveys muutoshinta-leveys})
             (when-not paallystysnakyma?
               {:otsikko "Toteutunut hinta" :nimi :toteutunut_hinta :fmt fmt/euro-opt :muokattava? (constantly false) :tyyppi :numero :leveys toteutunut-hinta-leveys})
             {:otsikko "Arvon\u00ADväh." :nimi :arvonvahennykset :fmt fmt/euro-opt :tyyppi :numero :leveys arvonvahennykset-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Bitumi\u00ADindeksi" :nimi :bitumi_indeksi :fmt fmt/euro-opt :tyyppi :numero :leveys bitumi-indeksi-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Kaasu\u00ADindeksi" :nimi :kaasuindeksi :fmt fmt/euro-opt :tyyppi :numero :leveys kaasuindeksi-leveys :validoi [[:ei-tyhja "Anna arvo"]]}
             {:otsikko "Kokonais\u00ADhinta (indeksit mukana)" :muokattava? (constantly false) :nimi :kokonaishinta :fmt fmt/euro-opt :tyyppi :numero :leveys yhteensa-leveys
              :hae     (fn [rivi] (+ (:sopimuksen_mukaiset_tyot rivi)
                                     (:muutoshinta rivi)
                                     (:arvonvahennykset rivi)
                                     (:bitumi_indeksi rivi)
                                     (:kaasuindeksi rivi)))}]
            @muut-tyot]

           [grid/grid
            {:otsikko "Yhteensä"
             :tyhja   (if (nil? {}) [ajax-loader "Lasketaan..."] "")}
            [{:otsikko "" :nimi :tyhja :tyyppi :string :leveys haitari-leveys}
             {:otsikko "" :nimi :kohdenumero :tyyppi :string :leveys id-leveys}
             {:otsikko "" :nimi :nimi :tyyppi :string :leveys kohde-leveys}
             (when paallystysnakyma?
               {:otsikko "Tarjous\u00ADhinta" :nimi :sopimuksen_mukaiset_tyot :fmt fmt/euro-opt :tyyppi :numero :leveys tarjoushinta-leveys})
             (when paallystysnakyma?
               {:otsikko "Muutok\u00ADset" :nimi :muutoshinta :fmt fmt/euro-opt :tyyppi :numero :leveys muutoshinta-leveys})
             (when-not paallystysnakyma?
               {:otsikko "Toteutunut hinta" :nimi :toteutunut_hinta :fmt fmt/euro-opt :tyyppi :numero :leveys toteutunut-hinta-leveys})
             {:otsikko "Arvon\u00ADväh." :nimi :arvonvahennykset :fmt fmt/euro-opt :tyyppi :numero :leveys arvonvahennykset-leveys}
             {:otsikko "Bitumi\u00ADindeksi" :nimi :bitumi_indeksi :fmt fmt/euro-opt :tyyppi :numero :leveys bitumi-indeksi-leveys}
             {:otsikko "Kaasu\u00ADindeksi" :nimi :kaasuindeksi :fmt fmt/euro-opt :tyyppi :numero :leveys kaasuindeksi-leveys}
             {:otsikko "Kokonais\u00ADhinta (indeksit mukana)" :nimi :kokonaishinta :fmt fmt/euro-opt :tyyppi :numero :leveys yhteensa-leveys}
             {:otsikko "" :nimi :muokkaustoiminnot-tyhja :tyyppi :string :leveys 3}]
            @yhteensa]])))))
