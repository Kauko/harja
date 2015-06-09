(ns harja.tiedot.urakka.laadunseuranta
  "Urakan tarkastukset: tiestötarkastukset, talvihoitotarkastukset sekä soratietarkastukset."
  (:require [harja.asiakas.kommunikaatio :as k]
            [reagent.core :refer [atom] :as r]))

(defonce laadunseurannassa? (atom false)) ; jos true, laadunseurantaosio nyt käytössä

(defonce valittu-valilehti (atom :havainnot))

(def +sanktio-skeema+
  [{:otsikko "Perintäpvm" :nimi :perintapvm :tyyppi :pvm :leveys 2}
   {:otsikko "Sakkoryhmä" :tyyppi :valinta :leveys 2
    :nimi :ryhma
    :valinnat [:A :B :C :muistutus]
    :valinta-nayta #(case %
                      :A "Ryhmä A"
                      :B "Ryhmä B"
                      :C "Ryhmä C"
                      :muistutus "Muistutus"
                      "- valitse ryhmä -")}
   {:otsikko "Sakko (€)" :nimi :summa :tyyppi :numero :leveys 2}
   {:otsikko "Sidotaan indeksiin" :nimi :indeksi :leveys 3
    :tyyppi :valinta
    :valinnat ["MAKU 2005" "MAKU 2010"] ;; FIXME: haetaanko indeksit tiedoista?
    :valinta-nayta #(or % "Ei sidota indeksiin")}
   ])


(defn hae-urakan-tarkastukset
  "Hakee annetun urakan tarkastukset urakka id:n ja ajan perusteella."
  [urakka-id alkupvm loppupvm]
  (k/post! :hae-urakan-tarkastukset {:urakka-id urakka-id
                                     :alkupvm alkupvm
                                     :loppupvm loppupvm}))

(defn hae-urakan-havainnot
  "Hakee annetun urakan havainnot urakka id:n ja aikavälin perusteella."
  [listaus urakka-id alkupvm loppupvm]
  (k/post! :hae-urakan-havainnot {:listaus listaus
                                  :urakka-id urakka-id
                                  :alku alkupvm
                                  :loppu loppupvm}))

(defn hae-havainnon-tiedot
  "Hakee urakan havainnon tiedot urakan id:n ja havainnon id:n perusteella.
  Palauttaa kaiken tiedon mitä havainnon muokkausnäkymään tarvitaan."
  [urakka-id havainto-id]
  (k/post! :hae-havainnon-tiedot {:urakka-id urakka-id
                                  :havainto-id havainto-id}))

(defn hae-tarkastus
  "Hakee tarkastuksen kaikki tiedot urakan id:n ja tarkastuksen id:n perusteella. Tähän liittyy havainnot sekä niiden reklamaatiot."
  [urakka-id tarkastus-id]
  (k/post! :hae-tarkastus {:urakka-id urakka-id
                           :tarkastus-id tarkastus-id}))

(defn tallenna-havainto [havainto]
  (k/post! :tallenna-havainto havainto))

  
(defn hae-urakan-sanktiot
  "Hakee urakan sanktiot annetulle hoitokaudelle."
  [urakka-id [alku loppu]]
  (k/post! :hae-urakan-sanktiot {:urakka-id urakka-id
                                 :alku alku
                                 :loppu loppu}))

