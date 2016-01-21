(ns harja.palvelin.komponentit.tapahtumat
  "Klusteritason tapahtumien kuuntelu"
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [thread]]
            [taoensso.timbre :as log])
  (:import [com.mchange.v2.c3p0 C3P0ProxyConnection]
           [org.postgresql PGNotification]))

(defn- aseta-ps-parametrit [ps parametrit]
  (loop [i 1
         [p & parametrit] parametrit]
    (when p
      (.setString ps i p)
      (recur (inc i) parametrit))))

(defn- u [c sql & parametrit]
  (with-open [ps (.prepareStatement c sql)]
    (aseta-ps-parametrit ps parametrit)
    (.executeUpdate ps)))

(defn- q [c sql & parametrit]
  (with-open [ps (.prepareStatement c sql)]
    (aseta-ps-parametrit ps parametrit)
    (.next (.executeQuery ps))))


(def get-notifications (->> (Class/forName "org.postgresql.jdbc4.Jdbc4Connection")
                            .getMethods
                            (filter #(= (.getName %) "getNotifications"))
                            first))

(defn- kanava-nimi [kw]
  (-> kw
      name
      (.replace "-" "_")
      (.replace "!" "")
      (.replace "?" "")
      (.replace "<" "")
      (.replace ">" "")))


(defprotocol Kuuntele
  (kuuntele! [this kanava callback]))

(defprotocol Julkaise
  (julkaise! [this kanava payload]))

(defprotocol Kuuroudu
  (kuuroudu! [this kanava]))

(defrecord Tapahtumat [connection kuuntelijat ajossa]
  component/Lifecycle
  (start [this]
    (reset! connection (.getConnection (:datasource (:db this))))
    (reset! kuuntelijat {})
    (reset! ajossa true)
    (thread (loop []
              (when @ajossa
                (with-open [stmt (.createStatement @connection)
                            rs (.executeQuery stmt "SELECT 1")]
                  (doseq [^PGNotification notification (seq (.rawConnectionOperation @connection
                                                                                     get-notifications
                                                                                     C3P0ProxyConnection/RAW_CONNECTION
                                                                                     (into-array Object [])))]
                    (log/info "TAPAHTUI" (.getName notification) " => " (.getParameter notification)) 
                    (doseq [kasittelija (get @kuuntelijat (.getName notification))]
                      ;; Käsittelijä ei sitten saa blockata
                      (kasittelija (.getParameter notification)))))
                
                (Thread/sleep 150)
                (recur))))
    this)

  (stop [this]
    (reset! ajossa false)
    (run! #(u @connection (str "UNLISTEN " % ";"))
          (map first @kuuntelijat))
    (.close @connection)
    this)

  Kuuroudu
  (kuuroudu! [this kanava]
    (swap! kuuntelijat #(dissoc % kanava))
    (u @connection (str "UNLISTEN " kanava ";")))
  
  Kuuntele
  (kuuntele! [_ kanava callback]
    (let [kanava (kanava-nimi kanava)]
      (when-not (get @kuuntelijat kanava)
        ;; LISTEN
        (u @connection (str "LISTEN " kanava ";")))
      (swap! kuuntelijat update-in [kanava] conj callback)))

  Julkaise
  (julkaise! [_ kanava payload]
    (let [kanava (kanava-nimi kanava)]
      (q @connection "SELECT pg_notify(?, ?)" kanava (str payload))))
  )

(defn luo-tapahtumat []
  (->Tapahtumat (atom nil) (atom nil) (atom false)))


  
    
