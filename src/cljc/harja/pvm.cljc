(ns harja.pvm
  "Yleiset päivämääräkäsittelyn asiat."
  (:require
    #?(:cljs [cljs-time.format :as df])
    #?(:cljs [cljs-time.core :as t])
    #?(:cljs [cljs-time.coerce :as tc])
    #?(:cljs [harja.loki :refer [log]])
    #?(:cljs [cljs-time.extend])
    #?(:clj [clj-time.format :as df])
    #?(:clj
            [clj-time.core :as t])
    #?(:clj
            [clj-time.coerce :as tc])
    #?(:clj
            [clj-time.local :as l])
    #?(:clj
            [taoensso.timbre :as log])
            [clojure.string :as str])

  #?(:cljs (:import (goog.date DateTime))
     :clj
           (:import (java.util Date Calendar)
                    (java.text SimpleDateFormat))))


#?(:cljs
   ;; Toteutetaan hash ja equiv, jotta voimme käyttää avaimena hashejä
   (extend-type DateTime
     IHash
     (-hash [o]
       (hash (tc/to-long o)))))

(defn aikana [dt tunnit minuutit sekunnit millisekunnit]
  #?(:cljs
     (doto (goog.date.DateTime.)
       (.setYear (.getYear dt))
       (.setMonth (.getMonth dt))
       (.setDate (.getDate dt))
       (.setHours tunnit)
       (.setMinutes minuutit)
       (.setSeconds sekunnit)
       (.setMilliseconds millisekunnit))

     :clj
     (.getTime (doto (Calendar/getInstance)
                 (.setTime dt)
                 (.set Calendar/HOUR_OF_DAY tunnit)
                 (.set Calendar/MINUTE minuutit)
                 (.set Calendar/SECOND sekunnit)
                 (.set Calendar/MILLISECOND millisekunnit)))))

(defn millisekunteina [pvm]
  (tc/to-long pvm))

(defn nyt []
  #?(:cljs (DateTime.)
     :clj  (Date.)))

(defn luo-pvm [vuosi kk pv]
  #?(:cljs (DateTime. vuosi kk pv 0 0 0 0)
     :clj  (Date. (- vuosi 1900) kk pv)))

(defn sama-pvm? [eka toka]
  (and (= (t/year eka) (t/year toka))
       (= (t/month eka) (t/month toka))
       (= (t/day eka) (t/day toka))))


#?(:cljs
   (defn ennen? [eka toka]
     (if (and eka toka)
       (t/before? eka toka)
       false))

   :clj
   (defn ennen? [eka toka]
     (if (and eka toka)
       (.before eka toka)
       false)))

(defn sama-tai-ennen? [eka toka]
  (if-not (or (nil? eka) (nil? toka))
    (or (ennen? eka toka) (= (millisekunteina eka) (millisekunteina toka)))
    false))

(defn jalkeen? [eka toka]
  (if-not (or (nil? eka) (nil? toka))
    (t/after? eka toka)
    false))

(defn sama-tai-jalkeen? [eka toka]
  (if-not (or (nil? eka) (nil? toka))
    (or (t/after? eka toka) (= (millisekunteina eka) (millisekunteina toka)))
    false))

(defn sama-kuukausi?
  "Tarkistaa onko ensimmäinen ja toinen päivämäärä saman vuoden samassa kuukaudessa."
  [eka toka]
  (and (= (t/year eka) (t/year toka))
       (= (t/month eka) (t/month toka))))

(defn valissa?
  "Tarkistaa onko annettu pvm alkupvm:n ja loppupvm:n välissä."
  [pvm alkupvm loppupvm]
  (and (sama-tai-jalkeen? pvm alkupvm) (sama-tai-ennen? pvm loppupvm)))

(defn- luo-format [str]
  #?(:cljs (df/formatter str)
     :clj  (SimpleDateFormat. str)))
(defn- formatoi [format date]
  #?(:cljs (df/unparse format date)
     :clj  (.format format date)))
