(ns harja.views.urakka.paallystysilmoitukset
  "Urakan päällystysilmoitukset"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<!]]

            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.lomake :as lomake]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.ui.kommentit :as kommentit]
            [harja.ui.yleiset :as yleiset]

            [harja.domain.paallystysilmoitus :as pot]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]

            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.paallystys :as paallystys]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.muokkauslukko :as lukko]

            [harja.fmt :as fmt]
            [harja.loki :refer [log logt tarkkaile!]]

            [harja.asiakas.kommunikaatio :as k]
            [harja.views.kartta :as kartta]
            [harja.domain.tierekisteri :as tierekisteri-domain]
            [harja.ui.napit :as napit]
            [harja.domain.oikeudet :as oikeudet]
            [harja.views.urakka.yllapitokohteet :as yllapitokohteet]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
            [harja.tiedot.urakka :as urakka]
            [harja.tiedot.istunto :as istunto])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(def urakkasopimuksen-mukainen-kokonaishinta (reaction (:kokonaishinta @paallystys/paallystysilmoitus-lomakedata)))
(def muutokset-kokonaishintaan
  (reaction (let [lomakedata @paallystys/paallystysilmoitus-lomakedata
                  tulos (pot/laske-muutokset-kokonaishintaan (get-in lomakedata [:ilmoitustiedot :tyot]))]
              (log "[PÄÄLLYSTYS] Muutokset kokonaishintaan laskettu: " tulos)
              tulos)))

(def toteuman-kokonaishinta (reaction (+ @urakkasopimuksen-mukainen-kokonaishinta @muutokset-kokonaishintaan)))

(tarkkaile! "[PÄÄLLYSTYS] Lomakedata: " paallystys/paallystysilmoitus-lomakedata)

(defn yhteenveto []
  (let []
    [yleiset/taulukkotietonakyma {}
     "Urakkasopimuksen mukainen kokonaishinta: " (fmt/euro-opt (or @urakkasopimuksen-mukainen-kokonaishinta 0))
     "Muutokset kokonaishintaan ilman kustannustasomuutoksia: " (fmt/euro-opt (or @muutokset-kokonaishintaan 0))
     "Yhteensä: " (fmt/euro-opt @toteuman-kokonaishinta)]))

