(ns harja.ui.palaute
  (:require [clojure.string :as string]
            [harja.ui.ikonit :as ikonit]
            [clojure.string :as str]
            [reagent.core :refer [atom]]
            [harja.tiedot.istunto :as istunto]
            [harja.asiakas.tapahtumat :as t]))

(def sahkoposti "harjapalaute@solita.fi")

(def url (atom nil))
(defonce aseta-url
  (t/kuuntele! :url-muuttui #(reset! url (:url %))))


;; Huomaa että rivinvaihto tulee mukaan tekstiin
(def palaute-otsikko
  "Palautetta HARJAsta")
(def palaute-body
  (str "Kerro meille mitä yritit tehdä, ja millaiseen ongelmaan törmäsit. Harkitse kuvakaappauksen "
       "mukaan liittämistä, ne ovat meille erittäin hyödyllisiä. "
       "Ota kuvakaappaukseen mukaan koko selainikkuna."))

(def virhe-otsikko
  "HARJA räsähti")

(defn tekniset-tiedot [kayttaja url]
  (let [enc #(.encodeURIComponent js/window %)]
    (str "\n---\n"
         "Sijainti Harjassa: " (enc url) "\n"
         "Käyttäjä: " (enc (pr-str kayttaja)))))

(defn virhe-body [virheviesti kayttaja url]
  (str
   "\n---\n"
   "Kirjoita ylle, mitä olit tekemässä, kun virhe tuli vastaan. Kuvakaappaukset ovat meille myös "
   "hyvä apu. Ethän pyyhi alla olevia virheen teknisiä tietoja pois."
   "\n---\nTekniset tiedot:\n"
   virheviesti
   (tekniset-tiedot kayttaja url)))

(defn- mailto []
  (str "mailto:" sahkoposti))

(defn- ilman-valimerkkeja [str]
  (-> str
      (string/replace " " "%20")
      (string/replace "\n" "%0A")))

(defn- lisaa-kentta
  ([kentta pohja lisays] (lisaa-kentta kentta pohja lisays "&"))
  ([kentta pohja lisays valimerkki] (str pohja valimerkki kentta (ilman-valimerkkeja lisays))))

(defn- subject
  ([pohja lisays] (lisaa-kentta "subject=" pohja lisays))
  ([pohja lisays valimerkki] (lisaa-kentta "subject=" pohja lisays valimerkki)))

(defn- body
  ([pohja lisays] (lisaa-kentta "body=" pohja lisays))
  ([pohja lisays valimerkki] (lisaa-kentta "body=" pohja lisays valimerkki)))


(defn palaute-linkki []
  [:a#palautelinkki
   {:href (-> (mailto)
              (subject palaute-otsikko "?")
              (body (str palaute-body (tekniset-tiedot @istunto/kayttaja @url))))}
   [:span (ikonit/livicon-kommentti) " Palautetta!"]])

(defn virhe-palaute [virhe]
  [:a#palautelinkki
   {:href (-> (mailto)
              (subject virhe-otsikko "?")
              (body (virhe-body virhe @istunto/kayttaja @url)))
    :on-click #(.stopPropagation %)}
   [:span
    [ikonit/envelope]
    [:span " Hupsista, Harja räsähti! Olemme pahoillamme. Kuulisimme mielellämme miten sait vian esiin. Klikkaa tähän, niin pääset lähettämään virheraportin."]]])
