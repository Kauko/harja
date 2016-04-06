(ns harja.domain.oikeudet.makrot
  "Makro, joka lukee oikeusmatriisin Excelistä ja määrittelee sen Clojure datana"
  (:require [dk.ative.docjure.spreadsheet :as xls]
            [clojure.string :as str]))

(defn- lue-workbook []
  (xls/load-workbook-from-file "resources/roolit.xlsx"))

(defn- roolimappaus [sheet]
  (->> sheet
       (xls/select-columns {:A :nimi :B :kuvaus :C :osapuoli :D :linkki})
       (drop 1)))

(defn- roolit-sheet [workbook]
  (xls/select-sheet "Roolit" workbook))

(defn- oikeudet-sheet [workbook]
  (xls/select-sheet "Oikeudet" workbook))

(defn- roolien-sarakkeet
  "Palauttaa Oikeudet sheetiltä löytyneet oikeudet ja niiden sarakkeet."
  [sheet]
  (keep (fn [sarake]
          (if-let [rooli (some-> sarake
                                 char
                                 (str 6)
                                 (xls/select-cell sheet)
                                 xls/read-cell)]
            {:sarake (char sarake) :rooli rooli}))
        (range (int \C) (int \Z))))

(defn oikeuksien-rivit
  "Palauttaa eri oikeudet ja niiden rivinumerot."
  [sheet]
  (keep (fn [rivi]
          (let [osio (some-> (str "A" rivi)
                             (xls/select-cell sheet)
                             xls/read-cell)
                nakyma (some-> (str "B" rivi)
                               (xls/select-cell sheet)
                               xls/read-cell)]
            (when (and osio nakyma)
              {:osio osio :nakyma nakyma :rivi rivi})))
        (range 7 100)))

(defn- kanonisoi [teksti]
  (-> teksti
      str/lower-case
      (str/replace " " "")
      (str/replace "/" "-")
      (str/replace "ä" "a")
      (str/replace "ö" "o")))

(defn- oikeus-sym
  "Tekee symbol nimen oikeudelle osion ja näkymän yhdistämällä"
  [osio nakyma]
  (symbol (str (kanonisoi osio)
               "-"
               (kanonisoi nakyma))))

(defn- oikeudet [roolit sheet]
  (let [sarakkeet (roolien-sarakkeet sheet)
        rivit (oikeuksien-rivit sheet)
        kuvaus->rooli (into {}
                            (map (juxt :kuvaus :nimi))
                            roolit)]
    (map
     (fn [{:keys [osio nakyma rivi]}]
       (let [roolien-oikeudet (keep (fn [{:keys [sarake rooli]}]
                                      (when-let [oikeus (some-> (str sarake rivi)
                                                                (xls/select-cell sheet)
                                                                xls/read-cell)]
                                        {:rooli (kuvaus->rooli rooli)
                                         :luku? (.contains oikeus "R")
                                         :kirjoitus? (.contains oikeus "W")}))
                                    sarakkeet)]
         {:sym (oikeus-sym osio nakyma)
          :kuvaus (str "Osio '" osio "' näkymä '" nakyma "'")
          :luku (into #{}
                      (keep :rooli
                            (filter :luku? roolien-oikeudet)))
          :kirjoitus (into #{}
                           (keep :rooli
                                 (filter :kirjoitus? roolien-oikeudet)))}))
     rivit)))

(defn- lue-oikeudet []
  (let [wb (lue-workbook)
        roolimappaus (-> wb roolit-sheet roolimappaus)
        oikeudet (->> wb oikeudet-sheet (oikeudet roolimappaus))]
    {:roolimappaus roolimappaus
     :oikeudet oikeudet}))

(defmacro maarittele-oikeudet! []
  (let [{:keys [roolimappaus oikeudet]} (lue-oikeudet)]
    `(do
       (def ~'roolit ~(into {}
                            (map (juxt :nimi identity))
                            roolimappaus))
       ~@(mapv
          (fn [oikeudet]
            `(do
               ~@(mapv (fn [{:keys [sym kuvaus luku kirjoitus]}]
                         `(def ~sym (harja.domain.oikeudet/->KayttoOikeus ~kuvaus ~luku ~kirjoitus)))
                       oikeudet)))
          (partition 16 16 [] oikeudet)))))
