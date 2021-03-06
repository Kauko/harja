(ns harja.palvelin.palvelut.toteumat
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.konversio :as konv]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [clojure.java.jdbc :as jdbc]

            [harja.kyselyt.toteumat :as q]
            [harja.kyselyt.materiaalit :as materiaalit-q]
            [harja.kyselyt.muutoshintaiset-tyot :as mht-q]

            [harja.palvelin.palvelut.materiaalit :as materiaalipalvelut]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [harja.geo :as geo]
            [harja.domain.oikeudet :as oikeudet]))

(defn geometriaksi [reitti]
  (when reitti (geo/geometry (geo/clj->pg reitti))))

(def toteuma-xf
  (comp (map #(-> %
                  (konv/array->vec :tehtavat)
                  (konv/array->vec :materiaalit)))))

(def muunna-desimaaliluvut-xf
  (map #(-> %
            (assoc :maara
                   (or (some-> % :maara double) 0)))))

(def toteumien-tehtavat->map-xf
  (map #(-> %
            (assoc :tehtavat
                   (mapv (fn [tehtava]
                           (let [splitattu (str/split tehtava #"\^")]
                             {:tehtava-id (Integer/parseInt (first splitattu))
                              :tpk-id (Integer/parseInt (second splitattu))
                              :nimi (get splitattu 2)
                              :maara (when-let [maara (get splitattu 3)]
                                       (Double/parseDouble maara))}))
                         (:tehtavat %))))))

(def tyhja-tr-osoite {:numero nil :alkuosa nil :alkuetaisyys nil :loppuosa nil :loppuetaisyys nil})

(defn toteuman-parametrit [toteuma kayttaja]
  (let [{:keys [numero alkuosa alkuetaisyys loppuosa loppuetaisyys]} (:tr toteuma)]
    (merge tyhja-tr-osoite
           (:tr toteuma)
           {:urakka (:urakka-id toteuma)
            :sopimus (:sopimus-id toteuma)
            :alkanut (konv/sql-timestamp (:alkanut toteuma))
            :paattynyt (konv/sql-timestamp (or (:paattynyt toteuma)
                                               (:alkanut toteuma)))
            :tyyppi (name (:tyyppi toteuma))
            :kayttaja (:id kayttaja)
            :suorittaja (:suorittajan-nimi toteuma)
            :ytunnus (:suorittajan-ytunnus toteuma)
            :lisatieto (:lisatieto toteuma)
            :ulkoinen_id nil
            :reitti (geometriaksi (:reitti toteuma))
            :lahde "harja-ui"})))

(defn toteumatehtavan-parametrit [toteuma kayttaja]
  [(get-in toteuma [:tehtava :toimenpidekoodi]) (get-in toteuma [:tehtava :maara]) (:id kayttaja)
   (get-in toteuma [:tehtava :paivanhinta])])


(defn hae-urakan-toteuma [db user {:keys [urakka-id toteuma-id]}]
  (log/debug "Haetaan urakan toteuma id:llä: " toteuma-id)
  (oikeudet/lue oikeudet/urakat-toteumat-kokonaishintaisettyot user urakka-id)
  (let [toteuma (into []
                      (comp
                        toteuma-xf
                        toteumien-tehtavat->map-xf
                        (map konv/alaviiva->rakenne))
                      (q/hae-urakan-toteuma db urakka-id toteuma-id))]
    (first toteuma)))

(defn hae-urakan-toteumien-tehtavien-summat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpide-id tehtava-id]}]
  (log/debug "Haetaan urakan toteuman tehtävien summat: " urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpide-id tehtava-id)
  (oikeudet/lue (case tyyppi 
                  :kokonaishintainen  oikeudet/urakat-toteumat-kokonaishintaisettyot  
                  :yksikkohintainen  oikeudet/urakat-toteumat-yksikkohintaisettyot  
                  (:akillinen-hoitotyo :lisatyo :muutostyo :vahinkojen-korjaukset)   oikeudet/urakat-toteumat-muutos-ja-lisatyot  
                  :materiaali  oikeudet/urakat-toteumat-materiaalit

                  :default oikeudet/urakat-toteumat-kokonaishintaisettyot  )
                user urakka-id)
  (into []
        muunna-desimaaliluvut-xf
        (q/hae-toteumien-tehtavien-summat db
                                          urakka-id
                                          sopimus-id
                                          (konv/sql-date alkupvm)
                                          (konv/sql-date loppupvm)
                                          (name tyyppi)
                                          toimenpide-id
                                          tehtava-id)))

