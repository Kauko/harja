(ns harja.palvelin.integraatiot.sampo.vienti-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [hiccup.core :refer [html]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [com.stuartsierra.component :as component]
            [harja.palvelin.integraatiot.sampo.sampo-komponentti :refer [->Sampo] :as sampo]
            [harja.palvelin.integraatiot.integraatioloki :refer [->Integraatioloki]]
            [harja.testi :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.jms :refer [feikki-sonja]]
            [harja.palvelin.komponentit.sonja :as sonja]
            [harja.tyokalut.xml :as xml]))

(def +lahetysjono-sisaan+ "lahetysjono-sisaan")
(def +kuittausjono-sisaan+ "kuittausjono-sisaan")
(def +lahetysjono-ulos+ "lahetysjono-ulos")
(def +kuittausjono-ulos+ "kuittausjono-ulos")

(def +xsd-polku+ "xsd/sampo/outbound/")

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :sonja (feikki-sonja)
                        :integraatioloki (component/using (->Integraatioloki nil) [:db])
                        :sampo (component/using (->Sampo +lahetysjono-sisaan+ +kuittausjono-sisaan+ +lahetysjono-ulos+ +kuittausjono-ulos+ nil) [:db :sonja :integraatioloki])))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once jarjestelma-fixture)

(deftest yrita-laheta-maksuera-jota-ei-ole-olemassa
  (is (= {:virhe :maksueran-lukitseminen-epaonnistui} (:maksuera (sampo/laheta-maksuera-sampoon (:sampo jarjestelma) 666)))))

(deftest laheta-maksuera
  (let [viestit (atom [])]
    (sonja/kuuntele (:sonja jarjestelma) +lahetysjono-ulos+ #(swap! viestit conj (.getText %)))
    (is (sampo/laheta-maksuera-sampoon (:sampo jarjestelma) 1) "Lähetys onnistui")
    (odota-ehdon-tayttymista #(= 2 (count @viestit)) "Sekä kustannussuunnitelma, että maksuerä on lähetetty." 10000)
    (let [sampoon-lahetetty-maksuera (first (filter #(not (.contains % "<CostPlans>")) @viestit))
          sampoon-lahetetty-kustannussuunnitelma (first (filter #(.contains % "<CostPlans>") @viestit))]
      (is (xml/validoi +xsd-polku+ "nikuxog_product.xsd" sampoon-lahetetty-maksuera))
      (is (xml/validoi +xsd-polku+ "nikuxog_costPlan.xsd" sampoon-lahetetty-kustannussuunnitelma)))))
