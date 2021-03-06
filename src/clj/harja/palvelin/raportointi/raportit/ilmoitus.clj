(ns harja.palvelin.raportointi.raportit.ilmoitus
  "Ilmoitusraportti"
  (:require [taoensso.timbre :as log]
            [harja.palvelin.raportointi.raportit.yleinen :refer
             [raportin-otsikko vuosi-ja-kk vuosi-ja-kk-fmt kuukaudet
              pylvaat-kuukausittain ei-osumia-aikavalilla-teksti]]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.domain.ilmoitukset :refer [+ilmoitustyypit+ ilmoitustyypin-lyhenne-ja-nimi +ilmoitustilat+]]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [harja.palvelin.palvelut.ilmoitukset :as ilmoituspalvelu]
            [harja.pvm :as pvm]
            [harja.kyselyt.ilmoitukset :as ilmoitukset]))

(defn hae-ilmoitukset-raportille
  [db user hallintayksikko-id urakka-id urakoitsija urakkatyyppi
   +ilmoitustilat+ +ilmoitustyypit+ [alkupvm loppupvm] hakuehto selite]
  (ilmoituspalvelu/hae-ilmoitukset db user
                                   {:hallintayksikko hallintayksikko-id
                                    :urakka urakka-id
                                    :urakoitsija urakoitsija
                                    :urakkatyyppi urakkatyyppi
                                    :tilat +ilmoitustilat+
                                    :tyypit +ilmoitustyypit+
                                    :kuittaustyypit #{:kuittaamaton :vastaanotto :aloitus
                                                      :lopetus :muutos :vastaus}
                                    :aikavali [alkupvm loppupvm]
                                    :hakuehto hakuehto
                                    :selite selite}))

