(ns harja.palvelin.integraatiot.tierekisteri.vastauksenkasittely
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]
            [harja.palvelin.integraatiot.tierekisteri.sanomat.vastaus :as vastaussanoma]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet])

  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn kasittele-vastaus-virheet [virheet virhe-viesti virhe-koodi]
  (throw+ {:type virheet/+ulkoinen-kasittelyvirhe-koodi+
           :virheet [{:viesti (str virhe-viesti " Virheet: " (string/join virheet))
                      :koodi virhe-koodi}]}))

(defn kirjaa-vastaus-varoitukset [virheet varoitus-viesti]
  (log/warn (str varoitus-viesti " Virheet: " (string/join virheet))))

(defn kasittele-vastaus [vastausxml virhe-viesti virhe-koodi varoitus-viesti]
  (let [vastausdata (vastaussanoma/lue vastausxml)
        onnistunut (:onnistunut vastausdata)
        virheet (:virheet vastausdata)]
    (if onnistunut
      (do
        (when (not-empty virheet)
          (kirjaa-vastaus-varoitukset virheet varoitus-viesti))
        vastausdata)
      (kasittele-vastaus-virheet virheet virhe-viesti virhe-koodi))))

