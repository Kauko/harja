(ns harja.tiedot.urakka
  "Tämä nimiavaruus hallinnoi urakan usealle toiminnolle yhteisiä tietoja"
  (:require [reagent.core :refer [atom] :as r]
            [cljs-time.core :as time]
            [cljs-time.coerce :as tc]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.tiedot.urakka.suunnittelu.muut-tyot :as muut-tyot]
            [harja.tiedot.urakka.organisaatio :as organisaatio]
            [harja.tiedot.toimenpidekoodit :as toimenpidekoodit]
            [harja.loki :refer [log tarkkaile!]]
            [harja.pvm :as pvm]
            [harja.atom :refer-macros [reaction<!]]
            [cljs-time.core :as t]
            [taoensso.truss :as truss :refer-macros [have]])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   
                   ))

(defonce valittu-sopimusnumero (let [val (atom nil)]
                                 (run! (reset! val (first (:sopimukset @nav/valittu-urakka))))
                                 val))

(defonce urakan-yks-hint-tyot (atom nil))
(defonce urakan-kok-hint-tyot (atom nil))

(defn valitse-sopimusnumero! [sn]
  (reset! valittu-sopimusnumero sn))

(defonce urakan-toimenpideinstanssit
         (reaction<! [ur (:id @nav/valittu-urakka)]
                     (when ur
                       (urakan-toimenpiteet/hae-urakan-toimenpiteet ur))))

(defonce valittu-toimenpideinstanssi (reaction (first @urakan-toimenpideinstanssit)))

