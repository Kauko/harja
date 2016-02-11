(ns harja.palvelin.integraatiot.tloik.tyokalut
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [hiccup.core :refer [html]]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.tloik.tloik-komponentti :refer [->Tloik]]
            [harja.palvelin.integraatiot.integraatioloki :refer [->Integraatioloki]]
            [harja.jms :refer [feikki-sonja]]
            [harja.palvelin.integraatiot.tloik.kasittely.ilmoitus :as ilmoitus]
            [harja.palvelin.integraatiot.tloik.sanomat.ilmoitus-sanoma :as ilmoitussanoma]))


(def +xsd-polku+ "xsd/tloik/")
(def +tloik-ilmoitusviestijono+ "tloik-ilmoitusviestijono")
(def +tloik-ilmoituskuittausjono+ "tloik-ilmoituskuittausjono")
(def +tloik-ilmoitustoimenpideviestijono+ "tloik-ilmoitustoimenpideviestijono")
(def +tloik-ilmoitustoimenpidekuittausjono+ "tloik-ilmoitustoimenpidekuittausjono")
(def +testi-ilmoitus-sanoma+ "<harja:ilmoitus xmlns:harja=\"http://www.liikennevirasto.fi/xsd/harja\">
  <viestiId>10a24e56-d7d4-4b23-9776-2a5a12f254af</viestiId>
  <ilmoitusId>123456789</ilmoitusId>
  <versionumero>1</versionumero>
  <ilmoitustyyppi>toimenpidepyynto</ilmoitustyyppi>
  <ilmoitettu>2015-09-29T14:49:45</ilmoitettu>
  <urakkatyyppi>hoito</urakkatyyppi>
  <otsikko>Korkeat vallit</otsikko>
  <lyhytSelite>Vanhat vallit ovat liian korkeat ja uutta lunta on satanut reippaasti.</lyhytSelite>
  <pitkaSelite>Vanhat vallit ovat liian korkeat ja uutta lunta on satanut reippaasti.</pitkaSelite>
  <yhteydenottopyynto>false</yhteydenottopyynto>
  <sijainti>
  <tienumero>4</tienumero>
  <x>452935</x>
  <y>7186873</y>
  </sijainti>
  <ilmoittaja>
  <etunimi>Matti</etunimi>
  <sukunimi>Meikäläinen</sukunimi>
  <matkapuhelin>08023394852</matkapuhelin>
  <sahkoposti>matti.meikalainen@palvelu.fi</sahkoposti>
  <tyyppi>tienkayttaja</tyyppi>
  </ilmoittaja>
  <lahettaja>
  <etunimi>Pekka</etunimi>
  <sukunimi>Päivystäjä</sukunimi>
  <matkapuhelin>929304449282</matkapuhelin>
  <sahkoposti>pekka.paivystaja@livi.fi</sahkoposti>
  </lahettaja>
  <seliteet>
  <selite>auraustarve</selite>
  <selite>aurausvallitNakemaesteena</selite>
  </seliteet>
  </harja:ilmoitus>
")

(defn luo-tloik-komponentti []
  (->Tloik {:ilmoitusviestijono +tloik-ilmoitusviestijono+
            :ilmoituskuittausjono +tloik-ilmoituskuittausjono+
            :toimenpidejono +tloik-ilmoitustoimenpideviestijono+
            :toimenpidekuittausjono +tloik-ilmoitustoimenpidekuittausjono+}))

(def +ilmoitus-ruotsissa+
  (clojure.string/replace
    (clojure.string/replace +testi-ilmoitus-sanoma+ "452935" "319130")
    "7186873" "7345904"))

(defn tuo-ilmoitus []
  (let [ilmoitus (ilmoitussanoma/lue-viesti +testi-ilmoitus-sanoma+)]
    (ilmoitus/tallenna-ilmoitus (:db jarjestelma) ilmoitus)))

(defn tuo-paallystysilmoitus []
  (let [sanoma (clojure.string/replace +testi-ilmoitus-sanoma+
                                       "<urakkatyyppi>hoito</urakkatyyppi>"
                                       "<urakkatyyppi>paallystys</urakkatyyppi>")
        ilmoitus (ilmoitussanoma/lue-viesti sanoma)]
    (ilmoitus/tallenna-ilmoitus (:db jarjestelma) ilmoitus)))

(defn hae-ilmoitus []
  (q "select * from ilmoitus where ilmoitusid = 123456789;"))

(defn poista-ilmoitus []
  (u "delete from ilmoitus where ilmoitusid = 123456789;"))