(defn kasittely
  "Ilmoituksen käsittelyosio, kun ilmoitus on valmis. Tilaaja voi muokata, urakoitsija voi tarkastella."
  [valmis-kasiteltavaksi?]
  (let [muokattava? (and
                      (oikeudet/on-muu-oikeus? "päätös"
                                               oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                                               (:id @nav/valittu-urakka)
                                               @istunto/kayttaja)
                      (not= (:tila @paallystys/paallystysilmoitus-lomakedata) :lukittu)
                      (false? @paallystys/paallystysilmoituslomake-lukittu?))
        paatos-tekninen-osa
        (r/wrap {:paatos-tekninen
                 (:paatos-tekninen-osa @paallystys/paallystysilmoitus-lomakedata)
                 :perustelu-tekninen-osa
                 (:perustelu-tekninen-osa @paallystys/paallystysilmoitus-lomakedata)
                 :kasittelyaika-tekninen-osa
                 (:kasittelyaika-tekninen-osa @paallystys/paallystysilmoitus-lomakedata)}
                (fn [uusi-arvo]
                  (swap! paallystys/paallystysilmoitus-lomakedata
                         #(-> %
                              (assoc :paatos-tekninen-osa
                                     (:paatos-tekninen uusi-arvo))
                              (assoc :perustelu-tekninen-osa
                                     (:perustelu-tekninen-osa uusi-arvo))
                              (assoc :kasittelyaika-tekninen-osa
                                     (:kasittelyaika-tekninen-osa uusi-arvo))))))
        paatos-taloudellinen-osa
        (r/wrap {:paatos-taloudellinen
                 (:paatos-taloudellinen-osa @paallystys/paallystysilmoitus-lomakedata)
                 :perustelu-taloudellinen-osa
                 (:perustelu-taloudellinen-osa @paallystys/paallystysilmoitus-lomakedata)
                 :kasittelyaika-taloudellinen-osa
                 (:kasittelyaika-taloudellinen-osa @paallystys/paallystysilmoitus-lomakedata)}
                (fn [uusi-arvo]
                  (swap! paallystys/paallystysilmoitus-lomakedata
                         #(-> %
                              (assoc :paatos-taloudellinen-osa
                                     (:paatos-taloudellinen uusi-arvo))
                              (assoc :perustelu-taloudellinen-osa
                                     (:perustelu-taloudellinen-osa uusi-arvo))
                              (assoc :kasittelyaika-taloudellinen-osa
                                     (:kasittelyaika-taloudellinen-osa uusi-arvo))))))]

    (when @valmis-kasiteltavaksi?
      [:div.pot-kasittely
       [:h3 "Käsittely"]

       [lomake/lomake
        {:otsikko "Tekninen osa"
         :muokkaa! (fn [uusi]
                     (reset! paatos-tekninen-osa uusi))
         :voi-muokata? muokattava?}
        [{:otsikko "Käsitelty"
          :nimi :kasittelyaika-tekninen-osa
          :tyyppi :pvm
          :validoi [[:ei-tyhja "Anna käsittelypäivämäärä"]
                    [:pvm-toisen-pvmn-jalkeen (:valmispvm-kohde @paallystys/paallystysilmoitus-lomakedata) "Käsittely ei voi olla ennen valmistumista"]]}

         {:otsikko "Päätös"
          :nimi :paatos-tekninen
          :tyyppi :valinta
          :valinnat [:hyvaksytty :hylatty]
          :validoi [[:ei-tyhja "Anna päätös"]]
          :valinta-nayta #(if % (paallystys-ja-paikkaus/kuvaile-paatostyyppi %) (if muokattava? "- Valitse päätös -" "-"))
          :palstoja 1}

         (when (:paatos-tekninen @paatos-tekninen-osa)
           {:otsikko "Selitys"
            :nimi :perustelu-tekninen-osa
            :tyyppi :text
            :koko [60 3]
            :pituus-max 2048
            :palstoja 2
            :validoi [[:ei-tyhja "Anna päätöksen selitys"]]})]
        @paatos-tekninen-osa]


       [lomake/lomake
        {:otsikko "Taloudellinen osa"
         :muokkaa! (fn [uusi]
                     (reset! paatos-taloudellinen-osa uusi))
         :voi-muokata? muokattava?}
        [{:otsikko "Käsitelty"
          :nimi :kasittelyaika-taloudellinen-osa
          :tyyppi :pvm
          :validoi [[:ei-tyhja "Anna käsittelypäivämäärä"]
                    [:pvm-toisen-pvmn-jalkeen (:valmispvm-kohde @paallystys/paallystysilmoitus-lomakedata) "Käsittely ei voi olla ennen valmistumista"]]}

         {:otsikko "Päätös"
          :nimi :paatos-taloudellinen
          :tyyppi :valinta
          :valinnat [:hyvaksytty :hylatty]
          :validoi [[:ei-tyhja "Anna päätös"]]
          :valinta-nayta #(if % (paallystys-ja-paikkaus/kuvaile-paatostyyppi %) (if muokattava? "- Valitse päätös -" "-"))
          :palstoja 1}

         (when (:paatos-taloudellinen @paatos-taloudellinen-osa)
           {:otsikko "Selitys"
            :nimi :perustelu-taloudellinen-osa
            :tyyppi :text
            :koko [60 3]
            :pituus-max 2048
            :palstoja 2
            :validoi [[:ei-tyhja "Anna päätöksen selitys"]]})]
        @paatos-taloudellinen-osa]])))

