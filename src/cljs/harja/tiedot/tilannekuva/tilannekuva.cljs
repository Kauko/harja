(ns harja.tiedot.tilannekuva.tilannekuva
  (:require [reagent.core :refer [atom]]
            [cljs.core.async :refer [<!]]
            [harja.loki :refer [log tarkkaile!]]
            [harja.asiakas.tapahtumat :as tapahtumat]
            [harja.asiakas.kommunikaatio :as k]
            [harja.views.kartta :as kartta]
            [harja.tiedot.tilannekuva.tilannekuva-kartalla :as tilannekuva-kartalla]
            [harja.atom :refer-macros [reaction<!] :refer [paivita-periodisesti]]
            [harja.pvm :as pvm]
            [cljs-time.core :as t]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
            [harja.tiedot.navigaatio :as nav])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(defonce nakymassa? (atom false))
(defonce valittu-tila (atom :nykytilanne))

(defonce bufferi 500)
(defonce hakutiheys 3000)

;; Jokaiselle suodattimelle teksti, jolla se esitetään käyttöliittymässä
(defonce suodattimien-nimet
         {:laatupoikkeamat                  "Laatupoikkeamat"
          :tarkastukset                     "Tarkastukset"
          :turvallisuuspoikkeamat           "Turvallisuuspoikkeamat"

          :toimenpidepyynto                 "TPP"
          :tiedoitus                        "TUR"
          :kysely                           "URK"

          :paallystys                       "Päällystystyöt"
          :paikkaus                         "Paikkaustyöt"

          "auraus ja sohjonpoisto"          "Auraus ja sohjonpoisto"
          "suolaus"                         "Suolaus"
          "pistehiekoitus"                  "Pistehiekoitus"
          "linjahiekoitus"                  "Linjahiekoitus"
          "lumivallien madaltaminen"        "Lumivallien madaltaminen"
          "sulamisveden haittojen torjunta" "Sulamisveden haittojen torjunta"
          "kelintarkastus"                  "Kelintarkastus"

          "tiestotarkastus"                 "Tiestötarkastus"
          "koneellinen niitto"              "Koneellinen niitto"
          "koneellinen vesakonraivaus"      "Koneellinen vesakonraivaus"

          "liikennemerkkien puhdistus"      "Liikennemerkkien puhdistus"

          "sorateiden muokkaushoylays"      "Sorateiden muokkaushöyläys"
          "sorateiden polynsidonta"         "Sorateiden pölynsidonta"
          "sorateiden tasaus"               "Sorateiden tasaus"
          "sorastus"                        "Sorastus"

          "harjaus"                         "Harjaus"
          "pinnan tasaus"                   "Pinnan tasaus"
          "paallysteiden paikkaus"          "Päällysteiden paikkaus"
          "paallysteiden juotostyot"        "Päällysteiden juotostyöt"

          "siltojen puhdistus"              "Siltojen puhdistus"

          "l- ja p-alueiden puhdistus"      "L- ja P-alueiden puhdistus"
          "muu"                             "Muu"})

;; Kartassa säilötään suodattimien tila, valittu / ei valittu.
(def suodattimet (atom {:yllapito       {:paallystys true
                                         :paikkaus   true}
                        :ilmoitukset    {:tyypit {:toimenpidepyynto true
                                                  :kysely           true
                                                  :tiedoitus        true}
                                         :tilat  #{:avoimet}} ; TODO Historiakuvassa näytetään avoimet ja suljetut
                        :turvallisuus   {:turvallisuuspoikkeamat true}
                        :laadunseuranta {:laatupoikkeamat true
                                         :tarkastukset    true}

                        ;; Näiden pitää osua työkoneen enumeihin
                        :talvi          {"auraus ja sohjonpoisto"          true
                                         "suolaus"                         true
                                         "pistehiekoitus"                  true
                                         "linjahiekoitus"                  true
                                         "lumivallien madaltaminen"        true
                                         "sulamisveden haittojen torjunta" true
                                         "kelintarkastus"                  true
                                         "muu"                             true}

                        :kesa           {"tiestotarkastus"            true
                                         "koneellinen niitto"         true
                                         "koneellinen vesakonraivaus" true

                                         "liikennemerkkien puhdistus" true

                                         "sorateiden muokkaushoylays" true
                                         "sorateiden polynsidonta"    true
                                         "sorateiden tasaus"          true
                                         "sorastus"                   true

                                         "harjaus"                    true
                                         "pinnan tasaus"              true
                                         "paallysteiden paikkaus"     true
                                         "paallysteiden juotostyot"   true

                                         "siltojen puhdistus"         true

                                         "l- ja p-alueiden puhdistus" true
                                         "muu"                        true}}))

(defn- tunteja-vuorokausissa [vuorokaudet]
  (* 24 vuorokaudet))

(defn- tunteja-viikoissa [viikot]
  "Palauttaa montako tuntia on n viikossa."
  (tunteja-vuorokausissa (* 7 viikot)))

;; Mäppi sisältää numeroarvot tekstuaaliselle esitykselle.
(defonce nykytilanteen-aikasuodatin-tunteina [["0-2h" 2]
                                              ["0-4h" 4]
                                              ["0-12h" 12]
                                              ["1 vrk" (tunteja-vuorokausissa 1)]
                                              ["2 vrk" (tunteja-vuorokausissa 2)]
                                              ["3 vrk" (tunteja-vuorokausissa 3)]
                                              ["1 vk" (tunteja-viikoissa 1)]
                                              ["2 vk" (tunteja-viikoissa 2)]
                                              ["3 vk" (tunteja-viikoissa 3)]])

(defonce historiakuvan-aikavali (atom (pvm/kuukauden-aikavali (pvm/nyt)))) ;; Valittu aikaväli vektorissa [alku loppu]
(defonce nykytilanteen-aikasuodattimen-arvo (atom 2))

(defonce tilannekuvan-asiat-kartalla
         (reaction
           @tilannekuva-kartalla/haetut-asiat
           (when @tilannekuva-kartalla/karttataso-tilannekuva
             (kartalla-esitettavaan-muotoon
               (concat (vals (:tyokoneet @tilannekuva-kartalla/haetut-asiat))
                       (apply concat (vals (dissoc @tilannekuva-kartalla/haetut-asiat :tyokoneet))))))))

(defn kasaa-parametrit []
  (merge
    {:hallintayksikko @nav/valittu-hallintayksikko
     :urakka-id       (:id @nav/valittu-urakka)
     :urakoitsija     (:id @nav/valittu-urakoitsija)
     :urakkatyyppi    (:arvo @nav/valittu-urakkatyyppi)
     :alue            @nav/kartalla-nakyva-alue
     :alku            (if (= @valittu-tila :nykytilanne)
                        (t/minus (pvm/nyt) (t/hours @nykytilanteen-aikasuodattimen-arvo))
                        (first @historiakuvan-aikavali))
     :loppu           (if (= @valittu-tila :nykytilanne)
                        (pvm/nyt)
                        (second @historiakuvan-aikavali))}
    @suodattimet))

(defn yhdista-tyokonedata [uusi]
  (let [vanhat (:tyokoneet @tilannekuva-kartalla/haetut-asiat)
        uudet (:tyokoneet uusi)]
    (assoc uusi :tyokoneet
                (merge-with
                  (fn [vanha uusi]
                    (let [vanha-reitti (:reitti vanha)]
                      (assoc uusi :reitti (if (= (:sijainti vanha) (:sijainti uusi))
                                            vanha-reitti
                                            (conj
                                              (or vanha-reitti [(:sijainti vanha)])
                                              (:sijainti uusi))))))
                  vanhat uudet))))

(def edellisen-haun-kayttajan-suodattimet (atom {:tila @valittu-tila
                                                 :aikavali-nykytilanne @nykytilanteen-aikasuodattimen-arvo
                                                 :aikavali-historia @historiakuvan-aikavali
                                                 :suodattimet @suodattimet} ))
(defn hae-asiat []
  (log "Tilannekuva: Hae asiat (" (pr-str @valittu-tila) ")")
  (go
    ;; Asetetaan kartalle "Päivitetään karttaa" viesti jos haku tapahtui käyttäjän vaihdettua suodattimia
    (when (or (not= @valittu-tila(:tila @edellisen-haun-kayttajan-suodattimet))
              (not= @nykytilanteen-aikasuodattimen-arvo (:aikavali-nykytilanne @edellisen-haun-kayttajan-suodattimet))
              (not= @historiakuvan-aikavali (:aikavali-historia @edellisen-haun-kayttajan-suodattimet))
              (not= @suodattimet (:suodattimet @edellisen-haun-kayttajan-suodattimet)))
      (reset! edellisen-haun-kayttajan-suodattimet {:tila @valittu-tila
                                                    :aikavali-nykytilanne @nykytilanteen-aikasuodattimen-arvo
                                                    :aikavali-historia @historiakuvan-aikavali
                                                    :suodattimet @suodattimet})
      (kartta/aseta-paivitetaan-karttaa-tila true))
    (let [yhteiset-parametrit (kasaa-parametrit)
          julkaise-tyokonedata! (fn [tulos]
                                  (tapahtumat/julkaise! {:aihe      :uusi-tyokonedata
                                                         :tyokoneet (:tyokoneet tulos)})
                                  tulos)
          lisaa-karttatyypit (fn [tulos]
                               (as-> tulos t
                                     (assoc t :ilmoitukset
                                              (map #(assoc % :tyyppi-kartalla (:ilmoitustyyppi %))
                                                   (:ilmoitukset t)))
                                     (assoc t :turvallisuuspoikkeamat
                                              (map #(assoc % :tyyppi-kartalla :turvallisuuspoikkeama)
                                                   (:turvallisuuspoikkeamat t)))
                                     (assoc t :tarkastukset
                                              (map #(assoc % :tyyppi-kartalla :tarkastus)
                                                   (:tarkastukset t)))
                                     (assoc t :laatupoikkeamat
                                              (map #(assoc % :tyyppi-kartalla :laatupoikkeama)
                                                   (:laatupoikkeamat t)))
                                     (assoc t :paikkaus
                                              (map #(assoc % :tyyppi-kartalla :paikkaus)
                                                   (:paikkaus t)))
                                     (assoc t :paallystys
                                              (map #(assoc % :tyyppi-kartalla :paallystys)
                                                   (:paallystys t)))

                                     ;; Tyokoneet on mäp, id -> työkone
                                     (assoc t :tyokoneet (into {}
                                                               (map
                                                                 (fn [[id tyokone]]
                                                                   {id (assoc tyokone :tyyppi-kartalla :tyokone)})
                                                                 (:tyokoneet t))))

                                     (assoc t :toteumat
                                              (map #(assoc % :tyyppi-kartalla :toteuma)
                                                   (:toteumat t)))))

          tulos (-> (<! (k/post! :hae-tilannekuvaan yhteiset-parametrit))
                    (yhdista-tyokonedata)
                    (julkaise-tyokonedata!)
                    (lisaa-karttatyypit))]
      (reset! tilannekuva-kartalla/haetut-asiat tulos)
      (kartta/aseta-paivitetaan-karttaa-tila false))))

(def asioiden-haku (reaction<!
                     [_ @valittu-tila
                      _ @suodattimet
                      _ @nykytilanteen-aikasuodattimen-arvo
                      _ @historiakuvan-aikavali
                      _ @nav/kartalla-nakyva-alue
                      _ @nav/valittu-urakka
                      nakymassa? @nakymassa?
                      _ @nav/valittu-hallintayksikko]
                     {:odota bufferi}
                     (when nakymassa?
                       (hae-asiat))))

(defonce lopeta-haku (atom nil)) ;; Säilöö funktion jolla pollaus lopetetaan

(defonce pollaus
         (run! (if @nakymassa?
                 (do
                   (when @lopeta-haku (@lopeta-haku))
                   (log "Tilannekuva: Aloitetaan haku (tai päivitetään tiheyttä)")
                   (reset! lopeta-haku (paivita-periodisesti asioiden-haku hakutiheys (fn []
                                                                                         (= :nykytilanne @valittu-tila)))))

                 (when @lopeta-haku (do
                                      (@lopeta-haku)
                                      (log "Tilannekuva: Lopetetaan haku")
                                      (reset! lopeta-haku nil))))))