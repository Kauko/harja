(ns harja.palvelin.palvelut.lampotilat
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.lampotilat :as q]
            [harja.kyselyt.pohjavesialueet :as pohjavesialueet-q]
            [harja.kyselyt.urakat :as urakat]
            [harja.domain.roolit :as roolit]
            [harja.kyselyt.konversio :as konv]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [harja.geo :as geo]
            [harja.palvelin.integraatiot.ilmatieteenlaitos :as ilmatieteenlaitos]
            [harja.pvm :as pvm]))


(defn hae-lampotilat-ilmatieteenlaitokselta [db user url vuosi]
  (log/debug "hae-lampotilat-ilmatieteenlaitokselta, url " url " vuosi " vuosi)
  (roolit/vaadi-rooli user roolit/jarjestelmavastuuhenkilo)
  (assert (and url vuosi) "Annettava url ja vuosi kun haetaan ilmatieteenlaitokselta lämpötiloja.")
  ;; Ilmatieteenlaitos käyttää :urakka-id -kentässään Harjan :alueurakkanro -kenttää, siksi muunnoksia alla
  (let [hoidon-urakoiden-lampotilat (into {}
                                          (map (juxt :urakka-id #(dissoc % :urakka-id)))
                                          (ilmatieteenlaitos/hae-talvikausi url vuosi))
        hoidon-urakka-ja-alueurakkanro-avaimet (urakat/hae-aktiivisten-hoitourakoiden-alueurakkanumerot db vuosi)]
    (into {}

          (comp
            (map (fn [urakka]
                   (merge urakka (get hoidon-urakoiden-lampotilat (:alueurakkanro urakka)))))
            (map (juxt :id identity)))
          hoidon-urakka-ja-alueurakkanro-avaimet)))

(defn hae-teiden-hoitourakoiden-lampotilat [db user hoitokausi]
  (log/debug "hae-teiden-hoitourakoiden-lampotilat hoitokaudella: " hoitokausi)
  (roolit/vaadi-rooli user roolit/jarjestelmavastuuhenkilo)
  (let [alkupvm (first hoitokausi)
        loppupvm (second hoitokausi)]
    (into {}
          (map (juxt :urakka identity)
               (q/hae-teiden-hoitourakoiden-lampotilat db alkupvm loppupvm)))))

(defn tallenna-teiden-hoitourakoiden-lampotilat [db user {:keys [hoitokausi lampotilat]}]
  (log/debug "tallenna-teiden-hoitourakoiden-lampotilat, hoitokausi " hoitokausi ", lämpötilat: " lampotilat)
  (roolit/vaadi-rooli user roolit/jarjestelmavastuuhenkilo)
  (jdbc/with-db-transaction [db db]
                            (doseq [lt lampotilat]
                              (let [id (:lampotilaid lt)
                                    parametrit [(:urakka lt) (:alkupvm lt) (:loppupvm lt)
                                                (:keskilampotila lt) (:pitkakeskilampotila lt)]]
                                (if id
                                    (apply q/paivita-lampotila<! db (concat parametrit [id]))
                                    (apply q/uusi-lampotila<! db parametrit))))
                            (hae-teiden-hoitourakoiden-lampotilat db user hoitokausi)))

(defn hae-urakan-suolasakot-ja-lampotilat
  [db user urakka-id]
  (log/debug "hae-urakan-suolasakot-ja-lampotilat")
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  {:suolasakot (into []
                      (map #(konv/decimal->double % :maara))
                     (q/hae-urakan-suolasakot db urakka-id))
   :lampotilat (into []
                     (comp
                       (map #(konv/decimal->double % :keskilampotila))
                       (map #(konv/decimal->double % :pitkakeskilampotila)))
                     (q/hae-urakan-lampotilat db urakka-id))
   :pohjavesialueet (into []
                          (geo/muunna-pg-tulokset :alue)
                          (pohjavesialueet-q/hae-urakan-pohjavesialueet db urakka-id))
   :pohjavesialue-talvisuola (q/hae-urakan-pohjavesialue-talvisuolarajat db urakka-id)})

(defn luo-suolasakko
  [params]
  (log/debug "luo suolasakko" params)
  (let [uusi (apply q/luo-suolasakko<! params)
        ]
    (:id uusi)))

(defn paivita-suolasakko
  [tiedot id]
  (log/debug "päivitä suolasakko" tiedot)
  (let [params (into [] (concat tiedot [id]))]
    (apply q/paivita-suolasakko! params))
  id)



(defn tallenna-suolasakko
  [db user urakka hoitokauden-alkuvuosi tiedot]
  (log/debug "tallenna suolasakko" tiedot)
  (let [suolasakon-id (:id (first (q/hae-suolasakko-id db urakka hoitokauden-alkuvuosi)))]
    (if suolasakon-id
      (do 
        (q/paivita-suolasakko! db (:maara tiedot) (:maksukuukausi tiedot)
                               (:indeksi tiedot) (:id user)
                               (:talvisuolaraja tiedot) suolasakon-id)
          suolasakon-id)
      
      (:id (q/luo-suolasakko<! db (:maara tiedot) hoitokauden-alkuvuosi (:maksukuukausi tiedot)
                               (:indeksi tiedot) urakka (:id user) (:talvisuolaraja tiedot))))))


(defn tallenna-pohjavesialue-talvisuola
  "Päivittää pohjavesialueen talvisuolarajan, tai luo uuden jos rajaa ei ole"
  [db user urakka hoitokauden-alkuvuosi pohjavesialue talvisuolaraja]
  (when (zero? (q/paivita-pohjavesialue-talvisuola! db talvisuolaraja urakka hoitokauden-alkuvuosi pohjavesialue))
    (q/tallenna-pohjavesialue-talvisuola<! db talvisuolaraja urakka hoitokauden-alkuvuosi pohjavesialue)))

;; TIEDOT:
;; {:suolasakko {:talvisuolaraja 444, :maksukuukausi 7, :indeksi "MAKU 2010", :maara 30}
;;  :pohjavesialue-talvisuola [{:pohjavesialue "43091941", :urakka 4, :hoitokauden_alkuvuosi 2015, :talvisuolaraja 4242}]
;;  :muokattu true
;;  :hoitokauden_alkuvuosi 2015}
(defn tallenna-suolasakko-ja-pohjavesialueet
  [db user {:keys [hoitokaudet urakka suolasakko pohjavesialue-talvisuola] :as tiedot}]
  (log/debug"tallenna-suolasakko-ja-pohjavesialueet tiedot: " (pr-str tiedot))
  (roolit/vaadi-rooli-urakassa user
                               #{roolit/urakanvalvoja}
                               urakka)
  (jdbc/with-db-transaction
    [db db]
    (doseq [hk hoitokaudet]
      (let [hoitokauden-alkuvuosi (pvm/vuosi (first hk))
            suolasakon-id (tallenna-suolasakko db user urakka
                                               hoitokauden-alkuvuosi suolasakko)]
        (doseq [{:keys [pohjavesialue talvisuolaraja]} pohjavesialue-talvisuola]
          (tallenna-pohjavesialue-talvisuola db user urakka
                                             hoitokauden-alkuvuosi pohjavesialue talvisuolaraja))))

    (hae-urakan-suolasakot-ja-lampotilat db user urakka)))


(defn aseta-suolasakon-kaytto [db user {:keys [urakka-id kaytossa?]}]
  (log/debug "Käytössä? " kaytossa?)
  (roolit/vaadi-urakanvalvoja user urakka-id)
  (jdbc/with-db-transaction
    [db db]
    (q/aseta-suolasakon-kaytto! db kaytossa? (:id user) urakka-id)
    (:kaytossa (first (q/onko-suolasakko-kaytossa? db urakka-id)))))

(defrecord Lampotilat [ilmatieteenlaitos-url]
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)]
      (julkaise-palvelu http :hae-lampotilat-ilmatieteenlaitokselta
                        (fn [user {:keys [vuosi]}]
                          (hae-lampotilat-ilmatieteenlaitokselta (:db this) user ilmatieteenlaitos-url vuosi)))
      (julkaise-palvelu http :hae-teiden-hoitourakoiden-lampotilat
                        (fn [user {:keys [hoitokausi]}]
                          (hae-teiden-hoitourakoiden-lampotilat (:db this) user hoitokausi)))
      (julkaise-palvelu http :tallenna-teiden-hoitourakoiden-lampotilat
                        (fn [user tiedot]
                          (tallenna-teiden-hoitourakoiden-lampotilat (:db this) user tiedot)))
      (julkaise-palvelu http :hae-urakan-suolasakot-ja-lampotilat
                        (fn [user urakka-id]
                          (hae-urakan-suolasakot-ja-lampotilat (:db this) user urakka-id)))
      (julkaise-palvelu http :tallenna-suolasakko-ja-pohjavesialueet
                        (fn [user tiedot]
                          (tallenna-suolasakko-ja-pohjavesialueet (:db this) user tiedot)))
      (julkaise-palvelu http :aseta-suolasakon-kaytto
                        (fn [user tiedot]
                          (aseta-suolasakon-kaytto (:db this) user tiedot)))
      this))

  (stop [this]
    (poista-palvelut (:http-palvelin this)
                     :hae-lampotilat-ilmatieteenlaitokselta
                     :hae-teiden-hoitourakoiden-lampotilat
                     :tallenna-teiden-hoitourakoiden-lampotilat
                     :hae-urakan-suolasakot-ja-lampotilat
                     :tallenna-suolasakko-ja-pohjavesialueet
                     :aseta-suolasakon-kaytto)
    this))
