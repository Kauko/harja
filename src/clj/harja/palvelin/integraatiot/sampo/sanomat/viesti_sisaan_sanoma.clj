(ns harja.palvelin.integraatiot.sampo.sanomat.viesti-sisaan-sanoma
  (:require [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :as z]
            [taoensso.timbre :as log]
            [harja.tyokalut.xml :as xml])
  (:import (java.io ByteArrayInputStream)
           (org.xml.sax SAXParseException)))

(def +xsd-polku+ "resources/xsd/sampo/inbound/")

(defn lue-xml [xml]
  (let [in (ByteArrayInputStream. (.getBytes xml "UTF-8"))]
    (try (xml-zip (parse in))
         (catch SAXParseException e
           (log/error e "Virheellinen XML-kuittaus vastaanotettu Samposta. XML: " xml)
           nil))))

(defn lue-hanke [program]
  {:hanke
   {:viesti-id              (z/xml1-> program (z/attr :message_Id))
    :sampo-id               (z/xml1-> program (z/attr :id))
    :nimi                   (z/xml1-> program (z/attr :name))
    :alkupvm                (z/xml1-> program (z/attr :schedule_finish))
    :loppupvm               (z/xml1-> program (z/attr :schedule_start))
    :alueurakkanro          (z/xml1-> program (z/attr :vv_alueurakkanro))
    :yhteyshenkilo-sampo-id (z/xml1-> program (z/attr :resourceId))}})

(defn lue-urakka [project]
  {:urakka
   {:viesti-id              (z/xml1-> project (z/attr :message_Id))
    :sampo-id               (z/xml1-> project (z/attr :id))
    :nimi                   (z/xml1-> project (z/attr :name))
    :alkupvm                (z/xml1-> project (z/attr :schedule_finish))
    :loppupvm               (z/xml1-> project (z/attr :schedule_start))
    :hanke-sampo-id         (z/xml1-> project (z/attr :programId))
    :yhteyshenkilo-sampo-id (z/xml1-> project (z/attr :resourceId))}})

(defn lue-sopimus [order]
  {:sopimus
   {:viesti-id            (z/xml1-> order (z/attr :message_Id))
    :sampo-id             (z/xml1-> order (z/attr :id))
    :nimi                 (z/xml1-> order (z/attr :name))
    :alkupvm              (z/xml1-> order (z/attr :schedule_finish))
    :loppupvm             (z/xml1-> order (z/attr :schedule_start))
    :urakka-sampo-id      (z/xml1-> order (z/attr :projectId))
    :urakoitsija-sampo-id (z/xml1-> order (z/attr :contractPartyId))}})

(defn lue-toimenpideinstanssi [operation]
  {:toimenpideinstanssi
   {:viesti-id             (z/xml1-> operation (z/attr :message_Id))
    :sampo-id              (z/xml1-> operation (z/attr :id))
    :nimi                  (z/xml1-> operation (z/attr :name))
    :alkupvm               (z/xml1-> operation (z/attr :schedule_finish))
    :loppupvm              (z/xml1-> operation (z/attr :schedule_start))
    :vastuuhenkilo-id      (z/xml1-> operation (z/attr :managerId))
    :talousosasto-id       (z/xml1-> operation (z/attr :financialDepartmentHash))
    :talousosasto-polku    (z/xml1-> operation (z/attr :financialDepartmentOBS))
    :tuote-id              (z/xml1-> operation (z/attr :productHash))
    :tuote-polku           (z/xml1-> operation (z/attr :productOBS))
    :urakka-sampo-id       (z/xml1-> operation (z/attr :projectId))
    :sampo-toimenpidekoodi (z/xml1-> operation (z/attr :vv_operation))
    }})

(defn lue-organisaatio [company]
  {:organisaatio
   {:viesti-id   (z/xml1-> company (z/attr :message_Id))
    :sampo-id    (z/xml1-> company (z/attr :id))
    :nimi        (z/xml1-> company (z/attr :name))
    :y-tunnus    (z/xml1-> company (z/attr :vv_corporate_id))
    :katuosoite  (z/xml1-> (z/xml1-> company) :contactInformation (z/attr :address))
    :postinumero (z/xml1-> (z/xml1-> company) :contactInformation (z/attr :postal_Code))
    :kaupunki    (z/xml1-> (z/xml1-> company) :contactInformation (z/attr :city))
    }})

(defn lue-yhteyshenkilo [resource]
  {:yhteyshenkilo
   {:viesti-id      (z/xml1-> resource (z/attr :message_Id))
    :sampo-id       (z/xml1-> resource (z/attr :id))
    :etunimi        (z/xml1-> resource (z/attr :first_name))
    :sukunimi       (z/xml1-> resource (z/attr :last_name))
    :kayttajatunnus (z/xml1-> resource (z/attr :user_Name))
    :sahkoposti     (z/xml1-> (z/xml1-> resource) :contactInformation (z/attr :email))
    }})


(defn lue-viesti [viesti]
  (when (not (xml/validoi +xsd-polku+ "Sampo2Harja.xsd" viesti))
    (throw (new RuntimeException "XML-sanoma ei ole XSD-skeeman Sampo2Harja.xsd mukaan validi.")))
  ;; todo: tee xsd-validointi viestille
  (let [data (lue-xml viesti)]
    {:hankkeet             (z/xml-> data :Program lue-hanke)
     :urakat               (z/xml-> data :Project lue-urakka)
     :sopimukset           (z/xml-> data :Order lue-sopimus)
     :toimenpideinstanssit (z/xml-> data :Operation lue-toimenpideinstanssi)
     :organisaatiot        (z/xml-> data :Company lue-organisaatio)
     :yhteyshenkilot       (z/xml-> data :Resource lue-yhteyshenkilo)
     }))