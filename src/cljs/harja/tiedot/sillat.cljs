(ns harja.tiedot.sillat
  "Sillat karttatason vaatimat tiedot. Sillat on jaettu geometrisesti hoidon alueurakoiden alueille."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [harja.tiedot.navigaatio :as nav]
            [harja.atom :refer-macros [reaction<!]]
            [harja.geo :as geo])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def karttataso-sillat (atom false))

(def listaus (atom :kaikki))

(defn- varita-silta [silta]
  ;; PENDING: tämä ehkä korjausstatuksen mukaan?
  ;; Nyt sillat ovat punaisia
  (-> silta
      (assoc-in [:alue :fill] "red")))

(defn- hae-urakan-siltalistaus [urakka listaus]
  (k/post! :hae-urakan-sillat
           {:urakka-id (:id urakka)
            :listaus listaus}))

(def haetut-sillat
  (reaction<! [paalla? @karttataso-sillat
               urakka @nav/valittu-urakka
               listaus @listaus]
              {:nil-kun-haku-kaynnissa? true}
              (when (and paalla? urakka)
                (log "Siltataso päällä, haetaan sillat urakalle: "
                     (:nimi urakka) " (id: " (:id urakka) ")")
                (go (into []
                          (comp (map #(assoc % :type :silta))
                                (map varita-silta))
                          (<! (hae-urakan-siltalistaus urakka listaus)))))))

(defn- skaalaa-sillat-zoom-tason-mukaan [koko sillat]
  ;; PENDING: Ei ole optimaalista, että sillat ovat "point", jotka
  ;; piirretään tietyllä radiuksella... ikoni olisi hyvä saada.
  (let [sillan-koko (* 0.003 koko)]
    (into []
          (map #(assoc-in % [:alue :radius] sillan-koko))
          sillat)))

(def sillat
  (reaction (skaalaa-sillat-zoom-tason-mukaan
             @nav/kartan-nakyvan-alueen-koko @haetut-sillat)))


(defn paivita-silta! [id funktio & args]
  (swap! sillat (fn [sillat]
                  (mapv (fn [silta]
                          (if (= id (:id silta))
                            (apply funktio silta args)
                            silta)) sillat))))
