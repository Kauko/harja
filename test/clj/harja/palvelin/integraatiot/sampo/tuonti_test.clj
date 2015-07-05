(ns harja.palvelin.integraatiot.sampo.tuonti-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [hiccup.core :refer [html]]
            [harja.testi :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.integraatiot.sampo.tuonti :as tuonti])
  (:import (javax.jms TextMessage)))

(def +testihanke-sanoma+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Sampo2harja xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SampToharja.xsd\">
    <Program id=\"TESTIHANKE\" manager_Id=\"A010098\" manager_User_Name=\"A010098\" message_Id=\"HankeMessageId\"
             name=\"Testi alueurakka 2009-2014\" schedule_finish=\"2013-12-31T00:00:00.0\"
             schedule_start=\"2009-01-01T00:00:00.0\" vv_alueurakkanro=\"1238\" vv_code=\"14-1177\">
        <documentLinks/>
    </Program>
</Sampo2harja>
")

(def +testiurakka-sanoma+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Sampo2harja xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SampToharja.xsd\">
    <Project id=\"TESTIURAKKA\" message_Id=\"UrakkaMessageId\" name=\"Testiurakka\" programId=\"TESTIHANKE\"
             resourceId=\"sampotesti\" schedule_finish=\"2020-12-31T17:00:00.0\" schedule_start=\"2013-01-01T08:00:00.0\">
        <documentLinks/>
    </Project>
</Sampo2harja>
")

(def +kuittausjono-sisaan+ "kuittausjono-sisaan")

(defn tee-viesti [sisalto]
  (reify TextMessage
    (getText [this] sisalto)))

(defn laheta-viesti-kasiteltavaksi [sisalto]
  (let [db (apply tietokanta/luo-tietokanta testitietokanta)
        viesti (tee-viesti sisalto)]
    (tuonti/kasittele-viesti db +kuittausjono-sisaan+ viesti)))

(defn tuo-hanke []
  (laheta-viesti-kasiteltavaksi +testihanke-sanoma+))

(defn tuo-urakka []
  (laheta-viesti-kasiteltavaksi +testiurakka-sanoma+))

(defn poista-hanke []
  (u "update urakka set hanke = null where hanke_sampoid = 'TESTIHANKE'")
  (u "delete from hanke where sampoid = 'TESTIHANKE'"))

(defn poista-urakka []
  (u "delete from yhteyshenkilo_urakka where urakka = (select id from urakka where sampoid = 'TESTIURAKKA')")
  (u "delete from urakka where sampoid = 'TESTIURAKKA'"))

(deftest tarkista-hankkeen-tallentuminen
  (tuo-hanke)
  (is (= 1 (count (q "select id from hanke where sampoid = 'TESTIHANKE';")))
      "Luonnin jälkeen hanke löytyy Sampo id:llä.")

  (tuo-hanke)
  (is (= 1 (count (q "select id from hanke where sampoid = 'TESTIHANKE';")))
      "Tuotaessa sama hanke uudestaan, päivitetään vanhaa eikä luoda uutta.")

  (poista-hanke))

(deftest tarkista-urakan-tallentuminen
  (tuo-urakka)
  (is (= 1 (count (q "select id from urakka where sampoid = 'TESTIURAKKA';")))
      "Luonnin jälkeen urakka löytyy Sampo id:llä.")

  (tuo-urakka)
  (is (= 1 (count (q "select id from urakka where sampoid = 'TESTIURAKKA';")))
      "Tuotaessa sama urakka uudestaan, päivitetään vanhaa eikä luoda uutta.")

  (is (= 1 (count (q "SELECT id FROM yhteyshenkilo_urakka
                      WHERE rooli = 'Sampo yhteyshenkilö' AND
                            urakka = (SELECT id FROM urakka
                            WHERE sampoid = 'TESTIURAKKA');")))
      "Urakalle löytyy luonnin jälkeen sampoid:llä sidottu yhteyshenkilö.")

  (poista-urakka))