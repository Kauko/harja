(ns harja.palvelin.raportointi.raportit.ilmoitus
  "Materiaaliraportti"
  (:require [taoensso.timbre :as log]
            [harja.domain.roolit :as roolit]
            [harja.kyselyt.ilmoitukset :as ilmoitukset-q]
            [harja.kyselyt.materiaalit :as materiaalit-q]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [harja.kyselyt.konversio :as konv]
            [harja.pvm :as pvm]))


(defn muodosta-ilmoitusraportti-urakalle [db user {:keys [urakka-id alkupvm loppupvm]}]
  (log/debug "Haetaan urakan ilmoitukset raporttia varten: " urakka-id alkupvm loppupvm)
  (roolit/vaadi-rooli user "tilaajan kayttaja")
  (let [parametrit [db
                            urakka-id
                            (konv/sql-timestamp alkupvm)
                            (konv/sql-timestamp loppupvm)]
        ilmoitukset (into []
                                      (apply ilmoitukset-q/hae-ilmoitukset parametrit))
        _ (log/debug "ilmoitukset ilmoitusrapsaa varten: " ilmoitukset)

        lopullinen-tulos (mapv
                           (fn [ilmoitus]
                             (if (nil? (:kokonaismaara ilmoitus))
                               (assoc ilmoitus :kokonaismaara 0)
                               ilmoitus))
                           ilmoitukset)]
    lopullinen-tulos))

(defn muodosta-ilmoitusraportti-hallintayksikolle [db user {:keys [hallintayksikko-id alkupvm loppupvm]}]
  (log/debug "Haetaan hallintayksikon toteutuneet materiaalit raporttia varten: " hallintayksikko-id alkupvm loppupvm)
  (roolit/vaadi-rooli user "tilaajan kayttaja")
  (let [toteutuneet-materiaalit (into []
                                      (materiaalit-q/hae-hallintayksikon-toteutuneet-materiaalit-raportille db
                                                                                                            (konv/sql-timestamp alkupvm)
                                                                                                            (konv/sql-timestamp loppupvm)
                                                                                                            hallintayksikko-id))]
    toteutuneet-materiaalit))

(defn muodosta-ilmoitusraportti-koko-maalle [db user {:keys [alkupvm loppupvm]}]
  (log/debug "Haetaan koko maan toteutuneet materiaalit raporttia varten: " alkupvm loppupvm)
  (roolit/vaadi-rooli user "tilaajan kayttaja")
  (let [toteutuneet-materiaalit (into []
                                      (materiaalit-q/hae-koko-maan-toteutuneet-materiaalit-raportille db
                                                                                                      (konv/sql-timestamp alkupvm)
                                                                                                      (konv/sql-timestamp loppupvm)))]
    toteutuneet-materiaalit))



(defn suorita [db user {:keys [urakka-id hk-alkupvm hk-loppupvm
                               hallintayksikko-id alkupvm loppupvm] :as parametrit}]
  (let [[konteksti toteumat]
        (cond
          (and urakka-id hk-alkupvm hk-loppupvm)
          [:urakka (muodosta-ilmoitusraportti-urakalle db user {:urakka-id urakka-id
                                                                  :alkupvm hk-alkupvm
                                                                  :loppupvm hk-loppupvm})]

          (and hallintayksikko-id alkupvm loppupvm)
          [:hallintayksikko (muodosta-ilmoitusraportti-hallintayksikolle db user {:hallintayksikko-id hallintayksikko-id
                                                                                    :alkupvm alkupvm
                                                                                    :loppupvm loppupvm})]
          
          (and alkupvm loppupvm)
          [:koko-maa (muodosta-ilmoitusraportti-koko-maalle db user {:alkupvm alkupvm :loppupvm loppupvm})]

          :default
          ;; FIXME Pitäisikö tässä heittää jotain, tänne ei pitäisi päästä, jos parametrit ovat oikein?
          nil)
        otsikko (str (case konteksti
                       :urakka (log/debug "Haetaan urakka id:llä " (pr-str urakka-id) (pr-str (first (urakat-q/hae-urakka db urakka-id))))
                       :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio db hallintayksikko-id)))
                       :koko-maa "KOKO MAA")
                     ", Ilmoitusraportti "
                     (pvm/pvm (or hk-alkupvm alkupvm)) " \u2010 " (pvm/pvm (or hk-loppupvm loppupvm)))
        materiaalit (distinct (map :materiaali_nimi toteumat))
        toteumat-urakan-mukaan (group-by :urakka_nimi toteumat)]
    (println "TOTEUMAT: " toteumat)
    [:raportti {:nimi otsikko}
     [:taulukko {:otsikko otsikko
                 :viimeinen-rivi-yhteenveto? true}
      (into []
            (concat 
             [{:otsikko "Urakka"}]
             (map (fn [mat]
                    {:otsikko mat}) materiaalit)))
      (into
       []
       (concat
        ;; Tehdään rivi jokaiselle urakalle, jossa sen yhteenlasketut toteumat
        (for [[urakka toteumat] toteumat-urakan-mukaan]
          (into []
                (concat [urakka]
                        (let [toteumat-materiaalin-mukaan (group-by :materiaali_nimi toteumat)]
                          (for [m materiaalit]
                            (reduce + (map :kokonaismaara (toteumat-materiaalin-mukaan m))))))))

        ;; Tehdään yhteensä rivi, jossa kaikki toteumat lasketaan yhteen materiaalin perusteella
        [(concat ["Yhteensä"]
                 (let [toteumat-materiaalin-mukaan (group-by :materiaali_nimi toteumat)]
                   (for [m materiaalit]
                     (reduce + (map :kokonaismaara (toteumat-materiaalin-mukaan m))))))]))]]))

    
