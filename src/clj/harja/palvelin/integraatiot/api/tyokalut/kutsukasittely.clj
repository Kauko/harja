(ns harja.palvelin.integraatiot.api.tyokalut.kutsukasittely
  "API:n kutsujen käsittely funktiot"
  (:require [cheshire.core :as cheshire]
            [taoensso.timbre :as log]
            [clojure.walk :as walk]
            [clojure.core.async :refer :all]
            [org.httpkit.server :refer [with-channel on-close send!]]
            [harja.tyokalut.json-validointi :as json]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet]
            [harja.palvelin.palvelut.kayttajat :as q]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.tyokalut.avaimet :as avaimet])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import [java.sql SQLException]
           (java.io StringWriter PrintWriter)))

(defn tee-lokiviesti [suunta body viesti]
  {:suunta suunta
   :sisaltotyyppi "application/json"
   :siirtotyyppi "HTTP"
   :sisalto body
   :otsikko (str (walk/keywordize-keys (:headers viesti)))
   :parametrit (str (:params viesti))})

(defn poista-liitteet-logituksesta
  "Etsii avainpolun, joka päättyy avaimeen liitteet. Käsittelee sen alta löytyvät liitteet tyhjentäen niiden
   sisällön"
  [body]
  (try+
    (let [body-clojure-mappina (cheshire/decode body true)
          avainpolut (avaimet/keys-in body-clojure-mappina)
          avainpolku-liitteet (first (filter
                                       (fn [avainpolku]
                                         (= (last avainpolku) :liitteet))
                                       avainpolut))
          liitteet-ilman-sisaltoja (when avainpolku-liitteet
                                     (mapv (fn [liite]
                                             (assoc-in liite [:liite :sisalto] "< Liitettä ei logiteta >"))
                                           (get-in body-clojure-mappina avainpolku-liitteet)))
          body-ilman-liittteiden-sisaltoa (when liitteet-ilman-sisaltoja
                                            (assoc-in body-clojure-mappina avainpolku-liitteet liitteet-ilman-sisaltoja))]
      (if avainpolku-liitteet
        (cheshire/encode body-ilman-liittteiden-sisaltoa)
        body))
    (catch Exception e
      (log/debug "Ei voida poistaa liitteitä bodystä: " (.getMessage e))
      body)))

(defn lokita-kutsu [integraatioloki resurssi request body]
  (log/debug "Vastaanotetiin kutsu resurssiin:" resurssi ".")
  (log/debug "Kutsu:" request)
  (log/debug "Parametrit: " (:params request))
  (log/debug "Headerit: " (:headers request))
  (log/debug "Sisältö:" (poista-liitteet-logituksesta body))

  (integraatioloki/kirjaa-alkanut-integraatio integraatioloki "api" (name resurssi) nil (tee-lokiviesti "sisään" body request)))

(defn lokita-vastaus [integraatioloki resurssi response tapahtuma-id]
  (log/debug "Lähetetään vastaus resurssiin:" resurssi "kutsuun.")
  (log/debug "Vastaus:" response)
  (log/debug "Headerit: " (:headers response))
  (log/debug "Sisältö:" (:body response))

  (if (= 200 (:status response))
    (do
      (log/debug "Kutsu resurssiin:" resurssi "onnistui. Palautetaan vastaus:" response)
      (integraatioloki/kirjaa-onnistunut-integraatio
        integraatioloki
        (tee-lokiviesti "ulos" (:body response) response) nil tapahtuma-id nil))
    (do
      (log/error "Kutsu resurssiin:" resurssi "epäonnistui. Palautetaan vastaus:" response)
      (integraatioloki/kirjaa-epaonnistunut-integraatio
        integraatioloki
        (tee-lokiviesti "ulos" (:body response) response) nil tapahtuma-id nil))))

(defn tee-virhevastaus
  "Luo virhevastauksen annetulla statuksella ja asettaa vastauksen bodyksi JSON muodossa virheet."
  [status virheet]
  (let [body (cheshire/encode
               {:virheet
                (mapv (fn [virhe]
                        {:virhe
                         {:koodi (:koodi virhe)
                          :viesti (:viesti virhe)}})
                      virheet)})]
    {:status status
     :headers {"Content-Type" "application/json"}
     :body body}))