(defn parsi [format teksti]
  #?(:cljs (df/parse-local format teksti)
     :clj (.parse format teksti)))


(def fi-pvm
  "Päivämäärän formatointi suomalaisessa muodossa"
  (luo-format "dd.MM.yyyy"))

(def fi-pvm-parse
  "Parsintamuoto päivämäärästä, sekä nolla etuliite ja ilman kelpaa."
  (luo-format "d.M.yyyy"))

(def fi-aika
  "Ajan formatointi suomalaisessa muodossa"
  (luo-format "HH:mm"))

(def fi-aika-sek
  "Ajan formatointi suomalaisessa muodossa"
  (luo-format "HH:mm:ss"))

(def fi-pvm-aika
  "Päivämäärän ja ajan formatointi suomalaisessa muodossa"
  (luo-format "d.M.yyyy H:mm"))

(def fi-pvm-aika-sek
  "Päivämäärän ja ajan formatointi suomalaisessa muodossa"
  (luo-format "dd.MM.yyyy HH:mm:ss"))

(def iso8601-aikaleimalla
  (luo-format "yyyy-MM-dd'T'HH:mm:ss.S"))

(def kuukausi-ja-vuosi-fmt
  (luo-format "MM/yy"))

(defn pvm-aika
  "Formatoi päivämäärän ja ajan suomalaisessa muodossa"
  [pvm]
  (formatoi fi-pvm-aika pvm))

(defn pvm-aika-opt
  "Formatoi päivämäärän ja ajan suomalaisessa muodossa tai tyhjä, jos nil."
  [p]
  (if p
    (pvm-aika p)
    ""))

(defn pvm-aika-sek
  "Formatoi päivämäärän ja ajan suomalaisessa muodossa sekuntitarkkuudella"
  [pvm]
  (formatoi fi-pvm-aika-sek pvm))

(defn pvm
  "Formatoi päivämäärän suomalaisessa muodossa"
  [pvm]
  (formatoi fi-pvm pvm))

(defn pvm-opt
  "Formatoi päivämäärän suomalaisessa muodossa tai tyhjä, jos nil."
  [p]
  (if p
    (pvm p)
    ""))

(defn aika
  "Formatoi ajan suomalaisessa muodossa"
  [pvm]
  (formatoi fi-aika pvm))

(defn aika-sek
  [pvm]
  (formatoi fi-aika-sek pvm))

(defn aika-iso8601
  [pvm]
  (formatoi iso8601-aikaleimalla pvm))

(defn kuukausi-ja-vuosi
  "Formatoi MM/yy lyhyen vuosi ja kuukausi tekstin. Esim \"01/15\" tammikuulle 2015."
  [pvm]
  (formatoi kuukausi-ja-vuosi-fmt pvm))

(defn ->pvm-aika [teksti]
  "Jäsentää tekstistä d.M.yyyy H:mm tai d.M.yyyy H muodossa olevan päivämäärän ja ajan.
  Jos teksti ei ole oikeaa muotoa, palauta nil."
  (let [teksti-kellonaika-korjattu (if (not= -1 (.indexOf teksti ":"))
                                     teksti
                                     (str teksti ":00"))]
    (try
      (parsi fi-pvm-aika (str/trim teksti-kellonaika-korjattu))
      (catch #?(:cljs js/Error
                :clj  Exception) e
        nil))))

(defn ->pvm-aika-sek [teksti]
  "Jäsentää tekstistä dd.MM.yyyy HH:mm:ss muodossa olevan päivämäärän ja ajan. Tämä on koneellisesti formatoitua päivämäärää varten, älä käytä ihmisen syöttämän tekstin jäsentämiseen!"
  (parsi fi-pvm-aika-sek teksti))

(defn ->pvm [teksti]
  "Jäsentää tekstistä dd.MM.yyyy muodossa olevan päivämäärän. Jos teksti ei ole oikeaa muotoa, palauta nil."
  (try
    (parsi fi-pvm-parse teksti)
    (catch #?(:cljs js/Error
              :clj  Exception) e
      nil)))