(defn hae-urakan-toteutuneet-tehtavat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi]}]
  (log/debug "Haetaan urakan toteutuneet tehtävät: " urakka-id sopimus-id alkupvm loppupvm tyyppi)
  (oikeudet/lue (case tyyppi 
                  :kokonaishintainen  oikeudet/urakat-toteumat-kokonaishintaisettyot  
                  :yksikkohintainen  oikeudet/urakat-toteumat-yksikkohintaisettyot  
                  (:akillinen-hoitotyo :lisatyo :muutostyo :vahinkojen-korjaukset)   oikeudet/urakat-toteumat-muutos-ja-lisatyot  
                  :materiaali  oikeudet/urakat-toteumat-materiaalit

                  :default oikeudet/urakat-toteumat-kokonaishintaisettyot  )
                user urakka-id)
  (let [toteutuneet-tehtavat (into []
                                   muunna-desimaaliluvut-xf
                                   (q/hae-urakan-ja-sopimuksen-toteutuneet-tehtavat db urakka-id sopimus-id (konv/sql-timestamp alkupvm) (konv/sql-timestamp loppupvm) (name tyyppi)))]
    (log/debug "Haetty urakan toteutuneet tehtävät: " toteutuneet-tehtavat)
    toteutuneet-tehtavat))

(defn hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpidekoodi]}]
  (log/debug "Haetaan urakan toteutuneet tehtävät tyypillä ja toimenpidekoodilla: " urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpidekoodi)
  (oikeudet/lue (case tyyppi 
                  :kokonaishintainen  oikeudet/urakat-toteumat-kokonaishintaisettyot  
                  :yksikkohintainen  oikeudet/urakat-toteumat-yksikkohintaisettyot  
                  (:akillinen-hoitotyo :lisatyo :muutostyo :vahinkojen-korjaukset)   oikeudet/urakat-toteumat-muutos-ja-lisatyot  
                  :materiaali  oikeudet/urakat-toteumat-materiaalit

                  :default oikeudet/urakat-toteumat-kokonaishintaisettyot  )
                user urakka-id)
  (into []
        muunna-desimaaliluvut-xf
        (q/hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db urakka-id sopimus-id (konv/sql-timestamp alkupvm) (konv/sql-timestamp loppupvm) (name tyyppi) toimenpidekoodi)))



(defn hae-urakan-tehtavat [db user urakka-id]
  (oikeudet/lue  oikeudet/urakat-toteumat-kokonaishintaisettyot   user urakka-id)
  (into []
        (q/hae-urakan-tehtavat db urakka-id)))

(defn kasittele-toteumatehtava [c user toteuma tehtava]
  (if (and (:tehtava-id tehtava) (pos? (:tehtava-id tehtava)))
    (do
      (if (:poistettu tehtava)
        (do (log/debug "Poistetaan tehtävä: " (pr-str tehtava))
            (q/poista-toteuman-tehtava! c (:tehtava-id tehtava)))
        (do (log/debug "Pävitetään tehtävä: " (pr-str tehtava))
            (q/paivita-toteuman-tehtava! c (:toimenpidekoodi tehtava) (:maara tehtava) (or (:poistettu tehtava) false)
                                         (or (:paivanhinta tehtava) nil)
                                         (:tehtava-id tehtava)))))
    (do
      (when (not (:poistettu tehtava))
        (log/debug "Luodaan uusi tehtävä.")
        (q/luo-tehtava<! c (:toteuma-id toteuma) (:toimenpidekoodi tehtava) (:maara tehtava) (:id user) nil)))))

(defn kasittele-toteuman-tehtavat [c user toteuma]
  (doseq [tehtava (:tehtavat toteuma)]
    (kasittele-toteumatehtava c user toteuma tehtava)))

(defn paivita-toteuma [c user toteuma]
  (q/paivita-toteuma! c (assoc (toteuman-parametrit toteuma user)
                               :id (:toteuma-id toteuma)))
  (kasittele-toteuman-tehtavat c user toteuma)
  (:toteuma-id toteuma))

(defn luo-toteuma [c user toteuma]
  (let [toteuman-parametrit (toteuman-parametrit toteuma user)
        uusi (q/luo-toteuma<! c toteuman-parametrit)
        id (:id uusi)
        toteumatyyppi (name (:tyyppi toteuma))]
    (doseq [{:keys [toimenpidekoodi maara]} (:tehtavat toteuma)]
      (q/luo-tehtava<! c id toimenpidekoodi maara (:id user) nil)
      (q/merkitse-toteuman-maksuera-likaiseksi! c toteumatyyppi toimenpidekoodi))
    id))