(defn tee-sisainen-kasittelyvirhevastaus
  [virheet]
  (tee-virhevastaus 500 virheet))

(defn tee-viallinen-kutsu-virhevastaus
  [virheet]
  (tee-virhevastaus 400 virheet))

(defn tee-vastaus
  "Luo JSON-vastauksen joko annetulla statuksella tai oletuksena statuksella 200 (ok). Payload on Clojure dataa, joka
  muunnetaan JSON-dataksi. Jokainen payload validoidaan annetulla skeemalla. Jos payload ei ole validi,
  palautetaan status 500 (sisäinen käsittelyvirhe)."
  ([skeema payload] (tee-vastaus 200 skeema payload))
  ([status skeema payload]
   (if payload
     (let [json (cheshire/encode payload)]
       (if skeema
         (do
           (json/validoi skeema json)
           {:status status
            :headers {"Content-Type" "application/json"}
            :body json})
         {:status status
          :headers {"Content-Type" "application/json"}}))
     (if skeema
       (throw+ {:type virheet/+sisainen-kasittelyvirhe+
                :virheet [{:koodi virheet/+tyhja-vastaus+
                           :viesti "Tyhja vastaus vaikka skeema annettu"}]})
       {:status status}))))

(defn kasittele-invalidi-json [virheet resurssi]
  (log/error (format "Resurssin: %s kutsun JSON on invalidi: %s" resurssi virheet))
  (tee-viallinen-kutsu-virhevastaus virheet))

(defn kasittele-viallinen-kutsu [virheet resurssi]
  (log/error (format "Resurssin: %s kutsu on viallinen: %s " resurssi virheet))
  (tee-viallinen-kutsu-virhevastaus virheet))

(defn kasittele-puutteelliset-parametrit [virheet resurssi]
  (log/error (format "Resurssin: %s kutsussa puutteelliset parametrit: %s " resurssi virheet))
  (tee-viallinen-kutsu-virhevastaus virheet))

(defn kasittele-sisainen-kasittelyvirhe [virheet resurssi]
  (log/error (format "Resurssin: %s kutsussa tapahtui sisäinen käsittelyvirhe: %s" resurssi virheet))
  (tee-sisainen-kasittelyvirhevastaus virheet))

(defn lue-kutsu
  "Lukee kutsun bodyssä tulevan datan, mikäli kyseessä on POST-, DELETE- tai PUT-kutsu. Muille kutsuille palauttaa arvon nil.
  Validoi annetun kutsun JSON-datan ja mikäli data on validia, palauttaa datan Clojure dataksi muunnettuna.
  Jos annettu data ei ole validia, palautetaan nil."
  [skeema request body]
  (log/debug "Luetaan kutsua")
  (when (or (= :post (:request-method request))
            (= :put (:request-method request))
            (= :delete (:request-method request)))
    (json/validoi skeema body)
    (cheshire/decode body true)))

(defn hae-kayttaja [db kayttajanimi]
  (let [kayttaja (q/hae-kayttaja-kayttajanimella db kayttajanimi)]
    (if kayttaja
      kayttaja
      (do
        (log/error "Tuntematon käyttäjätunnus: " kayttajanimi)
        (throw+ {:type virheet/+viallinen-kutsu+
                 :virheet [{:koodi virheet/+tuntematon-kayttaja-koodi+
                            :viesti (str "Tuntematon käyttäjätunnus: " kayttajanimi)}]})))))

