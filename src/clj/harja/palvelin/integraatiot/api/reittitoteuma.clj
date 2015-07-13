(ns harja.palvelin.integraatiot.api.reittitoteuma
  "Reittitoteuman kirjaaminen urakalle"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.palvelin.integraatiot.api.tyokalut.skeemat :as skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.kyselyt.havainnot :as havainnot]
            [harja.kyselyt.kommentit :as kommentit]
            [harja.kyselyt.toteumat :as toteumat]
            [harja.palvelin.integraatiot.api.toteuma :as api-toteuma]
            [harja.palvelin.komponentit.liitteet :refer [->Liitteet] :as liitteet]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :refer [dekoodaa-base64]]
            [harja.palvelin.integraatiot.api.tyokalut.json :refer [parsi-aika]]
            [clojure.java.jdbc :as jdbc])
  (:use [slingshot.slingshot :only [throw+]]))

(defn tee-onnistunut-vastaus []
  (let [vastauksen-data {:ilmoitukset "Reittitoteuma kirjattu onnistuneesti"}]
    vastauksen-data))

(defn tallenna-reitti [db kirjaaja reitti toteuma-id]
  ; FIXME tee
  )

(defn tallenna-reitin-tehtavat [db kirjaaja reitti toteuma-id]
  ; FIXME tee
  )

(defn tallenna-reitin-materiaalit [db kirjaaja reitti toteuma-id]
  ; FIXME tee
  )

(defn tallenna [db urakka-id kirjaaja data]
  (jdbc/with-db-transaction [transaktio db]
    (let [toteuma (get-in data [:reittitoteuma :toteuma])
          reitti (get-in data [:reittitoteuma :reitti])
          toteuma-id (api-toteuma/tallenna-toteuma transaktio urakka-id kirjaaja toteuma)]
      (log/debug "Toteuman perustiedot tallennettu. id: " toteuma-id)
      (log/debug "Tallennetaan toteuman sijainti")
      (api-toteuma/tallenna-tehtavat transaktio kirjaaja toteuma toteuma-id)
      (api-toteuma/tallenna-materiaalit transaktio kirjaaja toteuma toteuma-id)
      (tallenna-reitti transaktio kirjaaja reitti toteuma-id)
      (tallenna-reitin-tehtavat transaktio kirjaaja reitti toteuma-id)
      (tallenna-reitin-materiaalit transaktio kirjaaja reitti toteuma-id))))

(defn kirjaa-toteuma [db {id :id} data kirjaaja]
  (let [urakka-id (Integer/parseInt id)]
    (log/debug "Kirjataan uusi reittitoteuma urakalle id:" urakka-id " kayttäjän:" (:kayttajanimi kirjaaja) " (id:" (:id kirjaaja) " tekemänä.")
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kirjaaja)
    (tallenna db urakka-id kirjaaja data)
    (tee-onnistunut-vastaus)))

(defrecord Reittitoteuma []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki :as this}]
    (julkaise-reitti
      http :lisaa-reittitoteuma
      (POST "/api/urakat/:id/toteumat/reitti" request
        (kasittele-kutsu db integraatioloki :lisaa-reittitoteuma request skeemat/+reittitoteuman-kirjaus+ skeemat/+kirjausvastaus+
                         (fn [parametit data kayttaja] (kirjaa-toteuma db parametit data kayttaja)))))
    this)
  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :lisaa-pistetoteuma)
    this))
