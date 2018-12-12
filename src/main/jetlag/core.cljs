(ns jetlag.core
  (:require [re-frame.core :as rf]
            [jetlag.views :as views]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [jetlag.events :as events]))


(defn dev-setup []
  (enable-console-print!))

(defn mount-root []
  (r/render [views/main] (gdom/getElement "app")))

(defn on-js-reload []
  (rf/clear-subscription-cache!)
  (mount-root))

(defonce _init?
  (do
    (rf/dispatch-sync [::events/boot])
    (on-js-reload)))