(defn aja-virhekasittelyn-kanssa [resurssi ajo]
  (try+
    (ajo)
    (catch [:type virheet/+invalidi-json+] {:keys [virheet]}
      (kasittele-invalidi-json virheet resurssi))
    (catch [:type virheet/+viallinen-kutsu+] {:keys [virheet]}
      (kasittele-viallinen-kutsu virheet resurssi))
    (catch [:type virheet/+puutteelliset-parametrit+] {:keys [virheet]}
      (kasittele-puutteelliset-parametrit virheet resurssi))
    (catch [:type virheet/+sisainen-kasittelyvirhe+] {:keys [virheet]}
      (kasittele-sisainen-kasittelyvirhe virheet resurssi))
    (catch [:type virheet/+ulkoinen-kasittelyvirhe-koodi+] {:keys [virheet]}
      (kasittele-sisainen-kasittelyvirhe virheet resurssi))
    (catch [:type virheet/+virheellinen-liite+] {:keys [virheet]}
      (kasittele-sisainen-kasittelyvirhe virheet resurssi))
    (catch #(get % :virheet) poikkeus
      (kasittele-sisainen-kasittelyvirhe (:virheet poikkeus) resurssi))
    ;; Odottamattomat poikkeustilanteet (virhetietoja ei julkaista):
    (catch SQLException e
      (log/error e (format "Resurssin kutsun: %s yhteydessä tapahtui SQL-poikkeus: " resurssi))
      (let [w (StringWriter.)]
        (loop [ex (.getNextException e)]
          (when (not (nil? ex))
            (.printStackTrace ex (PrintWriter. w))
            (recur (.getNextException ex))))
        (log/error "Sisemmät virheet: " (.toString w)))
      (kasittele-sisainen-kasittelyvirhe
        [{:koodi virheet/+sisainen-kasittelyvirhe-koodi+
          :viesti "Sisäinen käsittelyvirhe"}]
        resurssi))
    (catch Exception e
      (log/error e (format "Resurssin kutsun: %s yhteydessä tapahtui poikkeus: " resurssi))
      (kasittele-sisainen-kasittelyvirhe
        [{:koodi virheet/+sisainen-kasittelyvirhe-koodi+
          :viesti "Sisäinen käsittelyvirhe"}]
        resurssi))
    (catch Object e
      (log/error (:throwable &throw-context) (format "Resurssin kutsun: %s yhteydessä tapahtui poikkeus: " e))
      (kasittele-sisainen-kasittelyvirhe
        [{:koodi virheet/+sisainen-kasittelyvirhe-koodi+
          :viesti "Sisäinen käsittelyvirhe"}]
        resurssi))))

(defn kasittele-kutsu
  "Käsittelee synkronisesti annetun kutsun ja palauttaa käsittelyn tuloksen mukaisen vastauksen. Vastaanotettu ja
  lähetetty data on JSON-formaatissa, joka muunnetaan Clojure dataksi ja toisin päin. Sekä sisääntuleva, että ulos
  tuleva data validoidaan käyttäen annettuja JSON-skeemoja.

  Käsittely voi palauttaa seuraavat HTTP-statukset: 200 = ok, 400 = kutsun data on viallista & 500 = sisäinen
  käsittelyvirhe."

  [db integraatioloki resurssi request kutsun-skeema vastauksen-skeema kasittele-kutsu-fn]

  (let [body (if (:body request)
               (slurp (:body request))
               nil)
        tapahtuma-id (when integraatioloki
                       (lokita-kutsu integraatioloki resurssi request body))
        vastaus (aja-virhekasittelyn-kanssa
                  resurssi
                  #(let
                    [parametrit (:params request)
                     kayttaja (hae-kayttaja db (get (:headers request) "oam_remote_user"))
                     kutsun-data (lue-kutsu kutsun-skeema request body)
                     vastauksen-data (kasittele-kutsu-fn parametrit kutsun-data kayttaja db)]
                    (tee-vastaus vastauksen-skeema vastauksen-data)))]
    (when integraatioloki
      (lokita-vastaus integraatioloki resurssi vastaus tapahtuma-id))
    vastaus))

(defn kasittele-kutsu-async
  "Käsittelee asynkronisesti annetun kutsun ja palauttaa käsittelyn tuloksen mukaisen vastauksen. Vastaanotettu ja
  lähetetty data  on JSON-formaatissa, joka muunnetaan Clojure dataksi ja toisin päin. Sekä sisääntuleva, että ulos
  tuleva data validoidaan käyttäen annettuja JSON-skeemoja.

  Käsittely voi palauttaa seuraavat HTTP-statukset: 200 = ok, 400 = kutsun data on viallista & 500 = sisäinen
  käsittelyvirhe."

  [db integraatioloki resurssi request kutsun-skeema vastauksen-skeema kasittele-kutsu-fn]

  (with-channel request channel
    (go
      (let [vastaus
            (<! (thread (kasittele-kutsu db integraatioloki resurssi request kutsun-skeema vastauksen-skeema kasittele-kutsu-fn)))]
        (send! channel vastaus)))))