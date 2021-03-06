(ns harja.palvelin.palvelut.laadunseuranta-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.laadunseuranta :as ls]
            [harja.palvelin.palvelut.karttakuvat :as karttakuvat]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.pvm :as pvm]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                       :db (tietokanta/luo-tietokanta testitietokanta)
                       :http-palvelin (testi-http-palvelin)
                       :karttakuvat (component/using
                                     (karttakuvat/luo-karttakuvat)
                                     [:http-palvelin :db])
                       :laadunseuranta (component/using
                                        (ls/->Laadunseuranta)
                                        [:http-palvelin :db :karttakuvat])))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(def soratietarkastus                                       ;; soratietarkastus
  {:uusi?          true
   :aika           #inst "2006-07-06T09:43:00.000-00:00"
   :tarkastaja     "Jalmari Järjestelmävastuuhenkilö"
   :sijainti       nil
   :tr             {:alkuosa 2, :numero 1, :alkuetaisyys 3, :loppuetaisyys 5, :loppuosa 4}
   :tyyppi         :soratie
   :soratiemittaus {:polyavyys     4
                    :hoitoluokka   1
                    :sivukaltevuus 5
                    :tasaisuus     1
                    :kiinteys      3}
   :havainnot     "kuvaus tähän"
   :laadunalitus true})

(use-fixtures :once jarjestelma-fixture)

(deftest tallenna-ja-paivita-soratietarkastus
  (let [urakka-id (hae-oulun-alueurakan-2005-2010-id)
        kuvaus (str "kuvaus nyt " (System/currentTimeMillis))
        soratietarkastus (assoc-in soratietarkastus [:havainnot] kuvaus)
        hae-tarkastukset #(kutsu-http-palvelua :hae-urakan-tarkastukset +kayttaja-jvh+
                                               {:urakka-id urakka-id
                                                :alkupvm   #inst "2005-10-01T00:00:00.000-00:00"
                                                :loppupvm  #inst "2006-09-30T00:00:00.000-00:00"
                                                :tienumero %
                                                :vain-laadunalitukset? false})
        tarkastuksia-ennen-kaikki (count (hae-tarkastukset nil))
        tarkastuksia-ennen-tie1 (count (hae-tarkastukset 1))
        tarkastuksia-ennen-tie2 (count (hae-tarkastukset 2))
        tarkastus-id (atom nil)]

    (testing "Soratietarkastuksen tallennus"
      (let [vastaus (kutsu-http-palvelua :tallenna-tarkastus +kayttaja-jvh+
                                         {:urakka-id urakka-id
                                          :tarkastus soratietarkastus})
            id (:id vastaus)]

        (is (number? id) "Tallennus palauttaa uuden id:n")

        ;; kaikki ja tie 1 listauksissa määrä kasvanut yhdellä
        (is (= (count (hae-tarkastukset nil)) (inc tarkastuksia-ennen-kaikki)))

        (let [listaus-tie1 (hae-tarkastukset 1)]
          (is (= (count listaus-tie1) (inc tarkastuksia-ennen-tie1)))
          (is (= :soratie
                 (:tyyppi (first (filter #(= (:id %) id) listaus-tie1))))))


        ;; tie 2 tarkastusmäärä ei ole kasvanut
        (is (= (count (hae-tarkastukset 2)) tarkastuksia-ennen-tie2))

        (reset! tarkastus-id id)))

    (testing "Tarkastuksen haku ja muokkaus"
      (let [tarkastus (kutsu-http-palvelua :hae-tarkastus +kayttaja-jvh+
                                           {:urakka-id    urakka-id
                                            :tarkastus-id @tarkastus-id})]
        (is (= kuvaus (:havainnot tarkastus)))

        (testing "Muokataan tarkastusta"
          (let [muokattu-tarkastus (kutsu-http-palvelua :tallenna-tarkastus +kayttaja-jvh+
                                                        {:urakka-id urakka-id
                                                         :tarkastus (-> tarkastus
                                                                        (assoc-in [:soratiemittaus :tasaisuus] 5)
                                                                        (assoc-in [:havainnot] "MUOKATTU KUVAUS"))})]

            ;; id on edelleen sama
            (is (= (:id muokattu-tarkastus) @tarkastus-id))

            ;; muokatut kentät tallentuivat
            (is (= "MUOKATTU KUVAUS" (get-in muokattu-tarkastus [:havainnot])))
            (is (= 5 (get-in muokattu-tarkastus [:soratiemittaus :tasaisuus])))))))))
; FIXME Siivoa tallennettu data

(deftest hae-laatupoikkeaman-tiedot
  (let [urakka-id (hae-oulun-alueurakan-2005-2010-id)
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-laatupoikkeaman-tiedot +kayttaja-jvh+ {:urakka-id   urakka-id
                                                                            :laatupoikkeama-id 1})]
    (is (not (empty? vastaus)))
    (is (string? (:kuvaus vastaus)))
    (is (>= (count (:kuvaus vastaus)) 10))))

(deftest hae-urakan-laatupoikkeamat
  (let [urakka-id (hae-oulun-alueurakan-2005-2010-id)
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakan-laatupoikkeamat +kayttaja-jvh+
                                {:listaus   :kaikki
                                 :urakka-id urakka-id
                                 :alku      (pvm/luo-pvm (+ 1900 100) 9 1)
                                 :loppu     (pvm/luo-pvm (+ 1900 110) 8 30)})]
    (is (not (empty? vastaus)))
    (is (>= (count vastaus) 1))))

(deftest hae-urakan-sanktiot
  (let [urakka-id (hae-oulun-alueurakan-2014-2019-id)
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakan-sanktiot +kayttaja-jvh+ {:urakka-id urakka-id
                                                                     :alku      (pvm/luo-pvm 2015 10 1)
                                                                     :loppu     (pvm/luo-pvm 2016 10 30)})]
    (is (not (empty? vastaus)))
    (is (>= (count vastaus) 8))))

(deftest hae-sanktiotyypit
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-sanktiotyypit +kayttaja-jvh+)]
    (is (not (empty? vastaus)))
    (is (>= (count vastaus) 9))))

