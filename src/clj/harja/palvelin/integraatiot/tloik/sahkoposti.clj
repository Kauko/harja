(ns harja.palvelin.integraatiot.tloik.sahkoposti
  "Ilmoitusten lähettäminen urakoitsijalle ja kuittausten vastaanottaminen"
  (:require [hiccup.core :refer [html]]
            [harja.domain.ilmoitukset :as apurit]
            [clojure.string :as str]
            [harja.palvelin.integraatiot.tloik.ilmoitustoimenpiteet :as ilmoitustoimenpiteet]
            [harja.kyselyt.yhteyshenkilot :as yhteyshenkilot]
            [harja.kyselyt.ilmoitukset :as ilmoitukset]
            [taoensso.timbre :as log]
            [harja.geo :as geo]))


(def ^{:doc "Ilmoituksen otsikon regex pattern, josta urakka ja ilmoitusid tunnistetaan" :const true :private true}
  otsikko-pattern #".*\#\[(\d+)/(\d+)\].*")

(def ^{:doc "Kuittaustyypit, joita sähköpostilla voi ilmoittaa" :const true :private true}
  kuittaustyypit [["Vastaanotettu" :vastaanotettu]
                  ["Aloitettu" :aloitettu]
                  ["Lopetettu" :lopetettu]])

(def ^{:doc "Kuittaustyypin tunnistava regex pattern" :const true :private true}
  kuittaustyyppi-pattern #"\[(Vastaanotettu|Aloitettu|Lopetettu)\]")

(def ^{:doc "Viesti, joka lähetetään vastaanottajalle kun saadaan sisään sähköposti, jota ei tunnisteta" :private true}
  +virheellinen-toimenpide-viesti+
  {:otsikko "Virheellinen kuittausviesti"
   :sisalto "Lähettämäsi viestistä ei voitu päätellä kuittauksen tietoja."})

(def ^{:doc "Viesti, joka lähetetään jos päivystäjätietoja tai ilmoitustietoja ei voida päätellä" :private true}
  +ilmoitustoimenpiteen-tallennus-epaonnistui+
  {:otsikko "Kuittausta ei voitu käsitellä"
   :sisalto "Varmista, että vastaat samalla sähköpostiosoitteella, johon ilmoitustiedot toimitettiin."})

(def ^{:doc "Viesti, joka lähetetään onnistuneen ilmoitustoimenpiteen tallennuksen jälkeen." :private true}
  +onnistunut-viesti+
  {:otsikko nil ;; tämä täydennetään ilmoituksen otsikolla
   :sisalto "Kuittaus käsiteltiin onnistuneesti. Kiitos!"})

(def ^{:doc "Template, jolla muodostetaan URL Google static map kuvalle" :private true :const true}
  goole-static-map-url-template
  "http://maps.googleapis.com/maps/api/staticmap?zoom=15&markers=color:red|%s,%s&size=400x300&key=%s")

(defn- otsikko [{:keys [ilmoitus-id urakka-id ilmoitustyyppi]}]
  (str "#[" urakka-id "/" ilmoitus-id "] " (apurit/ilmoitustyypin-nimi (keyword ilmoitustyyppi))))

(defn- html-nappi [napin-teksti linkki]
  [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
   [:tr
    [:td
     [:table {:border "0" :cellspacing "0" :cellpadding "0"}
      [:tr
       [:td {:bgcolor "#EB7035" :style "padding: 12px; border-radius: 3px;" :align "center"}
        [:a {:href linkki
             :style "font-size: 16px; font-family: Helvetica, Arial, sans-serif; font-weight: normal; color: #ffffff; text-decoration: none; display: inline-block;"}
         napin-teksti]]]]]]])

(defn- html-mailto-nappi
  "Luo HTML-fragmentin mailto: napin sähköpostia varten. Tämä täytyy tyylitellä inline, koska ei voida
resursseja liitää sähköpostiin mukaan luotettavasti."
  [vastausosoite napin-teksti subject body]
  (html-nappi napin-teksti
              (str "mailto:" vastausosoite "?subject=" subject "&body=" body)))

(defn- viesti [vastausosoite otsikko ilmoitus google-static-maps-key]
  (html
   [:div
    [:table
     (for [[kentta arvo] [["Ilmoitettu" (:ilmoitettu ilmoitus)]
                          ["Otsikko" (:otsikko ilmoitus)]
                          ["Paikan kuvaus" (:paikankuvaus ilmoitus)]
                          ["Selitteet" (apurit/parsi-selitteet (mapv keyword (:selitteet ilmoitus)))]
                          ["Ilmoittaja" (apurit/nayta-henkilo (:ilmoittaja ilmoitus))]]]
       [:tr
        [:td [:b kentta]]
        [:td arvo]])]
    [:blockquote (:lisatieto ilmoitus)]
    (when-let [sijainti (:sijainti ilmoitus)]
      (let [[lat lon] (geo/euref->wgs84 [(:x sijainti) (:y sijainti)])]
        [:img {:src (format goole-static-map-url-template
                            lat lon google-static-maps-key)}]))
    (for [teksti (map first kuittaustyypit)]
      [:div {:style "padding-top: 10px;"}
       (html-mailto-nappi vastausosoite teksti otsikko (str "[" teksti "]"))])]))

(defn otsikko-ja-viesti [vastausosoite ilmoitus google-static-maps-key]
  (let [otsikko (otsikko ilmoitus)
        viesti (viesti vastausosoite otsikko ilmoitus
                       google-static-maps-key)]
    [otsikko viesti]))

(defn viestin-kuittaustyyppi [sisalto]
  (when-let [nimi (some->> sisalto
                           (re-find kuittaustyyppi-pattern)
                           second)]
    (second (first (filter #(= (first %) nimi) kuittaustyypit)))))

(defn viesti-ilman-kuittaustyyppia [sisalto]
  (str/replace sisalto kuittaustyyppi-pattern ""))

(defn lue-kuittausviesti
  "Lukee annetun kuittausviestin otsikosta ja sisällöstä kuittauksen tiedot mäpiksi"
  [otsikko sisalto]
  (let [[_ urakka-id ilmoitus-id] (re-matches otsikko-pattern otsikko)
        kuittaustyyppi (viestin-kuittaustyyppi sisalto)
        kommentti (str/trim (viesti-ilman-kuittaustyyppia sisalto))]
    (if (and urakka-id ilmoitus-id kuittaustyyppi)
      {:urakka-id (Long/parseLong urakka-id)
       :ilmoitus-id (Long/parseLong ilmoitus-id)
       :kuittaustyyppi kuittaustyyppi
       :kommentti (when-not (str/blank? kommentti)
                    kommentti)}
      {:virhe "Viestistä ei löytynyt kuittauksen tietoja"})))

(def ^{:doc "Vastaanotetun kuittauksen mäppäys kuittaustyyppi tietokantaenumiksi" :private true}
  kuittaustyyppi->enum {:vastaanotettu "vastaanotto"
                        :aloitettu "aloitus"
                        :lopetettu "lopetus"})

(defn- tallenna-ilmoitustoimenpide [jms-lahettaja db lahettaja {:keys [urakka-id ilmoitus-id kuittaustyyppi kommentti]}]
  (let [paivystaja (first (yhteyshenkilot/hae-urakan-paivystaja-sahkopostilla db urakka-id lahettaja))
        ilmoitus (:id (first (ilmoitukset/hae-ilmoitus-ilmoitus-idlla db ilmoitus-id)))]
    (if-not paivystaja
      +ilmoitustoimenpiteen-tallennus-epaonnistui+
      (let [ilmoitustoimenpide-id (ilmoitustoimenpiteet/tallenna-ilmoitustoimenpide
                                   db ilmoitus ilmoitus-id kommentti
                                   (kuittaustyyppi->enum kuittaustyyppi) paivystaja)]
        (ilmoitustoimenpiteet/laheta-ilmoitustoimenpide jms-lahettaja db ilmoitustoimenpide-id)
        (assoc +onnistunut-viesti+
               :otsikko (otsikko {:ilmoitus-id (:ilmoitusid ilmoitus)
                                  :urakka-id (:urakka ilmoitus)
                                  :ilmoitustyyppi (:ilmoitustyyppi ilmoitus)}))))))

(defn vastaanota-sahkopostikuittaus
  "Käsittelee sisään tulevan sähköpostikuittauksen ja palauttaa takaisin viestin, joka lähetetään 
kuittauksen lähettäjälle."
  [jms-lahettaja db {:keys [lahettaja otsikko sisalto]}]
  (log/debug (format "Vastaanotettiin T-LOIK kuittaus sähköpostilla. Viesti: %s." viesti))
  (let [v (lue-kuittausviesti otsikko sisalto)]
    (if (:ilmoitus-id v)
      (tallenna-ilmoitustoimenpide jms-lahettaja db lahettaja v)
      +virheellinen-toimenpide-viesti+)))