(defn hae-urakan-kokonaishintaisten-toteumien-tehtavien-paivakohtaiset-summat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm toimenpide tehtava]}]
  (log/debug "Aikaväli: " (pr-str alkupvm) (pr-str loppupvm))
  (oikeudet/lue  oikeudet/urakat-toteumat-kokonaishintaisettyot   user urakka-id)
  (let [toteumat (into []
                       (comp
                         (filter #(not (nil? (:toimenpidekoodi %))))
                         (map konv/alaviiva->rakenne))
                       (q/hae-urakan-kokonaishintaiset-toteumat-paivakohtaisina-summina
                         db urakka-id
                         sopimus-id
                         (konv/sql-date alkupvm)
                         (konv/sql-date loppupvm)
                         toimenpide
                         tehtava))]
    toteumat))

(defn tallenna-toteuma-ja-yksikkohintaiset-tehtavat
  "Tallentaa toteuman. Palauttaa sen ja tehtävien summat."
  [db user toteuma]
  (oikeudet/kirjoita (case (:tyyppi  toteuma)
                       :kokonaishintainen  oikeudet/urakat-toteumat-kokonaishintaisettyot  
                       :yksikkohintainen  oikeudet/urakat-toteumat-yksikkohintaisettyot  
                       (:akillinen-hoitotyo :lisatyo :muutostyo :vahinkojen-korjaukset)   oikeudet/urakat-toteumat-muutos-ja-lisatyot  
                       :materiaali  oikeudet/urakat-toteumat-materiaalit

                       :default oikeudet/urakat-toteumat-kokonaishintaisettyot  )
                     user (:urakka-id toteuma))
  (log/debug "Toteuman tallennus aloitettu. Payload: " (pr-str toteuma))
  (jdbc/with-db-transaction [c db]
    (let [id (if (:toteuma-id toteuma)
               (paivita-toteuma c user toteuma)
               (luo-toteuma c user toteuma))
          paivitetyt-summat (hae-urakan-toteumien-tehtavien-summat c user
                                                 {:urakka-id     (:urakka-id toteuma)
                                                  :sopimus-id    (:sopimus-id toteuma)
                                                  :alkupvm       (konv/sql-timestamp (:hoitokausi-aloituspvm toteuma))
                                                  :loppupvm      (konv/sql-timestamp (:hoitokausi-lopetuspvm toteuma))
                                                  :toimenpide-id (:toimenpide-id toteuma)
                                                  :tyyppi        (:tyyppi toteuma)})]
      {:toteuma          (assoc toteuma :toteuma-id id)
       :tehtavien-summat paivitetyt-summat})))

(defn tallenna-toteuma-ja-kokonaishintaiset-tehtavat
  "Tallentaa toteuman. Palauttaa sen ja tehtävien summat."
  [db user toteuma hakuparametrit]
  (oikeudet/kirjoita (case (:tyyppi  toteuma)
                       :kokonaishintainen  oikeudet/urakat-toteumat-kokonaishintaisettyot  
                       :yksikkohintainen  oikeudet/urakat-toteumat-yksikkohintaisettyot  
                       (:akillinen-hoitotyo :lisatyo :muutostyo :vahinkojen-korjaukset)   oikeudet/urakat-toteumat-muutos-ja-lisatyot  
                       :materiaali  oikeudet/urakat-toteumat-materiaalit

                       :default oikeudet/urakat-toteumat-kokonaishintaisettyot  )
                     user (:urakka-id toteuma))
  (log/debug "Toteuman tallennus aloitettu. Payload: " (pr-str toteuma))
  (jdbc/with-db-transaction [db db]
                            (if (:toteuma-id toteuma)
                              (paivita-toteuma db user toteuma)
                              (luo-toteuma db user toteuma))

                            (hae-urakan-kokonaishintaisten-toteumien-tehtavien-paivakohtaiset-summat
                              db user hakuparametrit)))

