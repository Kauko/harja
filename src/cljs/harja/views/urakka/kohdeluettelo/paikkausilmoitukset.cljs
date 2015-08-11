(ns harja.views.urakka.kohdeluettelo.paikkausilmoitukset
  "Urakan kohdeluettelon paikkausilmoitukset"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.lomake :as lomake]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]

            [harja.tiedot.navigaatio :as nav]
            [harja.fmt :as fmt]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<!]]
            [harja.tiedot.urakka :as u]
            [harja.ui.lomake :refer [lomake]]
            [harja.tiedot.urakka.kohdeluettelo.paikkaus :as paikkaus]
            [harja.domain.roolit :as roolit]
            [harja.ui.kommentit :as kommentit]
            [harja.ui.yleiset :as yleiset])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

; FIXME Testin vuoksi atomissa on tyhjä mappi, jotta lomake saadaan näkyviin. Myöhemmin tulee atom nil.
(def lomakedata (atom {})) ; Vastaa rakenteeltaan paikkausilmoitus-taulun sisältöä

(defonce toteumarivit (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                   [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                   nakymassa? @paikkaus/paikkausilmoitukset-nakymassa?]
                                  (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                                    (log "PAI Haetaan paikkausilmoitukset")
                                    []
                                    ; TODO palvelu puuttuu
                                    #_(paallystys/hae-paallystystoteumat valittu-urakka-id valittu-sopimus-id))))

(defn kuvaile-paatostyyppi [paatos]
  (case paatos
    :hyvaksytty "Hyväksytty"
    :hylatty "Hylätty"))

(defn kasittely
  "Ilmoituksen käsittelyosio, kun ilmoitus on valmis. Tilaaja voi muokata, urakoitsija voi tarkastella."
  [valmis-kasiteltavaksi?]
  (let [muokattava? (constantly (and
                                  (roolit/roolissa? roolit/urakanvalvoja)
                                  (and (not (= (:tila @lomakedata) :lukittu)))))
        paatostiedot (r/wrap {:paatos        (:paatos @lomakedata)
                              :perustelu     (:perustelu @lomakedata)
                              :kasittelyaika (:kasittelyaika @lomakedata)}
                             (fn [uusi-arvo] (reset! lomakedata (-> (assoc @lomakedata :paatos (:paatos uusi-arvo))
                                                                    (assoc :perustelu (:perustelu uusi-arvo))
                                                                    (assoc :kasittelyaika (:kasittelyaika uusi-arvo))))))]
    (when @valmis-kasiteltavaksi?
      [:div.paikkausilmoitus-kasittely
       [:h3 "Käsittely"]
       [lomake/lomake
        {:luokka   :horizontal
         :muokkaa! (fn [uusi]
                     (reset! paatostiedot uusi))
         :voi-muokata? (muokattava?)}
        [{:otsikko     "Käsitelty"
          :nimi        :kasittelyaika
          :tyyppi      :pvm
          :validoi     [[:ei-tyhja "Anna käsittelypäivämäärä"]]}

         {:otsikko       "Päätös"
          :nimi          :paatos
          :tyyppi        :valinta
          :valinnat      [:hyvaksytty :hylatty]
          :validoi       [[:ei-tyhja "Anna päätös"]]
          :valinta-nayta #(if % (kuvaile-paatostyyppi %) (if (muokattava?) "- Valitse päätös -" "-"))
          :leveys-col    3}

         (when (:paatos @paatostiedot)
           {:otsikko     "Selitys"
            :nimi        :perustelu-tekninen-osa
            :tyyppi      :text
            :koko        [60 3]
            :pituus-max  2048
            :leveys-col  6
            :validoi     [[:ei-tyhja "Anna päätöksen selitys"]]})]
        @paatostiedot]])))

(defn tallennus
  [valmis-tallennettavaksi?]
  (let [huomautusteksti (reaction (let [valmispvm-kohde (:valmispvm_kohde @lomakedata)
                                        valmispvm-paikkaus (:valmispvm_paikkaus @lomakedata)
                                        paatos (:paatos @lomakedata)
                                        tila (:tila @lomakedata)]
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
      #(let [lomake @lomakedata
             lahetettava-data (-> (grid/poista-idt lomake [:ilmoitustiedot :osoitteet])
                                  (grid/poista-idt [:ilmoitustiedot :tyot]))]
        (log "PAI Lomake-data: " (pr-str @lomakedata))
        (log "PAIK Lähetetään data " (pr-str lahetettava-data))
        #_(paikkaus/tallenna-paallystysilmoitus urakka-id sopimus-id lahetettava-data)) ; TODO Palvelu puuttuu
      {:luokka       "nappi-ensisijainen"
       :disabled     (false? @valmis-tallennettavaksi?)
       :ikoni (ikonit/tallenna)
       :kun-onnistuu (fn [vastaus]
                       (log "PAI Lomake tallennettu, vastaus: " (pr-str vastaus))
                       (reset! toteumarivit vastaus)
                       (reset! lomakedata nil))}]]))

(defn paikkausilmoituslomake []
  (let [kohteen-tiedot (r/wrap {:aloituspvm     (:aloituspvm @lomakedata)
                                :valmispvm_kohde (:valmispvm_kohde @lomakedata)
                                :valmispvm_paikkaus (:valmispvm_paikkaus @lomakedata)}
                               (fn [uusi-arvo]
                                 (reset! lomakedata (-> (assoc @lomakedata :aloituspvm (:aloituspvm uusi-arvo))
                                                        (assoc :valmispvm_kohde (:valmispvm_kohde uusi-arvo))
                                                        (assoc :valmispvm_paikkaus (:valmispvm_paikkaus uusi-arvo))))))

        toteutuneet-osoitteet
        (r/wrap (zipmap (iterate inc 1) (:osoitteet (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :osoitteet] (grid/filteroi-uudet-poistetut uusi-arvo)))))
        toteutuneet-maarat
        (r/wrap (zipmap (iterate inc 1) (:tyot (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :tyot] (grid/filteroi-uudet-poistetut uusi-arvo)))))

        toteutuneet-osoitteet-virheet (atom {})
        toteutuneet-maarat-virheet (atom {})

        valmis-tallennettavaksi? (reaction
                                   (let [toteutuneet-osoitteet-virheet @toteutuneet-osoitteet-virheet
                                         toteutuneet-maarat-virheet @toteutuneet-maarat-virheet
                                         tila (:tila @lomakedata)]
                                     (and
                                       (not (= tila :lukittu))
                                       (empty? toteutuneet-osoitteet-virheet)
                                       (empty? toteutuneet-maarat-virheet))))
        valmis-kasiteltavaksi? (reaction
                                 (let [valmispvm-kohde (:valmispvm_kohde @lomakedata)
                                       tila (:tila @lomakedata)]
                                   (log "PAI valmis käsi " (pr-str valmispvm-kohde) (pr-str tila))
                                   (and tila
                                        valmispvm-kohde
                                        (not (= tila :aloitettu))
                                        (not (nil? valmispvm-kohde)))))]

    (komp/luo
      (fn []
        [:div.paikkausilmoituslomake

         [:button.nappi-toissijainen {:on-click #(reset! lomakedata nil)}
          (ikonit/chevron-left) " Takaisin ilmoitusluetteloon"]

         [:h2 "Paikkausilmoitus"]

         [:div.row
          [:div.col-md-6
           [:h3 "Perustiedot"]
           [lomake {:luokka   :horizontal
                    :voi-muokata? (not= :lukittu (:tila @lomakedata))
                    :muokkaa! (fn [uusi]
                                (log "PAI Muokataan kohteen tietoja: " (pr-str uusi))
                                (reset! kohteen-tiedot uusi))}
            [{:otsikko "Kohde" :nimi :kohde :hae (fn [_] (str "#" (:kohdenumero @lomakedata) " " (:kohdenimi @lomakedata))) :muokattava? (constantly false)}
             {:otsikko "Työ aloitettu" :nimi :aloituspvm :tyyppi :pvm}
             {:otsikko "Paikkaus valmistunut" :nimi :valmispvm_paallystys :tyyppi :pvm}
             {:otsikko "Kohde valmistunut" :nimi :valmispvm_kohde
              :vihje   (when (and
                               (:valmispvm_paallystys @lomakedata)
                               (:valmispvm_kohde @lomakedata)
                               (= :aloitettu (:tila @lomakedata)))
                         "Kohteen valmistumispäivämäärä annettu, ilmoitus tallennetaan valmiina urakanvalvojan käsiteltäväksi.")
              :tyyppi  :pvm :validoi [[:pvm-annettu-toisen-jalkeen :valmispvm_paallystys "Kohdetta ei voi merkitä valmistuneeksi ennen kuin paikkaus on valmistunut."]]}
             {:otsikko "Toteutunut hinta" :nimi :hinta :tyyppi :numero :leveys-col 2 :muokattava? (constantly false)}
             (when (or (= :valmis (:tila @lomakedata))
                       (= :lukittu (:tila @lomakedata)))
               {:otsikko     "Kommentit" :nimi :kommentit
                :komponentti [kommentit/kommentit {:voi-kommentoida? true
                                                   :voi-liittaa      false
                                                   :leveys-col       40
                                                   :placeholder      "Kirjoita kommentti..."
                                                   :uusi-kommentti   (r/wrap (:uusi-kommentti @lomakedata)
                                                                             #(swap! lomakedata assoc :uusi-kommentti %))}
                              (:kommentit @lomakedata)]})
             ]
            @kohteen-tiedot]]

          [:div.col-md-6
           (kasittely valmis-kasiteltavaksi?)]]

         [:fieldset.lomake-osa
          [:legend "Ilmoitustiedot"]

          #_[grid/muokkaus-grid
           {:otsikko      "Päikatut tierekisteriosoitteet"
            :tunniste     :tie
            :voi-muokata? (do
                            (log "PAI tila " (pr-str (:tila @lomakedata)) " Päätös tekninen: " (pr-str (:paatos_tekninen_osa @lomakedata)))
                            (not (or (= :lukittu (:tila @lomakedata))
                                     (= :hyvaksytty (:paatos_tekninen_osa @lomakedata)))))
            :rivinumerot? true
            :muutos       (fn [g]
                            (let [grid-data (vals @toteutuneet-osoitteet)]
                              (reset! toteutuneet-osoitteet (zipmap (iterate inc 1)
                                                                    (mapv
                                                                      (fn [rivi] (assoc rivi :tie (:tie (first grid-data))))
                                                                      grid-data)))
                              (reset! toteutuneet-osoitteet-virheet (grid/hae-virheet g))))}
           [{:otsikko     "Tie#" :nimi :tie :tyyppi :numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]
             :muokattava? (fn [rivi index] (if (> index 0) false true))}
            {:otsikko       "Ajorata"
             :nimi          :ajorata
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta #(if % (:nimi %) "- Valitse ajorata -")
             :valinnat      pot/+ajoradat+
             :leveys        "20%"
             :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko       "Suunta"
             :nimi          :suunta
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta #(if % (:nimi %) "- Valitse suunta -")
             :valinnat      pot/+suunnat+
             :leveys        "20%"
             :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko       "Kaista"
             :nimi          :kaista
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta #(if % (:nimi %) "- Valitse kaista -")
             :valinnat      pot/+kaistat+
             :leveys        "20%"
             :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko "Alkutieosa" :nimi :aosa :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko "Alkuetäisyys" :nimi :aet :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko "Lopputieosa" :nimi :losa :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko "Loppuetäisyys" :nimi :let :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
            {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :numero :muokattava? (constantly false) :hae (fn [rivi] (- (:let rivi) (:losa rivi)))}]
           toteutuneet-osoitteet]

          #_[grid/muokkaus-grid
           {:otsikko "Alustalle tehdyt toimet"
            :voi-muokata? (not (or (= :lukittu (:tila @lomakedata))
                                   (= :hyvaksytty (:paatos_tekninen_osa @lomakedata))))
            :muutos  #(reset! alustalle-tehdyt-toimet-virheet (grid/hae-virheet %))}
           [{:otsikko "Alkutieosa" :nimi :aosa :tyyppi :numero :leveys "10%" :pituus-max 256}
            {:otsikko "Alkuetäisyys" :nimi :aet :tyyppi :numero :leveys "10%"}
            {:otsikko "Lopputieosa" :nimi :losa :tyyppi :numero :leveys "10%"}
            {:otsikko "Loppuetäisyys" :nimi :let :leveys "10%" :tyyppi :numero}
            {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :numero :muokattava? (constantly false) :hae (fn [rivi] (- (:let rivi) (:losa rivi)))}
            {:otsikko       "Käsittelymenetelmä"
             :nimi          :kasittelymenetelma
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta (fn [rivi]
                              (if rivi
                                (str (:lyhenne rivi)  " - " (:nimi rivi))
                                "- Valitse menetelmä -"))
             :valinnat      pot/+alustamenetelmat+
             :leveys        "30%"}
            {:otsikko "Käsittelypaks. (cm)" :nimi :paksuus :leveys "10%" :tyyppi :numero}
            {:otsikko       "Verkkotyyppi"
             :nimi          :verkkotyyppi
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta #(if % (:nimi %) "- Valitse verkkotyyppi -")
             :valinnat      pot/+verkkotyypit+
             :leveys        "30%"}
            {:otsikko       "Tekninen toimenpide"
             :nimi          :tekninen-toimenpide
             :tyyppi        :valinta
             :valinta-arvo  :koodi
             :valinta-nayta #(if % (:nimi %) "- Valitse toimenpide -")
             :valinnat      pot/+tekniset-toimenpiteet+
             :leveys        "30%"}]
           alustalle-tehdyt-toimet]]

         (tallennus valmis-tallennettavaksi?)]))))

(defn ilmoitusluettelo
  []
  (let []

    (komp/luo
      (fn []
        [:div
         [grid/grid
          {:otsikko  "Paikkausilmoitukset"
           :tyhja    (if (nil? @toteumarivit) [ajax-loader "Haetaan ilmoituksia..."] "Ei ilmoituksia")
           :tunniste :kohdenumero}
          [{:otsikko "#" :nimi :kohdenumero :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Nimi" :nimi :nimi :muokattava? (constantly false) :tyyppi :string :leveys "50%"}
           {:otsikko "Paikkausilmoitus" :nimi :paikkausilmoitus :muokattava? (constantly false) :leveys "25%" :tyyppi :komponentti
            :komponentti (fn [rivi] (if (:tila rivi) [:button.nappi-toissijainen.nappi-grid {:on-click #(go
                                                                                                         ; TODO
                                                                                                         #_(let [urakka-id (:id @nav/valittu-urakka)
                                                                                                               [sopimus-id _] @u/valittu-sopimusnumero
                                                                                                               vastaus (<! (paallystys/hae-paallystysilmoitus-paallystyskohteella urakka-id sopimus-id (:paallystyskohde_id rivi)))]
                                                                                                           (log "PAI Rivi: " (pr-str rivi))
                                                                                                           (log "PAI Vastaus: " (pr-str vastaus))
                                                                                                           (if-not (k/virhe? vastaus)
                                                                                                             (reset! lomakedata (-> (assoc vastaus :paallystyskohde-id (:paallystyskohde_id rivi))
                                                                                                                                    (assoc :kokonaishinta (+ (:sopimuksen_mukaiset_tyot rivi)
                                                                                                                                                             (:arvonvahennykset rivi)
                                                                                                                                                             (:bitumi_indeksi rivi)
                                                                                                                                                             (:kaasuindeksi rivi))))))))}
                                                      [:span (ikonit/eye-open) " Paikkausilmoitus"]]
                                                     [:button.nappi-toissijainen.nappi-grid {:on-click #(reset! lomakedata {:kohdenumero        (:kohdenumero rivi)
                                                                                                                            :kohdenimi          (:nimi rivi)
                                                                                                                            :paikkauskohde-id   (:paikkauskohde_id rivi)})}
                                                      [:span " Tee paikkausilmoitus"]]))}]
          (sort-by
            (fn [toteuma] (case (:tila toteuma)
                            :lukittu 0
                            :valmis 1
                            :aloitettu 3
                            4))
            @toteumarivit)]]))))

(defn paikkausilmoitukset []
  (komp/luo
    (komp/lippu paikkaus/paikkausilmoitukset-nakymassa?)

    (fn []
      (if @lomakedata
        [paikkausilmoituslomake]
        [ilmoitusluettelo]))))