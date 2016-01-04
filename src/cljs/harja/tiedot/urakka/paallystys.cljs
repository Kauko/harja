(ns harja.tiedot.urakka.paallystys
  "Tämä nimiavaruus hallinnoi urakan päällystystietoja."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.tiedot.navigaatio :as nav]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]))

(defonce paallystysilmoitukset-nakymassa? (atom false))
(defonce paallystys-tai-paikkauskohteet-nakymassa (atom false))

(defn hae-paallystyskohteet [urakka-id sopimus-id]
  (k/post! :urakan-paallystyskohteet {:urakka-id  urakka-id
                                      :sopimus-id sopimus-id}))

(defn hae-paallystyskohdeosat [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystyskohdeosat {:urakka-id          urakka-id
                                        :sopimus-id         sopimus-id
                                        :paallystyskohde-id paallystyskohde-id}))

(defn hae-paallystystoteumat [urakka-id sopimus-id]
  (k/post! :urakan-paallystystoteumat {:urakka-id  urakka-id
                                       :sopimus-id sopimus-id}))

(defn hae-paallystysilmoitus-paallystyskohteella [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystysilmoitus-paallystyskohteella {:urakka-id          urakka-id
                                                           :sopimus-id         sopimus-id
                                                           :paallystyskohde-id paallystyskohde-id}))

(defn tallenna-paallystysilmoitus [urakka-id sopimus-id lomakedata]
  (k/post! :tallenna-paallystysilmoitus {:urakka-id          urakka-id
                                         :sopimus-id         sopimus-id
                                         :paallystysilmoitus lomakedata}))

(defn tallenna-paallystyskohteet [urakka-id sopimus-id kohteet]
  (k/post! :tallenna-paallystyskohteet {:urakka-id  urakka-id
                                        :sopimus-id sopimus-id
                                        :kohteet    kohteet}))

(defn tallenna-paallystyskohdeosat [urakka-id sopimus-id paallystyskohde-id osat]
  (k/post! :tallenna-paallystyskohdeosat {:urakka-id          urakka-id
                                          :sopimus-id         sopimus-id
                                          :paallystyskohde-id paallystyskohde-id
                                          :osat               osat}))

(def paallystyskohderivit (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                           [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                           nakymassa? @paallystys-tai-paikkauskohteet-nakymassa]
                                          (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                                            (hae-paallystyskohteet valittu-urakka-id valittu-sopimus-id))))

(defn paivita-kohde! [id funktio & argumentit]
  (swap! paallystyskohderivit
         (fn [kohderivit]
           (into []
                 (map (fn [kohderivi]
                        (if (= id (:id kohderivi))
                          (apply funktio kohderivi argumentit)
                          kohderivi)))
                 kohderivit))))

(defonce karttataso-paallystyskohteet (atom false))

(defonce paallystystoteumat (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                         [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                         nakymassa? @paallystysilmoitukset-nakymassa?]
                                        (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                                          (hae-paallystystoteumat valittu-urakka-id valittu-sopimus-id))))

(defonce paallystysilmoitus-lomakedata (atom nil)) ; Vastaa rakenteeltaan päällystysilmoitus-taulun sisältöä

(defonce paallystyskohteet-kartalla
         (reaction (let [taso @karttataso-paallystyskohteet
                         kohderivit @paallystyskohderivit
                         toteumarivit @paallystystoteumat
                         avoin-paallystysilmoitus (:paallystyskohde-id @paallystysilmoitus-lomakedata)]
                     (when (and taso
                                (or kohderivit toteumarivit))
                       (into []
                             (mapcat #(keep (fn [{sij :sijainti nimi :nimi :as osa}]
                                              (when sij
                                                (let [paallystyskohde-id (:paallystyskohde_id %)]
                                                  {:type               :paallystys
                                                   :kohde              %
                                                   :paallystyskohde-id paallystyskohde-id
                                                   :tila               (or (:paallystysilmoitus_tila %) (:tila %)) ; Eri keywordissa lähetetystä pyynnöstä riippuen
                                                   :nimi               (str (:nimi %) ": " nimi)
                                                   :osa                osa
                                                   :alue               (assoc sij
                                                                         :stroke {:color (case (or (:paallystysilmoitus_tila %) (:tila %))
                                                                                           :aloitettu "blue"
                                                                                           :valmis "green"
                                                                                           "orange")
                                                                                  :width (if (= paallystyskohde-id avoin-paallystysilmoitus) 8 6)})})))
                                            (:kohdeosat %)))
                             (concat (map #(assoc % :paallystyskohde_id (:id %)) ;; yhtenäistä id kohde ja toteumariveille
                                          kohderivit)
                                     toteumarivit))))))

(defn kuvaile-kohteen-tila [tila]
  (case tila
    :valmis "Valmis"
    :aloitettu "Aloitettu"
    "Ei aloitettu"))