(defn paivita-yk-hint-toiden-tehtavat
  "Päivittää yksikköhintaisen töiden toteutuneet tehtävät. Palauttaa päivitetyt tehtävät sekä tehtävien summat"
  [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi tehtavat toimenpide-id]}]
  (oikeudet/kirjoita oikeudet/urakat-toteumat-yksikkohintaisettyot   user urakka-id)
  (log/debug (str "Yksikköhintaisten töiden päivitys aloitettu. Payload: " (pr-str (into [] tehtavat))))

  (let [tehtavatidt (into #{} (map #(:tehtava_id %) tehtavat))]
    (jdbc/with-db-transaction [c db]
      (doseq [tehtava tehtavat]
        (log/debug (str "Päivitetään saapunut tehtävä. id: " (:tehtava_id tehtava)))
        (q/paivita-toteuman-tehtava! c (:toimenpidekoodi tehtava) (:maara tehtava) (:poistettu tehtava)
                                     (:paivanhinta tehtava) (:tehtava_id tehtava)))

      (log/debug "Merkitään tehtavien: " tehtavatidt " maksuerät likaisiksi")
      (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! c tehtavatidt)))

  (let [paivitetyt-tehtavat (hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db user
                                                                                {:urakka-id       urakka-id
                                                                                 :sopimus-id      sopimus-id
                                                                                 :alkupvm         alkupvm
                                                                                 :loppupvm        loppupvm
                                                                                 :tyyppi          tyyppi
                                                                                 :toimenpidekoodi (:toimenpidekoodi (first tehtavat))})
        paivitetyt-summat (hae-urakan-toteumien-tehtavien-summat db user
                                                                 {:urakka-id     urakka-id
                                                                  :sopimus-id    sopimus-id
                                                                  :alkupvm       alkupvm
                                                                  :loppupvm      loppupvm
                                                                  :toimenpide-id toimenpide-id
                                                                  :tyyppi        tyyppi})]
    (log/debug "Palautetaan päivitetyt tehtävät " (pr-str paivitetyt-tehtavat) " ja summat " (pr-str paivitetyt-summat))
    {:tehtavat paivitetyt-tehtavat :tehtavien-summat paivitetyt-summat}))

(def erilliskustannus-tyyppi-xf
  (map #(assoc % :tyyppi (keyword (:tyyppi %)))))

(def erilliskustannus-rahasumma-xf
  (map #(if (:rahasumma %)
         (assoc % :rahasumma (double (:rahasumma %)))
         (identity %))))

(def erilliskustannus-xf
  (comp
    erilliskustannus-tyyppi-xf
    erilliskustannus-rahasumma-xf
    ;; Asiakastyytyväisyysbonuksen indeksikorotus lasketaan eri kaavalla
    (map #(if (= (:tyyppi %) :asiakastyytyvaisyysbonus)
           (assoc % :indeksikorjattuna (:bonus-indeksikorjattuna %))
           %))))

(defn hae-urakan-erilliskustannukset [db user {:keys [urakka-id alkupvm loppupvm]}]
  (oikeudet/lue  oikeudet/urakat-toteumat-erilliskustannukset user urakka-id)
  (into []
        erilliskustannus-xf
        (q/listaa-urakan-hoitokauden-erilliskustannukset db urakka-id (konv/sql-date alkupvm) (konv/sql-date loppupvm))))

(defn tallenna-erilliskustannus [db user ek]
  (log/debug "tallenna erilliskustannus:" ek)
  (oikeudet/kirjoita  oikeudet/urakat-toteumat-erilliskustannukset user (:urakka-id ek))
  (jdbc/with-db-transaction
    [db db]
    (let [parametrit [db (:tyyppi ek) (:urakka-id ek) (:sopimus ek) (:toimenpideinstanssi ek)
                      (konv/sql-date (:pvm ek)) (:rahasumma ek) (:indeksin_nimi ek) (:lisatieto ek) (:id user)]]
      (if (not (:id ek))
        (apply q/luo-erilliskustannus<! parametrit)
        (apply q/paivita-erilliskustannus! (concat parametrit [(or (:poistettu ek) false) (:id ek)])))
      (q/merkitse-toimenpideinstanssin-kustannussuunnitelma-likaiseksi! db (:toimenpideinstanssi ek))
      (hae-urakan-erilliskustannukset db user {:urakka-id (:urakka-id ek)
                                              :alkupvm   (:alkupvm ek)
                                              :loppupvm  (:loppupvm ek)}))))


(def muut-tyot-rahasumma-xf
  (map #(if (:tehtava_paivanhinta %)
         (assoc % :tehtava_paivanhinta (double (:tehtava_paivanhinta %)))
         (identity %))))

(def muut-tyot-tyyppi-xf
  (map #(if (:tyyppi %)
         (assoc % :tyyppi (keyword (:tyyppi %)))
         (identity %))))

(def muut-tyot-maara-xf
  (map #(if (:tehtava_maara %)
         (assoc % :tehtava_maara (double (:tehtava_maara %)))
         (identity %))))


(def muut-tyot-xf
  (comp
    (harja.geo/muunna-pg-tulokset :reittipiste_sijainti)
    muut-tyot-rahasumma-xf
    muut-tyot-maara-xf
    (map konv/alaviiva->rakenne)
    muut-tyot-tyyppi-xf))

