(ns harja.palvelin.main
  (:require
   [taoensso.timbre :as log]
   ;; Yleiset palvelinkomponentit
   [harja.palvelin.komponentit.tietokanta :as tietokanta]
   [harja.palvelin.komponentit.http-palvelin :as http-palvelin]
   [harja.palvelin.komponentit.todennus :as todennus]
   [harja.palvelin.komponentit.fim :as fim]
   [harja.palvelin.komponentit.tapahtumat :as tapahtumat]
   [harja.palvelin.komponentit.sonja :as sonja]
   [harja.palvelin.komponentit.pdf-vienti :as pdf-vienti]

   ;; Integraatiokomponentit
   [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
   [harja.palvelin.integraatiot.sampo.sampo-komponentti :as sampo]
   [harja.palvelin.integraatiot.tloik.tloik-komponentti :as tloik]
   [harja.palvelin.integraatiot.tierekisteri.tierekisteri-komponentti
    :as tierekisteri]
   [harja.palvelin.integraatiot.labyrintti.sms :as labyrintti]
   [harja.palvelin.integraatiot.sonja.sahkoposti :as sonja-sahkoposti]
   [harja.palvelin.integraatiot.sahkoposti :as sahkoposti]

   ;; Raportointi
   [harja.palvelin.raportointi :as raportointi]

   ;; Harjan bisneslogiikkapalvelut
   [harja.palvelin.palvelut.kayttajatiedot :as kayttajatiedot]
   [harja.palvelin.palvelut.urakoitsijat :as urakoitsijat]
   [harja.palvelin.palvelut.haku :as haku]
   [harja.palvelin.palvelut.hallintayksikot :as hallintayksikot]
   [harja.palvelin.palvelut.indeksit :as indeksit]
   [harja.palvelin.palvelut.urakat :as urakat]
   [harja.palvelin.palvelut.urakan-toimenpiteet :as urakan-toimenpiteet]
   [harja.palvelin.palvelut.yksikkohintaiset-tyot :as yksikkohintaiset-tyot]
   [harja.palvelin.palvelut.kokonaishintaiset-tyot :as kokonaishintaiset-tyot]
   [harja.palvelin.palvelut.muut-tyot :as muut-tyot]
   [harja.palvelin.palvelut.toteumat :as toteumat]
   [harja.palvelin.palvelut.toimenpidekoodit :as toimenpidekoodit]
   [harja.palvelin.palvelut.yhteyshenkilot]
   [harja.palvelin.palvelut.paallystys :as paallystys]
   [harja.palvelin.palvelut.paikkaus :as paikkaus]
   [harja.palvelin.palvelut.ping :as ping]
   [harja.palvelin.palvelut.kayttajat :as kayttajat]
   [harja.palvelin.palvelut.pohjavesialueet :as pohjavesialueet]
   [harja.palvelin.palvelut.materiaalit :as materiaalit]
   [harja.palvelin.palvelut.selainvirhe :as selainvirhe]
   [harja.palvelin.palvelut.valitavoitteet :as valitavoitteet]
   [harja.palvelin.palvelut.siltatarkastukset :as siltatarkastukset]
   [harja.palvelin.palvelut.lampotilat :as lampotilat]
   [harja.palvelin.palvelut.maksuerat :as maksuerat]
   [harja.palvelin.palvelut.liitteet :as liitteet]
   [harja.palvelin.palvelut.muokkauslukko :as muokkauslukko]
   [harja.palvelin.palvelut.laadunseuranta :as laadunseuranta]
   [harja.palvelin.palvelut.ilmoitukset :as ilmoitukset]
   [harja.palvelin.palvelut.turvallisuuspoikkeamat :as turvallisuuspoikkeamat]
   [harja.palvelin.palvelut.integraatioloki :as integraatioloki-palvelu]
   [harja.palvelin.palvelut.raportit :as raportit]
   [harja.palvelin.palvelut.tyokoneenseuranta :as tyokoneenseuranta]
   [harja.palvelin.palvelut.tilannekuva :as tilannekuva]

   ;; karttakuvien renderöinti
   [harja.palvelin.palvelut.karttakuvat :as karttakuvat]


   ;; Tierekisteriosoitteen selvitys lokaalista tieverkkodatasta
   [harja.palvelin.palvelut.tierek-haku :as tierek-haku]

   ;; Harja API
   [harja.palvelin.integraatiot.api.urakat :as api-urakat]
   [harja.palvelin.integraatiot.api.laatupoikkeamat :as api-laatupoikkeamat]
   [harja.palvelin.integraatiot.api.paivystajatiedot :as api-paivystajatiedot]
   [harja.palvelin.integraatiot.api.pistetoteuma :as api-pistetoteuma]
   [harja.palvelin.integraatiot.api.reittitoteuma :as api-reittitoteuma]
   [harja.palvelin.integraatiot.api.varustetoteuma :as api-varustetoteuma]
   [harja.palvelin.integraatiot.api.siltatarkastukset :as api-siltatarkastukset]
   [harja.palvelin.integraatiot.api.tarkastukset :as api-tarkastukset]
   [harja.palvelin.integraatiot.api.tyokoneenseuranta :as api-tyokoneenseuranta]
   [harja.palvelin.integraatiot.api.tyokoneenseuranta-puhdistus :as tks-putsaus]
   [harja.palvelin.integraatiot.api.turvallisuuspoikkeama :as turvallisuuspoikkeama]
   [harja.palvelin.integraatiot.api.varusteet :as api-varusteet]
   [harja.palvelin.integraatiot.api.ilmoitukset :as api-ilmoitukset]

   ;; Ajastetut tehtävät
   [harja.palvelin.ajastetut-tehtavat.suolasakkojen-lahetys
    :as suolasakkojen-lahetys]
   [harja.palvelin.ajastetut-tehtavat.geometriapaivitykset :as geometriapaivitykset]

   [com.stuartsierra.component :as component]
   [harja.palvelin.asetukset
    :refer [lue-asetukset konfiguroi-lokitus validoi-asetukset]])
  (:import [java.util Locale])
  (:gen-class))

(defn luo-jarjestelma [asetukset]
  (let [{:keys [tietokanta tietokanta-replica http-palvelin kehitysmoodi]} asetukset]
    (konfiguroi-lokitus asetukset)
    (try
      (validoi-asetukset asetukset)
      (catch Exception e
        (log/error e "Validointivirhe asetuksissa!")))

    (component/system-map
      :db (tietokanta/luo-tietokanta tietokanta)
      :db-replica (tietokanta/luo-tietokanta tietokanta-replica)
      :klusterin-tapahtumat (component/using
                              (tapahtumat/luo-tapahtumat)
                              [:db])

      :todennus (component/using
                  (todennus/http-todennus (:testikayttajat asetukset))
                  [:db :klusterin-tapahtumat])
      :http-palvelin (component/using
                       (http-palvelin/luo-http-palvelin http-palvelin
                                                        kehitysmoodi)
                       [:todennus])

      :pdf-vienti (component/using
                    (pdf-vienti/luo-pdf-vienti)
                    [:http-palvelin])
      :liitteiden-hallinta (component/using
                             (harja.palvelin.komponentit.liitteet/->Liitteet)
                             [:db])

      ;; Integraatioloki
      :integraatioloki
      (component/using (integraatioloki/->Integraatioloki
                        (:paivittainen-lokin-puhdistusaika
                         (:integraatiot asetukset)))
                       [:db])

      ;; Sonja (Sonic ESB) JMS yhteyskomponentti
      :sonja (sonja/luo-sonja (:sonja asetukset))
      :sonja-sahkoposti
      (component/using
       (let [{:keys [vastausosoite jonot suora? palvelin]}
             (:sonja-sahkoposti asetukset)]
         (if suora?
           (sahkoposti/luo-vain-lahetys palvelin vastausosoite)
           (sonja-sahkoposti/luo-sahkoposti vastausosoite jonot)))
       [:sonja :integraatioloki :db])

      ;; FIM REST rajapinta
      :fim (fim/->FIM (:url (:fim asetukset)))

      ;; Sampo
      :sampo (component/using (let [sampo (:sampo asetukset)]
                                (sampo/->Sampo (:lahetysjono-sisaan sampo)
                                               (:kuittausjono-sisaan sampo)
                                               (:lahetysjono-ulos sampo)
                                               (:kuittausjono-ulos sampo)
                                               (:paivittainen-lahetysaika sampo)))
                              [:sonja :db :integraatioloki])

      ;; T-LOIK
      :tloik (component/using
              (tloik/->Tloik (:tloik asetukset))
              [:sonja :db :integraatioloki :klusterin-tapahtumat
               :sonja-sahkoposti :labyrintti])

      ;; Tierekisteri
      :tierekisteri (component/using
                     (tierekisteri/->Tierekisteri (:url (:tierekisteri asetukset)))
                     [:db :integraatioloki])

      ;; Labyrintti SMS Gateway
      :labyrintti (component/using
                   (labyrintti/luo-labyrintti (:labyrintti asetukset))
                   [:http-palvelin :db :integraatioloki])

      :raportointi (component/using
                     (raportointi/luo-raportointi)
                     {:db         :db-replica
                      :pdf-vienti :pdf-vienti})

      ;; Frontille tarjottavat palvelut
      :kayttajatiedot (component/using
                        (kayttajatiedot/->Kayttajatiedot (:testikayttajat asetukset))
                        [:http-palvelin :db])
      :urakoitsijat (component/using
                      (urakoitsijat/->Urakoitsijat)
                      [:http-palvelin :db])
      :hallintayksikot (component/using
                         (hallintayksikot/->Hallintayksikot)
                         [:http-palvelin :db])
      :ping (component/using
              (ping/->Ping)
              [:http-palvelin :db])
      :haku (component/using
              (haku/->Haku)
              [:http-palvelin :db])
      :indeksit (component/using
                  (indeksit/->Indeksit)
                  [:http-palvelin :db])
      :urakat (component/using
                (urakat/->Urakat)
                [:http-palvelin :db])
      :urakan-toimenpiteet (component/using
                             (urakan-toimenpiteet/->Urakan-toimenpiteet)
                             [:http-palvelin :db])
      :yksikkohintaiset-tyot (component/using
                               (yksikkohintaiset-tyot/->Yksikkohintaiset-tyot)
                               [:http-palvelin :db])
      :kokonaishintaiset-tyot (component/using
                                (kokonaishintaiset-tyot/->Kokonaishintaiset-tyot)
                                [:http-palvelin :db])
      :muut-tyot (component/using
                   (muut-tyot/->Muut-tyot)
                   [:http-palvelin :db])
      :toteumat (component/using
                  (toteumat/->Toteumat)
                  [:http-palvelin :db])
      :paallystys (component/using
                    (paallystys/->Paallystys)
                    [:http-palvelin :db])
      :muokkauslukko (component/using
                       (muokkauslukko/->Muokkauslukko)
                       [:http-palvelin :db])
      :paikkaus (component/using
                  (paikkaus/->Paikkaus)
                  [:http-palvelin :db])
      :yhteyshenkilot (component/using
                        (harja.palvelin.palvelut.yhteyshenkilot/->Yhteyshenkilot)
                        [:http-palvelin :db])
      :toimenpidekoodit (component/using
                          (toimenpidekoodit/->Toimenpidekoodit)
                          [:http-palvelin :db])
      :kayttajat (component/using
                   (kayttajat/->Kayttajat)
                   [:http-palvelin :db :fim :klusterin-tapahtumat])
      :pohjavesialueet (component/using
                         (pohjavesialueet/->Pohjavesialueet)
                         [:http-palvelin :db])
      :materiaalit (component/using
                     (materiaalit/->Materiaalit)
                     [:http-palvelin :db])
      :selainvirhe (component/using
                     (selainvirhe/->Selainvirhe)
                     [:http-palvelin])
      :valitavoitteet (component/using
                        (valitavoitteet/->Valitavoitteet)
                        [:http-palvelin :db])
      :siltatarkastukset (component/using
                           (siltatarkastukset/->Siltatarkastukset)
                           [:http-palvelin :db])
      :lampotilat (component/using
                   (lampotilat/->Lampotilat
                    (:lampotilat-url (:ilmatieteenlaitos asetukset)))
                   [:http-palvelin :db])
      :maksuerat (component/using
                   (maksuerat/->Maksuerat)
                   [:http-palvelin :sampo :db])

      :liitteet (component/using
                  (liitteet/->Liitteet)
                  [:http-palvelin :liitteiden-hallinta])

      :laadunseuranta (component/using
                        (laadunseuranta/->Laadunseuranta)
                        [:http-palvelin :db])

      :ilmoitukset (component/using
                     (ilmoitukset/->Ilmoitukset)
                     [:http-palvelin :db :tloik])

      :turvallisuuspoikkeamat (component/using
                                (turvallisuuspoikkeamat/->Turvallisuuspoikkeamat)
                                [:http-palvelin :db])

      :integraatioloki-palvelu (component/using
                                 (integraatioloki-palvelu/->Integraatioloki)
                                 [:http-palvelin :db])
      :raportit (component/using
                  (raportit/->Raportit)
                  [:http-palvelin :db :raportointi :pdf-vienti])

      :tyokoneenseuranta (component/using
                           (tyokoneenseuranta/->TyokoneseurantaHaku)
                           [:http-palvelin :db])

      :tr-haku (component/using
                (tierek-haku/->TierekisteriHaku)
                [:http-palvelin :db])

      :geometriapaivitykset (component/using
                             (geometriapaivitykset/->Geometriapaivitykset
                              (:geometriapaivitykset asetukset))
                             [:db :integraatioloki])

      :tilannekuva (component/using
                     (tilannekuva/->Tilannekuva)
                     [:http-palvelin :db :karttakuvat])
      :karttakuvat (component/using
                   (karttakuvat/luo-karttakuvat)
                   [:http-palvelin :db])

      ;; Harja API
      :api-urakat (component/using
                    (api-urakat/->Urakat)
                    [:http-palvelin :db :integraatioloki])
      :api-laatupoikkeamat (component/using
                             (api-laatupoikkeamat/->Laatupoikkeamat)
                             [:http-palvelin :db :liitteiden-hallinta
                              :integraatioloki])
      :api-paivystajatiedot (component/using
                              (api-paivystajatiedot/->Paivystajatiedot)
                              [:http-palvelin :db :integraatioloki])
      :api-pistetoteuma (component/using
                          (api-pistetoteuma/->Pistetoteuma)
                          [:http-palvelin :db :integraatioloki])
      :api-reittitoteuma (component/using
                           (api-reittitoteuma/->Reittitoteuma)
                           [:http-palvelin :db :integraatioloki])
      :api-varustetoteuma (component/using
                            (api-varustetoteuma/->Varustetoteuma)
                            [:http-palvelin :db :tierekisteri :integraatioloki])
      :api-siltatarkastukset (component/using
                               (api-siltatarkastukset/->Siltatarkastukset)
                               [:http-palvelin :db :integraatioloki])
      :api-tarkastukset (component/using
                          (api-tarkastukset/->Tarkastukset)
                          [:http-palvelin :db :integraatioloki :liitteiden-hallinta])
      :api-tyokoneenseuranta (component/using
                               (api-tyokoneenseuranta/->Tyokoneenseuranta)
                               [:http-palvelin :db])
      :api-tyokoneenseuranta-puhdistus (component/using
                                        (tks-putsaus/->TyokoneenseurantaPuhdistus)
                                        [:db])
      :api-turvallisuuspoikkeama (component/using
                                  (turvallisuuspoikkeama/->Turvallisuuspoikkeama)
                                  [:http-palvelin :db :integraatioloki
                                   :liitteiden-hallinta])
      :api-suolasakkojen-lahetys (component/using
                                  (suolasakkojen-lahetys/->SuolasakkojenLahetys)
                                  [:db])
      :api-varusteet (component/using
                      (api-varusteet/->Varusteet)
                      [:http-palvelin :db :integraatioloki :tierekisteri])
      :api-ilmoitukset (component/using
                        (api-ilmoitukset/->Ilmoitukset)
                        [:http-palvelin :db :integraatioloki :klusterin-tapahtumat
                         :tloik]))))

(defonce harja-jarjestelma nil)

(defn kaynnista-jarjestelma [asetusfile]
  (Locale/setDefault (Locale. "fi" "FI"))
  (alter-var-root #'harja-jarjestelma
                  (constantly
                    (-> (lue-asetukset asetusfile)
                        luo-jarjestelma
                        component/start))))

(defn sammuta-jarjestelma []
  (when harja-jarjestelma
    (alter-var-root #'harja-jarjestelma (fn [s]
                                          (component/stop s)
                                          nil))))

