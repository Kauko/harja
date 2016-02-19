(ns harja.views.urakka.toteumat.materiaalit
  (:require [harja.views.urakka.valinnat :as valinnat]
            [reagent.core :refer [atom wrap]]
            [harja.loki :refer [log]]
            [harja.ui.lomake :refer [lomake] :as lomake]
            [harja.ui.yleiset :refer [ajax-loader]]
            [harja.pvm :as pvm]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.komponentti :as komp]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.urakka :as u]
            [harja.ui.grid :as grid]
            [harja.ui.napit :as napit]

            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.suunnittelu.materiaalit :as materiaali-tiedot]

            [cljs.core.async :refer [<!]]
            [harja.views.kartta :as kartta])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defonce valittu-materiaalin-kaytto (atom nil))

(defonce urakan-materiaalin-kaytot
  (reaction<! [nakymassa? @materiaali-tiedot/materiaalinakymassa?
                      sopimusnumero (first @u/valittu-sopimusnumero)
                      [alku loppu] @u/valittu-hoitokausi
               ur @nav/valittu-urakka]
              {:nil-kun-haku-kaynnissa? true}
              (when (and nakymassa? sopimusnumero alku loppu ur)
                (materiaali-tiedot/hae-urakassa-kaytetyt-materiaalit (:id ur)
                                                                     alku
                                                                     loppu
                                                                     sopimusnumero))))

