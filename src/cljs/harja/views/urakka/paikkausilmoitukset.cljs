(ns harja.views.urakka.paikkausilmoitukset
  "Urakan paikkausilmoitukset"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.lomake :as lomake]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.muokkauslukko :as lukko]
            [harja.tiedot.navigaatio :as nav]
            [harja.fmt :as fmt]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<!]]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.paikkaus :as paikkaus]
            [harja.domain.roolit :as roolit]
            [harja.ui.kommentit :as kommentit]
            [harja.domain.paikkaus.minipot :as minipot]
            [harja.views.kartta :as kartta]
            [harja.ui.tierekisteri :as tierekisteri]
            [harja.ui.napit :as napit])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(def lomake-lukittu-muokkaukselta? (reaction (let [_ @lukko/nykyinen-lukko]
                                               (lukko/nykyinen-nakyma-lukittu?))))

(defn kuvaile-paatostyyppi [paatos]
  (case paatos
    :hyvaksytty "Hyväksytty"
    :hylatty "Hylätty"))

(defn nayta-tila [tila]
  (case tila
    :aloitettu "Aloitettu"
    :valmis "Valmis"
    :lukittu "Lukittu"
    "-"))

(defn nayta-paatos [tila]
  (case tila
    :hyvaksytty [:span.paikkausilmoitus-hyvaksytty "Hyväksytty"]
    :hylatty [:span.paikkausilmoitus-hylatty "Hylätty"]
    ""))

(defn lisaa-suoritteet-tyhjaan-toteumaan [toteumat]
  (if (or (nil? toteumat) (empty? toteumat))
    (mapv
      (fn [tyo] {:suorite (:koodi tyo)})
      minipot/+paikkaustyot+)
    toteumat))


(defn laske-tyon-alv
  "Ottaa työn hinnan (esim. 100) ja arvolisäveron (esim. 24) palauttaa työn hinnan alv:n kera"
  [tyon-hinta alv]
  (* tyon-hinta (+ (/ (double alv) 100) 1)))

(defn laske-paikkausprosentti [paikkausneliot tienpaallysteen-neliot]
  (if (and
        (not (nil? tienpaallysteen-neliot))
        (not= tienpaallysteen-neliot 0))
    (let [tulos (* (/ paikkausneliot tienpaallysteen-neliot) 100)]
      (.toFixed tulos 0))))

(defn laske-tienpaallysteen-neliot [pituus tienpaallysteen-leveys]
  (let [pituus (or pituus 0)
        tienpaallysteen-leveys (or tienpaallysteen-leveys 0)]
    (* pituus tienpaallysteen-leveys)))


(defn kasittely
  "Ilmoituksen käsittelyosio, kun ilmoitus on valmis. Tilaaja voi muokata, urakoitsija voi tarkastella."
  [valmis-kasiteltavaksi?]
  (let [muokattava? (and
                      (roolit/roolissa? roolit/urakanvalvoja)
                      (not= (:tila @paikkaus/paikkausilmoitus-lomakedata) :lukittu)
                      (false? @lomake-lukittu-muokkaukselta?))
        paatostiedot (r/wrap {:paatos        (:paatos @paikkaus/paikkausilmoitus-lomakedata)
                              :perustelu     (:perustelu @paikkaus/paikkausilmoitus-lomakedata)
                              :kasittelyaika (:kasittelyaika @paikkaus/paikkausilmoitus-lomakedata)}
                             (fn [uusi-arvo] (reset! paikkaus/paikkausilmoitus-lomakedata (-> (assoc @paikkaus/paikkausilmoitus-lomakedata :paatos (:paatos uusi-arvo))
                                                                                                (assoc :perustelu (:perustelu uusi-arvo))
                                                                                                (assoc :kasittelyaika (:kasittelyaika uusi-arvo))))))]
    (when @valmis-kasiteltavaksi?
      [:div.paikkausilmoitus-kasittely
       [:h3 "Käsittely"]
       [lomake/lomake
        {:luokka       :horizontal
         :muokkaa!     (fn [uusi]
                         (reset! paatostiedot uusi))
         :voi-muokata? muokattava?}
        [{:otsikko "Käsitelty"
          :nimi    :kasittelyaika
          :tyyppi  :pvm
          :validoi [[:ei-tyhja "Anna käsittelypäivämäärä"]
                    [:pvm-toisen-pvmn-jalkeen (:valmispvm_kohde @paikkaus/paikkausilmoitus-lomakedata) "Käsittely ei voi olla ennen valmistumista"]]}

         {:otsikko       "Päätös"
          :nimi          :paatos
          :tyyppi        :valinta
          :valinnat      [:hyvaksytty :hylatty]
          :validoi       [[:ei-tyhja "Anna päätös"]]
          :valinta-nayta #(if % (kuvaile-paatostyyppi %) (if muokattava? "- Valitse päätös -" "-"))
          :leveys-col    3}

         (when (:paatos @paatostiedot)
           {:otsikko    "Selitys"
            :nimi       :perustelu
            :tyyppi     :text
            :koko       [60 3]
            :pituus-max 2048
            :leveys-col 6
            :validoi    [[:ei-tyhja "Anna päätöksen selitys"]]})]
        @paatostiedot]])))

