(ns harja.palvelin.palvelut.liitteet
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [cognitect.transit :as t]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [harja.kyselyt.liitteet :as q]
            [taoensso.timbre :as log])
  (:import (java.io InputStream ByteArrayInputStream ByteArrayOutputStream)
           (org.postgresql PGConnection)
           (org.postgresql.largeobject LargeObject LargeObjectManager
                                       BlobInputStream BlobOutputStream)
           (com.mchange.v2.c3p0 C3P0ProxyConnection)
           (net.coobird.thumbnailator Thumbnailator)))

(def get-large-object-api (->> (Class/forName "org.postgresql.PGConnection")
                               .getMethods
                               (filter #(= (.getName %) "getLargeObjectAPI"))
                               first))

(defn- large-object-api [c]
  (.rawConnectionOperation c
                           get-large-object-api
                           C3P0ProxyConnection/RAW_CONNECTION
                           (into-array Object [])))

                         
(defn tallenna-lob [db ^InputStream in]
  (with-open [c (doto (.getConnection (:datasource db))
                  (.setAutoCommit false))]
    (let [lom (large-object-api c)
          oid (.create lom LargeObjectManager/READWRITE)]
      (try
        (with-open [obj (.open lom oid LargeObjectManager/WRITE)
                    out (.getOutputStream obj)]
          (io/copy in out)
          oid)
        (finally
          (.commit c))))))

(defn lue-lob [db oid]
  (with-open  [c (doto (.getConnection (:datasource db))
                   (.setAutoCommit false))]
    (let [lom (large-object-api c)]
      (with-open [obj (.open lom oid LargeObjectManager/READ)
                  in (.getInputStream obj)
                  out (ByteArrayOutputStream.)]
        (io/copy in out)
        (.toByteArray out)))))
    
(defn muodosta-pikkukuva
  "Ottaa ison kuvan (tiedosto) ja palauttaa pikkukuvan byte[] muodossa. Pikkukuva on aina png."
  [isokuva]
  (with-open [out (ByteArrayOutputStream.)]
    (Thumbnailator/createThumbnail (io/input-stream isokuva) out "png" 64 64)
    (.toByteArray out)))
    
    

(defn tallenna-liite [db req]
  (println "LIITEHÄN SIELTÄ TULI: " (:params req))

  (if-let [{:keys [filename content-type tempfile size]} (get (:params req) "liite")]
    (let [pikkukuva (muodosta-pikkukuva tempfile)
          oid (tallenna-lob db (io/input-stream tempfile))
          liite (q/tallenna-liite<! db filename content-type size oid pikkukuva (:id (:kayttja req)))]
      (log/debug "Tallennettu liite " filename " (" size " tavua)")
      (-> liite
          (dissoc :liite_oid :pikkukuva :luoja :luotu)))))


(defn lataa-liite [db req]
  (let [id (Integer/parseInt (get (:params req) "id"))
        ;; FIXME: tarkista pääsy liitteeseen! Ehkä pitää antaa linkki mistä liitettä haetaan (esim. kommentti tms)
        {:keys [liite_oid tyyppi koko]} (first (q/hae-liite-lataukseen db id))]
    {:status 200
     :headers {"Content-Type" tyyppi
               "Content-Length" koko}
     :body (java.io.ByteArrayInputStream. (lue-lob db liite_oid))}))

(defn lataa-pikkukuva [db req]
  (let [id (Integer/parseInt (get (:params req) "id"))
        ;; FIXME: tarkista pääsy liitteeseen! Ehkä pitää antaa linkki mistä liitettä haetaan (esim. kommentti tms)
        {:keys [pikkukuva]} (first (q/hae-pikkukuva-lataukseen db id))]
    (log/info "Ladataan pikkukuva " id)
    (if pikkukuva
      {:status 200
       :headers {"Content-Type" "image/png"
                 "Content-Length" (count pikkukuva)}
       :body (java.io.ByteArrayInputStream. pikkukuva)}
      {:status 404
       :body "Annetulle liittelle ei pikkukuvaa."})))
    
  
(defrecord Liitteet []
  component/Lifecycle
  (start [{:keys [http-palvelin] :as this}]
    (julkaise-palvelu http-palvelin :tallenna-liite
                      (wrap-multipart-params (fn [req] (tallenna-liite (:db this) req)))
                      {:ring-kasittelija? true})
    (julkaise-palvelu http-palvelin :lataa-liite
                      (wrap-params (fn [req]
                                     (lataa-liite (:db this) req)))
                      {:ring-kasittelija? true})
    (julkaise-palvelu http-palvelin :lataa-pikkukuva
                      (wrap-params (fn [req]
                                     (log/info "PIKKU KUVA")
                                     (lataa-pikkukuva (:db this) req)))
                      {:ring-kasittelija? true})
    this)

  (stop [{:keys [http-palvelin] :as this}]
    (poista-palvelut http-palvelin :tallenna-liite :lataa-liite :lataa-pikkukuva)))

  
                        
  
