(ns harja.e2e.test-test
  (:require [cemerick.cljs.test :as test :refer-macros [deftest is done]]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.sillat :as sillat]
            [harja.tiedot.urakka.laadunseuranta :as urakka-laadunseuranta]
            [harja.tiedot.urakka.laadunseuranta.tarkastukset :as tarkastukset]
            
            ;; [harja.loki :refer [log]]
            [dommy.core :refer-macros [sel sel1] :as dommy]
            [harja.e2e.testutils :as tu])
  (:require-macros [harja.e2e.macros :refer [wait-reactions]]))

(deftest ^:async e2e-testaus
  (nav/vaihda-sivu! :urakat)
  (tu/muokkaa-atomia (nav/valittu-valilehti-atom :urakat) :siltatarkastukset)
  (wait-reactions [sillat/sillat-kartalla]
                 (is (= (dommy/text (aget (sel [:.grid :tr :td]) 0)) "Oulujoen silta"))
                 (is (= (dommy/text (aget (sel [:.grid :tr :td]) 1)) "902"))))

(deftest ^:async laadunseuranta
  (nav/vaihda-sivu! :urakat)
  (tu/muokkaa-atomia (nav/valittu-valilehti-atom :urakat) :laadunseuranta)
  (tu/muokkaa-atomia (nav/valittu-valilehti-atom :laadunseuranta) :tarkastukset)
  (tu/muokkaa-atomia tarkastukset/tienumero 99)
  (wait-reactions [tarkastukset/urakan-tarkastukset]
                  (is (= (dommy/text (sel1 [:.grid :td])) "Ei tarkastuksia"))))


(deftest ^:async laadunseuranta-toimivalla-tienumerolla
  (nav/vaihda-sivu! :urakat)
  (tu/muokkaa-atomia (nav/valittu-valilehti-atom :urakat) :laadunseuranta)
  (tu/muokkaa-atomia (nav/valittu-valilehti-atom :laadunseuranta) :tarkastukset)
  (tu/muokkaa-atomia tarkastukset/tienumero nil)
  (wait-reactions [tarkastukset/urakan-tarkastukset]
                  (is (= (dommy/text (sel1 [:.grid :td])) "24.8.2015 17:55"))))