(defn tallennus
  [valmis-tallennettavaksi?]
  (let [huomautusteksti (reaction (let [valmispvm-kohde (:valmispvm_kohde @paikkaus/paikkausilmoitus-lomakedata)
                                        valmispvm-paikkaus (:valmispvm_paikkaus @paikkaus/paikkausilmoitus-lomakedata)
                                        paatos (:paatos @paikkaus/paikkausilmoitus-lomakedata)
                                        tila (:tila @paikkaus/paikkausilmoitus-lomakedata)]
                                    (cond (not (and valmispvm-kohde valmispvm-paikkaus))
                                          "Valmistusmispäivämäärää ei ole annettu, ilmoitus tallennetaan keskeneräisenä."
                                          (and (not= :lukittu tila)
                                               (= :hyvaksytty paatos))
                                          "Ilmoitus on hyväksytty, ilmoitus lukitaan tallennuksen yhteydessä."
                                          :else
                                          nil)))
        urakka-id (:id @nav/valittu-urakka)
        [sopimus-id _] @u/valittu-sopimusnumero]

    [:div.pot-tallennus
     (when @huomautusteksti
       (lomake/yleinen-huomautus @huomautusteksti))

     [harja.ui.napit/palvelinkutsu-nappi
      "Tallenna"
      #(let [lomake @paikkaus/paikkausilmoitus-lomakedata
             lahetettava-data (-> (grid/poista-idt lomake [:ilmoitustiedot :osoitteet])
                                  (grid/poista-idt [:ilmoitustiedot :toteumat]))]
        (log "PAI Lomake-data: " (pr-str @paikkaus/paikkausilmoitus-lomakedata))
        (log "PAIK Lähetetään data " (pr-str lahetettava-data))
        (paikkaus/tallenna-paikkausilmoitus urakka-id sopimus-id lahetettava-data))
      {:luokka       "nappi-ensisijainen"
       :disabled     (false? @valmis-tallennettavaksi?)
       :ikoni        (ikonit/tallenna)
       :kun-onnistuu (fn [vastaus]
                       (log "PAI Lomake tallennettu, vastaus: " (pr-str vastaus))
                       (reset! paikkaus/paikkaustoteumat vastaus)
                       (reset! paikkaus/paikkausilmoitus-lomakedata nil))}]]))

