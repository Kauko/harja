(ns harja.palvelin.integraatiot.api.tarkastukset
  "Tarkastusten kirjaaminen urakalle"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.tyokalut.json :as json]
            [harja.kyselyt.tarkastukset :as tarkastukset]
            [harja.kyselyt.laatupoikkeamat :as laatupoikkeamat]
            [clojure.java.jdbc :as jdbc]
            [slingshot.slingshot :refer [try+ throw+]]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :refer [tallenna-liitteet-laatupoikkeamalle]]))


(defn kirjaa-tarkastus [db liitteiden-hallinta kayttaja tyyppi {id :id} tarkastus]
  (let [urakka-id (Long/parseLong id)
        ulkoinen-id (-> tarkastus :tunniste :id)]
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
    (log/info "TARKASTUS TULOSSA: " tarkastus "; käyttäjä: " kayttaja)
    (log/debug "tyyppi: " tyyppi)
    (jdbc/with-db-transaction [db db]
      (let [{tarkastus-id :id laatupoikkeama-id :laatupoikkeama}
            (first
              (tarkastukset/hae-tarkastus-ulkoisella-idlla-ja-tyypilla db ulkoinen-id (name tyyppi) (:id kayttaja)))
            uusi? (nil? tarkastus-id)]

        (let [aika (json/pvm-string->java-sql-date (:paivamaara tarkastus))
              laatupoikkeama (merge (:laatupoikkeama tarkastus)
                              {:aika   aika
                               :id     laatupoikkeama-id
                               :urakka urakka-id
                               :tekija :urakoitsija})
              laatupoikkeama-id (laatupoikkeamat/luo-tai-paivita-laatupoikkeama db kayttaja laatupoikkeama)
              id (tarkastukset/luo-tai-paivita-tarkastus
                   db kayttaja urakka-id
                   {:id          tarkastus-id
                    :ulkoinen-id ulkoinen-id
                    :tyyppi      tyyppi
                    :aika        aika
                    :tarkastaja  (json/henkilo->nimi (:tarkastaja tarkastus))
                    :mittaaja    (json/henkilo->nimi (-> tarkastus :mittaus :mittaaja))
                    :tr          (json/sijainti->tr (:sijainti tarkastus))
                    :sijainti    (json/sijainti->point (:sijainti tarkastus))}
                   laatupoikkeama-id)
              liitteet (:liitteet (:laatupoikkeama tarkastus))]

          (tallenna-liitteet-laatupoikkeamalle db liitteiden-hallinta urakka-id laatupoikkeama-id kayttaja liitteet)

          (case tyyppi
            :talvihoito (tarkastukset/luo-tai-paivita-talvihoitomittaus
                          db id uusi? (-> tarkastus
                                          :mittaus
                                          (assoc :lumimaara (:lumisuus (:mittaus tarkastus)))))

            :soratie (tarkastukset/luo-tai-paivita-soratiemittaus
                       db id uusi? (:mittaus tarkastus))
            nil)

          nil)))))

;; Määritellään tarkastustyypit, joiden lisäämiselle tehdään API palvelut
(def tarkastukset
  [{:palvelu       :lisaa-tiestotarkastus
    :polku         "/api/urakat/:id/tarkastus/tiestotarkastus"
    :pyynto-skeema json-skeemat/+tiestotarkastuksen-kirjaus+
    :tyyppi        :tiesto}
   {:palvelu       :lisaa-talvihoitotarkastus
    :polku         "/api/urakat/:id/tarkastus/talvihoitotarkastus"
    :pyynto-skeema json-skeemat/+talvihoitotarkastuksen-kirjaus+
    :tyyppi        :talvihoito}
   {:palvelu       :lisaa-soratietarkastus
    :polku         "/api/urakat/:id/tarkastus/soratietarkastus"
    :pyynto-skeema json-skeemat/+soratietarkastuksen-kirjaus+
    :tyyppi        :soratie}])

(defrecord Tarkastukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db liitteiden-hallinta :liitteiden-hallinta integraatioloki :integraatioloki :as this}]
    (doseq [{:keys [palvelu polku pyynto-skeema tyyppi]} tarkastukset]
      (julkaise-reitti
        http palvelu
        (POST polku request
          (do
            (log/info "REQUEST: " (pr-str request))
            (kasittele-kutsu db integraatioloki palvelu request
                             pyynto-skeema nil
                             (fn [parametrit data kayttaja db]
                               (kirjaa-tarkastus db liitteiden-hallinta kayttaja tyyppi parametrit data)))))))

    this)

  (stop [{http :http-palvelin :as this}]
    (apply poista-palvelut http (map :palvelu tarkastukset))
    this))