(defn urakan-toimenpideinstanssi-toimenpidekoodille [tpk]
  (have integer? tpk)
  (first (filter #(= tpk (:id %)) @urakan-toimenpideinstanssit)))

(defn valitse-toimenpideinstanssi! [tpi]
  (reset! valittu-toimenpideinstanssi tpi))

(defn hoitokaudet
  "Palauttaa urakan hoitokaudet, jos kyseessä on hoidon alueurakka. Muille urakoille palauttaa
  urakan sopimuskaudet. Sopimuskaudet ovat sopimuksen kesto jaettuna sopimusvuosille (ensimmäinen
  ja viimeinen voivat olla vajaat)."
  [ur]
  (let [alkupvm (:alkupvm ur)
        loppupvm (:loppupvm ur)
        ensimmainen-vuosi (pvm/vuosi alkupvm)
        viimeinen-vuosi (pvm/vuosi loppupvm)]
    (if (= :hoito (:tyyppi ur))
      ;; Hoidon alueurakan hoitokaudet
      (mapv (fn [vuosi]
              [(pvm/hoitokauden-alkupvm vuosi)
               (pvm/hoitokauden-loppupvm (inc vuosi))])
            (range ensimmainen-vuosi viimeinen-vuosi))
      ;; Muiden urakoiden sopimusaika pilkottuna vuosiin
      (pvm/urakan-vuodet alkupvm loppupvm))))

(defn urakoiden-hoitokaudet
  "Palauttaa urakoiden hoitokaudet aikaisimmasta viimeiseen. Ei kuitenkaan palauta yli N vuotta
  vanhoja hoitokausia."
  [urakat n]
  (let [ensimmainen-vuosi (pvm/vuosi (t/earliest (map :alkupvm urakat)))
        viimeinen-vuosi (t/year (t/now))
        ensimmainen-vuosi (if (>= (- (t/year (t/now))
                                     ensimmainen-vuosi)
                                  n)
                            (- (t/year (t/now)) n)
                            ensimmainen-vuosi)]
      (mapv (fn [vuosi]
              [(pvm/hoitokauden-alkupvm vuosi)
               (pvm/hoitokauden-loppupvm (inc vuosi))])
            (range ensimmainen-vuosi viimeinen-vuosi))))

(defn edelliset-hoitokaudet
  "Palauttaa N edellistä hoitokautta alkaen nykyajasta."
  ([n] (edelliset-hoitokaudet n false))
  ([n nykyinenkin?]
   (let [ensimmainen-vuosi (- (t/year (t/now)) n)
         viimeinen-vuosi (+ (t/year (t/now))
                            (if nykyinenkin? 1 0))]
    (mapv (fn [vuosi]
            [(pvm/hoitokauden-alkupvm vuosi)
             (pvm/hoitokauden-loppupvm (inc vuosi))])
          (range ensimmainen-vuosi viimeinen-vuosi)))))

(defonce valitun-urakan-hoitokaudet
         (reaction (when-let [ur @nav/valittu-urakka]
                     (hoitokaudet ur))))

(defn paattele-valittu-hoitokausi [hoitokaudet]
  (when-not (empty? hoitokaudet)
    (let [[alku-pvm _] (first hoitokaudet)
          [_ loppu-pvm] (last hoitokaudet)
          nyt (pvm/nyt)]
      (cond
        ;; Jos urakka ei ole vielä alkanut, valitaan 1. hoitokausi
        (pvm/ennen? nyt alku-pvm)
        (first hoitokaudet)

        ;; Jos urakka on jo päättynyt, valitaan viimeinen hoitokausi
        (pvm/jalkeen? nyt loppu-pvm)
        (last hoitokaudet)

        ;; Jos urakka on käynnissä, valitaan hoitokausi, joka on käynnissä
        :default
        (or (first (filter (fn [[alku loppu]]
                             (pvm/valissa? nyt alku loppu))
                           hoitokaudet))
            ;; ultimate fallback, jos ei löydy jostain syystä, käytä ensimmäistä
            (first hoitokaudet))))))


(defonce valittu-hoitokausi
         (reaction (paattele-valittu-hoitokausi @valitun-urakan-hoitokaudet)))

(defonce valittu-aikavali (reaction [(first @valittu-hoitokausi) (second @valittu-hoitokausi)]))

(defn valitse-hoitokausi! [hk]
  (log "------- VALITAAN HOITOKAUSI:" (pr-str hk))
  (reset! valittu-hoitokausi hk))

(defonce valittu-hoitokauden-kuukausi
         (reaction
           (let [hk @valittu-hoitokausi
                 ur @nav/valittu-urakka
                 kuuluu-hoitokauteen? #(pvm/valissa? (second %) (first hk) (second hk))
                 nykyinen-kk (pvm/kuukauden-aikavali (pvm/nyt))
                 edellinen-kk (pvm/ed-kk-aikavalina (pvm/nyt))]
             (when (and hk ur)
               (cond
                 ;; Jos nykyhetkeä edeltävä kuukausi kuuluu valittuun hoitokauteen,
                 ;; valitaan se. (yleensä raportoidaan aiempaa kuukautta)
                 (kuuluu-hoitokauteen? edellinen-kk)
                 edellinen-kk

                 ;; Valitaan tämä kuukausi, jos se kuuluu hoitokauteen
                 (kuuluu-hoitokauteen? nykyinen-kk)
                 nykyinen-kk

                 ;; Jos hoitokausi ei vielä ole alkanut, valitaan ensimmäinen
                 (pvm/ennen? (pvm/nyt) (first hk))
                 (first (pvm/hoitokauden-kuukausivalit hk))

                 ;; fallback on hoitokauden viimeinen kuukausi
                 :default
                 (last (pvm/hoitokauden-kuukausivalit hk)))))))

(defn valitse-hoitokauden-kuukausi! [hk-kk]
  (reset! valittu-hoitokauden-kuukausi hk-kk))

;; rivit ryhmitelty tehtävittäin, rivissä oltava :alkupvm ja :loppupvm
(defn jaljella-olevien-hoitokausien-rivit
  "Palauttaa ne rivit joiden loppupvm on joku jaljella olevien kausien pvm:stä"
  [rivit-tehtavittain jaljella-olevat-kaudet]
  (mapv (fn [tehtavan-rivit]
          (filter (fn [tehtavan-rivi]
                    (some #(pvm/sama-pvm? (second %) (:loppupvm tehtavan-rivi)) jaljella-olevat-kaudet))
                  tehtavan-rivit)) rivit-tehtavittain))

(defn tulevat-hoitokaudet [ur hoitokausi]
  (drop-while #(not (pvm/sama-pvm? (second %) (second hoitokausi)))
              (hoitokaudet ur)))

(defn rivit-tulevillekin-kausille [ur rivit hoitokausi]
  (into []
        (mapcat (fn [[alku loppu]]
                  (map (fn [rivi]
                         ;; tässä hoitokausien alkupvm ja loppupvm liitetään töihin
                         (assoc rivi :alkupvm alku :loppupvm loppu)) rivit)))
        (tulevat-hoitokaudet ur hoitokausi)))



;; fixme if you can, man. En saanut kohtuullisessa ajassa tätä generalisoitua
;; siistiksi osaksi rivit-tulevillekin-kausille-funktiota
(defn rivit-tulevillekin-kausille-kok-hint-tyot [ur rivit hoitokausi]
  (into []
        (mapcat (fn [[alku loppu]]
                  (map (fn [rivi]
                         ;; maksupvm:n vuotta täytyy päivittää eikä se välttämättä ole sama kuin työn :vuosi
                         (let [tyon-kalenteri-vuosi (if (<= 10 (:kuukausi rivi) 12)
                                                      (pvm/vuosi alku)
                                                      (pvm/vuosi loppu))
                               maksupvmn-vuoden-erotus (if (:maksupvm rivi)
                                                         (- (time/year (:maksupvm rivi)) (:vuosi rivi))
                                                         0)
                               uusi-maksupvm (if (:maksupvm rivi)
                                               (pvm/luo-pvm (+ tyon-kalenteri-vuosi maksupvmn-vuoden-erotus)
                                                            (- (time/month (:maksupvm rivi)) 1)
                                                            (time/day (:maksupvm rivi)))
                                               nil)]
                           (assoc rivi :alkupvm alku
                                       :loppupvm loppu
                                       :vuosi tyon-kalenteri-vuosi
                                       :maksupvm uusi-maksupvm)))
                       rivit)))
        (tulevat-hoitokaudet ur hoitokausi)))

(defn ryhmittele-hoitokausittain
  "Ottaa rivejä, jotka sisältävät :alkupvm ja :loppupvm, ja palauttaa ne ryhmiteltynä hoitokausiin.
  Palauttaa mäpin, jossa avaimena on hoitokauden [alku loppu] ja arvona on sen hoitokauden rivit.
  Jos sekvenssi hoitokausia on annettu, varmistetaan että mäpissä on kaikille niille avaimet. Tällä tavalla
  voidaan luoda tyhjät ryhmät myös hoitokausille, joilla ei ole yhtään riviä."
  ([rivit] (ryhmittele-hoitokausittain rivit nil))
  ([rivit hoitokaudet]
   (loop [ryhmitelty (group-by (juxt :alkupvm :loppupvm)
                               rivit)
          [kausi & hoitokaudet] hoitokaudet]
     (if-not kausi
       ryhmitelty
       (if (contains? ryhmitelty kausi)
         (recur ryhmitelty hoitokaudet)
         (recur (assoc ryhmitelty kausi []) hoitokaudet))))))

(defonce urakan-valittu-valilehti (atom :yleiset))
(defonce suunnittelun-valittu-valilehti (atom :kokonaishintaiset))
(defonce toteumat-valilehti (atom :kokonaishintaiset-tyot))
(defonce laskutus-valittu-valilehti (atom :laskutusyhteenveto))
(defonce hallinnan-valittu-valilehti (atom :kayttajat))

(defonce urakan-toimenpiteet-ja-tehtavat
  (reaction<! [ur (:id @nav/valittu-urakka)
               ;; pitää hakea uudelleen jos toimenpidekoodeja muokataan
               _ @toimenpidekoodit/koodit]
              {:nil-kun-haku-kaynnissa? true}
              (when ur
                (urakan-toimenpiteet/hae-urakan-toimenpiteet-ja-tehtavat ur))))

(defonce urakan-kokonaishintaiset-toimenpiteet-ja-tehtavat-tehtavat
  (reaction<! [ur (:id @nav/valittu-urakka)
               ;; pitää hakea uudelleen jos toimenpidekoodeja muokataan
               _ @toimenpidekoodit/koodit]
              {:nil-kun-haku-kaynnissa? true}
              (when ur
                (urakan-toimenpiteet/hae-urakan-kokonaishintaiset-toimenpiteet-ja-tehtavat ur))))

(defonce valittu-kokonaishintainen-tehtava (atom nil))

(defn valitse-kokonaishintainen-tehtava! [tehtava]
  (reset! valittu-kokonaishintainen-tehtava tehtava))

(defonce urakan-tpin-kokonaishintaiset-tehtavat
         (reaction (let [tpi @valittu-toimenpideinstanssi
                         tehtavat @urakan-kokonaishintaiset-toimenpiteet-ja-tehtavat-tehtavat]
                     (reset! valittu-kokonaishintainen-tehtava nil)
                     (filter
                       (fn [rivi]
                         (if (:t3_koodi tpi)
                           (= (:t3_koodi rivi)
                              (:t3_koodi @valittu-toimenpideinstanssi))
                           true))
                       tehtavat))))

(defonce urakan-yksikkohintaiset-toimenpiteet-ja-tehtavat
  (reaction<! [ur (:id @nav/valittu-urakka)
               ;; pitää hakea uudelleen jos toimenpidekoodeja muokataan
               _ @toimenpidekoodit/koodit]
              {:nil-kun-haku-kaynnissa? true}
              (when ur
                (urakan-toimenpiteet/hae-urakan-yksikkohintaiset-toimenpiteet-ja-tehtavat ur))))

(defonce valittu-yksikkohintainen-tehtava (atom nil))

(defn valitse-yksikkohintainen-tehtava! [tehtava]
  (reset! valittu-yksikkohintainen-tehtava tehtava))

(defonce urakan-tpin-yksikkohintaiset-tehtavat
         (reaction (let [tpi @valittu-toimenpideinstanssi
                         tehtavat @urakan-yksikkohintaiset-toimenpiteet-ja-tehtavat]
                     (reset! valittu-yksikkohintainen-tehtava nil)
                     (into [] (keep (fn [[_ _ t3 t4]]
                                      (when (= (:koodi t3) (:t3_koodi tpi))
                                        t4))
                                    tehtavat)))))

(defonce urakan-muutoshintaiset-toimenpiteet-ja-tehtavat
  (reaction<! [ur (:id @nav/valittu-urakka)

               nakymassa? (or
                           (= :muut @suunnittelun-valittu-valilehti)
                           (= :muut-tyot @toteumat-valilehti))]
              {:nil-kun-haku-kaynnissa? true}
              (when (and ur nakymassa?)
                (urakan-toimenpiteet/hae-urakan-muutoshintaiset-toimenpiteet-ja-tehtavat ur))))

(defonce urakan-organisaatio
  (reaction<! [ur (:id @nav/valittu-urakka)]
              {:nil-kun-haku-kaynnissa? true}
              (when ur
                (organisaatio/hae-urakan-organisaatio ur))))

(defonce muutoshintaiset-tyot
  (reaction<! [ur (:id @nav/valittu-urakka)
               suunnittelun-sivu @suunnittelun-valittu-valilehti
               toteuman-sivu @toteumat-valilehti]
              {:nil-kun-haku-kaynnissa? true}
              (when (and ur (or
                             (= :muut suunnittelun-sivu)
                             (= :muut-tyot toteuman-sivu)))
                (muut-tyot/hae-urakan-muutoshintaiset-tyot ur))))

(defonce muut-tyot-hoitokaudella
  (reaction<! [ur (:id @nav/valittu-urakka)
               sopimus-id (first @valittu-sopimusnumero)
               aikavali @valittu-hoitokausi
               sivu @toteumat-valilehti]
              {:nil-kun-haku-kaynnissa? true}
              (when (and ur sopimus-id aikavali (= :muut-tyot sivu))
                (toteumat/hae-urakan-muut-tyot ur sopimus-id aikavali))))

(defonce erilliskustannukset-hoitokaudella
  (reaction<! [ur (:id @nav/valittu-urakka)
               aikavali @valittu-hoitokausi
               sivu @toteumat-valilehti
               _ @toteumat/erilliskustannukset-nakymassa?]
              {:nil-kun-haku-kaynnissa? true}
              (when (and ur aikavali (= :erilliskustannukset sivu))
                (toteumat/hae-urakan-erilliskustannukset ur aikavali))))

(defn vaihda-urakkatyyppi
  [urakka-id uusi-urakkatyyppi]
  (k/post! :tallenna-urakan-tyyppi
           {:urakka-id    urakka-id
            :urakkatyyppi uusi-urakkatyyppi}))

(def urakassa-kaytetty-indeksi
  (reaction (when-let [ur @nav/valittu-urakka]
              (when (= :hoito (:tyyppi ur))
                (let [urakan-alkuvuosi (pvm/vuosi (:alkupvm ur))]
                  (if (< urakan-alkuvuosi 2017)
                    "MAKU 2005"
                    "MAKU 2010"))))))
