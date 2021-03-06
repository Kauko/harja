(ns harja.palvelin.integraatiot.tierekisteri.sanomat.tietueen-hakukutsu
  (:require [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [taoensso.timbre :as log]
            [harja.tyokalut.xml :as xml]
            [hiccup.core :refer [html]])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def +xsd-polku+ "xsd/tierekisteri/skeemat/")

(defn muodosta-xml-sisalto [tunniste tietolajitunniste tilannepvm]
  [:ns2:haeTietue {:xmlns:ns2 "http://www.solita.fi/harja/tierekisteri/haeTietue"}
   [:tunniste tunniste]
   [:tietolajitunniste tietolajitunniste]
   (when tilannepvm [:tilannepvm tilannepvm])])

(defn muodosta-kutsu [tunniste tietolajitunniste tilannepvm]
  (let [sisalto (muodosta-xml-sisalto tunniste tietolajitunniste tilannepvm)
        xml (xml/tee-xml-sanoma sisalto)]
    (if (xml/validoi +xsd-polku+ "haeTietue.xsd" xml)
      xml
      (do
        (log/error "Tietueenhakukutsua ei voida lähettää. Kutsu XML ei ole validi.")
        (throw+
          {:type    :tietueen-haku-epaonnistui
           :virheet [{:koodi :ei-validi-xml :viesti "Tietueen hakukutsu Tierekisteriin ei ole validi"}]})))))