(ns harja.palvelin.raportointi.raportit.tiestotarkastus
  (:require [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [harja.kyselyt.tarkastukset :as tarkastukset-q]
            [harja.kyselyt.toimenpideinstanssit :refer [hae-urakan-toimenpideinstanssi]]
            [harja.fmt :as fmt]
            [harja.pvm :as pvm]
            [harja.palvelin.raportointi.raportit.yleinen :refer [raportin-otsikko]]
            [taoensso.timbre :as log]
            [harja.domain.roolit :as roolit]))

(defn hae-tarkastukset-urakalle [db {:keys [urakka-id alkupvm loppupvm tienumero]}]
  (tarkastukset-q/hae-urakan-tarkastukset db urakka-id alkupvm loppupvm (not (nil? tienumero)) tienumero true "tiestotarkastus"))

(defn hae-tarkastukset-hallintayksikolle [db {:keys [hallintayksikko-id alkupvm loppupvm tienumero]}]
  ; TODO Puuttuu
  []
  #_(q/hae-yksikkohintaiset-tyot-kuukausittain-hallintayksikolle db
                                                                 hallintayksikko-id alkupvm loppupvm
                                                                 (if toimenpide-id true false) toimenpide-id))

(defn hae-tarkastukset-koko-maalle [db {:keys [alkupvm loppupvm tienumero]}]
  ; TODO Puuttuu
  []
  #_(q/hae-yksikkohintaiset-tyot-kuukausittain-koko-maalle db
                                                           alkupvm loppupvm
                                                           (if toimenpide-id true false) toimenpide-id))

(defn hae-tiestotarkastukset [db {:keys [konteksti urakka-id hallintayksikko-id alkupvm loppupvm tienumero]}]
  (case konteksti
    :urakka
    (hae-tarkastukset-urakalle db
                               {:urakka-id urakka-id
                                :alkupvm   alkupvm
                                :tienumero tienumero})
    :hallintayksikko
    (hae-tarkastukset-hallintayksikolle db
                                        {:hallintayksikko-id hallintayksikko-id
                                         :alkupvm            alkupvm
                                         :loppupvm           loppupvm
                                         :tienumero          tienumero})
    :koko-maa
    (hae-tarkastukset-koko-maalle db
                                  {:alkupvm   alkupvm
                                   :loppupvm  loppupvm
                                   :tienumero tienumero})))

(defn suorita [db user {:keys [urakka-id hallintayksikko-id alkupvm loppupvm tienumero] :as parametrit}]
  (roolit/vaadi-rooli user "tilaajan kayttaja") ; FIXME Selvitä oikeudet
  (let [konteksti (cond urakka-id :urakka
                        hallintayksikko-id :hallintayksikko
                        :default :koko-maa)
        naytettavat-rivit (hae-tiestotarkastukset db {:konteksti konteksti
                                                      :urakka-id urakka-id
                                                      :alkupvm   alkupvm
                                                      :loppupvm  loppupvm
                                                      :tienumero nil})
        raportin-nimi "Tiestötarkastusraportti"
        otsikko (raportin-otsikko
                  (case konteksti
                    :urakka (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                    :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio db hallintayksikko-id)))
                    :koko-maa "KOKO MAA")
                  raportin-nimi alkupvm loppupvm)]
    [:raportti {:orientaatio :landscape
                :nimi        raportin-nimi}
     [:taulukko {:otsikko otsikko
                 :tyhja   (if (empty? naytettavat-rivit) "Ei raportoitavia tarkastuksia.")}
      (flatten (keep identity [{:leveys "10%" :otsikko "Päivämäärä"}
                               {:leveys "5%" :otsikko "Klo"}
                               {:leveys "5%" :otsikko "Tie"}
                               {:leveys "5%" :otsikko "Aosa"}
                               {:leveys "5%" :otsikko "Aet"}
                               {:leveys "5%" :otsikko "Losa"}
                               {:leveys "10%" :otsikko "Tarkastaja"}
                               {:leveys "20%" :otsikko "Havainnot"}
                               {:leveys "10%" :otsikko "Kuvanumerot"}]))
      (mapv (fn [rivi]
              [(:aika rivi)
               (:aika rivi)
               (:tr_numero rivi)
               (:tr_alkuosa rivi)
               (:tr_alkuetaisyys rivi)
               (:tr_loppuetaisyys rivi)
               (:tr_loppuosa rivi)
               (:tarkastaja rivi)
               (:havainnot rivi)
               (:kuvanumerot rivi)])
            naytettavat-rivit)]]))