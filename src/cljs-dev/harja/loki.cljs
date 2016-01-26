(ns harja.loki
  "Apufunktioita lokittamiseen.")

(def +mittaa-aika+ false)

(defn ajan-mittaus-paalle []
  (set! +mittaa-aika+ true))

(defn warn [& things]
  (.apply js/console.warn (apply array things)))

(defn log [& things]
  (.apply js/console.log js/console (apply array things)))

(defn logt
  "Logita taulukko (console.table), sisääntulevan datan on oltava sekvenssi mäppejä."
  [data]
  (if (aget js/console "table")
    (.table js/console (clj->js data))
    (.log js/console (pr-str data))))

(defn tarkkaile!
  [nimi atomi]
  (add-watch atomi :tarkkailija (fn [_ _ vanha uusi]
                                  (log nimi ": " (pr-str vanha) " => " (pr-str uusi))
                                  )))
