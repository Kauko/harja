(ns harja.palvelin.komponentit.pdf-vienti
  "PDF-vientin komponentti, tarjoaa reitin, jonka kautta PDF:n voi ladata selaimelle.
  Lisäksi tänne voi muut komponentit rekisteröidä PDF:n luontimekanismin.
  Tämä komponentti ei ota kantaa PDF:n sisältöön, se vain generoi Hiccup muotoisesta FOPista PDF:n."
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin
             :refer
             [julkaise-palvelu poista-palvelu]]
            [harja.transit :as t]
            [hiccup.core :refer [html]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.codec :as codec]
            [taoensso.timbre :as log]
            [ring.util.io :refer [piped-input-stream]])
  (:import javax.xml.transform.sax.SAXResult
           javax.xml.transform.stream.StreamSource
           javax.xml.transform.TransformerFactory
           [org.apache.fop.apps FopConfParser MimeConstants]
           java.io.ByteArrayInputStream))

(defprotocol PdfKasittelijat
  (rekisteroi-pdf-kasittelija! [this nimi kasittely-fn]
    "Julkaisee PDF käsittelijäfunktion annetulla keyword nimellä. Funktio ottaa parametriksi käyttäjän sekä HTTP request parametrit mäppeinä ja palauttaa PDF:n tiedot hiccup muotoisena FOPina.")
  (poista-pdf-kasittelija! [this nimi]))

(declare muodosta-pdf)

(defrecord PdfVienti [pdf-kasittelijat fop-factory]
  component/Lifecycle
  (start [{http :http-palvelin :as this}]
    (log/info "PDF-vientikomponentti aloitettu")
    (julkaise-palvelu http
                      :pdf (wrap-params (fn [req]
                                          (muodosta-pdf fop-factory @pdf-kasittelijat req)))
                      {:ring-kasittelija? true})
    this)

  (stop [{http :http-palvelin :as this}]
    (log/info "PDF-vientikomponentti lopetettu")
    (poista-palvelu http :pdf))

  PdfKasittelijat
  (rekisteroi-pdf-kasittelija! [_ nimi kasittely-fn]
    (log/info "Rekisteröidään PDF käsittelijä: " nimi)
    (swap! pdf-kasittelijat assoc nimi kasittely-fn))

  (poista-pdf-kasittelija! [_ nimi]
    (log/info "Poistetaan PDF käsittelijä: " nimi)
    (swap! pdf-kasittelijat dissoc nimi)))


(defn- luo-fop-factory []
  (let [conf (io/resource "fop/fop.xconf")
        conf-parser (FopConfParser. (io/input-stream conf)
                                    (.toURI conf)
                                    (org.apache.fop.apps.io.ResourceResolverFactory/createDefaultResourceResolver))]
    (-> conf-parser
        .getFopFactoryBuilder
        .build)))

(defn luo-pdf-vienti []
  (->PdfVienti (atom {}) (luo-fop-factory)))

(defn- hiccup->pdf [fop-factory hiccup out]
  (let [fop (.newFop fop-factory MimeConstants/MIME_PDF out)
        xform (.newTransformer (TransformerFactory/newInstance))
        src (StreamSource. (java.io.StringReader. (html hiccup)))
        res (SAXResult. (.getDefaultHandler fop))]
    (.transform xform src res)))


;; Jostain syystä wrap-params ei lue meidän POSTattua formia
;; Luetaan se ja otetaan "parametrit" niminen muuttuja ja
;; muunnetaan se transit+json muodosta Clojure dataksi
(defn- lue-body-parametrit [body]
  (-> body
      .bytes
      (String.)
      codec/form-decode
      (get "parametrit")
      .getBytes
      (ByteArrayInputStream.)
      t/lue-transit))

(defn- muodosta-pdf [fop-factory kasittelijat {kayttaja :kayttaja body :body
                                               query-params :params
                                               :as req}]
  (let [tyyppi (keyword (get query-params "_"))
        params (lue-body-parametrit body)
        kasittelija (get kasittelijat tyyppi)]
    (log/debug "PARAMS: " params)
    (if-not kasittelija
      {:status 404
       :body (str "Tuntematon PDF: " tyyppi)}
      (try
        (log/debug "Luodaan " tyyppi " PDF käyttäjälle " (:kayttajanimi kayttaja)
                   " parametreilla " params)
        {:status 200
         :headers {"Content-Type" "application/pdf"} ;; content-disposition!
         :body (piped-input-stream
                (fn [out]
                  (hiccup->pdf fop-factory (kasittelija kayttaja params) out)))}
        (catch Exception e
          (log/warn e "Virhe PDF-muodostuksessa: " tyyppi ", käyttäjä: " kayttaja)
          {:status 500
           :body "Virhe PDF-muodostuksessa"})))))