(defn tallenna-toteuma-ja-toteumamateriaalit!
  [tm m]
  (let [toteumamateriaalit (into []
                                 (comp
                                   (map #(assoc % :materiaalikoodi (:id (:materiaali %))))
                                   (map #(dissoc % :materiaali))
                                   (map #(assoc % :maara (if (string? (:maara %))
                                                           (js/parseInt (:maara %) 10)
                                                           (:maara %))))
                                   (map #(assoc % :id (:tmid %)))
                                   (map #(dissoc % :tmid))
                                   (map #(assoc % :toteuma (:id m))))
                                 tm)
        toteuma {:id                  (:id m) :urakka (:id @nav/valittu-urakka)
                 :alkanut             (:alkanut m) :paattynyt (:paattynyt m)
                 :sopimus             (first @u/valittu-sopimusnumero)
                 :tyyppi              "materiaali"
                 :suorittajan-nimi    (:suorittaja m)
                 :suorittajan-ytunnus (:ytunnus m)
                 :lisatieto           (:lisatieto m)}
        hoitokausi @u/valittu-hoitokausi
        sopimus-id (first @u/valittu-sopimusnumero)]
    (toteumat/tallenna-toteuma-ja-toteumamateriaalit! toteuma toteumamateriaalit hoitokausi sopimus-id)))

(def materiaalikoodit (reaction (into []
                                      (comp
                                        (map #(dissoc % :urakkatyyppi))
                                        (map #(dissoc % :kohdistettava)))
                                      @(materiaali-tiedot/hae-materiaalikoodit))))

(defn hae-tiedot-vetolaatikkoon
  [urakan-id materiaali-id]
  (let [hoitokausi @u/valittu-hoitokausi
        sopimusnumero (first @u/valittu-sopimusnumero)]
    (materiaali-tiedot/hae-toteumat-materiaalille
      urakan-id
      materiaali-id
      hoitokausi
      sopimusnumero)))

(defn tallenna-toteuma-materiaaleja
  [urakka atomi]
  "Tätä funktiota käytetään, kun materiaalitoteuman tietoja muutetaan suoraan pääsivulla,
  kun vetolaatikko on aukaistu. Parametrina saatava atomi sisältää vetolaatikossa näytettävät tiedot,
  ja se päivitetään kun tallennus on saatu tehtyä."
  (fn [materiaalit]
    (let [toteumamateriaalit (into []
                                   (comp
                                     (map #(assoc % :materiaalikoodi (:id (:materiaali %))))
                                     (map #(dissoc % :materiaali))
                                     (map #(assoc % :maara (if (string? (:maara (:toteuma %)))
                                                             (js/parseInt (:maara (:toteuma %)) 10)
                                                             (:maara (:toteuma %)))))
                                     (map #(assoc % :toteuma (:id %)))
                                     (map #(assoc % :id (:tmid %)))
                                     (map #(dissoc % :tmid)))
                                   materiaalit)]
      (go (let [tulos (<! (materiaali-tiedot/tallenna-toteuma-materiaaleja urakka
                                                                           toteumamateriaalit
                                                                           @u/valittu-hoitokausi
                                                                           (first @u/valittu-sopimusnumero)))]
            (reset! urakan-materiaalin-kaytot tulos)
            (reset!
              atomi
              (remove :poistettu
                      (sort-by
                        #(:alkanut (:toteuma %))
                        pvm/ennen?
                        (concat
                          (filter
                            (fn [kartta]
                              (nil? (some (fn [uusi-id] (= (:tmid kartta) uusi-id)) (map :tmid materiaalit))))
                            @atomi)
                          materiaalit)))))))))

(defn materiaalit-ja-maarat
  [materiaalit-atom virheet-atom koneen-lisaama?]

  [grid/muokkaus-grid
   {:tyhja        "Ei materiaaleja."
    :muutos       (fn [g] (reset! virheet-atom (grid/hae-virheet g)))
    :voi-muokata? (not koneen-lisaama?)}
   [{:otsikko       "Materiaali" :nimi :materiaali :tyyppi :valinta
     :valinnat      @materiaalikoodit
     :valinta-nayta #(if % (:nimi %) "- valitse materiaali -")
     :validoi       [[:ei-tyhja "Valitse materiaali."]]
     :leveys        "50%"}

    {:otsikko "Määrä" :nimi :maara :tyyppi :positiivinen-numero :leveys "40%"}
    {:otsikko "Yks." :muokattava? (constantly false) :nimi :yksikko :hae (comp :yksikko :materiaali) :leveys "5%"}]
   materiaalit-atom])

(defn materiaalit-tiedot
  [ur]
  "Valitun toteuman tietojen näkymä"
  [ur]
  (let [;; Organisaatiotiedot voidaan esitäyttää - nämä ylikirjoitetaan jos kyseessä on olemassaoleva toteuma
        tiedot (atom {:suorittaja (:nimi @u/urakan-organisaatio)
                      :ytunnus (:ytunnus @u/urakan-organisaatio)})
        vanha-toteuma? (if (:id @valittu-materiaalin-kaytto) true false)]

    (komp/luo
      {:component-will-mount
       (fn [_]
         (when (:id @valittu-materiaalin-kaytto)
           (go
             (reset! tiedot
                     (<! (materiaali-tiedot/hae-toteuman-materiaalitiedot (:id ur) (:id @valittu-materiaalin-kaytto)))))))}

      (fn [ur]
        (let [muokkaa! #(do (log "MATERIAALI: " (pr-str %)) (reset! tiedot %))
              materiaalitoteumat-mapissa (wrap (into {}
                                                     (map (juxt :tmid identity))
                                                     (:toteumamateriaalit @tiedot))
                                               (fn [rivit]
                                                 (swap! tiedot
                                                        assoc :toteumamateriaalit 
                                                        (keep
                                                         (fn [[id rivi]]
                                                           (when (not (and (neg? id)
                                                                           (:poistettu rivi)))
                                                             (assoc rivi :tmid id)))
                                                         rivit))))
              
              materiaalien-virheet (wrap (::materiaalivirheet @tiedot)
                                         #(swap! tiedot assoc ::materiaalivirheet %))
              muokattava-pred (constantly (not (:jarjestelmanlisaama tiedot)))
              tiedot @tiedot
              voi-tallentaa? (and (lomake/validi? tiedot)
                                  (> (count @materiaalitoteumat-mapissa) 0)
                                  (zero? (count @materiaalien-virheet)))]
          [:div.toteuman-tiedot
           [napit/takaisin "Takaisin materiaaliluetteloon" #(reset! valittu-materiaalin-kaytto nil)]
           [lomake {:otsikko (if vanha-toteuma?
                               "Muokkaa toteumaa"
                               "Luo uusi toteuma")
                    :luokka   :horizontal
                    :muokkaa! muokkaa!
                    :footer   [napit/palvelinkutsu-nappi
                               "Tallenna toteuma"
                               #(tallenna-toteuma-ja-toteumamateriaalit!
                                 (:toteumamateriaalit tiedot)
                                 tiedot)
                               {:luokka   "nappi-ensisijainen"
                                :ikoni    (ikonit/tallenna)
                                :kun-onnistuu
                                #(do
                                   (reset! urakan-materiaalin-kaytot %)
                                   (reset! valittu-materiaalin-kaytto nil))
                                :disabled (not voi-tallentaa?)}]}

            [{:otsikko "Sopimus" :nimi :sopimus :hae (fn [_] (second @u/valittu-sopimusnumero)) :muokattava? (constantly false)}
             {:otsikko     "Aloitus" :pakollinen? true :uusi-rivi? true
              :tyyppi :pvm :nimi :alkanut :validoi [[:ei-tyhja "Anna aloituspäivämäärä"]]
              :varoita     [[:urakan-aikana-ja-hoitokaudella]]
              :muokattava? muokattava-pred
              :aseta       (fn [rivi arvo]
                             (assoc
                              (if (or
                                   (not (:paattynyt rivi))
                                   (pvm/jalkeen? arvo (:paattynyt rivi)))
                                (assoc rivi :paattynyt arvo)
                                rivi)
                              :alkanut arvo))}
             {:otsikko     "Lopetus" :nimi :paattynyt
              :pakollinen? true
              :tyyppi :pvm  :validoi [[:ei-tyhja "Anna lopetuspäivämäärä"]
                                      [:pvm-kentan-jalkeen :alkanut "Lopetuksen pitää olla aloituksen jälkeen"]]
              :muokattava? muokattava-pred}
             (when (:jarjestelmanlisaama tiedot)
               {:otsikko "Lähde" :nimi :luoja :tyyppi :string
                :hae     (fn [rivi] (str "Järjestelmä (" (:kayttajanimi rivi) " / " (:organisaatio rivi) ")")) :muokattava? (constantly false)})
             {:otsikko "Materiaalit" :nimi :materiaalit :palstoja 2
              :komponentti [materiaalit-ja-maarat
                            materiaalitoteumat-mapissa
                            materiaalien-virheet
                            (:jarjestelmanlisaama tiedot)] :tyyppi :komponentti}
             {:otsikko "Suorittaja" :pakollinen? true :tyyppi :string :pituus-max 256 :muokattava? muokattava-pred :nimi :suorittaja :validoi [[:ei-tyhja "Anna suorittaja"]]}
             {:otsikko "Suorittajan y-tunnus" :pakollinen? true :tyyppi :string :pituus-max 256 :nimi :ytunnus :muokattava? muokattava-pred :validoi [[:ei-tyhja "Anna y-tunnus"]]}
             {:otsikko "Lisätietoja" :tyyppi :text :palstoja 2 :koko [80 :auto]
              :nimi :lisatieto :muokattava? muokattava-pred}]
            tiedot]])))))

(defn tarkastele-toteumaa-nappi [rivi]
  [:button.nappi-toissijainen.nappi-grid {:on-click #(reset! valittu-materiaalin-kaytto rivi)} (ikonit/eye-open) " Toteuma"])

(defn materiaalinkaytto-vetolaatikko
  [urakan-id mk]
  (let [tiedot (reaction<! [hk @u/valittu-hoitokausi
                            sop @u/valittu-sopimusnumero]
                           (materiaali-tiedot/hae-toteumat-materiaalille
                             urakan-id
                             (:id (:materiaali mk))
                             hk
                             (first sop)))]
    (komp/luo
      (fn [urakan-id vm]
        {:key (:id vm)}
        [:div
         [grid/grid
          {:otsikko     (str (get-in mk [:materiaali :nimi]) " toteumat")
           :tyhja       (if (nil? @tiedot) [ajax-loader "Ladataan toteumia"] "Ei toteumia")
           :tallenna    (tallenna-toteuma-materiaaleja urakan-id tiedot)
           :voi-lisata? false
           :tunniste :tmid}
          [{:otsikko "Päivämäärä" :tyyppi :pvm :nimi :aloitus :leveys "20%"
            :hae     (comp pvm/pvm :alkanut :toteuma) :muokattava? (constantly false)}
           {:otsikko "Määrä" :nimi :toteuman_maara :tyyppi :positiivinen-numero :hae (comp :maara :toteuma) :aseta #(assoc-in %1 [:toteuma :maara] %2)
            :leveys  "20%"}
           {:otsikko "Suorittaja" :nimi :suorittaja :pituus-max 256 :tyyppi :text :hae (comp :suorittaja :toteuma) :muokattava? (constantly false) :leveys "20%"}
           {:otsikko "Lisätietoja" :nimi :lisatiedot :tyyppi :text :hae (comp :lisatieto :toteuma) :muokattava? (constantly false) :leveys "20%"}
           {:otsikko     "Tarkastele koko toteumaa" :nimi :tarkastele-toteumaa :tyyppi :komponentti
            :komponentti (fn [rivi] (tarkastele-toteumaa-nappi rivi)) :muokattava? (constantly false) :leveys "20%"}]
          @tiedot]]))))

(defn materiaalit-paasivu
  [ur]
  [:div
   [valinnat/urakan-sopimus-ja-hoitokausi ur]
   [:button.nappi-ensisijainen {:on-click #(reset! valittu-materiaalin-kaytto {})}
    (ikonit/plus) " Lisää toteuma"]

   [grid/grid
    {:otsikko  "Suunnitellut ja toteutuneet materiaalit"
     :tyhja    (if (nil? @urakan-materiaalin-kaytot) [ajax-loader "Materiaaleja haetaan"] "Ei löytyneitä tietoja.")
     :tunniste #(:id (:materiaali %))
     :luokat   ["toteumat-paasisalto"]
     :vetolaatikot
               (into {}
                     (map
                       (juxt
                         (comp :id :materiaali)
                         (fn [mk] [materiaalinkaytto-vetolaatikko (:id ur) mk]))
                       )
                     (filter
                       (fn [rivi] (> (:kokonaismaara rivi) 0))
                       @urakan-materiaalin-kaytot))
     }

    ;; sarakkeet
    [{:tyyppi :vetolaatikon-tila :leveys "5%"}
     {:otsikko "Nimi" :nimi :materiaali_nimi :hae (comp :nimi :materiaali) :leveys "50%"}
     {:otsikko "Yksikkö" :nimi :materiaali_yksikko :hae (comp :yksikko :materiaali) :leveys "10%"}
     {:otsikko "Suunniteltu määrä" :nimi :sovittu_maara :hae :maara :leveys "20%"}
     {:otsikko "Käytetty määrä" :nimi :toteutunut_maara :hae :kokonaismaara :leveys "20%"}
     {:otsikko     "Jäljellä" :nimi :materiaalierotus :tyyppi :komponentti
      :muokattava? (constantly false) :leveys "20%"
      :komponentti
                   (fn [rivi]
                     (let [erotus (-
                                    (if (:maara rivi) (:maara rivi) 0)
                                    (:kokonaismaara rivi))]
                       (if (>= erotus 0)
                         [:span.materiaalierotus.materiaalierotus-positiivinen erotus]
                         [:span.materiaalierotus.materiaalierotus-negatiivinen erotus])))}
     ]

    (sort-by (comp :nimi :materiaali) @urakan-materiaalin-kaytot)]])

(defn materiaalit-nakyma [ur]
  (komp/luo
    (komp/lippu materiaali-tiedot/materiaalinakymassa?)
    (fn [ur]
      [:span
       [kartta/kartan-paikka]
       (if @valittu-materiaalin-kaytto
         [materiaalit-tiedot ur]
         [materiaalit-paasivu ur])])))