(defn hae-urakan-muut-tyot [db user {:keys [urakka-id sopimus-id alkupvm loppupvm]}]
  (log/debug "Haetaan urakan muut työt: " urakka-id " ajalta " alkupvm "-" loppupvm)
  (oikeudet/lue  oikeudet/urakat-toteumat-muutos-ja-lisatyot   user urakka-id)
  (konv/sarakkeet-vektoriin
    (into []
          muut-tyot-xf
          (q/listaa-urakan-hoitokauden-toteumat-muut-tyot db urakka-id sopimus-id (konv/sql-date alkupvm) (konv/sql-date loppupvm)))
    {:reittipiste :reittipisteet}
    #(get-in % [:toteuma :id])))

(defn paivita-muun-tyon-toteuma
  [c user toteuma]
  (log/debug "Päivitä toteuma" toteuma)
  (if (:poistettu toteuma)
    (let [params [c (:id user) (get-in toteuma [:toteuma :id])]]
      (log/debug "poista toteuma" (get-in toteuma [:toteuma :id]))
      (apply q/poista-toteuman-tehtavat! params)
      (apply q/poista-toteuma! params))
    (do (q/paivita-toteuma! c (konv/sql-date (:alkanut toteuma)) (konv/sql-date (:paattynyt toteuma)) (name (:tyyppi toteuma)) (:id user)
                            (:suorittajan-nimi toteuma) (:suorittajan-ytunnus toteuma) (:lisatieto toteuma) nil
                            nil nil nil nil nil
                            (get-in toteuma [:toteuma :id]) (:urakka-id toteuma))
        (kasittele-toteumatehtava c user toteuma (assoc (:tehtava toteuma)
                                                   :tehtava-id (get-in toteuma [:tehtava :id]))))))

(defn luo-muun-tyon-toteuma
  [c user toteuma]
  (log/debug "Luodaan uusi toteuma" toteuma)
  (let [toteuman-parametrit (toteuman-parametrit toteuma user)
        uusi (q/luo-toteuma<! c toteuman-parametrit)
        id (:id uusi)
        toteumatyyppi (name (:tyyppi toteuma))
        maksueratyyppi (case toteumatyyppi
                         "muutostyo" "muu"
                         "akillinen-hoitotyo" "akillinen-hoitotyo"
                         "lisatyo" "lisatyo"
                         "muu")
        toteumatehtavan-parametrit
        (into [] (concat [c id] (toteumatehtavan-parametrit toteuma user)))
        {:keys [toimenpidekoodi]} (:tehtava toteuma)]
    (log/debug (str "Luodaan uudelle toteumalle id " id " tehtävä" toteumatehtavan-parametrit))
    (apply q/luo-tehtava<! toteumatehtavan-parametrit)
    (log/debug "Merkitään maksuera likaiseksi maksuerätyypin: " maksueratyyppi " toteumalle jonka toimenpidekoodi on: " toimenpidekoodi)
    (q/merkitse-toteuman-maksuera-likaiseksi! c maksueratyyppi toimenpidekoodi)
    true))

(defn tallenna-muiden-toiden-toteuma
  [db user toteuma]
  (oikeudet/kirjoita  oikeudet/urakat-toteumat-muutos-ja-lisatyot   user (:urakka-id toteuma))
  (jdbc/with-db-transaction [c db]
    (if (get-in toteuma [:tehtava :id])
      (paivita-muun-tyon-toteuma c user toteuma)
      (luo-muun-tyon-toteuma c user toteuma))
    ;; lisätään tarvittaessa hinta muutoshintainen_tyo tauluun
    (when (:uusi-muutoshintainen-tyo toteuma)
      (let [parametrit [c (:yksikko toteuma) (:yksikkohinta toteuma) (:id user)
                        (:urakka-id toteuma) (:sopimus-id toteuma) (get-in toteuma [:tehtava :toimenpidekoodi])
                        (konv/sql-date (:urakan-alkupvm toteuma))
                        (konv/sql-date (:urakan-loppupvm toteuma))]]
        (apply mht-q/lisaa-muutoshintainen-tyo<! parametrit)))
    (hae-urakan-muut-tyot c user
                          {:urakka-id  (:urakka-id toteuma)
                           :sopimus-id (:sopimus-id toteuma)
                           :alkupvm    (konv/sql-timestamp (:hoitokausi-aloituspvm toteuma))
                           :loppupvm   (konv/sql-timestamp (:hoitokausi-lopetuspvm toteuma))})))



(defn tallenna-toteuma-ja-toteumamateriaalit
  "Tallentaa toteuman ja toteuma-materiaalin, ja palauttaa lopuksi kaikki urakassa käytetyt materiaalit (yksi rivi per materiaali).
  Tiedon mukana tulee yhteenlaskettu summa materiaalin käytöstä.
  * Jos tähän funktioon tehdään muutoksia, pitäisi muutokset tehdä myös
  materiaalit/tallenna-toteumamateriaaleja! funktioon (todnäk)"
  [db user t toteumamateriaalit hoitokausi sopimus]
  (oikeudet/kirjoita  oikeudet/urakat-toteumat-materiaalit   user (:urakka t))
  (log/debug "Tallenna toteuma: " (pr-str t) " ja toteumamateriaalit " (pr-str toteumamateriaalit))
  (jdbc/with-db-transaction [c db]
    ;; Jos toteumalla on positiivinen id, toteuma on olemassa
    (let [toteuma (if (and (:id t) (pos? (:id t)))
                    ;; Jos poistettu=true, halutaan toteuma poistaa.
                    ;; Molemmissa tapauksissa parametrina saatu toteuma tulee palauttaa
                    (if (:poistettu t)
                      (do
                        (log/debug "Poistetaan toteuma " (:id t))
                        (q/poista-toteuma! c (:id user) (:id t))
                        t)
                      (do
                        (log/debug "Pävitetään toteumaa " (:id t))
                        (q/paivita-toteuma! c (konv/sql-date (:alkanut t)) (konv/sql-date (:paattynyt t))
                                            (:tyyppi t) (:id user)
                                            (:suorittajan-nimi t) (:suorittajan-ytunnus t) (:lisatieto t) nil
                                            nil nil nil nil nil
                                            (:id t) (:urakka t))
                        t))
                    ;; Jos id:tä ei ole tai se on negatiivinen, halutaan luoda uusi toteuma
                    ;; Tässä tapauksessa palautetaan kyselyn luoma toteuma
                    (do
                      (log/debug "Luodaan uusi toteuma")
                      (q/luo-toteuma<!
                        c (:urakka t) (:sopimus t) (konv/sql-date (:alkanut t))
                        (konv/sql-date (:paattynyt t)) (:tyyppi t) (:id user)
                        (:suorittajan-nimi t)
                        (:suorittajan-ytunnus t)
                        (:lisatieto t)
                        nil
                        nil nil nil nil nil nil
                        "harja-ui")))]
      (log/debug "Toteuman tallentamisen tulos:" (pr-str toteuma))

      (doseq [tm toteumamateriaalit]
        ;; Positiivinen id = luodaan tai poistetaan toteuma-materiaali
        (if (and (:id tm) (pos? (:id tm)))
          (if (:poistettu tm)
            (do
              (log/debug "Poistetaan materiaalitoteuma " (:id tm))
              (materiaalit-q/poista-toteuma-materiaali! c (:id user) (:id tm)))
            (do
              (log/debug "Päivitä materiaalitoteuma "
                         (:id tm) " (" (:materiaalikoodi tm) ", " (:maara tm)
                         ", " (:poistettu tm) "), toteumassa " (:id toteuma))
              (materiaalit-q/paivita-toteuma-materiaali!
               c (:materiaalikoodi tm) (:maara tm) (:id user) (:id toteuma) (:id tm))))
          (do
            (log/debug "Luo uusi materiaalitoteuma (" (:materiaalikoodi tm)
                       ", " (:maara tm) ") toteumalle " (:id toteuma))
            (materiaalit-q/luo-toteuma-materiaali<! c (:id toteuma) (:materiaalikoodi tm)
                                                    (:maara tm) (:id user)))))

      ;; Päivitä käytetyt materiaalit toteuman päivälle
      (materiaalit-q/paivita-sopimuksen-materiaalin-kaytto c (:sopimus t) (:alkanut t))

      ;; Jos saatiin parametrina hoitokausi, voidaan palauttaa urakassa käytetyt materiaalit
      ;; Tämä ei ole ehkä paras mahdollinen tapa hoitaa tätä, mutta toteuma/materiaalit näkymässä
      ;; tarvitaan tätä tietoa. -Teemu K
      (when hoitokausi
        (materiaalipalvelut/hae-urakassa-kaytetyt-materiaalit c user (:urakka toteuma)
                                                              (first hoitokausi) (second hoitokausi)
                                                              sopimus)))))

(defn poista-toteuma!
  [db user t]
  (oikeudet/kirjoita  oikeudet/urakat-toteumat-materiaalit   user (:urakka t))
  (jdbc/with-db-transaction [c db]
    (let [mat-ja-teht (q/hae-toteuman-toteuma-materiaalit-ja-tehtavat c (:id t))
          tehtavaidt (filterv #(not (nil? %)) (map :tehtava_id mat-ja-teht))]

      (log/debug "Merkitään tehtavien: " tehtavaidt " maksuerät likaisiksi")
      (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! c tehtavaidt)

      (materiaalit-q/poista-toteuma-materiaali!
        c (:id user) (filterv #(not (nil? %)) (map :materiaali_id mat-ja-teht)))
      (q/poista-tehtava! c (:id user) tehtavaidt)
      (q/poista-toteuma! c (:id user) (:id t))
      true)))

(defn poista-tehtava!
  "Poistaa toteuma-tehtävän id:llä. Vaatii lisäksi urakan id:n oikeuksien tarkastamiseen.
  {:urakka X, :id [A, B, ..]}"
  [db user tiedot]
  (oikeudet/kirjoita  oikeudet/urakat-toteumat-yksikkohintaisettyot user (:urakka tiedot))
  (let [tehtavaid (:id tiedot)]
    (log/debug "Merkitään tehtava: " tehtavaid " maksuerä likaiseksi")
    (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! db tehtavaid)

    (q/poista-tehtava! db (:id user) (:id tiedot))))

(defn hae-urakan-kokonaishintaisten-toteumien-reitit [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tehtava]}]
  (oikeudet/lue  oikeudet/urakat-toteumat-kokonaishintaisettyot user urakka-id)
  (let [reitit (into []
                     (comp
                       (harja.geo/muunna-pg-tulokset :reitti)
                       (map konv/alaviiva->rakenne))
                     (q/hae-kokonaishintaisten-toiden-reitit db
                                                             urakka-id
                                                             sopimus-id
                                                             (konv/sql-date alkupvm)
                                                             (konv/sql-date loppupvm)
                                                             tehtava))
        kasitellyt-reitit (konv/sarakkeet-vektoriin
                            reitit
                            {:reittipiste :reittipisteet
                             :tehtava     :tehtavat}
                            :toteumaid)]
    kasitellyt-reitit))

(defn hae-urakan-yksikkohintaisten-toteumien-reitit [db user {:keys [urakka-id sopimus-id alkupvm loppupvm toimenpide tehtava]}]
  (oikeudet/lue  oikeudet/urakat-toteumat-yksikkohintaisettyot user urakka-id)
  (let [reitit (into []
                     (comp
                       (harja.geo/muunna-pg-tulokset :reitti)
                       (map konv/alaviiva->rakenne))
                     (q/hae-yksikkohintaisten-toiden-reitit db
                                                                   urakka-id
                                                                   sopimus-id
                                                                   (konv/sql-date alkupvm)
                                                                   (konv/sql-date loppupvm)
                                                                   toimenpide
                                                                   tehtava))
        kasitellyt-reitit (konv/sarakkeet-vektoriin
                            reitit
                            {:tehtava :tehtavat}
                            :toteumaid)]
    kasitellyt-reitit))

(defn hae-urakan-varustetoteumat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tienumero]}]
  (oikeudet/lue  oikeudet/urakat-toteumat-varusteet user urakka-id)
  (log/debug "Haetaan varustetoteumat: " urakka-id sopimus-id alkupvm loppupvm tienumero)
  (let [toteumat (into []
                       (comp
                         (map #(konv/string->keyword % :toimenpide))
                         (map #(konv/string->keyword % :toteumatyyppi))
                         (harja.geo/muunna-pg-tulokset :reittipiste_sijainti)
                         (map konv/alaviiva->rakenne))
                       (q/hae-urakan-varustetoteumat db
                                                     urakka-id
                                                     sopimus-id
                                                     (konv/sql-date alkupvm)
                                                     (konv/sql-date loppupvm)
                                                     (if tienumero true false)
                                                     tienumero))
        kasitellyt-toteumarivit (konv/sarakkeet-vektoriin
                                  toteumat
                                  {:reittipiste :reittipisteet
                                   :toteumatehtava :toteumatehtavat}
                                  :id)]
    (log/debug "Palautetaan " (count kasitellyt-toteumarivit) " varustetoteuma(a)")
    kasitellyt-toteumarivit))

(defn hae-kokonaishintaisen-toteuman-tiedot [db user urakka-id pvm toimenpidekoodi]
  (oikeudet/lue  oikeudet/urakat-toteumat-kokonaishintaisettyot user urakka-id)
  (into []
        (map konv/alaviiva->rakenne)
        (q/hae-kokonaishintaisen-toteuman-tiedot db urakka-id pvm toimenpidekoodi)))

(defn hae-toteuman-reitti-ja-tr-osoite [db user {:keys [id urakka-id]}]
  (oikeudet/lue  oikeudet/urakat-toteumat-kokonaishintaisettyot user urakka-id)
  (first
    (into []
          (comp
            (harja.geo/muunna-pg-tulokset :reitti)
            (map konv/alaviiva->rakenne))
          (q/hae-toteuman-reitti-ja-tr-osoite db id))))

(defrecord Toteumat []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)]
      (julkaise-palvelu http :urakan-toteuma
                        (fn [user tiedot]
                          (hae-urakan-toteuma db user tiedot)))
      (julkaise-palvelu http :urakan-toteumien-tehtavien-summat
                        (fn [user tiedot]
                          (hae-urakan-toteumien-tehtavien-summat db user tiedot)))
      (julkaise-palvelu http :poista-toteuma!
                        (fn [user toteuma]
                          (poista-toteuma! db user toteuma)))
      (julkaise-palvelu http :poista-tehtava!
                        (fn [user tiedot]
                          (poista-tehtava! db user tiedot)))
      (julkaise-palvelu http :urakan-toteutuneet-tehtavat-toimenpidekoodilla
                        (fn [user tiedot]
                          (hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db user tiedot)))
      (julkaise-palvelu http :hae-urakan-tehtavat
                        (fn [user urakka-id]
                          (hae-urakan-tehtavat db user urakka-id)))
      (julkaise-palvelu http :tallenna-urakan-toteuma-ja-yksikkohintaiset-tehtavat
                        (fn [user toteuma]
                          (tallenna-toteuma-ja-yksikkohintaiset-tehtavat db user toteuma)))
      (julkaise-palvelu http :tallenna-urakan-toteuma-ja-kokonaishintaiset-tehtavat
                        (fn [user {:keys [toteuma hakuparametrit]}]
                          (tallenna-toteuma-ja-kokonaishintaiset-tehtavat db user toteuma hakuparametrit)))
      (julkaise-palvelu http :paivita-yk-hint-toteumien-tehtavat
                        (fn [user tiedot]
                          (paivita-yk-hint-toiden-tehtavat db user tiedot)))
      (julkaise-palvelu http :urakan-erilliskustannukset
                        (fn [user tiedot]
                          (hae-urakan-erilliskustannukset db user tiedot)))
      (julkaise-palvelu http :tallenna-erilliskustannus
                        (fn [user toteuma]
                          (tallenna-erilliskustannus db user toteuma)))
      (julkaise-palvelu http :urakan-muut-tyot
                        (fn [user tiedot]
                          (hae-urakan-muut-tyot db user tiedot)))
      (julkaise-palvelu http :tallenna-muiden-toiden-toteuma
                        (fn [user toteuma]
                          (tallenna-muiden-toiden-toteuma db user toteuma)))
      (julkaise-palvelu http :tallenna-toteuma-ja-toteumamateriaalit
                        (fn [user tiedot]
                          (tallenna-toteuma-ja-toteumamateriaalit db user (:toteuma tiedot)
                                                                  (:toteumamateriaalit tiedot)
                                                                  (:hoitokausi tiedot)
                                                                  (:sopimus tiedot))))
      (julkaise-palvelu http :hae-urakan-kokonaishintaisten-toteumien-tehtavien-paivakohtaiset-summat
                        (fn [user tiedot]
                          (hae-urakan-kokonaishintaisten-toteumien-tehtavien-paivakohtaiset-summat db user tiedot)))
      (julkaise-palvelu http :hae-kokonaishintaisen-toteuman-tiedot
                        (fn [user {:keys [urakka-id pvm toimenpidekoodi]}]
                          (hae-kokonaishintaisen-toteuman-tiedot db user urakka-id pvm toimenpidekoodi)))
      (julkaise-palvelu http :urakan-kokonaishintaisten-toteumien-reitit
                        (fn [user tiedot]
                          (hae-urakan-kokonaishintaisten-toteumien-reitit db user tiedot)))
      (julkaise-palvelu http :urakan-yksikkohintaisten-toteumien-reitit
                        (fn [user tiedot]
                          (hae-urakan-yksikkohintaisten-toteumien-reitit db user tiedot)))
      (julkaise-palvelu http :urakan-varustetoteumat
                        (fn [user tiedot]
                          (hae-urakan-varustetoteumat db user tiedot)))
      (julkaise-palvelu http :hae-toteuman-reitti-ja-tr-osoite
                        (fn [user tiedot]
                          (hae-toteuman-reitti-ja-tr-osoite db user tiedot)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-toteuma
      :urakan-toteumien-tehtavien-summat
      :poista-toteuma!
      :poista-tehtava!
      :urakan-toteutuneet-tehtavat-toimenpidekoodilla
      :hae-urakan-tehtavat
      :tallenna-urakan-toteuma-ja-yksikkohintaiset-tehtavat
      :tallenna-urakan-toteuma-ja-kokonaishintaiset-tehtavat
      :paivita-yk-hint-toteumien-tehtavat
      :urakan-erilliskustannukset
      :tallenna-erilliskustannus
      :urakan-muut-tyot
      :tallenna-muiden-toiden-toteuma
      :tallenna-toteuma-ja-toteumamateriaalit
      :hae-urakan-kokonaishintaisten-toteumien-tehtavien-paivakohtaiset-summat
      :hae-kokonaishintaisen-toteuman-tiedot
      :urakan-kokonaishintaisten-toteumien-reitit
      :urakan-yksikkohintaisten-toteumien-reitit
      :urakan-varustetoteumat
      :hae-toteuman-reitti-ja-tr-osoite)
    this))
