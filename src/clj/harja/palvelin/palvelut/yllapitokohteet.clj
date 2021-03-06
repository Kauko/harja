(ns harja.palvelin.palvelut.yllapitokohteet
  "Ylläpitokohteiden palvelut"
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.konversio :as konv]
            [taoensso.timbre :as log]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [clojure.java.jdbc :as jdbc]
            [harja.palvelin.palvelut.yha :as yha]
            [harja.kyselyt.yllapitokohteet :as q]
            [harja.geo :as geo]
            [harja.domain.oikeudet :as oikeudet]))

(def kohdeosa-xf (geo/muunna-pg-tulokset :sijainti))

(defn hae-urakan-yllapitokohteet [db user {:keys [urakka-id sopimus-id]}]
  (oikeudet/lue oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
  (oikeudet/lue oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id)
  (log/debug "Haetaan urakan ylläpitokohteet.")
  (jdbc/with-db-transaction [db db]
    (let [vastaus (into []
                        (comp (map #(konv/string-polusta->keyword % [:paallystysilmoitus-tila]))
                              (map #(konv/string-polusta->keyword % [:paikkausilmoitus-tila]))
                              (map #(assoc % :kohdeosat
                                             (into []
                                                   kohdeosa-xf
                                                   (q/hae-urakan-yllapitokohteen-yllapitokohdeosat
                                                     db urakka-id sopimus-id (:id %))))))
                        (q/hae-urakan-yllapitokohteet db urakka-id sopimus-id))]
      (log/debug "Ylläpitokohteet saatu: " (count vastaus) " kpl")
      vastaus)))

(defn hae-urakan-yllapitokohdeosat [db user {:keys [urakka-id sopimus-id yllapitokohde-id]}]
  (log/debug "Haetaan urakan ylläpitokohdeosat. Urakka-id " urakka-id ", sopimus-id: " sopimus-id ", yllapitokohde-id: " yllapitokohde-id)
  (oikeudet/lue oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
  (oikeudet/lue oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id)
  (let [vastaus (into []
                      kohdeosa-xf
                      (q/hae-urakan-yllapitokohteen-yllapitokohdeosat db urakka-id sopimus-id yllapitokohde-id))]
    (log/debug "Ylläpitokohdeosat saatu: " (pr-str vastaus))
    vastaus))

(defn- hae-urakkatyyppi [db urakka-id]
  (keyword (:tyyppi (first (q/hae-urakan-tyyppi db urakka-id)))))

(defn hae-urakan-aikataulu [db user {:keys [urakka-id sopimus-id]}]
  (assert (and urakka-id sopimus-id) "anna urakka-id ja sopimus-id")
  (oikeudet/lue oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Haetaan urakan aikataulutiedot urakalle: " urakka-id)
  (jdbc/with-db-transaction [db db]
    (case (hae-urakkatyyppi db urakka-id)
      :paallystys
      (q/hae-paallystysurakan-aikataulu db urakka-id sopimus-id)
      :tiemerkinta
      (q/hae-tiemerkintaurakan-aikataulu db urakka-id sopimus-id))))

(defn hae-tiemerkinnan-suorittavat-urakat [db user {:keys [urakka-id]}]
  (oikeudet/lue oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Haetaan tiemerkinnän suorittavat urakat.")
  (q/hae-tiemerkinnan-suorittavat-urakat db))

(defn merkitse-kohde-valmiiksi-tiemerkintaan
  "Merkitsee kohteen valmiiksi tiemerkintään annettuna päivämääränä.
   Palauttaa päivitetyt kohteet aikataulunäkymään"
  [db user
   {:keys [urakka-id sopimus-id tiemerkintapvm kohde-id] :as tiedot}]
  (oikeudet/kirjoita oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Merkitään urakan " urakka-id " kohde " kohde-id " valmiiksi tiemerkintää päivämäärällä " tiemerkintapvm)
  (jdbc/with-db-transaction [db db]
    (q/merkitse-kohde-valmiiksi-tiemerkintaan<!
      db
      tiemerkintapvm
      kohde-id
      urakka-id)
    (hae-urakan-aikataulu db user {:urakka-id urakka-id
                                   :sopimus-id sopimus-id})))

(defn tallenna-yllapitokohteiden-aikataulu [db user {:keys [urakka-id sopimus-id kohteet]}]
  (assert (and urakka-id sopimus-id kohteet) "anna urakka-id ja sopimus-id ja kohteet")
  (oikeudet/kirjoita oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Tallennetaan urakan " urakka-id " ylläpitokohteiden aikataulutiedot: " kohteet)
  ;; Oma päivityskysely kullekin urakalle, sillä päällystysurakoitsija ja tiemerkkari
  ;; eivät saa muokata samoja asioita
  (jdbc/with-db-transaction [db db]
    (case (hae-urakkatyyppi db urakka-id)
      :paallystys
      (doseq [rivi kohteet]
        (q/tallenna-paallystyskohteen-aikataulu!
          db
          (:aikataulu-paallystys-alku rivi)
          (:aikataulu-paallystys-loppu rivi)
          (:aikataulu-kohde-valmis rivi)
          (:id user)
          (:suorittava-tiemerkintaurakka rivi)
          (:id rivi)
          urakka-id))
      :tiemerkinta
      (doseq [rivi kohteet]
        (q/tallenna-tiemerkintakohteen-aikataulu!
          db
          (:aikataulu-tiemerkinta-alku rivi)
          (:aikataulu-tiemerkinta-loppu rivi)
          (:id user)
          (:id rivi)
          urakka-id)))
    (hae-urakan-aikataulu db user {:urakka-id urakka-id
                                   :sopimus-id sopimus-id})))

(defn- luo-uusi-yllapitokohde [db user urakka-id sopimus-id
                               {:keys [kohdenumero nimi
                                       tr-numero tr-alkuosa tr-alkuetaisyys
                                       tr-loppuosa tr-loppuetaisyys tr-ajorata tr-kaista
                                       yllapitoluokka tyyppi
                                       sopimuksen-mukaiset-tyot arvonvahennykset bitumi-indeksi
                                       kaasuindeksi poistettu nykyinen-paallyste
                                       keskimaarainen-vuorokausiliikenne]}]
  (log/debug "Luodaan uusi ylläpitokohde tyyppiä " tyyppi)
  (when-not poistettu
    (q/luo-yllapitokohde<! db
                           urakka-id
                           sopimus-id
                           kohdenumero
                           nimi
                           tr-numero
                           tr-alkuosa
                           tr-alkuetaisyys
                           tr-loppuosa
                           tr-loppuetaisyys
                           tr-ajorata
                           tr-kaista
                           keskimaarainen-vuorokausiliikenne
                           yllapitoluokka,
                           nykyinen-paallyste,
                           sopimuksen-mukaiset-tyot
                           arvonvahennykset
                           bitumi-indeksi
                           kaasuindeksi
                           (when tyyppi
                             (name tyyppi)))))

(defn- paivita-yllapitokohde [db user urakka-id
                              {:keys [id kohdenumero nimi
                                      tr-numero tr-alkuosa tr-alkuetaisyys
                                      tr-loppuosa tr-loppuetaisyys tr-ajorata tr-kaista
                                      yllapitoluokka
                                      sopimuksen-mukaiset-tyot
                                      arvonvahennykset bitumi-indeksi kaasuindeksi
                                      nykyinen-paallyste keskimaarainen-vuorokausiliikenne poistettu]}]
  (if poistettu
    (do (log/debug "Tarkistetaan onko ylläpitokohteella ilmoituksia")
        (let [paallystysilmoitus (q/onko-olemassa-paallystysilmoitus? db id)
              paikkausilmoitus (q/onko-olemassa-paikkausilmioitus? db id)]
          (log/debug "Vastaus päällystysilmoitus: " paallystysilmoitus)
          (log/debug "Vastaus paikkausilmoitus: " paikkausilmoitus)
          (if (and (nil? paallystysilmoitus)
                   (nil? paikkausilmoitus))
            (do
              (log/debug "Ilmoituksia ei löytynyt, poistetaan ylläpitokohde")
              (q/poista-yllapitokohde! db id urakka-id))
            (log/debug "Ei voi poistaa, ylläpitokohteelle on kirjattu ilmoituksia!"))))
    (do (log/debug "Päivitetään ylläpitokohde")
        (q/paivita-yllapitokohde! db
                                  kohdenumero
                                  nimi
                                  tr-numero
                                  tr-alkuosa
                                  tr-alkuetaisyys
                                  tr-loppuosa
                                  tr-loppuetaisyys
                                  tr-ajorata
                                  tr-kaista
                                  keskimaarainen-vuorokausiliikenne
                                  yllapitoluokka,
                                  nykyinen-paallyste,
                                  sopimuksen-mukaiset-tyot
                                  arvonvahennykset
                                  bitumi-indeksi
                                  kaasuindeksi
                                  id
                                  urakka-id))))

(defn tallenna-yllapitokohteet [db user {:keys [urakka-id sopimus-id kohteet]}]
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id)
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (log/debug "Tallennetaan ylläpitokohteet: " (pr-str kohteet))
    (doseq [kohde kohteet]
      (log/debug (str "Käsitellään saapunut ylläpitokohde: " kohde))
      (if (and (:id kohde) (not (neg? (:id kohde))))
        (paivita-yllapitokohde c user urakka-id kohde)
        (luo-uusi-yllapitokohde c user urakka-id sopimus-id kohde)))
    (let [paallystyskohteet (hae-urakan-yllapitokohteet c user {:urakka-id urakka-id
                                                                :sopimus-id sopimus-id})]
      (log/debug "Tallennus suoritettu. Tuoreet ylläpitokohteet: " (pr-str paallystyskohteet))
      paallystyskohteet)))

(defn- luo-uusi-yllapitokohdeosa [db user yllapitokohde-id
                                  {:keys [nimi tr-numero tr-alkuosa tr-alkuetaisyys tr-loppuosa
                                          tr-loppuetaisyys tr-ajorata tr-kaista toimenpide poistettu sijainti]}]
  (log/debug "Luodaan uusi ylläpitokohdeosa, jonka ylläpitokohde-id: " yllapitokohde-id)
  (when-not poistettu
    (q/luo-yllapitokohdeosa<! db
                              yllapitokohde-id
                              nimi
                              tr-numero
                              tr-alkuosa
                              tr-alkuetaisyys
                              tr-loppuosa
                              tr-loppuetaisyys
                              tr-ajorata
                              tr-kaista
                              toimenpide
                              (when sijainti
                                (geo/geometry (geo/clj->pg sijainti))))))

(defn- paivita-yllapitokohdeosa [db user urakka-id
                                 {:keys [id nimi tr-numero tr-alkuosa tr-alkuetaisyys
                                         tr-loppuosa tr-loppuetaisyys tr-ajorata
                                         tr-kaista toimenpide poistettu sijainti]}]

  (if poistettu
    (do (log/debug "Poistetaan ylläpitokohdeosa")
        (q/poista-yllapitokohdeosa! db id urakka-id)
        nil)
    (do (log/debug "Päivitetään ylläpitokohdeosa")
        (q/paivita-yllapitokohdeosa<! db
                                      nimi
                                      tr-numero
                                      tr-alkuosa
                                      tr-alkuetaisyys
                                      tr-loppuosa
                                      tr-loppuetaisyys
                                      tr-ajorata
                                      tr-kaista
                                      toimenpide
                                      (when-not (empty? sijainti)
                                        (geo/geometry (geo/clj->pg sijainti)))
                                      id
                                      urakka-id))))

(defn tallenna-yllapitokohdeosa
  "Tallentaa yksittäisen ylläpitokohdeosan kantaan.
   Tarkistaa, tuleeko kohdeosa päivittää, poistaa vai luoda uutena.
   Palauttaa päivitetyn kohdeosan (tai nil jos kohdeosa poistettiin"
  [db user {:keys [urakka-id sopimus-id yllapitokohde-id osa]}]
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id)
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (log/debug "Tallennetaan ylläpitokohdeosa. Ylläpitokohde-id: " yllapitokohde-id)
    (log/debug (str "Käsitellään saapunut ylläpitokohdeosa"))
    (let [uusi-osa (if (and (:id osa) (not (neg? (:id osa))))
                     (paivita-yllapitokohdeosa c user urakka-id osa)
                     (luo-uusi-yllapitokohdeosa c user yllapitokohde-id osa))]
      (yha/paivita-yllapitourakan-geometriat c urakka-id)
      uusi-osa)))

(defn tallenna-yllapitokohdeosat
  "Tallentaa ylläpitokohdeosat kantaan.
   Tarkistaa, tuleeko kohdeosat päivittää, poistaa vai luoda uutena.
   Palauttaa kohteen päivittyneet kohdeosat."
  [db user {:keys [urakka-id sopimus-id yllapitokohde-id osat]}]
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
  (oikeudet/kirjoita oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id)
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (log/debug "Tallennetaan ylläpitokohdeosat. Ylläpitokohde-id: " yllapitokohde-id)
    (doseq [osa osat]
      (log/debug (str "Käsitellään saapunut ylläpitokohdeosa"))
      (if (and (:id osa) (not (neg? (:id osa))))
        (paivita-yllapitokohdeosa c user urakka-id osa)
        (luo-uusi-yllapitokohdeosa c user yllapitokohde-id osa)))
    (yha/paivita-yllapitourakan-geometriat c urakka-id)
    (let [yllapitokohdeosat (hae-urakan-yllapitokohdeosat c user {:urakka-id urakka-id
                                                                  :sopimus-id sopimus-id
                                                                  :yllapitokohde-id yllapitokohde-id})]
      (log/debug "Tallennus suoritettu. Tuoreet ylläpitokohdeosat: " (pr-str yllapitokohdeosat))
      yllapitokohdeosat)))


(defrecord Yllapitokohteet []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)]
      (julkaise-palvelu http :urakan-yllapitokohteet
                        (fn [user tiedot]
                          (hae-urakan-yllapitokohteet db user tiedot)))
      (julkaise-palvelu http :urakan-yllapitokohdeosat
                        (fn [user tiedot]
                          (hae-urakan-yllapitokohdeosat db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohteet
                        (fn [user tiedot]
                          (tallenna-yllapitokohteet db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohdeosat
                        (fn [user tiedot]
                          (tallenna-yllapitokohdeosat db user tiedot)))
      (julkaise-palvelu http :hae-aikataulut
                        (fn [user tiedot]
                          (hae-urakan-aikataulu db user tiedot)))
      (julkaise-palvelu http :hae-tiemerkinnan-suorittavat-urakat
                        (fn [user tiedot]
                          (hae-tiemerkinnan-suorittavat-urakat db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohteiden-aikataulu
                        (fn [user tiedot]
                          (tallenna-yllapitokohteiden-aikataulu db user tiedot)))
      (julkaise-palvelu http :merkitse-kohde-valmiiksi-tiemerkintaan
                        (fn [user tiedot]
                          (merkitse-kohde-valmiiksi-tiemerkintaan db user tiedot)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-yllapitokohteet
      :urakan-yllapitokohdeosat
      :tallenna-yllapitokohteet
      :tallenna-yllapitokohdeosat
      :hae-aikataulut
      :tallenna-yllapitokohteiden-aikataulu)
    this))
