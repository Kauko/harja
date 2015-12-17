(ns harja.palvelin.index
  (:require [hiccup.core :refer [html]])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [java.util Base64]))

(def anti-forgery-secret-key "d387gcsb8137hd9h192hdijsha9hd91hdiubisab98f7g7812g8dfheiqufhsaiud8713")

(defn laske-mac [data]
  (let [secret-key (SecretKeySpec. (.getBytes anti-forgery-secret-key "UTF-8") "HmacSHA256")
        mac (Mac/getInstance "HmacSHA256")]
    (.init mac secret-key)
    (String. (.encode (Base64/getEncoder) (.doFinal mac (.getBytes data "UTF-8"))))))

(defn tee-random-avain []
  (apply str (map (fn [_] (rand-nth "0123456789abcdefghijklmnopqrstuvwyz")) (range 128))))

(defn tee-paasivu [token devmode]
  (html
   (if devmode
     [:html
      [:head
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:link {:href "//fonts.googleapis.com/css?family=Open+Sans:400,700" :rel "stylesheet" :type "text/css"}]
       [:link {:rel "stylesheet/less" :type "text/css" :href "less/application/application.less"}]
       [:link {:rel "icon" :type "image/png" :href "images/harja_favicon.png"}]
       [:script {:type "text/javascript" :src "js/less-2.5.0.js"}]
       [:script {:type "text/javascript" :src "js/proj4.js"}]
       [:script {:type "text/javascript"}
        "proj4.defs(\"urn:x-ogc:def:crs:EPSG:3067\", \"+proj=utm +zone=35 +ellps=GRS80 +units=m +no_defs\");"
        "proj4.defs(\"EPSG:3067\", proj4.defs(\"urn:x-ogc:def:crs:EPSG:3067\"));"]]
      [:body {:onload "harja.asiakas.main.harja()" :data-anti-csrf-token token}
       [:div#app]
       [:script {:src "js/out/goog/base.js" :type "text/javascript"}]
       [:script {:src "js/harja.js" :type "text/javascript"}]
       [:script {:type "text/javascript"}
        "goog.require(\"harja.asiakas.main\");"]]]

     [:html
      [:head
       [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:link {:href "//fonts.googleapis.com/css?family=Open+Sans:400,700" :rel "stylesheet" :type "text/css"}]
       [:link {:href "css/application.css" :rel "stylesheet" :type "text/css"}]
       [:link {:rel "icon" :type "image/png" :href "images/harja_favicon.png"}]
       [:script {:type "text/javascript" :src "js/harja.js"}]
       [:script {:type "text/javascript" :src "js/proj4.js"}]
       [:script {:type "text/javascript"}
        "proj4.defs(\"urn:x-ogc:def:crs:EPSG:3067\", \"+proj=utm +zone=35 +ellps=GRS80 +units=m +no_defs\");"
        "proj4.defs(\"EPSG:3067\", proj4.defs(\"urn:x-ogc:def:crs:EPSG:3067\"));"]]
      [:body {:onload "harja.asiakas.main.harja()" :data-anti-csrf-token token}
       [:div#app]]])))
