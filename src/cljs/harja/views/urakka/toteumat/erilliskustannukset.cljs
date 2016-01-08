(ns harja.views.urakka.toteumat.erilliskustannukset
  "Urakan 'Toteumat' välilehden Erilliskustannuksien osio"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.bootstrap :as bs]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.modal :refer [modal] :as modal]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko +korostuksen-kesto+]]
            [harja.ui.napit :as napit]
            [harja.ui.viesti :as viesti]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.indeksit :as i]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.views.urakka.valinnat :as valinnat]

            [harja.ui.lomake :refer [lomake]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [cljs.core.async :refer [<! >! chan]]
            [clojure.string :as str]
            [cljs-time.core :as t]
            [cljs.core.async :refer [<! timeout]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.domain.roolit :as roolit])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defonce valittu-kustannus (atom nil))

(defn tallenna-erilliskustannus [muokattu]
  (go (let [sopimus-id (first (:sopimus muokattu))
            tpi-id (:tpi_id (:toimenpideinstanssi muokattu))
            tyyppi (name (:tyyppi muokattu))
            indeksi (if (= yleiset/+ei-sidota-indeksiin+ (:indeksin_nimi muokattu))
                      nil
                      (:indeksin_nimi muokattu))
            rahasumma (if (= (:maksaja muokattu) :urakoitsija)
                        (- (:rahasumma muokattu))
                        (:rahasumma muokattu))
            vanhat-idt (into #{} (map #(:id %) @u/erilliskustannukset-hoitokaudella))
            res (<! (toteumat/tallenna-erilliskustannus (assoc muokattu
                                                          :urakka-id (:id @nav/valittu-urakka)
                                                          :alkupvm (first @u/valittu-hoitokausi)
                                                          :loppupvm (second @u/valittu-hoitokausi)
                                                          :sopimus sopimus-id
                                                          :toimenpideinstanssi tpi-id
                                                          :tyyppi tyyppi
                                                          :rahasumma rahasumma
                                                          :indeksin_nimi indeksi)))
            uuden-id (:id (first (filter #(not (vanhat-idt (:id %)))
                                         res)))]
        (reset! u/erilliskustannukset-hoitokaudella res)
        (or uuden-id true))))

(def +valitse-tyyppi+
  "- Valitse tyyppi -")

(defn erilliskustannustyypin-teksti [avainsana]
  "Erilliskustannustyypin teksti avainsanaa vastaan"
  (case avainsana
    :asiakastyytyvaisyysbonus "Asiakastyytyväisyysbonus"
    :muu "Muu"
    +valitse-tyyppi+))

;; "tilaajan_maa-aines" -tyyppisiä erilliskustannuksia otetaan vastaan Aura-konversiossa
;; mutta ei anneta enää syöttää Harjaan käyttöliittymän kautta. -Anne L. palaverissa 2015-06-02"
(def +erilliskustannustyypit+
  [:asiakastyytyvaisyysbonus :muu])

(defn luo-kustannustyypit [urakkatyyppi]
  ;; jätetään funktio tähän, jos jatkossa halutaan urakkatyypin perusteella antaa eri vaihtoehtoja
  +erilliskustannustyypit+)

(defn maksajavalinnan-teksti [avain]
  (case avain
    :tilaaja "Tilaaja"
    :urakoitsija "Urakoitsija"
    "Maksajaa ei asetettu"))

(def +maksajavalinnat+
  [:tilaaja :urakoitsija])


(def korostettavan-rivin-id (atom nil))

(defn korosta-rivia
  ([id] (korosta-rivia id +korostuksen-kesto+))
  ([id kesto]
   (reset! korostettavan-rivin-id id)
   (go (<! (timeout kesto))
       (reset! korostettavan-rivin-id nil))))

(def +rivin-luokka+ "korosta")

(defn aseta-rivin-luokka [korostettavan-rivin-id]
  (fn [rivi]
    (if (= korostettavan-rivin-id (:id rivi))
      +rivin-luokka+
      "")))

(defn erilliskustannusten-toteuman-muokkaus
  "Erilliskustannuksen muokkaaminen ja lisääminen"
  []
  (let [ur @nav/valittu-urakka
        urakan-indeksi @u/urakassa-kaytetty-indeksi
        muokattu (atom (if (:id @valittu-kustannus)
                         (assoc @valittu-kustannus
                           :sopimus @u/valittu-sopimusnumero
                           :toimenpideinstanssi @u/valittu-toimenpideinstanssi
                           ;; jos maksaja on urakoitsija, rahasumma kannassa miinusmerkkisenä
                           :maksaja (if (neg? (:rahasumma @valittu-kustannus))
                                      :urakoitsija
                                      :tilaaja)
                           :rahasumma (Math/abs (:rahasumma @valittu-kustannus)))
                         (assoc @valittu-kustannus
                           :sopimus @u/valittu-sopimusnumero
                           :toimenpideinstanssi @u/valittu-toimenpideinstanssi
                           :maksaja :tilaaja
                           :indeksin_nimi yleiset/+ei-sidota-indeksiin+)))
        valmis-tallennettavaksi? (reaction (let [m @muokattu]
                                             (not (and
                                                    (:toimenpideinstanssi m)
                                                    (:tyyppi m)
                                                    (:pvm m)
                                                    (:rahasumma m)))))
        tallennus-kaynnissa (atom false)]

    (komp/luo
      (fn []
        [:div.erilliskustannuksen-tiedot
         [napit/takaisin " Takaisin kustannusluetteloon" #(reset! valittu-kustannus nil)]
         (if (:id @valittu-kustannus)
           [:h3 "Muokkaa kustannusta"]
           [:h3 "Luo uusi kustannus"])

         [lomake {:luokka   :horizontal
                  :muokkaa! (fn [uusi]
                              (log "MUOKATAAN " (pr-str uusi))
                              (reset! muokattu uusi))
                  :footer   [:span
                             [napit/palvelinkutsu-nappi
                              " Tallenna kustannus"
                              #(tallenna-erilliskustannus @muokattu)
                              {:luokka       "nappi-ensisijainen"
                               :disabled     @valmis-tallennettavaksi?
                               :kun-onnistuu #(let [muokatun-id (or (:id @muokattu) %)]
                                               (do
                                                 (korosta-rivia muokatun-id)
                                                 (reset! tallennus-kaynnissa false)
                                                 (reset! valittu-kustannus nil)))
                               :kun-virhe    (reset! tallennus-kaynnissa false)}]
                             (when (and
                                     (roolit/rooli-urakassa? roolit/urakanvalvoja (:id ur))
                                     (:id @muokattu))
                               [:button.nappi-kielteinen
                                {:class (when @tallennus-kaynnissa "disabled")
                                 :on-click
                                        (fn [e]
                                          (.preventDefault e)
                                          (modal/nayta! {:otsikko "Erilliskustannuksen poistaminen"
                                                         :footer  [:span
                                                                   [:button.nappi-toissijainen {:type     "button"
                                                                                                :on-click #(do (.preventDefault %)
                                                                                                               (modal/piilota!))}
                                                                    "Peruuta"]
                                                                   [:button.nappi-kielteinen {:type     "button"
                                                                                              :on-click #(do (.preventDefault %)
                                                                                                             (modal/piilota!)
                                                                                                             (reset! tallennus-kaynnissa true)
                                                                                                             (go (let [res (tallenna-erilliskustannus
                                                                                                                             (assoc @muokattu :poistettu true))]
                                                                                                                   (if res
                                                                                                                     ;; Tallennus ok
                                                                                                                     (do (viesti/nayta! "Kustannus poistettu")
                                                                                                                         (reset! tallennus-kaynnissa false)
                                                                                                                         (reset! valittu-kustannus nil))

                                                                                                                     ;; Epäonnistui jostain syystä
                                                                                                                     (reset! tallennus-kaynnissa false)))))}
                                                                    "Poista kustannus"]]}
                                                        [:div (str "Haluatko varmasti poistaa erilliskustannuksen "
                                                                   (Math/abs (:rahasumma @muokattu)) "€ päivämäärällä "
                                                                   (pvm/pvm (:pvm @muokattu)) "?")]))}
                                (ikonit/trash) " Poista kustannus"])]}

          [{:otsikko       "Sopimusnumero" :nimi :sopimus
            :pakollinen?   true
            :tyyppi        :valinta
            :valinta-nayta second
            :valinnat      (:sopimukset ur)
            :fmt           second
            :leveys-col    4}
           {:otsikko       "Toimenpide" :nimi :toimenpideinstanssi
            :pakollinen?   true
            :tyyppi        :valinta
            :valinta-nayta #(:tpi_nimi %)
            :valinnat      @u/urakan-toimenpideinstanssit
            :fmt           #(:tpi_nimi %)
            :leveys-col    4}
           {:otsikko       "Tyyppi" :nimi :tyyppi
            :pakollinen?   true
            :tyyppi        :valinta
            :valinta-nayta #(if (nil? %) +valitse-tyyppi+ (erilliskustannustyypin-teksti %))
            :valinnat      (luo-kustannustyypit (:tyyppi ur))
            :fmt           #(erilliskustannustyypin-teksti %)
            :validoi       [[:ei-tyhja "Anna kustannustyyppi"]]
            :leveys-col    4
            :aseta         (fn [rivi arvo]
                             (assoc (if (and
                                          urakan-indeksi
                                      (= :asiakastyytyvaisyysbonus arvo))
                               (assoc rivi :indeksin_nimi urakan-indeksi)
                               rivi)
                             :tyyppi arvo))}
           {:otsikko "Toteutunut pvm" :nimi :pvm :tyyppi :pvm
            :pakollinen?   true
            :validoi [[:ei-tyhja "Anna kustannuksen päivämäärä"]] :leveys-col 4
            :varoita [[:urakan-aikana-ja-hoitokaudella]]}
           {:otsikko     "Rahamäärä"
            :nimi        :rahasumma
            :pakollinen? true
            :yksikko     "€"
            :tyyppi      :positiivinen-numero
            :validoi     [[:ei-tyhja "Anna rahamäärä"]]
            :leveys-col  4}
           {:otsikko     "Indeksi" :nimi :indeksin_nimi :tyyppi :valinta
            :pakollinen? true
            ;; hoitourakoissa as.tyyt.bonuksen laskennan indeksi menee urakan alkamisvuoden mukaan
            :muokattava? #(not (and
                                 (= :asiakastyytyvaisyysbonus (:tyyppi @muokattu))
                                 (= :hoito (:tyyppi ur))))
            :valinnat    (conj @i/indeksien-nimet yleiset/+ei-sidota-indeksiin+)
            :fmt         #(if (nil? %)
                           yleiset/+valitse-indeksi+
                           (str %))
            :leveys-col  4
            :vihje       (when (and
                                 (= :asiakastyytyvaisyysbonus (:tyyppi @muokattu))
                                 (= :hoito (:tyyppi ur)))
                           "Asiakastyytyväisyysbonuksen indeksitarkistus lasketaan automaattisesti laskutusyhteenvedossa.
                   Käytettävä indeksi on määritelty urakan kilpailuttamisajankohdan perusteella eikä sitä voi muuttaa.")
            }
           {:otsikko       "Maksaja" :nimi :maksaja :tyyppi :valinta
            :pakollinen?   true
            :valinta-nayta #(maksajavalinnan-teksti %)
            :valinnat      +maksajavalinnat+
            :fmt           #(maksajavalinnan-teksti %)
            :leveys-col    4
            }
           {:otsikko     "Lisätieto" :nimi :lisatieto :tyyppi :text :pituus-max 1024
            :placeholder "Kirjoita tähän lisätietoa" :koko [80 :auto]}
           ]

          @muokattu]]))))

