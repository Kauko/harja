(ns harja.palvelin.integraatiot.api.urakan-haku-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.api.urakat :as api-urakat]
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
            [harja.palvelin.integraatiot.api.reittitoteuma :as api-reittitoteuma]
            [cheshire.core :as cheshire])
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
                        :api-urakat (component/using
                                            (api-urakat/->Urakat)
                                            [:http-palvelin :db :integraatioloki])))))

  (alter-var-root #'urakka
                  (fn [_]
                    (ffirst (q (str "SELECT id FROM urakka WHERE urakoitsija=(SELECT organisaatio FROM kayttaja WHERE kayttajanimi='" kayttaja "') "
                                    " AND tyyppi='hoito'::urakkatyyppi")))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once jarjestelma-fixture)

(deftest urakan-haku-idlla-toimii
  (let [vastaus (api-tyokalut/get-kutsu ["/api/urakat/" urakka] kayttaja portti)
        encoodattu-body (cheshire/decode (:body vastaus) true)]
    (log/debug "Urakan haku id:llä: " encoodattu-body)
    (is (= 200 (:status vastaus)))
    (is (not (nil? (:urakka encoodattu-body))))
    (is (= (get-in encoodattu-body [:urakka :tiedot :id]) urakka))
    (is (>= (count (get-in encoodattu-body [:urakka :sopimukset])) 1))
    (is (>= (count (get-in (first (get-in encoodattu-body [:urakka :sopimukset])) [:sopimus :kokonaishintaisetTyot])) 1))
    (is (>= (count (get-in (first (get-in encoodattu-body [:urakka :sopimukset])) [:sopimus :yksikkohintaisetTyot])) 1))
    (is (>= (count (get-in (first (get-in encoodattu-body [:urakka :sopimukset])) [:sopimus :materiaalinKaytot])) 1))))

(deftest urakan-haku-idlla-ei-toimi-ilman-oikeuksia
  (let [vastaus (api-tyokalut/get-kutsu ["/api/urakat/" urakka] "Erkki Esimerkki" portti)]
    (is (not (= 200 (:status vastaus))))))

(deftest urakan-haku-ytunnuksella-toimii
  (let [vastaus (api-tyokalut/get-kutsu ["/api/urakat/haku/" "1565583-5"] kayttaja portti)
        encoodattu-body (cheshire/decode (:body vastaus) true)]
    (log/debug "Urakan haku ytunnuksella löytyi " (count (:urakat encoodattu-body)) " urakkaa: " (:body vastaus))
    (is (= 200 (:status vastaus)))
    (is (>= (count (:urakat encoodattu-body)) 2))))

(deftest urakan-haku-ytunnuksella-ei-toimi-ilman-oikeuksia
  (let [vastaus (api-tyokalut/get-kutsu ["/api/urakat/haku/" "1565583-5"] "Erkki Esimerkki" portti)]
    (is (not (= 200 (:status vastaus))))))