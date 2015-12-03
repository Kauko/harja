(ns harja.palvelin.palvelut.laadunseuranta
  "Laadunseuranta: Tarkastukset, Laatupoikkeamat ja Sanktiot"

  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelut poista-palvelut]]

            [harja.kyselyt.laatupoikkeamat :as laatupoikkeamat]
            [harja.kyselyt.kommentit :as kommentit]
            [harja.kyselyt.liitteet :as liitteet]
            [harja.kyselyt.sanktiot :as sanktiot]
            [harja.kyselyt.tarkastukset :as tarkastukset]
            [harja.kyselyt.kayttajat :as kayttajat-q]
            [harja.kyselyt.urakat :as urakat-q]

            [harja.kyselyt.konversio :as konv]
            [harja.domain.roolit :as roolit]
            [harja.geo :as geo]

            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]))

(def laatupoikkeama-xf
  (comp
   (geo/muunna-pg-tulokset :sijainti)
   (map konv/alaviiva->rakenne)
   (map #(assoc % :selvitys-pyydetty (:selvityspyydetty %)))
   (map #(dissoc % :selvityspyydetty))
   (map #(assoc % :tekija (keyword (:tekija %))))
   (map #(update-in % [:paatos :paatos]
                    (fn [p]
                      (when p (keyword p)))))
   (map #(update-in % [:paatos :kasittelytapa]
                    (fn [k]
                      (when k (keyword k)))))
   (map #(if (nil? (:kasittelyaika (:paatos %)))
           (dissoc % :paatos)
           %))))

