(ns harja.views.urakka.valitavoitteet
  "Ylläpidon urakoiden välitavoitteiden näkymä"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.urakka.valitavoitteet :as vt]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as y]
            [harja.pvm :as pvm]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.fmt :as fmt]
            [cljs-time.core :as t]
            [harja.domain.roolit :as roolit]
            [cljs.core.async :refer [<!]])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))


(defn valmiustilan-kuvaus [{:keys [valmis takaraja]}]
  (if-not takaraja
    "Uusi"
    (let [valmis-pvm (:pvm valmis)]
      (if valmis-pvm
        (str "Valmistunut " (pvm/pvm valmis-pvm))
        (if (t/after? (pvm/nyt) takaraja)
          "Myöhässä"
          "Ei valmis")))))

(defn valitavoite-valmis-lomake [_ ur vt]
  (let [valmis-pvm (atom nil)
        kommentti (atom "")
        tallennus-kaynnissa (atom false)]
    (fn [{aseta-tavoitteet :aseta-tavoitteet} ur vt]
      [:div {:style {:position "relative" }}
       (when @tallennus-kaynnissa
         (y/lasipaneeli (y/keskita (y/ajax-loader))))
       [:form
        [:div.form-group
         [:label {:for "valmispvm"} "Valmistumispäivä"]
         [tee-kentta {:tyyppi :pvm}
          valmis-pvm]]

        [:div.form-group
         [:label {:for "kommentti"} "Kommentti"]
         [:textarea#kommentti.form-control {:on-change #(reset! kommentti (-> % .-target .-value))
                                            :rows 3
                                            :value @kommentti}]]

        [:div.toiminnot
         [:button.btn.btn-default {:disabled (nil? @valmis-pvm)
                                   :on-click #(do (.preventDefault %)
                                                  (reset! tallennus-kaynnissa true)
                                                  (go (when-let [res (<! (vt/merkitse-valmiiksi! (:id ur) (:id vt)
                                                                                                 @valmis-pvm @kommentti))]
                                                        (aseta-tavoitteet res)
                                                        (reset! tallennus-kaynnissa false)))
                                                  (log "merkitään " (pr-str vt) " valmiiksi"))}
          "Merkitse valmiiksi"]]]])))
            
(defn valitavoite-lomake [opts ur vt]
  (let [{:keys [pvm merkitsija merkitty kommentti]} (:valmis vt)]
    [:div.valitavoite
     [:div.valmis
      (when pvm
        [y/rivi
         {:koko y/tietopaneelin-elementtikoko}
       
       
         [y/otsikolla "Valmistunut" (fmt/pvm pvm)]
         
         (when merkitty
           [y/otsikolla "Merkitty valmiiksi"
            [:span (fmt/pvm-opt merkitty) " " (fmt/kayttaja-opt merkitsija)]])
         
         
         (when kommentti
           [y/otsikolla "Urakoitsijan kommentti" kommentti])])]
     
     (when (and (nil? pvm)
                (roolit/rooli-urakassa? roolit/urakoitsijan-urakkaroolit-kirjoitus ur))
       ;; Ei ole valmis, sallitaan urakoitsijan käyttäjän merkitä se valmiiksi
       [valitavoite-valmis-lomake opts ur vt]
         )]))
    
(defn valitavoitteet
  "Urakan välitavoitteet näkymä. Ottaa parametrinä urakan ja hakee välitavoitteet sille."
  [ur]
  (let [tavoitteet (atom nil)
        vaihda-urakka! (fn [ur]
                         (go (reset! tavoitteet (<! (vt/hae-urakan-valitavoitteet (:id ur))))))
        tallennus-kaynnissa (atom false)]
    (vaihda-urakka! ur)
    (komp/luo
     
     {:component-will-receive-props (fn [_ & [_ ur]]
                                      (log "uusi urakka: " (pr-str (dissoc ur :alue)))
                                      (vaihda-urakka! ur))}
     
     (fn [ur]
       [:div.valitavoitteet {:style {:position "relative"}}
        
        (when @tallennus-kaynnissa (y/lasipaneeli (y/keskita (y/ajax-loader))))
        [grid/grid
         {:otsikko "Urakan välitavoitteet"
          :tallenna #(go (reset! tallennus-kaynnissa true)
                         (go
                           (reset! tavoitteet (<! (vt/tallenna! (:id ur) %)))
                           (reset! tallennus-kaynnissa false)))
                         
          :vetolaatikot (into {}
                              (map (juxt :id (partial valitavoite-lomake {:aseta-tavoitteet #(reset! tavoitteet %)} ur)))
                              @tavoitteet)}

         [{:tyyppi :vetolaatikon-tila :leveys "5%"}
          {:otsikko "Nimi" :leveys "55%" :nimi :nimi :tyyppi :string :pituus-max 128}
          {:otsikko "Takaraja" :leveys "20%" :nimi :takaraja :fmt pvm/pvm :tyyppi :pvm}
          {:otsikko "Tila" :leveys "25%" :tyyppi :string :muokattava? (constantly false)
           :nimi :valmiustila :hae identity :fmt valmiustilan-kuvaus}]
         @tavoitteet]


        [grid/grid
         {:otsikko "Kohteiden välitavoitteet"
          :tunniste :yha-id
          :tallenna #(go (reset! tallennus-kaynnissa true)
                         (go
                           (reset! tavoitteet (<! (vt/tallenna! (:id ur) %)))
                           (reset! tallennus-kaynnissa false)))

          :vetolaatikot (into {}
                              (map (juxt :id (partial valitavoite-lomake {:aseta-tavoitteet #(reset! tavoitteet %)} ur)))
                              @tavoitteet)}

         [{:tyyppi :vetolaatikon-tila :leveys "5%"}
          {:otsikko "YHA-ID" :leveys "15%" :nimi :yha-id :tyyppi :string :pituus-max 128 :muokattava? (constantly false)}
          {:otsikko "Kohde" :leveys "60%" :nimi :kohde :tyyppi :string :muokattava? (constantly false)}
          {:otsikko "Tila" :leveys "20%" :tyyppi :string :muokattava? (constantly false)
           :nimi :tila }]
         [{:yha-id 1 :kohde "Mt 22 Ruohonjuuren pätkä" :tila "Kaikki valmiina"}
          {:yha-id 2 :kohde "Mt 22 Terilän silta" :tila "Kaikki valmiina"}
          {:yha-id 3 :kohde "Mt 22 Matulan  pätkä" :tila "Kohde kesken"}
          {:yha-id 4 :kohde "Mt 22 koskenlaskijan kuru" :tila "Kohde kesken"}
          {:yha-id 5 :kohde "Mt 22 rampit" :tila "Kaikki valmiina"}
          ]]]))))