(defn tallennus
  [valmis-tallennettavaksi?]
  (let [huomautusteksti
        (reaction (let [valmispvm-kohde (:valmispvm-kohde @paallystys/paallystysilmoitus-lomakedata)
                        valmispvm-paallystys (:valmispvm-paallystys @paallystys/paallystysilmoitus-lomakedata)
                        paatos-tekninen (:paatos-tekninen-osa @paallystys/paallystysilmoitus-lomakedata)
                        paatos-taloudellinen (:paatos-taloudellinen-osa @paallystys/paallystysilmoitus-lomakedata)
                        tila (:tila @paallystys/paallystysilmoitus-lomakedata)]
                    (cond (not (and valmispvm-kohde valmispvm-paallystys))
                          "Valmistusmispäivämäärää ei ole annettu, ilmoitus tallennetaan keskeneräisenä."
                          (and (not= :lukittu tila)
                               (= :hyvaksytty paatos-tekninen)
                               (= :hyvaksytty paatos-taloudellinen))
                          "Ilmoituksen molemmat osat on hyväksytty, ilmoitus lukitaan tallennuksen yhteydessä."
                          :else
                          nil)))
        urakka-id (:id @nav/valittu-urakka)
        [sopimus-id _] @u/valittu-sopimusnumero]

    [:div.pot-tallennus
     (when @huomautusteksti
       (lomake/yleinen-huomautus @huomautusteksti))

     [harja.ui.napit/palvelinkutsu-nappi
      "Tallenna"
      #(let [lomake @paallystys/paallystysilmoitus-lomakedata
             lahetettava-data (-> (grid/poista-idt lomake [:ilmoitustiedot :osoitteet])
                                  (grid/poista-idt [:ilmoitustiedot :alustatoimet])
                                  (grid/poista-idt [:ilmoitustiedot :tyot]))]
        (log "[PÄÄLLYSTYS] Lomake-data: " (pr-str @paallystys/paallystysilmoitus-lomakedata))
        (log "[PÄÄLLYSTYS] Lähetetään data " (pr-str lahetettava-data))
        (paallystys/tallenna-paallystysilmoitus! urakka-id sopimus-id lahetettava-data))
      {:luokka "nappi-ensisijainen"
       :disabled (false? @valmis-tallennettavaksi?)
       :ikoni (ikonit/tallenna)
       :virheviesti "Tallentaminen epäonnistui"
       :kun-onnistuu (fn [vastaus]
                       (log "[PÄÄLLYSTYS] Lomake tallennettu, vastaus: " (pr-str vastaus))
                       (urakka/lukitse-urakan-yha-sidonta! urakka-id)
                       (reset! paallystys/paallystysilmoitukset vastaus)
                       (reset! paallystys/paallystysilmoitus-lomakedata nil))}]]))

