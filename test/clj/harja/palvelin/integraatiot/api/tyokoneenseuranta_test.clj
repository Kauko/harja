(ns harja.palvelin.integraatiot.api.tyokoneenseuranta-test
  (:require [harja.palvelin.integraatiot.api.tyokoneenseuranta :refer :all :as tyokoneenseuranta]
            [com.stuartsierra.component :as component]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [clojure.test :refer :all]))

(def kayttaja "fastroi")

(def jarjestelma-fixture (laajenna-integraatiojarjestelmafixturea kayttaja
                          :api-tyokoneenseuranta (component/using
                                                  (tyokoneenseuranta/->Tyokoneenseuranta)
                                                  [:http-palvelin :db :integraatioloki])))

(use-fixtures :once (compose-fixtures tietokanta-fixture
                                      jarjestelma-fixture))

(deftest tallenna-tyokoneen-seurantakirjaus-olemassaoleva
  (let [kutsu (api-tyokalut/post-kutsu
               ;; tyokone 31337 on jo kannassa, katsotaan muuttuuko raportoidut koordinaatit esimerkin mukaiseksi
               ["/api/seuranta/tyokone"] kayttaja portti (slurp "test/resurssit/api/tyokoneseuranta.json"))]
    (let [s (ffirst (q "SELECT sijainti FROM tyokonehavainto WHERE tyokoneid=31337"))]
      (is (= 200 (:status kutsu)))
      (is (= (str s) "(429005.0,7198151.0)")))))

(deftest tallenna-tyokoneen-seurantakirjaus-uusi
  (let [kutsu (api-tyokalut/post-kutsu
               ;; kokonaan uusi tyokone, kantaan pitäisi tulla uusi rivi
               ["/api/seuranta/tyokone"] kayttaja portti (slurp "test/resurssit/api/tyokoneseuranta_uusi.json"))]
    (let [s (ffirst (q "SELECT sijainti FROM tyokonehavainto WHERE tyokoneid=666"))]
      (is (= 200 (:status kutsu)))
      (is (= (str s) "(429015.0,7198161.0)")))))
