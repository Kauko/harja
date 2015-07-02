(ns harja.palvelin.integraatiot.sampo.sanomat.maksuera-sanoma-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.palvelin.integraatiot.sampo.sanomat.maksuera_sanoma :as maksuera_sanoma]
            [hiccup.core :refer [html]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :as z]
            [harja.testi :refer :all]
            [harja.tyokalut.xml :as xml])
  (:import (java.io ByteArrayInputStream)
           (java.text SimpleDateFormat)))

(def +xsd-polku+ "resources/xsd/sampo/outbound/")

(defn parsi-paivamaara [teksti]
  (.parse (SimpleDateFormat. "dd.MM.yyyy") teksti))

(def +maksuera+ {:numero              123456789
                 :maksuera
                                      {:nimi   "Testimaksuera"
                                       :tyyppi "kokonaishintainen"
                                       :summa  1304.89}
                 :toimenpideinstanssi {:alkupvm       (parsi-paivamaara "12.12.2015")
                                       :loppupvm      (parsi-paivamaara "1.1.2017")
                                       :vastuuhenkilo "A009717"
                                       :talousosasto  "talousosasto"
                                       :talousosastopolku  "polku/talousosasto"
                                       :tuotepolku    "polku/tuote"
                                       :sampoid       "SAMPOID"}
                 :urakka              {:sampoid "PR00020606"}
                 :sopimus             {:sampoid "00LZM-0033600"}})

(deftest tarkista-maksueran-validius
  (let [maksuera (html (maksuera_sanoma/muodosta +maksuera+))
        xsd "nikuxog_product.xsd"]
    (is (xml/validoi +xsd-polku+ xsd maksuera) "Muodostettu XML-tiedosto on XSD-skeeman mukainen")))

(deftest tarkista-maksueran-sisalto
  (let [maksuera-xml (xml-zip (parse (ByteArrayInputStream. (.getBytes (html (maksuera_sanoma/muodosta +maksuera+)) "UTF-8"))))]
    (is (= "2015-12-12T00:00:00.0" (z/xml1-> maksuera-xml :Products :Product (z/attr :start))))
    (is (= "2017-01-01T00:00:00.0" (z/xml1-> maksuera-xml :Products :Product (z/attr :finish))))
    (is (= "A009717" (z/xml1-> maksuera-xml :Products :Product (z/attr :managerUserName))))
    (is (= "Testimaksuera" (z/xml1-> maksuera-xml :Products :Product (z/attr :name))))
    (is (= "HA123456789" (z/xml1-> maksuera-xml :Products :Product (z/attr :objectID))))
    (is (= "SAMPOID" (z/xml1-> maksuera-xml :Products :Product :InvestmentAssociations :Allocations :ParentInvestment (z/attr :InvestmentID))))
    (is (= "kulu2015" (z/xml1-> maksuera-xml :Products :Product :InvestmentResources :Resource (z/attr :resourceID))))
    (is (= "kulu2015" (z/xml1-> maksuera-xml :Products :Product :InvestmentTasks :Task :Assignments :TaskLabor (z/attr :resourceID))))
    (is (= "Testimaksuera" (z/xml1-> maksuera-xml :Products :Product :InvestmentTasks :Task (z/attr :name))))
    (is (= "polku/talousosasto" (z/xml1-> maksuera-xml :Products :Product :OBSAssocs :OBSAssoc (z/attr= :id "LiiviKP") (z/attr :unitPath))))
    (is (= "polku/tuote" (z/xml1-> maksuera-xml :Products :Product :OBSAssocs :OBSAssoc (z/attr= :id "tuote2013") (z/attr :unitPath))))
    (is (= "00LZM-0033600" (z/xml1-> maksuera-xml :Products :Product :CustomInformation :ColumnValue (z/attr= :name "vv_tilaus") z/text)))
    (is (= "2" (z/xml1-> maksuera-xml :Products :Product :CustomInformation :ColumnValue (z/attr= :name "vv_me_type") z/text)))
    (is (= "123456789" (z/xml1-> maksuera-xml :Products :Product :CustomInformation :ColumnValue (z/attr= :name "vv_inst_no") z/text)))
    (is (= "AL123456789" (z/xml1-> maksuera-xml :Products :Product :CustomInformation :instance (z/attr :instanceCode))))
    (is (= "1304.89" (z/xml1-> maksuera-xml :Products :Product :CustomInformation :instance :CustomInformation :ColumnValue (z/attr= :name "vv_paym_sum") z/text)))))
