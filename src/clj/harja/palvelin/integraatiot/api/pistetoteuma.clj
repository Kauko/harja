(ns harja.palvelin.integraatiot.api.pistetoteuma
  "Pistetoteuman kirjaaminen urakalle"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.toteuma :as api-toteuma]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :refer [dekoodaa-base64]]
            [harja.palvelin.integraatiot.api.tyokalut.json :refer [aika-string->java-sql-date]]
            [clojure.java.jdbc :as jdbc]
            [harja.palvelin.integraatiot.api.validointi.toteumat :as toteuman-validointi])
  (:use [slingshot.slingshot :only [throw+]]))

(defn tee-onnistunut-vastaus []
  (let [vastauksen-data {:ilmoitukset "Pistetoteuma kirjattu onnistuneesti"}]
    vastauksen-data))

(defn tallenna-yksittainen-pistetoteuma [db urakka-id kirjaaja pistetoteuma]
  (log/debug "Käsitellään yksittäinen pistetoteuma tunnisteella " (get-in pistetoteuma [:toteuma :tunniste :id]))
  (let [toteuma (assoc (:toteuma pistetoteuma) :reitti nil)
        toteuma-id (api-toteuma/paivita-tai-luo-uusi-toteuma db urakka-id kirjaaja toteuma)
        sijainti (:sijainti pistetoteuma)
        aika (aika-string->java-sql-date (get-in pistetoteuma [:toteuma :alkanut]))]
    (log/debug "Toteuman perustiedot tallennettu. id: " toteuma-id)
    (log/debug "Aloitetaan sijainnin tallennus")
    (api-toteuma/tallenna-sijainti db sijainti aika toteuma-id)
    (log/debug "Aloitetaan toteuman tehtävien tallennus")
    (api-toteuma/tallenna-tehtavat db kirjaaja toteuma toteuma-id)))

(defn tallenna-kaikki-pyynnon-pistetoteumat [db urakka-id kirjaaja data]
  (jdbc/with-db-transaction [transaktio db]
    (when (:pistetoteuma data)
      (tallenna-yksittainen-pistetoteuma db urakka-id kirjaaja (:pistetoteuma data)))
    (doseq [pistetoteuma (:pistetoteumat data)]
      (tallenna-yksittainen-pistetoteuma db urakka-id kirjaaja (:pistetoteuma pistetoteuma)))))

(defn tarkista-kaikki-pyynnon-pistetoteumat [db urakka-id data]
  (when (:pistetoteuma data)
    (toteuman-validointi/tarkista-tehtavat db urakka-id (get-in data [:pistetoteuma :toteuma :tehtavat])))
  (doseq [pistetoteuma (:pistetoteumat data)]
    (toteuman-validointi/tarkista-tehtavat db urakka-id (get-in pistetoteuma [:pistetoteuma :toteuma :tehtavat]))))

(defn kirjaa-toteuma [db {id :id} data kirjaaja]
  (let [urakka-id (Integer/parseInt id)]
    (log/debug "Kirjataan uusi pistetoteuma urakalle id:" urakka-id " kayttäjän:" (:kayttajanimi kirjaaja) " (id:" (:id kirjaaja) " tekemänä.")
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kirjaaja)
    (tarkista-kaikki-pyynnon-pistetoteumat db urakka-id data)
    (tallenna-kaikki-pyynnon-pistetoteumat db urakka-id kirjaaja data)
    (tee-onnistunut-vastaus)))

(defrecord Pistetoteuma []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki :as this}]
    (julkaise-reitti
      http :lisaa-pistetoteuma
      (POST "/api/urakat/:id/toteumat/piste" request
        (kasittele-kutsu db integraatioloki :lisaa-pistetoteuma request json-skeemat/+pistetoteuman-kirjaus+ json-skeemat/+kirjausvastaus+
                         (fn [parametit data kayttaja db] (kirjaa-toteuma db parametit data kayttaja)))))
    this)
  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :lisaa-pistetoteuma)
    this))
