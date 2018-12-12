(ns jetlag.subs
  (:require [jetlag.engine :as engine]
            [re-frame.core :as rf :refer [reg-sub subscribe]]
            [clojure.set :as set]))

(reg-sub
 ::bed-time-str
 (fn [db]
   (let [{:keys [bed-time-hour bed-time-minute]} (:input db)]
     (str (cond->> bed-time-hour (< bed-time-hour 10) (str "0"))
          ":"
          (cond->> bed-time-minute (< bed-time-minute 10) (str "0"))))))

(reg-sub
 ::melatonin-dose-mg
 (fn [db] (get-in db [:input :melatonin-dose-mg])))

(reg-sub
 ::sleep-duration-hours
 (fn [db] (get-in db [:input :sleep-duration-hours])))

(reg-sub
 ::time-zone-delta
 (fn [db] (get-in db [:input :time-zone-delta])))

(reg-sub
 ::use-light-box?
 (fn [db] (get-in db [:input :use-light-box?])))

(reg-sub
 ::input
 (fn [db] (:input db)))

(reg-sub
 ::schedule
 (fn [_query-v _]
   (subscribe [::input]))
 (fn [input _]
   ;; ensure that all keys have been set, i.e. don't provide a
   ;; schedule if input is missing
   ;;
   ;; TODO: it should actually handle partial input better than this
   (when (empty? (set/difference
                  #{:time-zone-delta :bed-time-hour :bed-time-minute
                    :sleep-duration-hours}
                  (-> input keys set)))
     (engine/generate-schedule input))))

(reg-sub
 ::inverse-schedule
 ;; this generates a schedule but "inverts" `time-zone-delta`,
 ;; i.e. when I fly to Bangkok (+6hrs from Berlin), I don't set
 ;; `time-zone-delta` to 6, but instead to 24-6 = 18. This can be
 ;; beneficial as we humans can phase delay (sleep later) for more
 ;; hrs/day than phase advance (sleep earlier)
 (fn [_query-v _]
   (subscribe [::input]))
 (fn [input _]
   ;; ensure that all keys have been set, i.e. don't provide a
   ;; schedule if input is missing
   ;;
   ;; TODO: it should actually handle partial input better than this
   (when (empty? (set/difference
                  #{:time-zone-delta :bed-time-hour :bed-time-minute
                    :sleep-duration-hours}
                  (-> input keys set)))
     (engine/generate-schedule (update input :time-zone-delta
                                       #(if (pos? %) (- % 24) (+ % 24)))))))

(reg-sub
 ::consider-inverse-schedule?
 ;; this is cool: if it's actually shorter to adapt "the other way
 ;; round" to a new time zone, consider doing that instead!
 (fn [_query-v _]
   [(subscribe [::schedule]) (subscribe [::inverse-schedule])])
 (fn [[schedule inverse-schedule] _]
   (let [schedule-length (count schedule)
         inverse-schedule-length (count inverse-schedule)]
     (> schedule-length inverse-schedule-length))))
