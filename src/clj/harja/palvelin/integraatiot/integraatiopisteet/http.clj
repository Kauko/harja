(ns harja.palvelin.integraatiot.integraatiopisteet.http
  (:require [taoensso.timbre :as log]
            [org.httpkit.client :as http]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn rakenna-http-kutsu [metodi otsikot parametrit kayttajatunnus salasana kutsudata]
  (let [kutsu {}]
    (-> kutsu
        (cond-> (not-empty otsikot) (assoc :headers otsikot))
        (cond-> (not-empty parametrit) (assoc :query-params parametrit))
        (cond-> (and (not-empty kayttajatunnus)) (assoc :basic-auth [kayttajatunnus salasana]))
        (cond-> (or (= metodi "post") (= metodi "put")) (assoc :body kutsudata)))))

(defn tee-http-kutsu [integraatioloki jarjestelma integraatio tapahtuma-id url metodi otsikot parametrit kayttajatunnus salasana kutsudata]
  (try
    (let [kutsu (rakenna-http-kutsu metodi otsikot parametrit kayttajatunnus salasana kutsudata)]
      (case metodi
        "post" @(http/post url kutsu)
        "get" @(http/get url kutsu)
        "put" @(http/put url kutsu)
        "delete" @(http/delete url kutsu)
        "head" @(http/head url kutsu)
        (throw+
          {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
           :virheet [{:koodi :tuntematon-http-metodi :viesti (str "Tuntematon HTTP metodi:" metodi)}]})))
    (catch Exception e
      (log/error e (format "HTTP-kutsukäsittelyssä tapahtui poikkeus.  (järjestelmä: %s, integraatio: %s, URL: %s)" jarjestelma integraatio url))
      (integraatioloki/kirjaa-epaonnistunut-integraatio integraatioloki nil (str " Tapahtui poikkeus: " e) tapahtuma-id nil)
      (throw+
        {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
         :virheet [{:koodi :poikkeus :viesti (str "Poikkeus :" (.getMessage e))}]}))))

(defn laheta-kutsu
  ([integraatioloki integraatio jarjestelma url metodi otsikot parametrit kutsudata kasittele-vastaus]
   (laheta-kutsu integraatioloki integraatio jarjestelma url metodi otsikot parametrit nil nil kutsudata kasittele-vastaus))
  ([integraatioloki integraatio jarjestelma url metodi otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus]
   (log/debug (format "Lähetetään HTTP %s -kutsu integraatiolle: %s, järjestelmään: %s, osoite: %s, metodi: %s, data: %s, otsikkot: %s, parametrit: %s"
                      metodi integraatio jarjestelma url metodi kutsudata otsikot parametrit))

   (let [tapahtuma-id (integraatioloki/kirjaa-alkanut-integraatio integraatioloki jarjestelma integraatio nil nil)
         sisaltotyyppi (get otsikot " Content-Type ")]

     (integraatioloki/kirjaa-rest-viesti integraatioloki tapahtuma-id "ulos" url sisaltotyyppi kutsudata otsikot nil)
     (let [{:keys [status body error headers]} (tee-http-kutsu integraatioloki jarjestelma integraatio tapahtuma-id url metodi otsikot parametrit kayttajatunnus salasana kutsudata)
           lokiviesti (integraatioloki/tee-rest-lokiviesti "sisään" url sisaltotyyppi body headers nil)]
       (log/debug (format " Palvelu palautti: tila: %s , otsikot: %s , data: %s" status headers body))

       (if (or error (not (= 200 status)))
         (do
           (log/error (format "Kutsu palveluun: %s epäonnistui. Virhe: %s " url error))
           (integraatioloki/kirjaa-epaonnistunut-integraatio integraatioloki lokiviesti (str " Virhe: " error) tapahtuma-id nil)
           (throw+ {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
                    :virheet [{:koodi :ulkoinen-jarjestelma-palautti-virheen :viesti (str "Virhe :" error)}]}))
         (do
           (let [vastausdata (kasittele-vastaus body headers)]
             (log/debug (format "Kutsu palveluun: %s onnistui." url))
             (integraatioloki/kirjaa-onnistunut-integraatio integraatioloki lokiviesti nil tapahtuma-id nil)
             vastausdata)))))))

(defn laheta-get-kutsu
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "get" otsikot parametrit kayttajatunnus salasana nil kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "get" otsikot parametrit nil kasittele-vastaus-fn)))

(defn laheta-post-kutsu
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "post" otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kutsudata kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "post" otsikot parametrit kutsudata kasittele-vastaus-fn)))

(defn laheta-head-kutsu
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "head" otsikot parametrit kayttajatunnus salasana nil kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "head" otsikot parametrit nil kasittele-vastaus-fn)))
