(ns harja.palvelin.integraatiot.sampo.sanomat.maksuera_sanoma
  (:require [hiccup.core :refer [html]]
            [clojure.string :as str]
            [harja.pvm :as pvm])
  (:import (java.util Date Calendar)))

(defn muodosta-kulu-id []
  (str/join "" ["kulu"
                (let [calendar (Calendar/getInstance)]
                  (.setTime calendar (Date.))
                  (.get calendar Calendar/YEAR))]))

(defn muodosta-maksueranumero [numero]
  (str/join "" ["HA" numero]))

(defn muodosta-instance-code [numero]
  (str/join "" ["AL" numero]))

(defn luo-custom-information [values & content]
  [:CustomInformation
   (for [[key value] values]
     [:ColumnValue {:name key} value])
   content])

(defn paattele-tyyppi [tyyppi]
  (case tyyppi
    "yksikkohintainen" 6
    "kokonaishintainen" 2
    "lisatyo" 6
    "indeksi" 7
    "bonus" 8
    "sakko" 9
    "akillinen-hoitotyo" 10
    99))

(defn muodosta [maksuera]
  (let [{:keys [alkupvm loppupvm vastuuhenkilo talousosasto talousosastopolku tuotepolku sampoid]} (:toimenpideinstanssi maksuera)
        maksueranumero (muodosta-maksueranumero (:numero maksuera))
        kulu-id (muodosta-kulu-id)
        instance-code (muodosta-instance-code (:numero maksuera))]

    [:NikuDataBus
     [:Header {:objectType "product" :action "write" :externalSource "NIKU" :version "8.0"}]
     [:Products
      [:Product {:name                  (apply str (take 80 (or (:nimi (:maksuera maksuera)) "N/A")))
                 :financialProjectClass "INVCLASS"
                 :start                 (pvm/aika-iso8601 alkupvm)
                 :finish                (.replace (pvm/aika-iso8601 loppupvm) "00:00:00.0" "17:00:00.0")
                 :financialWipClass     "WIPCLASS"
                 :financialDepartment   talousosasto
                 :managerUserName       vastuuhenkilo
                 :objectID              maksueranumero
                 :financialLocation     "Kpito"}
       [:InvestmentAssociations
        [:Allocations
         [:ParentInvestment {:defaultAllocationPercent "1.0"
                             :InvestmentType           "project"
                             :InvestmentID             sampoid}]]]
       [:InvestmentResources
        [:Resource {:resourceID kulu-id}]]
       [:InvestmentTasks
        [:Task {:outlineLevel "1"
                :name         (:nimi (:maksuera maksuera))
                :taskID       "~rmw"}
         [:Assignments
          [:TaskLabor {:resourceID kulu-id}]]]]
       [:OBSAssocs {:completed "false"}
        [:OBSAssoc#LiiviKP {:unitPath talousosastopolku
                            :name     "Kustannuspaikat"}]
        [:OBSAssoc#LiiviSIJ {:unitPath "/Kirjanpito"
                             :name     "Sijainti"}]
        [:OBSAssoc#tuote2013 {:unitPath tuotepolku
                              :name     "Tuoteryhma/Tuote"}]]
       (luo-custom-information {"vv_tilaus"      (:sampoid (:sopimus maksuera))
                                "vv_inst_no"     (:numero maksuera)
                                "vv_code"        maksueranumero
                                "vv_me_type"     (paattele-tyyppi (:tyyppi (:maksuera maksuera)))
                                "vv_type"        "me"
                                "vv_status"      "2"
                                "travel_cost_ok" "false"}
                               [:instance {:parentInstanceCode maksueranumero
                                           :parentObjectCode   "Product"
                                           :objectCode         "vv_invoice_receipt"
                                           :instanceCode       instance-code}
                                (luo-custom-information {"code"                 instance-code
                                                         ;; PENDING: Taloushallinnosta pitää kertoa mikä on oikea maksupäivä.
                                                         ;; Nyt maksuerät ovat koko urakan ajan kestoisia.
                                                         "vv_payment_date"      (pvm/aika-iso8601 (Date.))
                                                         "vv_paym_sum"          (:summa (:maksuera maksuera))
                                                         "vv_paym_sum_currency" "EUR"
                                                         "name"                 "Laskutus- ja maksutiedot"})])]]]))
