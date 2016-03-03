(ns harja.palvelin.palvelut.kayttajat
  "Palvelu, jolla voi hakea ja tallentaa käyttäjätietoja"
  (:require [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [com.stuartsierra.component :as component]

            [harja.domain.roolit :as roolit]
            [harja.kyselyt.kayttajat :as q]
            [harja.kyselyt.konversio :as konv]
            [harja.palvelin.komponentit.fim :as fim]
            [harja.palvelin.komponentit.tapahtumat :refer [julkaise!]]

            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [clojure.set :as set]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]))

(declare hae-kayttajat
         hae-kayttajan-tiedot
         tallenna-kayttajan-tiedot
         poista-kayttaja
         hae-fim-kayttaja
         hae-organisaatioita)

(defrecord Kayttajat []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-kayttajat (fn [user params]
                                       (apply hae-kayttajat (:db this) user params)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-kayttajan-tiedot
                      (fn [user kayttaja-id]
                        (hae-kayttajan-tiedot (:db this) user kayttaja-id)))
    (julkaise-palvelu (:http-palvelin this)
                      :tallenna-kayttajan-tiedot
                      (fn [user tiedot]
                        (tallenna-kayttajan-tiedot (:db this) (:integraatioloki this) (:fim this) (:klusterin-tapahtumat this) user tiedot)))
    (julkaise-palvelu (:http-palvelin this)
                      :poista-kayttaja
                      (fn [user kayttaja-id]
                        (poista-kayttaja (:db this) user kayttaja-id)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-fim-kayttaja
                      (fn [user tunnus]
                        (hae-fim-kayttaja (:db this) (:fim this) user tunnus)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-organisaatioita
                      (fn [user teksti]
                        (hae-organisaatioita (:db this) user teksti)))
    
                           
    this)
  (stop [this]
    (poista-palvelu (:http-palvelin this) :hae-kayttajat)
    (poista-palvelu (:http-palvelin this) :hae-kayttajan-tiedot)
    (poista-palvelu (:http-palvelin this) :tallenna-kayttajan-tiedot)
    (poista-palvelu (:http-palvelin this) :poista-kayttaja)
    (poista-palvelu (:http-palvelin this) :hae-fim-kayttaja)
    (poista-palvelu (:http-palvelin this) :hae-organisaatioita)
    
    this))


(defn hae-kayttajat
  "Hae käyttäjät tiedot frontille varten"
  [db user hakuehto alku maara]
  
  (let [kayttajat (into []
                        (comp (map konv/organisaatio)
                              (map #(konv/array->set % :roolit)))
                        (q/hae-kayttajat db (:id user) hakuehto alku maara))
        lkm (:lkm (first (q/hae-kayttajat-lkm db (:id user) hakuehto)))]
    
    [lkm kayttajat]))

(defn hae-kayttaja [db kayttaja-id]
  (when-let [k (first (q/hae-kayttaja db kayttaja-id))]
    (konv/array->set (konv/organisaatio k) :roolit)))

(defn hae-kayttaja-kayttajanimella [db kayttajanimi]
  (when-let [k (first (q/hae-kayttaja-kayttajanimella db kayttajanimi))]
    (konv/array->set (konv/organisaatio k) :roolit)))

(defn hae-kayttajan-tiedot
  "Hakee käyttäjän tarkemmat tiedot muokkausnäkymää varten."
  [db user kayttaja-id]
  (merge (hae-kayttaja db kayttaja-id)
         {:roolit (into #{} (:roolit (konv/array->vec (first (q/hae-kayttajan-roolit db kayttaja-id)) :roolit)))
          :urakka-roolit (into []
                               (map konv/alaviiva->rakenne)
                               (q/hae-kayttajan-urakka-roolit db kayttaja-id))}))

(def organisaatio-xf
  (map #(assoc % :tyyppi (keyword (:tyyppi %)))))

(defn hae-fim-kayttaja [db fim user tunnus]
  (if-let [tulos (fim/hae fim tunnus)]
    (if-not (number? tulos) ;; Tulos on virhekoodi
      (let [org (first (into [] organisaatio-xf (q/hae-organisaatio-nimella db (:organisaatio tulos))))
            olemassaoleva (some->> tunnus
                                   (q/hae-kirjautumistiedot db)
                                   first :id
                                   (hae-kayttajan-tiedot db user))]

        (merge
         olemassaoleva
         (if org
           ;; Liitetään olemassaoleva organisaatio käyttäjälle
           (assoc tulos :organisaatio (assoc org :tyyppi (keyword (:tyyppi org))))

           ;; FIMistä tulleella nimellä ei löydy organisaatiota, käyttäjä joutuu valitsemaan sen
           (dissoc tulos :organisaatio))))

      tulos) ;; Palauta statuskoodi
    :ei-loydy))

(defn- tarkista-oikeus-tuoda-fim-kayttaja [user organisaatio-id tapahtuma-id integraatioloki]
  (when (and (not (roolit/roolissa? user roolit/jarjestelmavastuuhenkilo))
             (roolit/roolissa? user roolit/urakoitsijan-paakayttaja)
             ;; Urakoitsijan pk saa antaa vain omaan organisaatioon
             (not (= organisaatio-id (:id (:organisaatio user)))))
    (let [virheviesti (log/warn "Käyttäjä " user " on urakoitsijan pääkäyttäjä, mutta yritti tuoda käyttäjän organisaatioon: " organisaatio-id)]
      (log/warn virheviesti)
      (integraatioloki/kirjaa-epaonnistunut-integraatio integraatioloki virheviesti nil tapahtuma-id nil)
      (throw (RuntimeException. "Käyttöoikeus puuttuu")))))

(defn- tuo-fim-kayttaja [db integraatioloki fim user tunnus organisaatio-id]
  (let [tapahtuma-id (integraatioloki/kirjaa-alkanut-integraatio integraatioloki "api" "tuo-fim-kayttaja" nil
                                                                 (format "Käyttäjä %s yrittää tuoda käyttäjän %s" user tunnus))]
    (tarkista-oikeus-tuoda-fim-kayttaja user organisaatio-id tapahtuma-id integraatioloki)
    (let [k (hae-fim-kayttaja db fim user tunnus)]
      (when-not (= :ei-loydy k)
        (integraatioloki/kirjaa-onnistunut-integraatio integraatioloki (format "Käyttäjä %s toi käyttäjän %s " user tunnus) nil tapahtuma-id nil)
        (log/info "Tuodaan FIM käyttäjä Harjaan: " k)
        (q/luo-kayttaja<! db (:kayttajatunnus k) (:etunimi k) (:sukunimi k)
                          (:sahkoposti k) (:puhelin k) organisaatio-id)))))


(defn tallenna-kayttajan-tiedot
  "Tallentaa käyttäjän uudet käyttäjäoikeustiedot. Palauttaa lopuksi käyttäjän tiedot."
  [db integraatioloki fim tapahtumat user {:keys [kayttaja-id kayttajatunnus organisaatio-id tiedot]}]
  (roolit/vaadi-rooli user #{roolit/jarjestelmavastuuhenkilo
                             roolit/hallintayksikon-vastuuhenkilo
                             roolit/urakoitsijan-paakayttaja})
  (log/info "Tallennetaan käyttäjälle " kayttaja-id " tiedot: " tiedot)
  (let [kayttajan-tiedot
        (jdbc/with-db-transaction [c db]

          (let [luotu-kayttaja (when (nil? kayttaja-id)
                                 (tuo-fim-kayttaja c integraatioloki fim user kayttajatunnus organisaatio-id))
                kayttaja-id (if luotu-kayttaja
                              (:id luotu-kayttaja) 
                              kayttaja-id)
                vanhat-tiedot (hae-kayttajan-tiedot c user kayttaja-id)
                vanhat-roolit (:roolit vanhat-tiedot)
                
                vanhat-urakka-roolit (group-by (comp :id :urakka) (:urakka-roolit vanhat-tiedot))] ;; HAE roolit,  urakka-id => #{"rooli1" "rooli2"}
            ;; FIXME:
            ;; Käydään läpi per rooli tallennukset, tallennetaan vain niitä mitä käyttäjä saa tallentaa
                                        ;(when (oik/roolissa? user roolit/jarjestelmavastuuhenkilo)
            
            ;; Järjestelmävastuuhenkilö saa antaa rooleja: urakanvalvoja
                                        ;  )

            (log/info "VANHAT-ROOLIT " vanhat-roolit)
            (log/info "VANHAT-URAKKA-ROOLIT " vanhat-urakka-roolit)
            
            (doseq [rooli (:roolit tiedot)]
              (if (vanhat-roolit rooli)
                (log/info "Käyttäjällä on rooli " rooli " ja se jatkuu.")
                (do (log/info "Lisätään käyttäjälle rooli: " rooli)
                    (q/lisaa-rooli<! c (:id user) kayttaja-id rooli))))

            (doseq [poistettava-rooli (set/difference vanhat-roolit (:roolit tiedot))]
              (log/info "Poistetaan käyttäjältä rooli " poistettava-rooli)
              (q/poista-rooli! c (:id user) kayttaja-id poistettava-rooli)
              (q/poista-urakka-roolit! c (:id user) kayttaja-id poistettava-rooli)
              )
            
            
            (doseq [{:keys [rooli urakka] :as urakka-rooli} (:urakka-roolit tiedot)]
              
              (if (:poistettu urakka-rooli)
                (do (log/info "Poistetaan käyttäjän " kayttaja-id " rooli " rooli " urakasta " (:id urakka))
                    (q/poista-urakka-rooli! c (:id user) kayttaja-id (:id urakka) rooli))
                
                (let [roolit-urakassa (into #{} (map :rooli (get vanhat-urakka-roolit (:id urakka) [])))]
                  (if (roolit-urakassa rooli)
                    (log/info "Käyttäjällä on rooli " rooli " urakassa " (:id urakka) " ja se jatkuu...")
                    (do
                      (log/info "Lisätään käyttäjän " kayttaja-id " rooli " rooli " urakkaan " (:id urakka))
                      (q/lisaa-urakka-rooli<! c  (:id user) kayttaja-id (:id urakka) rooli)
                      )))))

            ;; Lopuksi palautetaan päivitetty käyttäjä
            (merge (hae-kayttaja c kayttaja-id)
                   (hae-kayttajan-tiedot c user kayttaja-id))))]
    (julkaise! tapahtumat :kayttaja-muokattu (:kayttajanimi kayttajan-tiedot))  
    kayttajan-tiedot))


(defn poista-kayttaja [db user kayttaja-id]
  (jdbc/with-db-transaction [c db]
    (= 1 (q/poista-kayttaja! c (:id user) kayttaja-id))))
            
(defn hae-organisaatioita [db user teksti]
  (into []
        organisaatio-xf
        (q/hae-organisaatioita db (str "%" teksti "%"))))