(defn paivan-alussa [dt]
  (aikana dt 0 0 0 0))

(defn paivan-lopussa [dt]
  (aikana dt 23 59 59 999))

(defn kuukauden-nimi [kk]
  (case kk
    1 "tammikuu"
    2 "helmikuu"
    3 "maaliskuu"
    4 "huhtikuu"
    5 "toukokuu"
    6 "kesäkuu"
    7 "heinäkuu"
    8 "elokuu"
    9 "syyskuu"
    10 "lokakuu"
    11 "marraskuu"
    12 "joulukuu"
    "kk ei välillä 1-12"))

(defn kuukauden-lyhyt-nimi [kk]
  (case kk
    1 "tammi"
    2 "helmi"
    3 "maalis"
    4 "huhti"
    5 "touko"
    6 "kesä"
    7 "heinä"
    8 "elo"
    9 "syys"
    10 "loka"
    11 "marras"
    12 "joulu"
    "tuntematon"))

;; hoidon alueurakoiden päivämääräapurit
(defn vuoden-eka-pvm
  "Palauttaa vuoden ensimmäisen päivän 1.1.vuosi"
  [vuosi]
  (luo-pvm vuosi 0 1))

(defn vuoden-viim-pvm
  "Palauttaa vuoden viimeisen päivän 31.12.vuosi"
  [vuosi]
  (luo-pvm vuosi 11 31))

(defn vuoden-aikavali [vuosi]
  [(paivan-alussa (vuoden-eka-pvm vuosi))
   (paivan-lopussa (vuoden-viim-pvm vuosi))])

(defn hoitokauden-alkupvm
  "Palauttaa hoitokauden alkupvm:n 1.10.vuosi"
  [vuosi]
  (luo-pvm vuosi 9 1))

(defn hoitokauden-loppupvm
  "Palauttaa hoitokauden loppupvm:n 30.9.vuosi"
  [vuosi]
  (luo-pvm vuosi 8 30))

(defn- d [x]
  #?(:cljs x
     :clj  (if (instance? Date x)
             (tc/from-date x)
             x)))

(defn vuosi
  "Palauttaa annetun DateTimen vuoden, esim 2015."
  [pvm]
  (t/year (d pvm)))

(defn kuukausi
  "Palauttaa annetun DateTime kuukauden."
  [pvm]
  ;; PENDING: tämä ei clj puolella toimi, jos ollaan kk alussa
  ;; esim 2015-09-30T21:00:00.000-00:00 (joka olisi keskiyöllä meidän aikavyöhykkeellä)
  ;; pitäisi joda date timeihin vaihtaa koko backend puolella
  (t/month (d pvm)))

(defn paiva
  "Palauttaa annetun DateTime päivän."
  [pvm]
  (t/day (d pvm)))

(defn paivamaaran-hoitokausi
  "Palauttaa hoitokauden [alku loppu], johon annettu pvm kuuluu"
  [pvm]
  (let [vuosi (vuosi pvm)]
    (if (ennen? pvm (hoitokauden-alkupvm vuosi))
      [(hoitokauden-alkupvm (dec vuosi))
       (hoitokauden-loppupvm vuosi)]
      [(hoitokauden-alkupvm vuosi)
       (hoitokauden-loppupvm (inc vuosi))])))

(defn paiva-kuukausi
  "Palauttaa päivän ja kuukauden suomalaisessa muodossa pp.kk."
  [pvm]
  (str (paiva pvm) "." (kuukausi pvm) "."))

(defn hoitokauden-edellinen-vuosi-kk [vuosi-kk]
  (let [vuosi (first vuosi-kk)
        kk (second vuosi-kk)
        ed-vuosi (if (= 1 kk)
                   (dec vuosi)
                   vuosi)
        ed-kk (if (= 1 kk)
                12
                (dec kk))
        ]
    [ed-vuosi ed-kk]))



