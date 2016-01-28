(ns harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.sillat
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [clj-time.periodic :refer [periodic-seq]]
            [chime :refer [chime-at]]
            [harja.kyselyt.siltatarkastukset :as s]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.shapefile :as shapefile]))

(defn luo-tai-paivita-silta [db silta]
  (let [tyyppi (:siltatyy silta)
        numero (int (:nro silta))
        nimi (:nimi silta)
        geometria (.toString (:the_geom silta))
        tie (:tie silta)
        alkuosa (:aosa silta)
        alkuetaisyys (:aet silta)
        tunnus (:tunnus silta)
        id (int (:silta_id silta))]
    (if (first (s/hae-silta-tunnuksella db (:tunnus silta)))
      (s/paivita-silta-idlla! db tyyppi numero nimi geometria tie alkuosa alkuetaisyys tunnus id)
      (s/luo-silta! db tyyppi numero nimi geometria tie alkuosa alkuetaisyys tunnus id))))

(defn vie-silta-entry [db silta]
  (if (:the_geom silta)
    (luo-tai-paivita-silta db silta)
    (log/warn "Siltaa ei voida tuoda ilman geometriaa. Virheviesti: " (:loc_error silta))))

(defn vie-sillat-kantaan [db shapefile]
  (if shapefile
    (do
      (log/debug (str "Tuodaan sillat kantaan tiedostosta " shapefile))
      (jdbc/with-db-transaction [transaktio db]
        (doseq [silta (shapefile/tuo shapefile)]
          (vie-silta-entry transaktio silta)))
      (s/paivita-urakoiden-sillat! db)
      (log/debug "Siltojen tuonti kantaan valmis."))
    (log/debug "Siltojen tiedostoa ei löydy konfiguraatiosta. Tuontia ei suoriteta.")))
