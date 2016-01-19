(ns harja.views.haku
  "Harjan haku"
  (:require [reagent.core :refer [atom] :as r]
            [clojure.string :as str]

            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.listings :refer [suodatettu-lista]]
            [harja.ui.modal :refer [modal] :as modal]
            [harja.ui.yleiset :refer [tietoja kaksi-palstaa-otsikkoja-ja-arvoja]]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.navigaatio :as nav]
            [harja.atom :refer-macros [reaction<!]])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))


(def hakutermi (atom ""))

(def hakutulokset
  (reaction<! [termi @hakutermi]
              {:odota 500
               :nil-kun-haku-kaynnissa? true}
              (when (> (count termi) 1)
                (k/post! :hae termi))))

(defn nayta-organisaation-yhteystiedot
  [o]
  (modal/nayta! {:otsikko (:nimi o)
                 :luokka  "yhteystieto"
                 :footer  [:span
                           [:button.nappi-toissijainen {:type     "button"
                                                        :on-click #(do (.preventDefault %)
                                                                       (modal/piilota!))}
                            "Sulje"]]}
                [:div.kayttajan-tiedot
                 [tietoja {}
                  "Org. tyyppi:" (name (:tyyppi o))
                  "Y-tunnus:" (:ytunnus o)
                  "Osoite" (:katuosoite o)
                  "Postinumero" (:postinumero o)
                  "Sampoid:" (or (:sampoid o) "Ei annettu")
                  (if (= (:tyyppi o) :hallintayksikko)
                    "Liikennemuoto:" (case (:liikennemuoto o)
                                       "T" "Tie"
                                       "V" "Vesi"
                                       "R" "Rata"
                                       "Ei annettu"))]
                 (when-let [urakat (:urakat o)]
                   [:span
                    [:span.tietokentta (if (= :urakoitsija (:tyyppi o))
                                         "Urakoitsijana urakoissa:"
                                         "Tilaajana urakoissa:")]
                    [:div.mukana-urakoissa
                     (if (empty? urakat)
                       "Ei urakoita"
                       (for [u urakat]
                         ^{:key (:nimi u)}
                         [:li.tietoarvo (:nimi u)]))]])]))

(defn valitse-organisaatio
  [o]
  (if (= :urakoitsija (:tyyppi o))
    (nayta-organisaation-yhteystiedot o)
    (nav/valitse-hallintayksikko o)))

(defn nayta-kayttaja
  [k]
  (modal/nayta! {:otsikko (str (:etunimi k) " " (:sukunimi k))
                 :luokka  "yhteystieto"
                 :footer  [:span
                           [:button.nappi-toissijainen {:type     "button"
                                                        :on-click #(do (.preventDefault %)
                                                                       (modal/piilota!))}
                            "Sulje"]]}
                [:div.kayttajan-tiedot
                 [kaksi-palstaa-otsikkoja-ja-arvoja {}
                  "Organisaatio:" [:a.klikattava {:on-click #(do (.preventDefault %)
                                                                 (modal/piilota!)
                                                                 (valitse-organisaatio (:organisaatio k)))}
                                   (name (get-in k [:organisaatio :nimi]))]
                  "Org. tyyppi:" (name (get-in k [:organisaatio :tyyppi]))
                  "Käyttäjänimi:" (get k :kayttajanimi)
                  "Puhelin:" (get k :puhelin)
                  "Sähköposti:" [:a {:href (str "mailto:" (get k :sahkoposti))}
                                 (get k :sahkoposti)]

                  "Roolit:" (if (not-empty (get k :roolit))
                              (str/join ", " (get k :roolit))
                              "Ei rooleja")]
                 (when-let [urakkaroolit (get k :urakka-roolit)]
                   [:span
                    [:span.tietokentta "Mukana urakoissa:"]
                    [:div.mukana-urakoissa
                     (if (empty? urakkaroolit)
                       "Ei urakkarooleja"
                       (for [urakkarooli urakkaroolit]
                         ^{:key (get-in urakkarooli [:urakka :id])}
                         [:div.tietorivi [:div.tietokentta (str (get-in urakkarooli [:urakka :nimi]))]
                          [:span.tietoarvo.rooli (get urakkarooli :rooli)]]))]])]))

(defn valitse-hakutulos
  [tulos]
  (reset! hakutermi "")
  (go (when-let [valitun-tyyppi (:tyyppi tulos)]
        (case valitun-tyyppi
          :urakka
          (let [haettu-urakka (<! (k/post! :hae-urakka (:id tulos)))]
            (nav/aseta-hallintayksikko-ja-urakka
              (get-in haettu-urakka [:hallintayksikko :id])
              (:id haettu-urakka)))
          :kayttaja (let [haettu-kayttaja (<! (k/post! :hae-kayttajan-tiedot (:id tulos)))]
                      (nayta-kayttaja haettu-kayttaja))
          :organisaatio
          (let [haettu-organisaatio (<! (k/post! :hae-organisaatio (:id tulos)))]
            (valitse-organisaatio haettu-organisaatio))))))

(defn liikaa-osumia?
  [tulokset]
  (when-let [ryhmitellyt (vals (group-by :tyyppi tulokset))]
    (some #(> (count %) 10) ryhmitellyt)))

(defn haku
  []
  (let [tulokset @hakutulokset
        termi @hakutermi]
    [:form.navbar-form.navbar-left {:role "search"}
     [:div.form-group.haku
      [suodatettu-lista {:format         :hakusanat
                         :haku           :hakusanat
                         :term           (r/wrap termi (fn [uusi-termi]
                                                         (reset! hakutermi
                                                                 (str/triml (str/replace uusi-termi #"\s{2,}" " ")))))
                         :ryhmittely     :tyyppi
                         :ryhman-otsikko #(case %
                                           :urakka "Urakat"
                                           :kayttaja "Käyttäjät"
                                           :organisaatio "Organisaatiot"
                                           "Muut")
                         :on-select      #(valitse-hakutulos %)
                         :aputeksti      "Hae Harjasta"
                         :tunniste       #((juxt :tyyppi :id) %)
                         :vinkki         #(if (liikaa-osumia? tulokset)
                                           "Paljon osumia, tarkenna hakua..."
                                           (when (= [] tulokset)
                                             (str "Ei tuloksia haulla " termi)))}
       tulokset]]]))
