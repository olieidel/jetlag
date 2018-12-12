(ns jetlag.events
  (:require [jetlag.db :as db]
            [re-frame.core :as rf :refer [debug reg-event-db reg-event-fx trim-v]]))


(def ^:private interceptors
  [debug trim-v])

(reg-event-fx
 ::boot
 interceptors
 (fn [_ _]
   {:db db/default-db}))

(reg-event-db
 ::invert-time-zone-delta
 interceptors
 (fn [db [time-zone-delta]]
   (update-in db [:input :time-zone-delta]
              #(if (pos? %) (- % 24) (+ % 24)))))

(reg-event-db
 ::set-time-zone-delta
 interceptors
 (fn [db [time-zone-delta]]
   (assoc-in db [:input :time-zone-delta] time-zone-delta)))

(reg-event-db
 ::set-bed-time
 interceptors
 (fn [db [{:keys [hour minute]}]]
   (-> db
       (assoc-in [:input :bed-time-hour] hour)
       (assoc-in [:input :bed-time-minute] minute))))

(reg-event-db
 ::set-sleep-duration
 interceptors
 (fn [db [hours]]
   (assoc-in db [:input :sleep-duration-hours] hours)))

(reg-event-db
 ::use-light-box?
 interceptors
 (fn [db [use-light-box?]]
   (assoc-in db [:input :use-light-box?] use-light-box?)))

(reg-event-db
 ::set-melatonin-dose-mg
 interceptors
 (fn [db [melatonin-dose-mg]]
   (assoc-in db [:input :melatonin-dose-mg] melatonin-dose-mg)))