(deftest hae-urakan-tarkastukset
  (let [urakka-id (hae-oulun-alueurakan-2005-2010-id)
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakan-tarkastukset +kayttaja-jvh+
                                {:urakka-id urakka-id
                                 :alkupvm   (pvm/luo-pvm (+ 1900 100) 9 1)
                                 :loppupvm  (pvm/luo-pvm (+ 1900 110) 8 30)
                                 :tienumero nil
                                 :tyyppi    nil
                                 :vain-laadunalitukset? false})]
    (is (not (empty? vastaus)))
    (is (>= (count vastaus) 1))
    (let [tarkastus (first vastaus)]
      (is (= #{:ok? :jarjestelma :havainnot :laadunalitus :vakiohavainnot :aika
               :tr :tekija :id :tyyppi :tarkastaja}
             (into #{} (keys tarkastus)))))))

(deftest hae-tarkastus
  (let [urakka-id (hae-oulun-alueurakan-2005-2010-id)
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-tarkastus +kayttaja-jvh+ {:urakka-id    urakka-id
                                                               :tarkastus-id 1})]
    (is (not (empty? vastaus)))
    (is (>= (count vastaus) 1))))

(deftest hae-urakan-sanktiot-test
  (is (oikeat-sarakkeet-palvelussa?
        [:id :perintapvm :summa :laji :indeksi :suorasanktio :toimenpideinstanssi
         [:laatupoikkeama :id] [:laatupoikkeama :kohde] [:laatupoikkeama :aika] [:laatupoikkeama :tekija] [:laatupoikkeama :urakka]
         [:laatupoikkeama :tekijanimi] [:laatupoikkeama :kuvaus] [:laatupoikkeama :sijainti] [:laatupoikkeama :tarkastuspiste]
         [:laatupoikkeama :selvityspyydetty] [:laatupoikkeama :selvitysannettu]

         [:laatupoikkeama :paatos :kasittelyaika] [:laatupoikkeama :paatos :paatos] [:laatupoikkeama :paatos :kasittelytapa]
         [:laatupoikkeama :paatos :muukasittelytapa] [:laatupoikkeama :paatos :perustelu]

         [:laatupoikkeama :tr :numero] [:laatupoikkeama :tr :alkuosa] [:laatupoikkeama :tr :loppuosa]
         [:laatupoikkeama :tr :alkuetaisyys] [:laatupoikkeama :tr :loppuetaisyys]]
        :hae-urakan-sanktiot
        {:urakka-id (hae-oulun-alueurakan-2014-2019-id)
         :alku      (pvm/luo-pvm 2015 10 1)
         :loppu    (pvm/luo-pvm 2016 10 30)
         :tpi 1})))
