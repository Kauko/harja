(ns harja.palvelin.integraatiot.api.tarkastukset
  "Tarkastusten kirjaaminen urakalle"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.palvelin.integraatiot.api.tyokalut.skeemat :as skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.tyokalut.json :as json]
            [harja.kyselyt.konversio :as konversio]
            [harja.kyselyt.tarkastukset :as tarkastukset]
            [harja.kyselyt.havainnot :as havainnot]
            [clojure.java.jdbc :as jdbc]
            [harja.geo :as geo]
            [slingshot.slingshot :refer [try+ throw+]]))


(defn kirjaa-tarkastus [db kayttaja tyyppi {id :id} tarkastus]
  (let [urakka-id (Long/parseLong id)
        ulkoinen-id (-> tarkastus :tunniste :id)]
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
    (log/info "TARKASTUS TULOSSA: " tarkastus "; käyttäjä: " kayttaja)
    (jdbc/with-db-transaction [db db]
      (let [{tarkastus-id :id havainto-id :havainto} (first (tarkastukset/hae-tarkastus-ulkoisella-idlla db ulkoinen-id (:id kayttaja)))
            uusi? (nil? tarkastus-id)]

        (let [aika (json/parsi-aika (:paivamaara tarkastus))
              havainto (merge (:havainto tarkastus)
                              {:aika aika
                               :id havainto-id
                               :urakka urakka-id
                               :tekija :urakoitsija})
              havainto-id (havainnot/luo-tai-paivita-havainto db kayttaja havainto)
              id (tarkastukset/luo-tai-paivita-tarkastus
                  db kayttaja urakka-id 
                  {:id tarkastus-id
                   :ulkoinen-id ulkoinen-id
                   :tyyppi tyyppi
                   :aika aika
                   :tarkastaja (json/henkilo->nimi (:tarkastaja tarkastus))
                   :mittaaja (json/henkilo->nimi (-> tarkastus :mittaus :mittaaja))
                   :tr (json/sijainti->tr (:sijainti tarkastus))
                   :sijainti (json/sijainti->point (:sijainti tarkastus))}
                  havainto-id)]

          (case tyyppi
            :talvihoito (tarkastukset/luo-tai-paivita-talvihoitomittaus
                         db id uusi? (-> tarkastus
                                         :mittaus
                                         (assoc :lumimaara (:lumisuus (:mittaus tarkastus)))))
                                                                        
            :soratie  (tarkastukset/luo-tai-paivita-soratiemittaus
                       db id uusi? (:mittaus tarkastus))
            nil)

          nil)))))



;; Määritellään tarkastustyypit, joiden lisäämiselle tehdään API palvelut
(def tarkastukset
  [{:palvelu :api-lisaa-tiestotarkastus
    :polku "/api/urakat/:id/tarkastus/tiestotarkastus"
    :skeema skeemat/+tiestotarkastuksen-kirjaus+
    :tyyppi :tiesto}
   {:palvelu :api-lisaa-talvihoitotarkastus
    :polku "/api/urakat/:id/tarkastus/talvihoitotarkastus"
    :skeema skeemat/+talvihoitotarkastuksen-kirjaus+
    :tyyppi :talvihoito}
   {:palvelu :api-lisaa-soratietarkastus
    :polku "/api/urakat/:id/tarkastus/soratietarkastus"
    :skeema skeemat/+soratietarkastuksen-kirjaus+
    :tyyppi :soratie}])

(defrecord Tarkastukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db :as this}]
    (doseq [{:keys [palvelu polku skeema tyyppi]} tarkastukset]
      (julkaise-reitti
       http palvelu
       (POST polku request
             (do
               (log/info "REQUEST: " (pr-str request))
               (kasittele-kutsu db palvelu request
                              skeema nil
                              (fn [parametrit data kayttaja]
                                (kirjaa-tarkastus  db kayttaja tyyppi parametrit data)))))))
    
    this)

  (stop [{http :http-palvelin :as this}]
    (apply poista-palvelut http (map :palvelu tarkastukset))
    this))