(defn paikkausilmoituslomake []
  (let [kokonaishinta (reaction (minipot/laske-kokonaishinta (get-in @paikkaus/paikkausilmoitus-lomakedata [:ilmoitustiedot :toteumat])))]

    (komp/luo
      (komp/lukko (lukko/muodosta-lukon-id "paikkausilmoitus" (:kohdenumero @paikkaus/paikkausilmoitus-lomakedata)))
      (fn []
        (let [kohteen-tiedot (r/wrap {:aloituspvm         (:aloituspvm @paikkaus/paikkausilmoitus-lomakedata)
                                      :valmispvm_kohde    (:valmispvm_kohde @paikkaus/paikkausilmoitus-lomakedata)
                                      :valmispvm_paikkaus (:valmispvm_paikkaus @paikkaus/paikkausilmoitus-lomakedata)}
                                     (fn [uusi-arvo]
                                       (reset! paikkaus/paikkausilmoitus-lomakedata (-> (assoc @paikkaus/paikkausilmoitus-lomakedata :aloituspvm (:aloituspvm uusi-arvo))
                                                                                          (assoc :valmispvm_kohde (:valmispvm_kohde uusi-arvo))
                                                                                          (assoc :valmispvm_paikkaus (:valmispvm_paikkaus uusi-arvo))))))

              toteutuneet-osoitteet
              (r/wrap (zipmap (iterate inc 1) (:osoitteet (:ilmoitustiedot @paikkaus/paikkausilmoitus-lomakedata)))
                      (fn [uusi-arvo] (reset! paikkaus/paikkausilmoitus-lomakedata
                                              (assoc-in @paikkaus/paikkausilmoitus-lomakedata [:ilmoitustiedot :osoitteet] (grid/filteroi-uudet-poistetut uusi-arvo)))))
              toteutuneet-maarat
              (r/wrap (zipmap (iterate inc 1) (lisaa-suoritteet-tyhjaan-toteumaan (:toteumat (:ilmoitustiedot @paikkaus/paikkausilmoitus-lomakedata))))
                      (fn [uusi-arvo] (reset! paikkaus/paikkausilmoitus-lomakedata
                                              (assoc-in @paikkaus/paikkausilmoitus-lomakedata [:ilmoitustiedot :toteumat] (grid/filteroi-uudet-poistetut uusi-arvo)))))

              toteutuneet-osoitteet-virheet (atom {})
              toteutuneet-maarat-virheet (atom {})

              valmis-tallennettavaksi? (reaction
                                         (let [toteutuneet-osoitteet-virheet @toteutuneet-osoitteet-virheet
                                               toteutuneet-maarat-virheet @toteutuneet-maarat-virheet
                                               tila (:tila @paikkaus/paikkausilmoitus-lomakedata)
                                               lomake-lukittu-muokkaukselta? @lomake-lukittu-muokkaukselta?]
                                           (and
                                             (not (= tila :lukittu))
                                             (empty? toteutuneet-osoitteet-virheet)
                                             (empty? toteutuneet-maarat-virheet)
                                             (false? lomake-lukittu-muokkaukselta?))))
              valmis-kasiteltavaksi? (reaction
                                       (let [valmispvm-kohde (:valmispvm_kohde @paikkaus/paikkausilmoitus-lomakedata)
                                             tila (:tila @paikkaus/paikkausilmoitus-lomakedata)]
                                         (log "PAI valmis käsi " (pr-str valmispvm-kohde) (pr-str tila))
                                         (and tila
                                              valmispvm-kohde
                                              (not (= tila :aloitettu))
                                              (not (nil? valmispvm-kohde)))))]
          [:div.paikkausilmoituslomake
           [napit/takaisin "Takaisin ilmoitusluetteloon" #(reset! paallystys/paikkausilmoitus-lomakedata nil)]


           (when @lomake-lukittu-muokkaukselta?
             (lomake/lomake-lukittu-huomautus @lukko/nykyinen-lukko))

           [:h2 "Paikkausilmoitus"]

           [:div.row
            [:div.col-md-6
             [:h3 "Perustiedot"]
             [lomake/lomake {:luokka       :horizontal
                             :voi-muokata? (and (not= :lukittu (:tila @paikkaus/paikkausilmoitus-lomakedata))
                                                (false? @lomake-lukittu-muokkaukselta?))
                             :muokkaa!     (fn [uusi]
                                             (log "PAI Muokataan kohteen tietoja: " (pr-str uusi))
                                             (reset! kohteen-tiedot uusi))}
              [{:otsikko "Kohde" :nimi :kohde :hae (fn [_] (str "#" (:kohdenumero @paikkaus/paikkausilmoitus-lomakedata) " " (:kohdenimi @paikkaus/paikkausilmoitus-lomakedata))) :muokattava? (constantly false)}
               {:otsikko "Työ aloitettu" :nimi :aloituspvm :tyyppi :pvm}
               {:otsikko "Paikkaus valmistunut" :nimi :valmispvm_paikkaus :tyyppi :pvm}
               {:otsikko "Kohde valmistunut" :nimi :valmispvm_kohde
                :vihje   (when (and
                                 (:valmispvm_paikkaus @paikkaus/paikkausilmoitus-lomakedata)
                                 (:valmispvm_kohde @paikkaus/paikkausilmoitus-lomakedata)
                                 (= :aloitettu (:tila @paikkaus/paikkausilmoitus-lomakedata)))
                           "Kohteen valmistumispäivämäärä annettu, ilmoitus tallennetaan valmiina urakanvalvojan käsiteltäväksi.")
                :tyyppi  :pvm :validoi [[:pvm-ei-annettu-ennen-toista :valmispvm_paikkaus "Kohdetta ei voi merkitä valmistuneeksi ennen kuin paikkaus on valmistunut."]]}
               {:otsikko "Toteutunut hinta" :nimi :hinta :tyyppi :positiivinen-numero :leveys-col 2 :hae #(fmt/euro-opt @kokonaishinta) :muokattava? (constantly false)}
               (when (or (= :valmis (:tila @paikkaus/paikkausilmoitus-lomakedata))
                         (= :lukittu (:tila @paikkaus/paikkausilmoitus-lomakedata)))
                 {:otsikko     "Kommentit" :nimi :kommentit
                  :komponentti [kommentit/kommentit {:voi-kommentoida? (not= :lukittu (:tila @paikkaus/paikkausilmoitus-lomakedata))
                                                     :voi-liittaa      false
                                                     :leveys-col       40
                                                     :placeholder      "Kirjoita kommentti..."
                                                     :uusi-kommentti   (r/wrap (:uusi-kommentti @paikkaus/paikkausilmoitus-lomakedata)
                                                                               #(swap! paikkaus/paikkausilmoitus-lomakedata assoc :uusi-kommentti %))}
                                (:kommentit @paikkaus/paikkausilmoitus-lomakedata)]})
               ]
              @kohteen-tiedot]]

            [:div.col-md-6
             (kasittely valmis-kasiteltavaksi?)]]

           [:fieldset.lomake-osa
            [:legend "Ilmoitustiedot"]

            [grid/muokkaus-grid
             {:otsikko      "Paikatut tierekisteriosoitteet"
              :tunniste     :tie
              :voi-muokata? (do
                              (log "PAI tila " (pr-str (:tila @paikkaus/paikkausilmoitus-lomakedata)) " Päätös: " (pr-str (:paatos_tekninen_osa @paikkaus/paikkausilmoitus-lomakedata)))
                              (and (not= :lukittu (:tila @paikkaus/paikkausilmoitus-lomakedata))
                                   (not= :hyvaksytty (:paatos @paikkaus/paikkausilmoitus-lomakedata))
                                   (false? @lomake-lukittu-muokkaukselta?)))
              :virheet      toteutuneet-osoitteet-virheet
              :uusi-id      (inc (count @toteutuneet-osoitteet))}
             [{:otsikko "Tie#" :nimi :tie :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Alku\u00ADtieosa" :nimi :aosa :leveys "10%" :tyyppi :positiivinen-numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Alku\u00ADetäisyys" :nimi :aet :leveys "10%" :tyyppi :positiivinen-numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Loppu\u00ADtieosa" :nimi :losa :leveys "10%" :tyyppi :positiivinen-numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Loppu\u00ADetäisyys" :nimi :let :leveys "10%" :tyyppi :positiivinen-numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :positiivinen-numero :muokattava? (constantly false) :hae (fn [rivi]
                                                                                                                        (tierekisteri/laske-tien-pituus rivi))}
              {:otsikko "Tiepääl\u00ADlysteen leveys" :nimi :paallysteen_leveys :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Tiepääl\u00ADlysteen neliöt" :nimi :paallysteen_neliot :tyyppi :positiivinen-numero :leveys "10%" :muokattava? (constantly false) :hae (fn [rivi]
                                                                                                                                               (laske-tienpaallysteen-neliot (tierekisteri/laske-tien-pituus rivi) (:paallysteen_leveys rivi)))}
              {:otsikko "Paik\u00ADkaus\u00ADneliöt" :nimi :paikkausneliot :tyyppi :positiivinen-numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Paik\u00ADkaus-%" :nimi :paikkausprosentti :tyyppi :string :leveys "10%" :muokattava? (constantly false) :hae (fn [rivi]
                                                                                                                                   (laske-paikkausprosentti (:paikkausneliot rivi)
                                                                                                                                                            (laske-tienpaallysteen-neliot (tierekisteri/laske-tien-pituus rivi) (:paallysteen_leveys rivi))))}]
             toteutuneet-osoitteet]

            [grid/muokkaus-grid
             {:otsikko      "Toteutuneet suoritemäärät"
              :voi-muokata? (and (not= :lukittu (:tila @paikkaus/paikkausilmoitus-lomakedata))
                                 (not= :hyvaksytty (:paatos @paikkaus/paikkausilmoitus-lomakedata))
                                 (false? @lomake-lukittu-muokkaukselta?))
              :voi-lisata?  false
              :voi-kumota?  false
              :voi-poistaa? (constantly false)
              :virheet      toteutuneet-maarat-virheet
              :uusi-id      (inc (count @toteutuneet-maarat))}
             [{:otsikko "Suorite" :nimi :suorite :tyyppi :string :leveys "10%" :pituus-max 256
               :hae     (fn [rivi] (minipot/hae-paikkaustyo-koodilla (:suorite rivi))) :muokattava? (constantly false)}
              {:otsikko "Yksikkö" :nimi :yksikko :tyyppi :string :leveys "10%" :pituus-max 256}
              {:otsikko "Määrä" :nimi :maara :tyyppi :positiivinen-numero :leveys "10%" :kokonaisosan-maara 6}
              {:otsikko "Yks.hinta (alv 0%)" :nimi :yks_hint_alv_0 :tyyppi :positiivinen-numero :leveys "10%" :kokonaisosan-maara 4}
              {:otsikko "Yks.hinta (alv 24%)" :nimi :yks_hint_alv_24 :leveys "10%" :tyyppi :positiivinen-numero :muokattava? (constantly false) :hae (fn [rivi] (laske-tyon-alv (:yks_hint_alv_0 rivi) 24))}
              {:otsikko "Yht. (alv 0%)" :nimi :yht :leveys "10%" :tyyppi :positiivinen-numero :muokattava? (constantly false)
               :hae     (fn [rivi] (* (:yks_hint_alv_0 rivi) (:maara rivi)))}
              {:otsikko "Takuupvm" :nimi :takuupvm :leveys "10%" :tyyppi :pvm}]
             toteutuneet-maarat]]

           (tallennus valmis-tallennettavaksi?)])))))

(defn avaa-paikkausilmoitus [paikkauskohteen-id]
  (go
    (let [urakka-id (:id @nav/valittu-urakka)
          [sopimus-id _] @u/valittu-sopimusnumero
          vastaus (<! (paikkaus/hae-paikkausilmoitus-paikkauskohteella urakka-id sopimus-id paikkauskohteen-id))]
      (log "Paikkausilmoitus kohteelle " paikkauskohteen-id " => " (pr-str vastaus))
      (if-not (k/virhe? vastaus)
        (reset! paikkaus/paikkausilmoitus-lomakedata vastaus)))))

(defn ilmoitusluettelo
  []
  (komp/luo
    (komp/kuuntelija :avaa-paikkausilmoitus
                     (fn [_ rivi]
                       (avaa-paikkausilmoitus (:paikkauskohde-id rivi))))
    (fn []
      [:div
       [grid/grid
        {:otsikko  "Paikkausilmoitukset"
         :tyhja    (if (nil? @paikkaus/paikkaustoteumat) [ajax-loader "Haetaan ilmoituksia..."] "Ei ilmoituksia")
         :tunniste :kohdenumero}
        [{:otsikko "#" :nimi :kohdenumero :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
         {:otsikko "Nimi" :nimi :nimi :muokattava? (constantly false) :tyyppi :string :leveys "50%"}
         {:otsikko "Tila" :nimi :tila :muokattava? (constantly false) :tyyppi :string :leveys "20%" :hae (fn [rivi]
                                                                                                           (nayta-tila (:tila rivi)))}
         {:otsikko "Päätös" :nimi :paatos :muokattava? (constantly false) :tyyppi :komponentti :leveys "20%" :komponentti (fn [rivi]
                                                                                                                            (nayta-paatos (:paatos rivi)))}
         {:otsikko     "Paikkaus\u00ADilmoitus" :nimi :paikkausilmoitus :muokattava? (constantly false) :leveys "25%" :tyyppi :komponentti
          :komponentti (fn [rivi] (if (:tila rivi) [:button.nappi-toissijainen.nappi-grid {:on-click #(avaa-paikkausilmoitus (:paikkauskohde_id rivi))}
                                                    [:span (ikonit/eye-open) " Paikkausilmoitus"]]
                                                   [:button.nappi-toissijainen.nappi-grid {:on-click #(avaa-paikkausilmoitus (:paikkauskohde_id rivi))}
                                                    [:span "Aloita paikkausilmoitus"]]))}]
        (sort-by
          (juxt (fn [toteuma] (case (:tila toteuma)
                                :lukittu 0
                                :valmis 1
                                :aloitettu 3
                                4))
                (fn [toteuma] (case (:paatos toteuma)
                                :hyvaksytty 0
                                :hylatty 1
                                3)))
          @paikkaus/paikkaustoteumat)]])))

(defn paikkausilmoitukset []
  (komp/luo
    (komp/lippu paikkaus/paikkausilmoitukset-nakymassa?)

    (fn []
      [:span
       [kartta/kartan-paikka]
       (if @paikkaus/paikkausilmoitus-lomakedata
         [paikkausilmoituslomake]
         [ilmoitusluettelo])])))