(defn erilliskustannusten-toteumalistaus
  "Erilliskustannusten toteumat"
  []
  (let [urakka @nav/valittu-urakka
        valitut-kustannukset
        (reaction (let [[sopimus-id _] @u/valittu-sopimusnumero
                        toimenpideinstanssi (:tpi_id @u/valittu-toimenpideinstanssi)]
                    (reverse (sort-by :pvm (filter #(and
                                                     (= sopimus-id (:sopimus %))
                                                     (= (:toimenpideinstanssi %) toimenpideinstanssi))
                                                   @u/erilliskustannukset-hoitokaudella)))))]

    (komp/luo
      (komp/lippu toteumat/erilliskustannukset-nakymassa?)
      (fn []
        (let [aseta-rivin-luokka (aseta-rivin-luokka @korostettavan-rivin-id)]
          [:div.erilliskustannusten-toteumat
           [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide urakka]
           [:button.nappi-ensisijainen {:on-click #(reset! valittu-kustannus {})}
            (ikonit/plus) " Lisää kustannus"]

           [grid/grid
            {:otsikko       (str "Erilliskustannukset ")
             :tyhja         (if (nil? @valitut-kustannukset)
                              [ajax-loader "Erilliskustannuksia haetaan..."]
                              "Ei erilliskustannuksia saatavilla.")
             :rivi-klikattu #(reset! valittu-kustannus %)
             :rivin-luokka  #(aseta-rivin-luokka %)}
            [{:otsikko "Tyyppi" :nimi :tyyppi :fmt erilliskustannustyypin-teksti :leveys "20%"}
             {:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :pvm :leveys "10%"}
             {:otsikko "Rahamäärä (€)" :tyyppi :string :nimi :rahasumma :hae #(Math/abs (:rahasumma %)) :fmt fmt/euro-opt :leveys "12%"}
             {:otsikko "Ind. korjattuna (€)" :tyyppi :string :nimi :indeksikorjattuna
              :hae     #(if (nil? (:indeksin_nimi %))
                         "Ei sidottu indeksiin"
                         (if (and
                               (not (nil? (:indeksin_nimi %)))
                               (nil? (:indeksikorjattuna %)))
                           nil
                           (fmt/euro-opt (:indeksikorjattuna %))))
              :fmt     #(if (nil? %)
                         [:span.ei-arvoa "Ei indeksiarvoa"]
                         (str %))
              :leveys  "13%"}
             {:otsikko "Indeksi" :nimi :indeksin_nimi :leveys "10%"}
             {:otsikko "Maksaja" :tyyppi :string :nimi :maksaja
              :hae     #(if (neg? (:rahasumma %)) "Urakoitsija" "Tilaaja") :leveys "10%"}
             {:otsikko "Lisätieto" :nimi :lisatieto :leveys "35%" :pituus-max 1024}]
            @valitut-kustannukset]])))))

(defn erilliskustannusten-toteumat []
  (fn []
    [:span
     (if @valittu-kustannus
       [erilliskustannusten-toteuman-muokkaus]
       [erilliskustannusten-toteumalistaus])]))