(def tarkastus-xf
  (comp
    (geo/muunna-pg-tulokset :sijainti)
    (map konv/alaviiva->rakenne)
    (map #(konv/string->keyword % :tyyppi [:laatupoikkeama :tekija]))
    (map #(-> %1
              (assoc-in [:laatupoikkeama :selvitys-pyydetty] (get-in %1 [:laatupoikkeama :selvitys-pyydetty]))
              (update-in [:laatupoikkeama] dissoc :selvityspyydetty)
              (update-in [:laatupoikkeama] (fn [h]
                                             (if (nil? (:selvitys-pyydetty h))
                                               (dissoc h :selvitys-pyydetty)
                                               h)))))

    (map #(dissoc % :sopimus))                              ;; tarvitaanko sopimusta?
    (map (fn [tarkastus]
           (condp = (:tyyppi tarkastus)
             :talvihoito (dissoc tarkastus :soratiemittaus)
             :soratie (dissoc tarkastus :talvihoitomittaus)
             :tiesto (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
             :laatu (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
             :pistokoe (dissoc tarkastus :soratiemittaus :talvihoitomittaus))))))

(defn hae-urakan-laatupoikkeamat [db user {:keys [listaus urakka-id alku loppu]}]
  (when urakka-id (roolit/vaadi-lukuoikeus-urakkaan user urakka-id))
  (jdbc/with-db-transaction [db db]
    (let [listaus (or listaus :tilannekuva)
          urakka-idt (if-not (nil? urakka-id)
                       (if (vector? urakka-id) urakka-id [urakka-id])

                       (if (get (:roolit user) "jarjestelmavastuuhenkilo")
                         (mapv :id (urakat-q/hae-kaikki-urakat-aikavalilla db (konv/sql-date alku) (konv/sql-date loppu)))
                         (mapv :urakka_id (kayttajat-q/hae-kayttajan-urakka-roolit db (:id user)))))
          _ (log/debug "Haetaan laatupoikkeamaja urakoista " (pr-str urakka-idt))
          tulos (apply (comp vec flatten merge)
                       (for [urakka-id urakka-idt]
                         (into []
                               laatupoikkeama-xf
                               
                               (if (= :omat listaus)
                                 (apply laatupoikkeamat/hae-omat-laatupoikkeamat
                                        (conj [db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu)] (:id user)))
                                 (apply (case listaus
                                          :kaikki laatupoikkeamat/hae-kaikki-laatupoikkeamat
                                          :selvitys laatupoikkeamat/hae-selvitysta-odottavat-laatupoikkeamat
                                          :kasitellyt laatupoikkeamat/hae-kasitellyt-laatupoikkeamat
                                          :tilannekuva laatupoikkeamat/hae-laatupoikkeamat-tilannekuvaan)
                                        [db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu)])))))]
      (log/debug "Löydettiin laatupoikkeamat: " (pr-str (mapv :id tulos)))
      tulos)))


(defn hae-laatupoikkeaman-tiedot
  "Hakee yhden laatupoikkeaman kaiken tiedon muokkausnäkymää varten: laatupoikkeaman perustiedot, kommentit ja liitteet, päätös ja sanktiot.
   Ottaa urakka-id:n ja laatupoikkeama-id:n. Urakka id:tä käytetään oikeustarkistukseen, laatupoikkeaman tulee olla annetun urakan
   toimenpiteeseen kytketty."
  [db user urakka-id laatupoikkeama-id]
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [laatupoikkeama (first (into []
                              laatupoikkeama-xf
                              (laatupoikkeamat/hae-laatupoikkeaman-tiedot db urakka-id laatupoikkeama-id)))]
    (when laatupoikkeama
      (assoc laatupoikkeama
        :kommentit (into []
                         (comp (map konv/alaviiva->rakenne)
                               (map #(assoc % :tekija (name (:tekija %))))
                               (map (fn [{:keys [liite] :as kommentti}]
                                      (if (:id liite)
                                        kommentti
                                        (dissoc kommentti :liite)))))
                         (laatupoikkeamat/hae-laatupoikkeaman-kommentit db laatupoikkeama-id))
        :sanktiot (into []
                        (comp (map #(konv/array->set % :tyyppi_sanktiolaji keyword))
                              (map konv/alaviiva->rakenne)
                              (map #(konv/string->keyword % :laji))
                              (map #(assoc %
                                     :sakko? (not (nil? (:summa %)))
                                     :summa (some-> % :summa double))))
                        (sanktiot/hae-laatupoikkeaman-sanktiot db laatupoikkeama-id))
        :liitteet (into [] (laatupoikkeamat/hae-laatupoikkeaman-liitteet db laatupoikkeama-id))))))

(defn hae-urakan-sanktiot
  "Hakee urakan sanktiot perintäpvm:n mukaan"
  [db user {:keys [urakka-id alku loppu]}]

  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (log/debug "Hae sanktiot (" urakka-id alku loppu ")")
  (into []
        (comp (geo/muunna-pg-tulokset :laatupoikkeama_sijainti)
              (map #(konv/string->keyword % :laatupoikkeama_paatos_kasittelytapa))
              (map konv/alaviiva->rakenne)
              (map #(konv/decimal->double % :summa))
              (map #(assoc % :laji (keyword (:laji %)))))
        (sanktiot/hae-urakan-sanktiot db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu))))

(defn tallenna-laatupoikkeaman-sanktio
  [db user {:keys [id perintapvm laji tyyppi summa indeksi suorasanktio] :as sanktio} laatupoikkeama urakka]
  (log/debug "TALLENNA sanktio: " sanktio ", urakka: " urakka ", tyyppi: " tyyppi ", laatupoikkeamaon " laatupoikkeama)
  (if (or (nil? id) (neg? id))
    (let [uusi-sanktio (sanktiot/luo-sanktio<!
                         db (konv/sql-timestamp perintapvm)
                         (name laji) (:id tyyppi)
                         urakka
                         summa indeksi laatupoikkeama (or suorasanktio false))]
      (sanktiot/merkitse-maksuera-likaiseksi! db (:id uusi-sanktio))
      (:id uusi-sanktio))

    (do
      (sanktiot/paivita-sanktio!
        db (konv/sql-timestamp perintapvm)
        (name laji) (:id tyyppi)
        urakka
        summa indeksi laatupoikkeama (or suorasanktio false)
        id)
      (sanktiot/merkitse-maksuera-likaiseksi! db id)
      id)))

(defn tallenna-laatupoikkeama [db user {:keys [urakka] :as laatupoikkeama}]
  (log/info "Tuli laatupoikkeama: " laatupoikkeama)
  (roolit/vaadi-rooli-urakassa user roolit/laadunseuranta-kirjaus urakka)
  (jdbc/with-db-transaction [c db]

    (let [osapuoli (roolit/osapuoli user urakka)
          laatupoikkeama (assoc laatupoikkeama
                     ;; Jos osapuoli ei ole urakoitsija, voidaan asettaa selvitys-pyydetty päälle
                     :selvitys-pyydetty (and (not= :urakoitsija osapuoli)
                                             (:selvitys-pyydetty laatupoikkeama))

                     ;; Jos urakoitsija kommentoi, asetetaan selvitys annettu
                     :selvitys-annettu (and (:uusi-kommentti laatupoikkeama)
                                            (= :urakoitsija osapuoli)))
          id (laatupoikkeamat/luo-tai-paivita-laatupoikkeama c user laatupoikkeama)]
      ;; Luodaan uudet kommentit
      (when-let [uusi-kommentti (:uusi-kommentti laatupoikkeama)]
        (log/info "UUSI KOMMENTTI: " uusi-kommentti)
        (let [liite (some->> uusi-kommentti
                             :liite
                             :id
                             (liitteet/hae-urakan-liite-id c urakka)
                             first
                             :id)
              kommentti (kommentit/luo-kommentti<! c
                                                   (name (:tekija laatupoikkeama))
                                                   (:kommentti uusi-kommentti)
                                                   liite
                                                   (:id user))]
          ;; Liitä kommentti laatupoikkeamaon
          (laatupoikkeamat/liita-kommentti<! c id (:id kommentti))))

      ;; Liitä liite laatupoikkeamaon
      (when-let [uusi-liite (:uusi-liite laatupoikkeama)]
        (log/info "UUSI LIITE: " uusi-liite)
        (laatupoikkeamat/liita-liite<! c id (:id uusi-liite)))


      (when (:paatos (:paatos laatupoikkeama))
        ;; Urakanvalvoja voi kirjata päätöksen
        (roolit/vaadi-rooli-urakassa user roolit/urakanvalvoja urakka)
        (log/info "Kirjataan päätös havainnolle: " id ", päätös: " (:paatos laatupoikkeama))
        (let [{:keys [kasittelyaika paatos perustelu kasittelytapa muukasittelytapa]} (:paatos laatupoikkeama)]
          (laatupoikkeamat/kirjaa-laatupoikkeaman-paatos! c
                                              (konv/sql-timestamp kasittelyaika)
                                              (name paatos) perustelu
                                              (name kasittelytapa) muukasittelytapa
                                              (:id user)
                                              id))
        (when (= :sanktio (:paatos (:paatos laatupoikkeama)))
          (doseq [sanktio (:sanktiot laatupoikkeama)]
            (tallenna-laatupoikkeaman-sanktio c user sanktio id urakka))))

      (hae-laatupoikkeaman-tiedot c user urakka id))))

(defn hae-sanktiotyypit
  "Palauttaa kaikki sanktiotyypit, hyvin harvoin muuttuvaa dataa."
  [db user]
  (into []
        ;; Muunnetaan sanktiolajit arraysta, keyword setiksi
        (map #(konv/array->set % :laji keyword))
        (sanktiot/hae-sanktiotyypit db)))


(defn hae-urakan-tarkastukset
  "Palauttaa urakan tarkastukset annetulle aikavälille."
  [db user {:keys [urakka-id alkupvm loppupvm tienumero tyyppi]}]
  (when urakka-id (roolit/vaadi-lukuoikeus-urakkaan user urakka-id))

  (jdbc/with-db-transaction [db db]
    (let [urakka-idt (if-not (nil? urakka-id)
                       (if (vector? urakka-id) urakka-id [urakka-id])

                       (if (get (:roolit user) "jarjestelmavastuuhenkilo")
                         (mapv :id (urakat-q/hae-kaikki-urakat-aikavalilla db (konv/sql-date alkupvm) (konv/sql-date loppupvm)))
                         (mapv :urakka_id (kayttajat-q/hae-kayttajan-urakka-roolit db (:id user)))))
          _ (log/debug "Haetaan tarkastuksia urakoista " (pr-str urakka-idt))
          tulos (apply (comp vec flatten merge)
                       (for [urakka-id urakka-idt]
                         (into []
                               tarkastus-xf
                               (tarkastukset/hae-urakan-tarkastukset db urakka-id
                                                                     (konv/sql-timestamp alkupvm)
                                                                     (konv/sql-timestamp loppupvm)
                                                                     (if tienumero true false) tienumero
                                                                     (if tyyppi true false) (and tyyppi (name tyyppi))))))]
      (log/debug "Löydettiin tarkastukset: " (pr-str (mapv :id tulos)))
      tulos)))

(defn hae-tarkastus [db user urakka-id tarkastus-id]
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [tarkastus (first (into [] tarkastus-xf (tarkastukset/hae-tarkastus db urakka-id tarkastus-id)))]
    (assoc tarkastus
      :laatupoikkeama (hae-laatupoikkeaman-tiedot db user urakka-id (:id (:laatupoikkeama tarkastus))))))

(defn tallenna-tarkastus [db user urakka-id tarkastus]
  (roolit/vaadi-rooli-urakassa user roolit/laadunseuranta-kirjaus urakka-id)
  (try
    (jdbc/with-db-transaction [c db]
      (let [laatupoikkeama (merge (:laatupoikkeama tarkastus)
                                  {:aika   (:aika tarkastus)
                                   :urakka urakka-id})
            
            uusi? (nil? (:id tarkastus))
            id (tarkastukset/luo-tai-paivita-tarkastus c user urakka-id tarkastus)]

        (condp = (:tyyppi tarkastus)
          :talvihoito (tarkastukset/luo-tai-paivita-talvihoitomittaus c id uusi? (:talvihoitomittaus tarkastus))
          :soratie (tarkastukset/luo-tai-paivita-soratiemittaus c id uusi? (:soratiemittaus tarkastus))
          nil)


        (log/info "SAATIINPA urakalle " urakka-id " tarkastus: " tarkastus)
        (hae-tarkastus c user urakka-id id)))
    (catch Exception e
      (log/info e "Tarkastuksen tallennuksessa poikkeus!"))))

(defn tallenna-suorasanktio [db user sanktio laatupoikkeama urakka]
  ;; Roolien tarkastukset on kopioitu laatupoikkeaman kirjaamisesta,
  ;; riittäisi varmaan vain roolit/urakanvalvoja?
  (log/info "Tallenna suorasanktio " (:id sanktio) " laatupoikkeamaon " (:id laatupoikkeama) ", urakassa " urakka)
  (roolit/vaadi-rooli-urakassa user roolit/laadunseuranta-kirjaus urakka)
  (roolit/vaadi-rooli-urakassa user roolit/urakanvalvoja urakka)

  (jdbc/with-db-transaction [c db]
    (let [;; FIXME: Suorasanktiolle pyydetty/annettu flagit?
          #_osapuoli #_(roolit/osapuoli user urakka)
          #_laatupoikkeama #_(assoc laatupoikkeama
                         :selvitys-pyydetty (and (not= :urakoitsija osapuoli)
                                                 (:selvitys-pyydetty laatupoikkeama))
                         :selvitys-annettu (and (:uusi-kommentti laatupoikkeama)
                                                (= :urakoitsija osapuoli)))
          id (laatupoikkeamat/luo-tai-paivita-laatupoikkeama c user (assoc laatupoikkeama :tekija "tilaaja"))]

      (let [{:keys [kasittelyaika paatos perustelu kasittelytapa muukasittelytapa]} (:paatos laatupoikkeama)]
        (laatupoikkeamat/kirjaa-laatupoikkeaman-paatos! c
                                            (konv/sql-timestamp kasittelyaika)
                                            (name paatos) perustelu
                                            (name kasittelytapa) muukasittelytapa
                                            (:id user)
                                            id))

      ;; Frontilla oletetaan että palvelu palauttaa tallennetun sanktion id:n
      ;; Jos tämä muuttuu, pitää frontillekin tehdä muutokset.
      (tallenna-laatupoikkeaman-sanktio c user sanktio id urakka))))

(defrecord Laadunseuranta []
  component/Lifecycle
  (start [{:keys [http-palvelin db] :as this}]

    (julkaise-palvelut
      http-palvelin

      :hae-urakan-laatupoikkeamat
      (fn [user tiedot]
        (hae-urakan-laatupoikkeamat db user tiedot))

      :tallenna-laatupoikkeama
      (fn [user laatupoikkeama]
        (tallenna-laatupoikkeama db user laatupoikkeama))

      :tallenna-suorasanktio
      (fn [user tiedot]
        (tallenna-suorasanktio db user (:sanktio tiedot) (:laatupoikkeama tiedot) (get-in tiedot [:laatupoikkeama :urakka])))

      :hae-laatupoikkeaman-tiedot
      (fn [user {:keys [urakka-id laatupoikkeama-id]}]
        (hae-laatupoikkeaman-tiedot db user urakka-id laatupoikkeama-id))

      :hae-urakan-sanktiot
      (fn [user tiedot]
        (hae-urakan-sanktiot db user tiedot))

      :hae-sanktiotyypit
      (fn [user]
        (hae-sanktiotyypit db user))

      :hae-urakan-tarkastukset
      (fn [user tiedot]
        (hae-urakan-tarkastukset db user tiedot))

      :tallenna-tarkastus
      (fn [user {:keys [urakka-id tarkastus]}]
        (tallenna-tarkastus db user urakka-id tarkastus))


      :hae-tarkastus
      (fn [user {:keys [urakka-id tarkastus-id]}]
        (hae-tarkastus db user urakka-id tarkastus-id)))
    this)

  (stop [{:keys [http-palvelin] :as this}]
    (poista-palvelut http-palvelin
                     :hae-urakan-laatupoikkeamat
                     :tallenna-laatupoikkeama
                     :hae-laatupoikkeaman-tiedot
                     :hae-urakan-sanktiot
                     :hae-sanktiotyypit
                     :hae-urakan-tarkastukset
                     :tallenna-tarkastus
                     :tallenna-suorasanktio
                     :hae-tarkastus)
    this))
            