(defn paallystysilmoituslomake []
  (let [alikohteet-virheet (atom {})
        paallystystoimenpide-virheet (atom {})
        alustalle-tehdyt-toimet-virheet (atom {})
        toteutuneet-maarat-virheet (atom {})

        valmis-tallennettavaksi?
        (reaction
          (let [alikohteet-virheet @alikohteet-virheet
                paallystystoimenpide-virheet @paallystystoimenpide-virheet
                alustalle-tehdyt-toimet-virheet @alustalle-tehdyt-toimet-virheet
                toteutuneet-maarat-virheet @toteutuneet-maarat-virheet
                tila (:tila @paallystys/paallystysilmoitus-lomakedata)
                lomake-lukittu-muokkaukselta? @paallystys/paallystysilmoituslomake-lukittu?]
            (and
              (not (= tila :lukittu))
              (empty? alikohteet-virheet)
              (empty? paallystystoimenpide-virheet)
              (empty? alustalle-tehdyt-toimet-virheet)
              (empty? toteutuneet-maarat-virheet)
              (false? lomake-lukittu-muokkaukselta?))))
        valmis-kasiteltavaksi?
        (reaction
          (let [valmispvm-kohde (:valmispvm-kohde @paallystys/paallystysilmoitus-lomakedata)
                tila (:tila @paallystys/paallystysilmoitus-lomakedata)]
            (log "[PÄÄLLYSTYS] valmis käsi " (pr-str valmispvm-kohde) (pr-str tila))
            (and tila
                 valmispvm-kohde
                 (not (= tila :aloitettu))
                 (not (nil? valmispvm-kohde)))))]

    (komp/luo
      (komp/ulos #(kartta/poista-popup!))
      (komp/lukko (lukko/muodosta-lukon-id "paallystysilmoitus" (:kohdenumero @paallystys/paallystysilmoitus-lomakedata)))
      (fn []
        (let [lomakedata-nyt @paallystys/paallystysilmoitus-lomakedata
              kohteen-tiedot (r/wrap {:aloituspvm (:aloituspvm lomakedata-nyt)
                                      :valmispvm-kohde (:valmispvm-kohde lomakedata-nyt)
                                      :valmispvm-paallystys (:valmispvm-paallystys lomakedata-nyt)
                                      :takuupvm (:takuupvm lomakedata-nyt)}
                                     (fn [uusi-arvo]
                                       (reset! paallystys/paallystysilmoitus-lomakedata
                                               (-> (assoc lomakedata-nyt :aloituspvm (:aloituspvm uusi-arvo))
                                                   (assoc :valmispvm-kohde (:valmispvm-kohde uusi-arvo))
                                                   (assoc :valmispvm-paallystys (:valmispvm-paallystys uusi-arvo))
                                                   (assoc :takuupvm (:takuupvm uusi-arvo))
                                                   (assoc :hinta (:hinta uusi-arvo))))))

              ; Sisältää alikohteen päällystystoimenpiteen tiedot
              paallystystoimenpiteet
              (r/wrap (zipmap (iterate inc 1) (:osoitteet (:ilmoitustiedot lomakedata-nyt)))
                      (fn [uusi-arvo]
                        (reset! paallystys/paallystysilmoitus-lomakedata
                                  (assoc-in lomakedata-nyt [:ilmoitustiedot :osoitteet]
                                            (grid/filteroi-uudet-poistetut uusi-arvo)))))
              alustalle-tehdyt-toimet
              (r/wrap (zipmap (iterate inc 1) (:alustatoimet (:ilmoitustiedot lomakedata-nyt)))
                      (fn [uusi-arvo]
                        (reset! paallystys/paallystysilmoitus-lomakedata
                                (assoc-in lomakedata-nyt
                                          [:ilmoitustiedot :alustatoimet] (grid/filteroi-uudet-poistetut uusi-arvo)))))
              toteutuneet-maarat
              (r/wrap (zipmap (iterate inc 1) (:tyot (:ilmoitustiedot lomakedata-nyt)))
                      (fn [uusi-arvo]
                        (reset! paallystys/paallystysilmoitus-lomakedata
                                (assoc-in lomakedata-nyt [:ilmoitustiedot :tyot]
                                          (grid/filteroi-uudet-poistetut uusi-arvo)))))]
          [:div.paallystysilmoituslomake

           [napit/takaisin "Takaisin ilmoitusluetteloon" #(reset! paallystys/paallystysilmoitus-lomakedata nil)]

           (when @paallystys/paallystysilmoituslomake-lukittu?
             (lomake/lomake-lukittu-huomautus @lukko/nykyinen-lukko))

           [:h2 "Päällystysilmoitus"]

           [:div.row
            [:div.col-md-6
             [:h3 "Perustiedot"]
             [lomake/lomake {:voi-muokata? (and (not= :lukittu (:tila lomakedata-nyt))
                                                (false? @paallystys/paallystysilmoituslomake-lukittu?))
                             :muokkaa! (fn [uusi]
                                         (log "[PÄÄLLYSTYS] Muokataan kohteen tietoja: " (pr-str uusi))
                                         (swap! paallystys/paallystysilmoitus-lomakedata merge uusi))}
              [{:otsikko "Kohde" :nimi :kohde
                :hae (fn [_]
                       (str "#" (:kohdenumero lomakedata-nyt) " " (:kohdenimi lomakedata-nyt)))
                :muokattava? (constantly false)
                :palstoja 2}
               {:otsikko "Työ aloitettu" :nimi :aloituspvm :tyyppi :pvm :palstoja 2}
               {:otsikko "Päällystys valmistunut" :nimi :valmispvm-paallystys :tyyppi :pvm :palstoja 2}
               {:otsikko "Kohde valmistunut" :nimi :valmispvm-kohde :palstoja 2
                :vihje (when (and
                               (:valmispvm-paallystys lomakedata-nyt)
                               (:valmispvm-kohde lomakedata-nyt)
                               (= :aloitettu (:tila lomakedata-nyt)))
                         "Kohteen valmistumispäivämäärä annettu, ilmoitus tallennetaan valmiina urakanvalvojan käsiteltäväksi.")
                :tyyppi :pvm
                :validoi [[:pvm-ei-annettu-ennen-toista :valmispvm-paallystys "Kohdetta ei voi merkitä valmistuneeksi ennen kuin päällystys on valmistunut."]]}
               {:otsikko "Takuupvm" :nimi :takuupvm :tyyppi :pvm :palstoja 2}
               {:otsikko "Toteutunut hinta" :nimi :hinta :tyyppi :numero
                :palstoja 2 :hae #(fmt/euro-opt @toteuman-kokonaishinta) :muokattava? (constantly false)}
               (when (or (= :valmis (:tila lomakedata-nyt))
                         (= :lukittu (:tila lomakedata-nyt)))
                 {:otsikko "Kommentit" :nimi :kommentit
                  :palstoja 2
                  :tyyppi :komponentti
                  :komponentti
                  [kommentit/kommentit {:voi-kommentoida?
                                        (not= :lukittu (:tila lomakedata-nyt))
                                        :voi-liittaa false
                                        :palstoja 40
                                        :placeholder "Kirjoita kommentti..."
                                        :uusi-kommentti (r/wrap (:uusi-kommentti lomakedata-nyt)
                                                                #(swap! paallystys/paallystysilmoitus-lomakedata assoc :uusi-kommentti %))}
                   (:kommentit lomakedata-nyt)]})
               ]
              @kohteen-tiedot]]

            [:div.col-md-6
             (kasittely valmis-kasiteltavaksi?)]]

           [:fieldset.lomake-osa
            [:h3 "Tekninen osa"]

            [grid/muokkaus-grid
             {:otsikko "Tierekisteriosoitteet"
              :tunniste :tie
              :voi-muokata? (do
                              (log "[PÄÄLLYSTYS] tila " (pr-str (:tila lomakedata-nyt)) " Päätös tekninen: " (pr-str (:paatos-tekninen-osa lomakedata-nyt)))
                              (and (not= :lukittu (:tila lomakedata-nyt))
                                   (not= :hyvaksytty (:paatos-tekninen-osa lomakedata-nyt))
                                   (false? @paallystys/paallystysilmoituslomake-lukittu?)))
              :virheet alikohteet-virheet
              :rivinumerot? true
              :uusi-id (inc (count @paallystystoimenpiteet))}
             ;; FIXME Tarkista TR-osoite (samalla tavalla kuin yllapitokohteet-namespacessa)
             (yllapitokohteet/tierekisteriosoite-sarakkeet
               10
               [{:nimi :nimi}
                {:nimi :tie}
                {:nimi :ajorata}
                {:nimi :kaista}
                {:nimi :aosa}
                {:nimi :aet}
                {:nimi :losa}
                {:nimi :let}])
             paallystystoimenpiteet]

            [grid/muokkaus-grid
             {:otsikko "Päällystystoimenpiteen tiedot"
              :validoi-aina? true
              :voi-lisata? false
              :voi-kumota? false
              :voi-poistaa? (constantly false)
              :voi-muokata? (and (not= :lukittu (:tila lomakedata-nyt))
                                 (not= :hyvaksytty (:paatos-tekninen-osa lomakedata-nyt))
                                 (false? @paallystys/paallystysilmoituslomake-lukittu?))
              :virheet paallystystoimenpide-virheet
              :rivinumerot? true}
             [{:otsikko "Päällyste"
               :nimi :paallystetyyppi
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta (fn [rivi muokattava?]
                                (if rivi
                                  (str (:lyhenne rivi) " - " (:nimi rivi))
                                  (if muokattava?
                                    "- Valitse päällyste -"
                                    "")))
               :valinnat paallystys-ja-paikkaus/+paallystetyypit+
               :leveys "30%"}
              {:otsikko "Rae\u00ADkoko" :nimi :raekoko :tyyppi :numero :leveys "10%" :tasaa :oikea}
              {:otsikko "Massa (kg/m2)" :nimi :massa :tyyppi :positiivinen-numero :tasaa :oikea
               :leveys "10%"}
              {:otsikko "RC-%" :nimi :rc% :leveys "10%" :tyyppi :numero :tasaa :oikea}
              {:otsikko "Pääll. työ\u00ADmenetelmä"
               :nimi :tyomenetelma
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta (fn [rivi muokattava?]
                                (if rivi
                                  (str (:lyhenne rivi) " - " (:nimi rivi))
                                  (if muokattava?
                                    "- Valitse menetelmä -"
                                    "")))
               :valinnat pot/+tyomenetelmat+
               :leveys "30%"}
              {:otsikko "Leveys (m)" :nimi :leveys :leveys "10%" :tyyppi :positiivinen-numero
               :tasaa :oikea}
              {:otsikko "Massamäärä (kg/m2)" :nimi :massamaara :leveys "15%" :tyyppi :positiivinen-numero
               :tasaa :oikea}
              {:otsikko "Pinta-ala (m2)" :nimi :pinta-ala :leveys "10%" :tyyppi :positiivinen-numero
               :tasaa :oikea}
              {:otsikko "Edellinen päällyste"
               :nimi :edellinen-paallystetyyppi
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta (fn [rivi muokattava?]
                                (if rivi
                                  (str (:lyhenne rivi) " - " (:nimi rivi))
                                  (if muokattava?
                                    "- Valitse päällyste -"
                                    "")))
               :valinnat paallystys-ja-paikkaus/+paallystetyypit+
               :leveys "30%"}
              {:otsikko "Kuulamylly"
               :nimi :kuulamylly
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta (fn [rivi]
                                (if rivi
                                  (:nimi rivi)
                                  "- Valitse kuulamylly -"))
               :valinnat (conj pot/+kuulamyllyt+ {:nimi "Ei kuulamyllyä" :lyhenne "Ei kuulamyllyä" :koodi nil})
               :leveys "30%"}]
             paallystystoimenpiteet]

            [grid/muokkaus-grid
             {:otsikko "Kiviaines ja sideaine"
              :rivinumerot? true
              :validoi-aina? true
              :voi-lisata? false
              :voi-kumota? false
              :voi-poistaa? (constantly false)
              :voi-muokata? (and (not= :lukittu (:tila lomakedata-nyt))
                                 (not= :hyvaksytty (:paatos-tekninen-osa lomakedata-nyt))
                                 (false? @paallystys/paallystysilmoituslomake-lukittu?))
              :virheet paallystystoimenpide-virheet}
             [{:otsikko "Kiviaines\u00ADesiintymä" :nimi :esiintyma :tyyppi :string :pituus-max 256
               :leveys "30%"}
              {:otsikko "KM-arvo" :nimi :km-arvo :tyyppi :string :pituus-max 256 :leveys "20%"}
              {:otsikko "Muoto\u00ADarvo" :nimi :muotoarvo :tyyppi :string :pituus-max 256
               :leveys "20%"}
              {:otsikko "Sideaine\u00ADtyyppi" :nimi :sideainetyyppi :leveys "30%"
               :tyyppi :string :pituus-max 256}
              {:otsikko "Pitoisuus" :nimi :pitoisuus :leveys "20%" :tyyppi :numero :tasaa :oikea}
              {:otsikko "Lisä\u00ADaineet" :nimi :lisaaineet :leveys "20%" :tyyppi :string
               :pituus-max 256}]
             paallystystoimenpiteet]

            [grid/muokkaus-grid
             {:otsikko "Alustalle tehdyt toimet"
              :voi-muokata? (and (not= :lukittu (:tila lomakedata-nyt))
                                 (not= :hyvaksytty (:paatos-tekninen-osa lomakedata-nyt))
                                 (false? @paallystys/paallystysilmoituslomake-lukittu?))
              :uusi-id (inc (count @alustalle-tehdyt-toimet))
              :virheet alustalle-tehdyt-toimet-virheet}
             [{:otsikko "Aosa" :nimi :aosa :tyyppi :positiivinen-numero :leveys "10%"
               :pituus-max 256 :validoi [[:ei-tyhja "Tieto puuttuu"]] :tasaa :oikea}
              {:otsikko "Aet" :nimi :aet :tyyppi :positiivinen-numero :leveys "10%"
               :validoi [[:ei-tyhja "Tieto puuttuu"]] :tasaa :oikea}
              {:otsikko "Losa" :nimi :losa :tyyppi :positiivinen-numero :leveys "10%"
               :validoi [[:ei-tyhja "Tieto puuttuu"]] :tasaa :oikea}
              {:otsikko "Let" :nimi :let :leveys "10%" :tyyppi :positiivinen-numero
               :validoi [[:ei-tyhja "Tieto puuttuu"]] :tasaa :oikea}
              {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :numero :tasaa :oikea
               :muokattava? (constantly false) :hae (fn [rivi] (tierekisteri-domain/laske-tien-pituus rivi))}
              {:otsikko "Käsittely\u00ADmenetelmä"
               :nimi :kasittelymenetelma
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta (fn [rivi]
                                (if rivi
                                  (str (:lyhenne rivi) " - " (:nimi rivi))
                                  "- Valitse menetelmä -"))
               :valinnat pot/+alustamenetelmat+
               :leveys "30%"}
              {:otsikko "Käsit\u00ADtely\u00ADpaks. (cm)" :nimi :paksuus :leveys "15%"
               :tyyppi :positiivinen-numero :tasaa :oikea}
              {:otsikko "Verkko\u00ADtyyppi"
               :nimi :verkkotyyppi
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta #(if % (:nimi %) "- Valitse verkkotyyppi -")
               :valinnat pot/+verkkotyypit+
               :leveys "25%"}
              {:otsikko "Verkon sijainti"
               :nimi :verkon-sijainti
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta #(if % (:nimi %) "- Valitse verkon sijainti -")
               :valinnat pot/+verkon-sijainnit+
               :leveys "25%"}
              {:otsikko "Verkon tarkoitus"
               :nimi :verkon-tarkoitus
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta #(if % (:nimi %) "- Valitse verkon tarkoitus -")
               :valinnat pot/+verkon-tarkoitukset+
               :leveys "25%"}
              {:otsikko "Tekninen toimen\u00ADpide"
               :nimi :tekninen-toimenpide
               :tyyppi :valinta
               :valinta-arvo :koodi
               :valinta-nayta #(if % (:nimi %) "- Valitse toimenpide -")
               :valinnat pot/+tekniset-toimenpiteet+
               :leveys "30%"}]
             alustalle-tehdyt-toimet]]

           [:fieldset.lomake-osa
            [:h3 "Taloudellinen osa"]

            [grid/muokkaus-grid
             {:otsikko "Toteutuneet määrät"
              :voi-muokata? (and (not= :lukittu (:tila lomakedata-nyt))
                                 (not= :hyvaksytty (:paatos-taloudellinen-osa lomakedata-nyt))
                                 (false? @paallystys/paallystysilmoituslomake-lukittu?))
              :validoi-aina? true
              :uusi-id (inc (count @toteutuneet-maarat))
              :virheet toteutuneet-maarat-virheet}
             [{:otsikko "Päällyste\u00ADtyön tyyppi"
               :nimi :tyyppi
               :tyyppi :valinta
               :valinta-arvo :avain
               :valinta-nayta #(if % (:nimi %) "- Valitse työ -")
               :valinnat pot/+paallystystyon-tyypit+
               :leveys "30%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Työ" :nimi :tyo :tyyppi :string :leveys "30%" :pituus-max 256
               :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Yks." :nimi :yksikko :tyyppi :string :leveys "10%" :pituus-max 20
               :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Tilattu määrä" :nimi :tilattu-maara :tyyppi :positiivinen-numero :tasaa :oikea
               :kokonaisosan-maara 6 :leveys "15%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Toteu\u00ADtunut määrä" :nimi :toteutunut-maara :leveys "15%" :tasaa :oikea
               :tyyppi :positiivinen-numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Ero" :nimi :ero :leveys "15%" :tyyppi :numero :muokattava? (constantly false)
               :hae (fn [rivi] (- (:toteutunut-maara rivi) (:tilattu-maara rivi)))}
              {:otsikko "Yks.\u00ADhinta" :nimi :yksikkohinta :leveys "10%" :tasaa :oikea
               :tyyppi :positiivinen-numero :kokonaisosan-maara 4 :validoi [[:ei-tyhja "Tieto puuttuu"]]}
              {:otsikko "Muutos hintaan" :nimi :muutos-hintaan :leveys "15%" :tasaa :oikea
               :muokattava? (constantly false) :tyyppi :numero
               :hae (fn [rivi]
                      (* (- (:toteutunut-maara rivi) (:tilattu-maara rivi)) (:yksikkohinta rivi)))}]
             toteutuneet-maarat]]

           (yhteenveto)
           (tallennus valmis-tallennettavaksi?)])))))

(defn avaa-paallystysilmoitus [paallystyskohteen-id]
  (go
    (let [urakka-id (:id @nav/valittu-urakka)
          [sopimus-id _] @u/valittu-sopimusnumero
          vastaus (<! (paallystys/hae-paallystysilmoitus-paallystyskohteella urakka-id paallystyskohteen-id))]
      (log "Päällystysilmoitus kohteelle " paallystyskohteen-id " => " (pr-str vastaus))
      (if-not (k/virhe? vastaus)
        (reset! paallystys/paallystysilmoitus-lomakedata vastaus)))))

(defn ilmoitusluettelo
  []
  (komp/luo
    (komp/ulos #(kartta/poista-popup!))
    (komp/kuuntelija :avaa-paallystysilmoitus
                     (fn [_ rivi]
                       (avaa-paallystysilmoitus (:paallystyskohde-id rivi))))


    (fn []
      [:div
       [:h3 "Päällystysilmoitukset"]
       [grid/grid
        {:otsikko ""
         :tyhja (if (nil? @paallystys/paallystysilmoitukset) [ajax-loader "Haetaan ilmoituksia..."] "Ei ilmoituksia")
         :tunniste :kohdenumero}
        [{:otsikko "Kohdenumero" :nimi :kohdenumero :muokattava? (constantly false) :tyyppi :numero :leveys 10}
         {:otsikko "Nimi" :nimi :nimi :muokattava? (constantly false) :tyyppi :string :leveys 50}
         {:otsikko "Tila" :nimi :tila :muokattava? (constantly false) :tyyppi :string :leveys 20
          :hae (fn [rivi]
                 (paallystys-ja-paikkaus/nayta-tila (:tila rivi)))}
         {:otsikko "Päätös, tekninen" :nimi :paatos-tekninen-osa :muokattava? (constantly false) :tyyppi :komponentti :leveys 20
          :komponentti (fn [rivi]
                         (paallystys-ja-paikkaus/nayta-paatos (:paatos-tekninen-osa rivi)))}
         {:otsikko "Päätös, taloudel\u00ADlinen" :nimi :paatos-taloudellinen-osa :muokattava? (constantly false) :tyyppi :komponentti :leveys 20
          :komponentti (fn [rivi]
                         (paallystys-ja-paikkaus/nayta-paatos (:paatos-taloudellinen-osa rivi)))}
         {:otsikko "Päällystys\u00ADilmoitus" :nimi :paallystysilmoitus :muokattava? (constantly false) :leveys 25 :tyyppi :komponentti
          :komponentti (fn [rivi]
                         (if (:tila rivi)
                           [:button.nappi-toissijainen.nappi-grid
                            {:on-click #(avaa-paallystysilmoitus (:paallystyskohde-id rivi))}
                            [:span (ikonit/eye-open) " Päällystysilmoitus"]]
                           [:button.nappi-toissijainen.nappi-grid {:on-click #(avaa-paallystysilmoitus (:paallystyskohde-id rivi))}
                            [:span "Aloita päällystysilmoitus"]]))}]
        (sort-by
          (juxt (fn [toteuma] (case (:tila toteuma)
                                :lukittu 0
                                :valmis 1
                                :aloitettu 3
                                4))
                (fn [toteuma] (case (:paatos-tekninen-osa toteuma)
                                :hyvaksytty 0
                                :hylatty 1
                                3))
                (fn [toteuma] (case (:paatos-taloudellinen-osa toteuma)
                                :hyvaksytty 0
                                :hylatty 1
                                3)))
          @paallystys/paallystysilmoitukset)]])))

(defn paallystysilmoitukset []
  (komp/luo
    (komp/ulos #(kartta/poista-popup!))
    (komp/lippu paallystys/paallystysilmoitukset-nakymassa?)

    (fn []
      [:div.paallystysilmoitukset
       [kartta/kartan-paikka]
       (if @paallystys/paallystysilmoitus-lomakedata
         [paallystysilmoituslomake]
         [ilmoitusluettelo])])))
