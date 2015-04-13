(ns harja.views.urakka.kokonaishintaiset-tyot
  "Urakan 'Kokonaishintaiset työt' välilehti:"
  (:require [reagent.core :refer [atom] :as reagent]
            [bootstrap :as bs]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      alasveto-ei-loydoksia alasvetovalinta radiovalinta]]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.tiedot.urakka.kokonaishintaiset-tyot :as kok-hint-tyot]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.tiedot.istunto :as istunto]

            [harja.loki :refer [log]]
            [harja.pvm :as pvm]

            [clojure.set :refer [difference]]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [cljs-time.core :as t]

            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.ui.yleiset :refer [deftk]]))


(defn luo-tyhja-tyo [tpi hk kk sn]
  (let [
        alkupvm (first hk)
        loppupvm (second hk)
        tyon-kalenteri-vuosi (if (<= 10 kk 12)
                               (pvm/vuosi alkupvm)
                               (pvm/vuosi loppupvm))
        rivi {:toimenpideinstanssi tpi, :summa nil :kuukausi kk :vuosi tyon-kalenteri-vuosi
              :maksupvm nil :alkupvm alkupvm :loppupvm loppupvm :sopimus sn}]
    rivi))

(defn tallenna-tyot [ur sopimusnumero valittu-hoitokausi tyot uudet-tyot tuleville?]
  (go (let [tallennettavat-hoitokaudet (if tuleville?
                                         (s/tulevat-hoitokaudet ur valittu-hoitokausi)
                                         valittu-hoitokausi)
            muuttuneet
            (into []
                  (if tuleville?
                    (s/rivit-tulevillekin-kausille-kok-hint-tyot ur uudet-tyot valittu-hoitokausi)
                    uudet-tyot
                    ))
            res (<! (kok-hint-tyot/tallenna-kokonaishintaiset-tyot (:id ur) sopimusnumero muuttuneet))
            res-jossa-hoitokausitieto (map #(kok-hint-tyot/aseta-hoitokausi %) res)]
        (reset! tyot res-jossa-hoitokausitieto)
        true)))

(defn ryhmittele-tehtavat
  "Ryhmittelee 4. tason tehtävät. Lisää väliotsikot eri tehtävien väliin"
  [toimenpiteet-tasoittain tyorivit]
  (let [otsikko (fn [{:keys [tehtava]}]
                  (some (fn [[t1 t2 t3 t4]]
                          (when (= (:id t4) tehtava)
                            (str (:nimi t1) " / " (:nimi t2) " / " (:nimi t3))))
                        toimenpiteet-tasoittain))
        otsikon-mukaan (group-by otsikko tyorivit)]
    (mapcat (fn [[otsikko rivit]]
              (concat [(grid/otsikko otsikko)] rivit))
            (seq otsikon-mukaan))))


(deftk kokonaishintaiset-tyot [ur]

       [urakan-kok-hint-tyot (<! (kok-hint-tyot/hae-urakan-kokonaishintaiset-tyot (:id ur)))
        toimenpiteet (<! (urakan-toimenpiteet/hae-urakan-toimenpiteet (:id ur)))

        ;; ryhmitellään valitun sopimusnumeron materiaalit hoitokausittain
        sopimuksen-tyot-hoitokausittain
        :reaction (let [[sopimus-id _] @s/valittu-sopimusnumero
                        sopimuksen-tyot (filter #(= sopimus-id (:sopimus %))
                                                @urakan-kok-hint-tyot)]
                    (s/ryhmittele-hoitokausittain sopimuksen-tyot (s/hoitokaudet ur)))


        ;; valitaan materiaaleista vain valitun hoitokauden
        valitun-hoitokauden-tyot :reaction (let [hk @s/valittu-hoitokausi]
                                (get @sopimuksen-tyot-hoitokausittain hk))

        valittu-toimenpide :reaction (first @toimenpiteet)
        valitun-toimenpiteen-ja-hoitokauden-tyot :reaction (let [valittu-tp-id (:id @valittu-toimenpide)]
                                                             (filter #(= valittu-tp-id (:toimenpide %))
                                                                     @valitun-hoitokauden-tyot))
        tyorivit :reaction (let [kirjatut-kkt (into #{} (map #(:kuukausi %)
                                                             @valitun-toimenpiteen-ja-hoitokauden-tyot))
                       tyhjat-kkt (difference (into #{} (range 1 13)) kirjatut-kkt)
                       tyhjat-tyot (map #(luo-tyhja-tyo (:tpi_id @valittu-toimenpide)
                                                        @s/valittu-hoitokausi
                                                        %
                                                        (first @s/valittu-sopimusnumero))
                                        tyhjat-kkt)]
                             (vec (sort-by (juxt :vuosi :kuukausi)
                                           (concat @valitun-toimenpiteen-ja-hoitokauden-tyot tyhjat-tyot))))
        ;; kopioidaanko myös tuleville kausille (oletuksena false, vaarallinen)
        tuleville? false

        ;; jos tulevaisuudessa on dataa, joka poikkeaa tämän hoitokauden materiaaleista, varoita ylikirjoituksesta
        varoita-ylikirjoituksesta?
        :reaction (let [kopioi? @tuleville?
                        varoita? (s/varoita-ylikirjoituksesta? @sopimuksen-tyot-hoitokausittain
                                                               @s/valittu-hoitokausi)]
                    (if-not kopioi?
                      false
                      varoita?))
        kaikki-sopimuksen-ja-tpin-rivit :reaction (let [sopimus-id (first @s/valittu-sopimusnumero)
                                                 tpi-id (:tpi_id @valittu-toimenpide)]
                                             (filter #(and
                                                       (= sopimus-id (:sopimus %))
                                                      (= tpi-id (:toimenpideinstanssi %)))
                                                     @urakan-kok-hint-tyot))
        kaikkien-hoitokausien-kustannukset
        :reaction (s/toiden-kustannusten-summa
                    @kaikki-sopimuksen-ja-tpin-rivit
                    :summa)
        valitun-hoitokauden-kustannukset
        :reaction (s/toiden-kustannusten-summa (let [hk (first @s/valittu-hoitokausi)]
                                                 (filter
                                                   #(pvm/sama-pvm?
                                                     (:alkupvm %) hk)
                                                   @kaikki-sopimuksen-ja-tpin-rivit))
                                               :summa)

        ]

       (do
         [:div.kokonaishintaiset-tyot
         [:div.alasvetovalikot
              [:div.label-ja-alasveto
               [:span.alasvedon-otsikko "Toimenpide"]
               [alasvetovalinta {:valinta    @valittu-toimenpide
                                 ;;\u2014 on väliviivan unikoodi
                                 :format-fn  #(if % (str (:tpi_nimi %)) "Valitse")
                                 :valitse-fn #(reset! valittu-toimenpide %)
                                 :class      "alasveto"
                                 }
                @toimenpiteet]]]
          [grid/grid
           {:otsikko        (str "Kokonaishintaiset työt: " (:t2_nimi @valittu-toimenpide) " / " (:t3_nimi @valittu-toimenpide) " / " (:tpi_nimi @valittu-toimenpide))
            :tyhja          (if (nil? @toimenpiteet) [ajax-loader "Kokonaishintaisia töitä haetaan..."] "Ei kokonaishintaisia töitä")
            :tallenna       (istunto/jos-rooli-urakassa istunto/rooli-urakanvalvoja
                                                        (:id ur)
                                                        #(tallenna-tyot ur @s/valittu-sopimusnumero @s/valittu-hoitokausi
                                                                        urakan-kok-hint-tyot % @tuleville?)
                                                        :ei-mahdollinen)
            :tunniste       #((juxt :tpi_nimi :sopimus :vuosi :kuukausi) %)
            :voi-lisata?    false
            :voi-poistaa?   (constantly false)
            :muokkaa-footer (fn [g]
                              [raksiboksi "Tallenna tulevillekin hoitokausille"
                               @tuleville?
                               #(swap! tuleville? not)
                               [:div.raksiboksin-info (ikonit/warning-sign) "Tulevilla hoitokausilla eri tietoa, jonka tallennus ylikirjoittaa."]
                               (and @tuleville? @varoita-ylikirjoituksesta?)])
            }

           ;; sarakkeet
           [{:otsikko "Vuosi" :nimi :vuosi :muokattava? (constantly false) :tyyppi :numero :leveys "25%"}
            {:otsikko "Kuukausi" :nimi "kk" :hae #(pvm/kuukauden-nimi (:kuukausi %)) :muokattava? (constantly false)
             :tyyppi :numero :leveys "25%"}
            {:otsikko "Summa" :nimi :summa :fmt #(if % (str (.toFixed % 2) " \u20AC")) :tasaa :oikea
             :tyyppi :numero :leveys "25%"}
            {:otsikko "Maksupvm" :nimi :maksupvm :pvm-tyhjana #(pvm/luo-pvm (:vuosi %) (- (:kuukausi %) 1) 15)
             :tyyppi :pvm :fmt #(if % (pvm/pvm %)) :leveys "25%"}
            ]
           @tyorivit
           ]

          [:div.hoitokauden-kustannukset
           [:div "Toimenpiteen hoitokausi yhteensä "
            [:span (str (.toFixed @valitun-hoitokauden-kustannukset 2) "\u20AC")]
            ]
           [:div "Toimenpiteen kaikki hoitokaudet yhteensä "
            [:span (str (.toFixed @kaikkien-hoitokausien-kustannukset 2) "\u20AC")]
            ]]]))


