(ns harja.palvelin.integraatiot.api.paivystajatietojen-kirjaus-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.api.paivystajatiedot :as api-paivystajatiedot]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.komponentit.http-palvelin :as http-palvelin]
            [harja.palvelin.komponentit.todennus :as todennus]
            [harja.palvelin.komponentit.tapahtumat :as tapahtumat]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [com.stuartsierra.component :as component]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [harja.palvelin.integraatiot.api.tyokalut.json :as json-tyokalut]
            [harja.palvelin.integraatiot.api.reittitoteuma :as api-reittitoteuma])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def portti nil)
(def kayttaja "yit-rakennus")
(def urakka nil)

(defn jarjestelma-fixture [testit]
  (alter-var-root #'portti (fn [_] (arvo-vapaa-portti)))
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (apply tietokanta/luo-tietokanta testitietokanta)
                        :klusterin-tapahtumat (component/using
                                                (tapahtumat/luo-tapahtumat)
                                                [:db])

                        :todennus (component/using
                                    (todennus/http-todennus)
                                    [:db :klusterin-tapahtumat])
                        :http-palvelin (component/using
                                         (http-palvelin/luo-http-palvelin portti true)
                                         [:todennus])
                        :integraatioloki (component/using
                                           (integraatioloki/->Integraatioloki nil)
                                           [:db])
                        :api-paivystajatiedot (component/using
                                            (api-paivystajatiedot/->Paivystajatiedot)
                                            [:http-palvelin :db :integraatioloki])))))

  (alter-var-root #'urakka
                  (fn [_]
                    (ffirst (q (str "SELECT id FROM urakka WHERE urakoitsija=(SELECT organisaatio FROM kayttaja WHERE kayttajanimi='" kayttaja "') "
                                    " AND tyyppi='hoito'::urakkatyyppi")))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once jarjestelma-fixture)

(deftest tallenna-paivystajatiedot
  (let [ulkoinen-id (rand-int 10000)
        vastaus-lisays (api-tyokalut/post-kutsu ["/api/urakat/" urakka "/paivystajatiedot"] kayttaja portti
                                               (-> "test/resurssit/api/paivystajatiedot.json"
                                                   slurp
                                                   (.replace "__ID__" (str ulkoinen-id))
                                                   (.replace "__ETUNIMI__" "Päivi")
                                                   (.replace "__SUKUNIMI__" "Päivystäjä")))]
    (is (= 200 (:status vastaus-lisays)))
    (let [paivystaja-id (ffirst (q (str "SELECT id FROM yhteyshenkilo WHERE ulkoinen_id = '" (str ulkoinen-id)"';")))
          paivystaja (first (q (str "SELECT ulkoinen_id, etunimi, sukunimi FROM yhteyshenkilo WHERE ulkoinen_id = '" (str ulkoinen-id) "';")))
          paivystys (first (q (str "SELECT yhteyshenkilo FROM paivystys WHERE yhteyshenkilo = " paivystaja-id)))]
      (is (= paivystaja [(str ulkoinen-id) "Päivi" "Päivystäjä"]))
      (is (= paivystys [paivystaja-id]))

      (let [vastaus-paivitys (api-tyokalut/post-kutsu ["/api/urakat/" urakka "/paivystajatiedot"] kayttaja portti
                                                    (-> "test/resurssit/api/paivystajatiedot.json"
                                                        slurp
                                                        (.replace "__ID__" (str ulkoinen-id))
                                                        (.replace "__ETUNIMI__" "Taneli")
                                                        (.replace "__SUKUNIMI__" "Tähystäjä")))]
        (is (= 200 (:status vastaus-paivitys)))
        (let [paivystaja (first (q (str "SELECT ulkoinen_id, etunimi, sukunimi FROM yhteyshenkilo WHERE ulkoinen_id = '" (str ulkoinen-id) "';")))
              paivystys (first (q (str "SELECT yhteyshenkilo FROM paivystys WHERE yhteyshenkilo = " paivystaja-id)))]
          (is (= paivystaja [(str ulkoinen-id) "Taneli" "Tähystäjä"]))
          (is (= paivystys [paivystaja-id]))

        (u (str "DELETE FROM yhteyshenkilo WHERE ulkoinen_id = '" (str ulkoinen-id) "';"))
        (u (str "DELETE FROM paivystys WHERE yhteyshenkilo = " paivystaja-id)))))))