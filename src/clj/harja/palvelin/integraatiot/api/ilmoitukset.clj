(ns harja.palvelin.integraatiot.api.ilmoitukset
  "Ilmoitusten haku ja ilmoitustoimenpiteiden kirjaus"
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [with-channel on-close send!]]
            [compojure.core :refer [PUT GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu lokita-kutsu lokita-vastaus tee-vastaus aja-virhekasittelyn-kanssa hae-kayttaja]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.ilmoitusnotifikaatio :as notifikaatiot]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :refer [tallenna-liitteet-havainnolle]]
            [harja.palvelin.integraatiot.api.tyokalut.json :refer [pvm-string->java-sql-date]]
            [harja.kyselyt.ilmoitukset :as ilmoitukset]
            [harja.kyselyt.konversio :as konversio]
            [harja.palvelin.integraatiot.api.sanomat.ilmoitus-sanomat :as sanomat])
  (:use [slingshot.slingshot :only [throw+]]))

(defn kirjaa-ilmoitustoimenpide [db parametrit data kayttaja])

(defn hae-ilmoitus [db ilmoitus-id]
  (let [data (some->> ilmoitus-id (ilmoitukset/hae-ilmoitus db) first konversio/alaviiva->rakenne)]
    {:ilmoitukset [(sanomat/rakenna-ilmoitus data)]}))

(defn kaynnista-ilmoitusten-kuuntelu [db integraatioloki tapahtumat request]
  (let [parametrit (:params request)
        urakka-id (when (:id parametrit) (Integer/parseInt (:id parametrit)))
        viimeisin-id (when (:viimeisinId parametrit) (Integer/parseInt (:viimeisinId parametrit)))
        stream (if (get parametrit "stream") (not (Boolean/valueOf (get parametrit "stream"))) true)
        tapahtuma-id (lokita-kutsu integraatioloki :hae-ilmoitukset request nil)
        kayttaja (hae-kayttaja db (get (:headers request) "oam_remote_user"))]
    (log/debug (format "Käynnistetään ilmoitusten kuuntelu urakalle id: %s." urakka-id))
    (aja-virhekasittelyn-kanssa
      (fn []
        (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
        (with-channel request kanava
          (notifikaatiot/kuuntele-urakan-ilmoituksia
            tapahtumat
            urakka-id
            (fn [ilmoitus-id]
              (log/debug (format "Vastaanotettiin ilmoitus id:llä %s urakalle id:llä %s." ilmoitus-id urakka-id))
              (send! kanava
                     ;; todo: jos viimeisin haettu id annettu, pitää hakea kaikki ilmoitukset, jotka ovat saapuneet viimeksi haetun jälkeen
                     (aja-virhekasittelyn-kanssa
                       (fn []
                         (let [ilmoitus-id (Integer/parseInt ilmoitus-id)
                               data (hae-ilmoitus db ilmoitus-id)
                               vastaus (tee-vastaus json-skeemat/+ilmoitusten-haku+ data)]
                           ;; todo: merkitse ilmoitus välitetyksi ja lähetä t-loik:n välitystiedot
                           (lokita-vastaus integraatioloki :hae-ilmoitukset vastaus tapahtuma-id)
                           vastaus)))
                     stream)))
          (on-close kanava
                    (fn [_]
                      (log/debug (format "Suljetaan urakan id: %s ilmoitusten kuuntelu." urakka-id))
                      (notifikaatiot/lopeta-ilmoitusten-kuuntelu tapahtumat urakka-id))))))))

(defrecord Ilmoitukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki klusterin-tapahtumat :klusterin-tapahtumat :as this}]
    (julkaise-reitti
      http :hae-ilmoitukset
      (GET "/api/urakat/:id/ilmoitukset" request
        (kaynnista-ilmoitusten-kuuntelu db integraatioloki klusterin-tapahtumat request)))

    (julkaise-reitti
      http :kirjaa-ilmoitustoimenpide
      (PUT "/api/ilmoitukset/:id/" request
        (kasittele-kutsu db integraatioloki :kirjaa-ilmoitustoimenpide request json-skeemat/+ilmoituskuittauksen-kirjaaminen+ json-skeemat/+kirjausvastaus+
                         (fn [parametrit data kayttaja db] (kirjaa-ilmoitustoimenpide db parametrit data kayttaja)))))
    this)
  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :hae-ilmoitukset)
    (poista-palvelut http :kirjaa-ilmoitustoimenpide)
    this))