(defn kasittele-summat [summat]
  (let [nolla-jos-nil (fn [numero] (if (nil? numero) 0 numero))]
    [(nolla-jos-nil (:numero (first (filter #(= (:ilmoitustyyppi %) "toimenpidepyynto") summat))))
     (nolla-jos-nil (:numero (first (filter #(= (:ilmoitustyyppi %) "tiedoitus") summat))))
     (nolla-jos-nil (:numero (first (filter #(= (:ilmoitustyyppi %) "kysely") summat))))
     (nolla-jos-nil (:numero (first (filter #(= (:ilmoitustyyppi %) nil) summat))))]))

(defn ilmoitukset-asiakaspalauteluokittain [db urakka-id hallintayksikko-id alkupvm loppupvm]
  (let [data (ilmoitukset/hae-ilmoitukset-asiakaspalauteluokittain db urakka-id hallintayksikko-id alkupvm loppupvm)
        ilman-kokonaismaaria (filter #(not-empty (:nimi %)) data)
        rivit (mapv (fn [[nimi summat]] (into [nimi] (kasittele-summat summat)))
                    (group-by :nimi ilman-kokonaismaaria))]
    [:taulukko {:otsikko "Ilmoitukset asiakaspalauteluokittain"}
     [{:leveys 6 :otsikko "Asiakaspalauteluokka"}
      {:leveys 2 :otsikko "TPP (Toimenpidepyyntö)"}
      {:leveys 2 :otsikko "TUR (Tiedoksi)"}
      {:leveys 2 :otsikko "URK (Kysely)"}
      {:leveys 2 :otsikko "Yhteensä"}]
     rivit]))

(defn suorita [db user {:keys [urakka-id hallintayksikko-id alkupvm loppupvm urakkatyyppi] :as parametrit}]
  (let [konteksti (cond urakka-id :urakka
                        hallintayksikko-id :hallintayksikko
                        :default :koko-maa)
        kyseessa-kk-vali? (pvm/kyseessa-kk-vali? alkupvm loppupvm)
        ;; vielä ei ole implementoitu selitevalintaa, mutta jos se tulee, niin logiikka tähän
        selite nil

        ilmoitukset (hae-ilmoitukset-raportille
                      db user hallintayksikko-id urakka-id
                      nil urakkatyyppi
                      +ilmoitustilat+ +ilmoitustyypit+
                      [alkupvm loppupvm] "" selite)

        ;; graafia varten haetaan joko ilmoitukset pitkältä aikaväliltä tai jos kk raportti, niin hoitokaudelta
        hoitokauden-alkupvm (first (pvm/paivamaaran-hoitokausi alkupvm))
        hoitokauden-loppupvm (second (pvm/paivamaaran-hoitokausi alkupvm))
        ilmoitukset-hoitokaudella (when kyseessa-kk-vali?
                                    (hae-ilmoitukset-raportille
                                      db user hallintayksikko-id urakka-id
                                      nil nil
                                      +ilmoitustilat+ +ilmoitustyypit+
                                      [hoitokauden-alkupvm hoitokauden-loppupvm] "" selite))
        ilmoitukset-kuukausittain (group-by ffirst
                                            (frequencies (map (juxt (comp vuosi-ja-kk :ilmoitettu)
                                                                    :ilmoitustyyppi)
                                                              (if kyseessa-kk-vali?
                                                                ilmoitukset-hoitokaudella
                                                                ilmoitukset))))
        ilmoitukset-kuukausittain-tyyppiryhmiteltyna (reduce-kv (fn [tulos kk ilmot]
                                                                  (assoc tulos kk
                                                                               [(some #(when (= :toimenpidepyynto (second (first %)))
                                                                                        (second %)) ilmot)
                                                                                (some #(when (= :tiedoitus (second (first %)))
                                                                                        (second %)) ilmot)
                                                                                (some #(when (= :kysely (second (first %)))
                                                                                        (second %)) ilmot)]))
                                                                {} ilmoitukset-kuukausittain)
        graafin-alkupvm (if kyseessa-kk-vali?
                          hoitokauden-alkupvm
                          alkupvm)
        graafin-loppupvm (if kyseessa-kk-vali?
                          hoitokauden-loppupvm
                          loppupvm)
        hoitokaudella-tahan-asti-opt (if kyseessa-kk-vali? " hoitokaudella " "")
        raportin-nimi "Ilmoitusraportti"
        otsikko (raportin-otsikko
                  (case konteksti
                    :urakka (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                    :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio db hallintayksikko-id)))
                    :koko-maa "KOKO MAA")
                  raportin-nimi alkupvm loppupvm)
        ilmoitukset-hyn-mukaan (sort-by #(or (:id (first %)) 100000)
                                        (seq (group-by :hallintayksikko
                                                       ilmoitukset)))
        nayta-pylvaat? (or (and (> (count ilmoitukset) 0)
                                (not= (vuosi-ja-kk alkupvm) (vuosi-ja-kk loppupvm)))
                           (and (> (count ilmoitukset-hoitokaudella) 0)
                                kyseessa-kk-vali?))]
    [:raportti {:nimi raportin-nimi}
     [:taulukko {:otsikko otsikko
                 :viimeinen-rivi-yhteenveto? true
                 :sheet-nimi raportin-nimi}
      (into []
            (concat
              [{:otsikko "Urakka" :leveys 31}]
              (map (fn [ilmoitustyyppi]
                     {:otsikko (ilmoitustyypin-lyhenne-ja-nimi ilmoitustyyppi)
                      :leveys 23})
                   [:toimenpidepyynto :tiedoitus :kysely])))
      (keep identity
            (into
              []
              (concat
                (apply concat
                       ;; Tehdään rivi jokaiselle urakalle, ja näytetään niiden erityyppistem ilmoitusten määrä
                       (for [[hy ilmoitukset] ilmoitukset-hyn-mukaan]
                         (concat
                           [{:otsikko (or (:nimi hy) "Ilmoitukset ilman urakkaa")}]
                           (for [[urakka hyn-ilmoitukset] (group-by :urakka ilmoitukset)
                                 :let [urakan-nimi (or (:nimi (first (urakat-q/hae-urakka db urakka))) "Ei urakkaa")
                                       tpp (count (filter #(= :toimenpidepyynto (:ilmoitustyyppi %)) hyn-ilmoitukset))
                                       tur (count (filter #(= :tiedoitus (:ilmoitustyyppi %)) hyn-ilmoitukset))
                                       urk (count (filter #(= :kysely (:ilmoitustyyppi %)) hyn-ilmoitukset))]]
                             [urakan-nimi tpp tur urk])
                           ;; lasketaan myös hallintayksiköiden summarivi
                           (when (= :koko-maa konteksti)
                             (let [hy-tpp-yht (count (filter #(= :toimenpidepyynto (:ilmoitustyyppi %)) ilmoitukset))
                                   hy-tur-yht (count (filter #(= :tiedoitus (:ilmoitustyyppi %)) ilmoitukset))
                                   hy-urk-yht (count (filter #(= :kysely (:ilmoitustyyppi %)) ilmoitukset))]
                               (when (:nimi hy)
                                 [(seq [(str (:nimi hy) " yhteensä") hy-tpp-yht hy-tur-yht hy-urk-yht])]))))))

                ;; Tehdään yhteensä rivi, jossa kaikki ilmoitukset lasketaan yhteen materiaalin perusteella
                (when (and (not= :urakka konteksti)
                           (not (empty? ilmoitukset)))
                  (let [tpp-yht (count (filter #(= :toimenpidepyynto (:ilmoitustyyppi %)) ilmoitukset))
                        tur-yht (count (filter #(= :tiedoitus (:ilmoitustyyppi %)) ilmoitukset))
                        urk-yht (count (filter #(= :kysely (:ilmoitustyyppi %)) ilmoitukset))]
                    [(concat ["Yhteensä"]
                             [tpp-yht tur-yht urk-yht])])))))]

     (when nayta-pylvaat?
       (if-not (empty? ilmoitukset-kuukausittain-tyyppiryhmiteltyna)
         (pylvaat-kuukausittain {:otsikko (str "Ilmoitukset kuukausittain" hoitokaudella-tahan-asti-opt)
                                 :alkupvm graafin-alkupvm :loppupvm graafin-loppupvm
                                 :kuukausittainen-data ilmoitukset-kuukausittain-tyyppiryhmiteltyna :piilota-arvo? #{0}
                                 :legend ["TPP" "TUR" "URK"]})
         (ei-osumia-aikavalilla-teksti "ilmoituksia" graafin-alkupvm graafin-loppupvm)))

     (ilmoitukset-asiakaspalauteluokittain db urakka-id hallintayksikko-id alkupvm loppupvm)]))