(defn -main [& argumentit]
  (kaynnista-jarjestelma (or (first argumentit) "asetukset.edn"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. sammuta-jarjestelma)))

(defn dev-start []
  (if harja-jarjestelma
    (println "Harja on jo käynnissä!")
    (kaynnista-jarjestelma "asetukset.edn")))

(defn dev-stop []
  (sammuta-jarjestelma))

(defn dev-restart []
  (dev-stop)
  (dev-start))


(defn dev-julkaise
  "REPL käyttöön: julkaise uusi palvelu (poistaa ensin vanhan samalla nimellä)."
  [nimi fn]
  (http-palvelin/poista-palvelu (:http-palvelin harja-jarjestelma) nimi)
  (http-palvelin/julkaise-palvelu (:http-palvelin harja-jarjestelma) nimi fn))

(defmacro with-db [s & body]
  `(let [~s (:db harja-jarjestelma)]
     ~@body))

(defn q
  "Kysele Harjan kannasta, REPL kehitystä varten"
  [& sql]
  (with-open [c (.getConnection (:datasource (:db harja-jarjestelma)))
              ps (.prepareStatement c (reduce str sql))
              rs (.executeQuery ps)]
    (let [cols (-> (.getMetaData rs) .getColumnCount)]
      (loop [res []
             more? (.next rs)]
        (if-not more?
          res
          (recur (conj res (loop [row []
                                  i 1]
                             (if (<= i cols)
                               (recur (conj row (.getObject rs i)) (inc i))
                               row)))
                 (.next rs)))))))

(defn u
  "UPDATE Harjan kantaan"
  [& sql]
  (with-open [c (.getConnection (:datasource (:db harja-jarjestelma)))
              ps (.prepareStatement c (reduce str sql))]
    (.executeUpdate ps)))

(defn explain [sql]
  (q "EXPLAIN (ANALYZE, COSTS, VERBOSE, BUFFERS, FORMAT JSON) " sql))