#?(:cljs
   (defn
     kuukauden-aikavali
     "Palauttaa kuukauden aikavälin vektorina [alku loppu], jossa alku on kuukauden ensimmäinen päivä
  kello 00:00:00.000 ja loppu on kuukauden viimeinen päivä kello 23:59:59.999 ."
     [dt]
     (let [alku (aikana (t/first-day-of-the-month dt)
                        0 0 0 0)
           loppu (aikana (t/last-day-of-the-month dt)
                         23 59 59 999)]
       ;; aseta aika
       [alku loppu])))

#?(:cljs
   (defn hoitokauden-kuukausivalit
     "Palauttaa vektorin kuukauden aikavälejä (ks. kuukauden-aikavali funktio) annetun hoitokauden
   jokaiselle kuukaudelle."
     [[alkupvm loppupvm]]
     (let [alku (t/first-day-of-the-month alkupvm)]
       (loop [kkt [(kuukauden-aikavali alkupvm)]
              kk (t/plus alku (t/months 1))]
         (if (t/after? kk loppupvm)
           kkt
           (recur (conj kkt
                        (kuukauden-aikavali kk))
                  (t/plus kk (t/months 1))))))))

#?(:cljs
   (defn vuoden-kuukausivalit
     "Palauttaa vektorin kuukauden aikavälejä (ks. kuukauden-aikavali funktio) annetun vuoden jokaiselle kuukaudelle."
     [alkuvuosi]
     (let [alku (t/first-day-of-the-month (luo-pvm alkuvuosi 0 1))]
       (loop [kkt [(kuukauden-aikavali alku)]
              kk (t/plus alku (t/months 1))]
         (if (not= (vuosi kk) alkuvuosi)
           kkt
           (recur (conj kkt
                        (kuukauden-aikavali kk))
                  (t/plus kk (t/months 1))))))))

#?(:cljs
   (defn ed-kk-aikavalina
     [p]
     (let [pvm-ed-kkna (t/minus p (t/months 1))]
       [(t/first-day-of-the-month pvm-ed-kkna)
        (t/last-day-of-the-month pvm-ed-kkna)])))

#?(:clj
   (defn kyseessa-kk-vali?
     "Kertoo onko annettu pvm-väli täysi kuukausi. Käyttää aikavyöhykekonversiota mistä halutaan ehkä joskus eroon."
     [alkupvm loppupvm]
     (let [alku (l/to-local-date-time alkupvm)
           loppu (l/to-local-date-time loppupvm)
           paivia-kkssa (t/number-of-days-in-the-month alku)
           loppu-pv (paiva loppu)]
       (and (and (= (vuosi alku)
                    (vuosi loppu))
                 (= (kuukausi alku)
                    (kuukausi loppu)))
            (= paivia-kkssa loppu-pv)))))

#?(:clj
   (defn kuukautena-ja-vuonna
     "Palauttaa tekstiä esim tammikuussa 2016"
     [alkupvm]
     (str (kuukauden-nimi (kuukausi alkupvm)) "ssa "
          (vuosi alkupvm))))
<<<<<<< HEAD
=======

(defn urakan-vuodet [alkupvm loppupvm]
  (let [ensimmainen-vuosi (vuosi alkupvm)
        viimeinen-vuosi (vuosi loppupvm)]
    (if (= ensimmainen-vuosi viimeinen-vuosi)
      [[alkupvm loppupvm]]

      (vec (concat [[alkupvm (vuoden-viim-pvm ensimmainen-vuosi)]]
                   (mapv (fn [vuosi]
                           [(vuoden-eka-pvm vuosi) (vuoden-viim-pvm vuosi)])
                         (range (inc ensimmainen-vuosi) viimeinen-vuosi))
                   [[(vuoden-eka-pvm viimeinen-vuosi) loppupvm]])))))

>>>>>>> b4725a9d576c34bff86834a18a82749a9cd998dc
