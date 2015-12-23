(ns harja.views.main
  "Harjan päänäkymä"
  (:require [harja.ui.bootstrap :as bs]
            [reagent.core :refer [atom]]
            [harja.tiedot.istunto :as istunto]
            [harja.ui.komponentti :as komp]
            [harja.ui.listings :refer [suodatettu-lista]]
            [harja.ui.yleiset :refer [linkki elementti-idlla sijainti] :as yleiset]
            [harja.ui.modal :refer [modal-container]]
            [harja.ui.viesti :refer [viesti-container]]
            [harja.tiedot.navigaatio :as nav]
            [harja.loki :refer [log logt]]
            [harja.views.murupolku :as murupolku]
            [harja.views.haku :as haku]

            [harja.views.urakat :as urakat]
            [harja.views.raportit :as raportit]
            [harja.views.tilannekuva.tilannekuva :as tilannekuva]
            [harja.views.ilmoitukset :as ilmoitukset]
            [harja.views.kartta :as kartta]
            [harja.views.hallinta :as hallinta]
            [harja.views.about :as about]
            [harja.virhekasittely :as virhekasittely]))

(defn kayttajatiedot [kayttaja]
  (let [{:keys [etunimi sukunimi]} @kayttaja
        kayttajainfo [:a {:href "#" :on-click #(do
                                  (.preventDefault %)
                                  (haku/nayta-kayttaja @kayttaja))}
       etunimi " " sukunimi]]
    (if-not (istunto/testikaytto-mahdollista?)
      kayttajainfo
      
      (let [testikayttaja @istunto/testikayttaja]
        [:span
         (if testikayttaja
           [:span.alert-warning "TESTIKÄYTTÖ"]
           kayttajainfo)
         [yleiset/livi-pudotusvalikko {:valinta testikayttaja
                                       :class      "testikaytto-alasveto"
                                       :title "Järjestelmän vastuuhenkilönä voit testata Harjaa myös muissa rooleissa."
                                       :format-fn #(if %
                                                     (:kuvaus %)
                                                     (str "- Ei testikäyttäjänä -"))
                                       :valitse-fn istunto/aseta-testikayttaja!}
          (concat [nil] @istunto/testikayttajat)]]))))


(defn header [s]
  [bs/navbar {}
   [:img#harja-brand-icon {:alt      "HARJA"
                           :src      "images/harja_logo_soft.svg"
                           :on-click #(.reload js/window.location)}]
   [haku/haku]

   [:ul#sivut.nav.nav-pills

    [:li {:role "presentation" :class (when (= s :urakat) "active")}
     [linkki "Urakat" #(nav/vaihda-sivu! :urakat)]]

    [:li {:role "presentation" :class (when (= s :raportit) "active")}
     [linkki "Raportit" #(nav/vaihda-sivu! :raportit)]]

    [:li {:role "presentation" :class (when (= s :tilannekuva) "active")}
     [linkki "Tilannekuva" #(nav/vaihda-sivu! :tilannekuva)]]

    [:li {:role "presentation" :class (when (= s :ilmoitukset) "active")}
     [linkki "Ilmoitukset" #(nav/vaihda-sivu! :ilmoitukset)]]

    [:li {:role "presentation" :class (when (= s :hallinta) "active")}
     [linkki "Hallinta" #(nav/vaihda-sivu! :hallinta)]]]
   :right
   [kayttajatiedot istunto/kayttaja]])

(defn ladataan []
  [:div {:style {:position "absolute" :top "50%" :left "50%"}}
   [:div {:style {:position "relative" :left "-50px" :top "-20px"}}
    [yleiset/ajax-loader "Ladataan..." {:luokka "ladataan-harjaa"}]]])

(defn main
  "Harjan UI:n pääkomponentti"
  []
  (komp/luo

    (fn []
      (if @nav/render-lupa?
        (let [sivu @nav/sivu
              aikakatkaistu? @istunto/istunto-aikakatkaistu
              korkeus @yleiset/korkeus
              kayttaja @istunto/kayttaja]

          (if aikakatkaistu?
            [:div "Harjan käyttö aikakatkaistu kahden tunnin käyttämättömyyden takia. Lataa sivu uudelleen."]
            (if (nil? kayttaja)
              [ladataan]
              (if (or (:poistettu kayttaja)
                      (empty? (:roolit kayttaja)))
                [:div.ei-kayttooikeutta "Ei Harja käyttöoikeutta. Ota yhteys pääkäyttäjään."]

                [:div
                 [:div.container
                  [header sivu]]

                 [:div.container
                  [murupolku/murupolku]]



                 [:div.container.sisalto {:style {:min-height (max 200 (- korkeus 220))}} ; contentin minimikorkeus pakottaa footeria alemmas
                  [:div.row.row-sisalto
                   [:div {:class (when-not (= sivu :tilannekuva) "col-sm-12")}
                    (case sivu
                      :urakat [urakat/urakat]
                      :raportit [raportit/raportit]
                      :ilmoitukset [ilmoitukset/ilmoitukset]
                      :hallinta [hallinta/hallinta]
                      :tilannekuva [tilannekuva/tilannekuva]
                      :about [about/about])]]]



                 [modal-container]
                 [viesti-container]

                 ;; kartta luodaan ja liitetään DOM:iin tässä. Se asemoidaan muualla #kartan-paikka divin avulla
                 ;; asetetaan alkutyyli siten, että kartta on poissa näkyvistä, jos näkymässä on kartta,
                 ;; se asemoidaan mountin jälkeen
                 [:div#kartta-container {:style {:position "absolute" :top (- @yleiset/korkeus)}}
                  [kartta/kartta]]]))))
        [ladataan]))))

