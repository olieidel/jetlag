(ns jetlag.engine
  "The engine (heh) of this application.

  Computes schedules and recommendations for adapting to different
  time zones.

  Exposes this via the public function `generate-schedule`.

  Literature references:

  * How To Travel the World Without Jet Lag (Eastman and Burgess,
  2009)

  * Preflight Adjustment to Eastward Travel: 3 Days of Advancing Sleep
  with and without Morning Bright Light (Burgess et al., 2003)"
  (:require [cljs-time.core :as cljs-time]))


(def ^:private ^:const possible-phase-changes-per-day
  "Provides the most realistic phase shift per day in
  hours. `:phase-advance` refers to sleeping earlier, `:phase-delay`
  means sleeping later."
  {;; phase advance means sleeping earlier, i.e. traveling from Berlin
   ;; to Bangkok. we humans are generally slower at phase advancing. a
   ;; light box provides some benefit.
   :phase-advance {{:use-light-box? true}  -1
                   {:use-light-box? false} -0.6}

   ;; phase delay means sleeping later, i.e. traveling from Berlin to
   ;; New York. we can move this by 1hr/day. if we use a light box, we
   ;; can even move it by 2hrs/day!
   :phase-delay {{:use-light-box? true}  2
                 {:use-light-box? false} 1}})

(defn- phase-change-per-day
  "Look up the phase change per day based on the given keys. This mainly
  maps the `time-zone-delta` to either `:phase-advance` or
  `:phase-delay`."
  [{:keys [time-zone-delta use-light-box?]
    :as modifiers}]
  (if (pos? time-zone-delta)
    (get-in possible-phase-changes-per-day
            [:phase-advance (select-keys modifiers [:use-light-box?])])
    (get-in possible-phase-changes-per-day
            [:phase-delay (select-keys modifiers [:use-light-box?])])))

(defn- calculate-t-min
  "Calculate the approximate Tmin during a sleep cycle. Here we
  approximate it to be 4 hours before waking (usually 3.5-5hrs
  before)."
  [wake-time]
  (cljs-time/minus wake-time (cljs-time/hours 4)))

(def ^:private ^:const melatonin-timing-by-dose
  "Map the dosage (in mg) of Melatonin to the hours before bed time it
  should be taken, e.g., if we have 0.5mg available, we should take it
  4.5 hours before bed time."
  {0.5 4.5
   3.0 7.5})

(defn- calculate-melatonin-timing
  "Calculate when to take Melatonin based on the bed time and the
  available dosage (0.5mg and 3.0 currently supported)."
  [{:keys [bed-time melatonin-dose-mg]}]
  (cljs-time/minus bed-time
                   (cljs-time/hours
                    (get melatonin-timing-by-dose melatonin-dose-mg))))

;; TODO: Research this
(def ^:private ^:const light-box-evening-magic-number
  "When to use a light box in the evening. For now, let's just assume we
  use it 2 hours before bed time. Still have to look up whether
  studies on how to calculate this are available."
  2)

(defn generate-schedule
  "Generate a sleeping schedule for adapting to a new time zone.

  Parameters:

  * `:time-zone-delta`: The time difference of the destination
  relative to your current location in hours, e.g. while you're in
  Berlin, the value for Bangkok would be `6` and for New York `-6`.

  * `:bed-time-hour`: The hour you usually go to bed (24hr format),
  e.g. `23`.

  * `:bed-time-minute`: The minute you usually go to bed, e.g. `55`.

  * `:sleep-duration-hours`: The duration you usually sleep in
  hours. Floats are allowed.

  * `:use-light-box?`: Whether to use a light box.

  * `:melatonin-dose-mg`: Whether to use melatonin and if yes, which
  does to take. Currently, 0.5mg and 3.0mg are supported as studies
  with these doses have been published."
  [{:keys [time-zone-delta
           bed-time-hour bed-time-minute
           sleep-duration-hours
           use-light-box? melatonin-dose-mg]
    :as   input}]
  (assert (not (zero? time-zone-delta))
          (str "time-zone-delta must not be zero!"))
  (let [now             (cljs-time/now)
        start-date-time (cljs-time/date-time (cljs-time/year now)
                                             (cljs-time/month now)
                                             (cljs-time/day now)
                                             bed-time-hour
                                             bed-time-minute)
        phase-change    (phase-change-per-day
                         (select-keys input [:time-zone-delta :use-light-box?]))]
    (for [day  (range (js/Math.ceil (js/Math.abs
                                     (/ time-zone-delta
                                        phase-change))))
          :let [bed-time (cljs-time/plus
                          start-date-time
                          ;; one day later, of course
                          (cljs-time/days day)
                          ;; but with a different bed time, based on
                          ;; the phase change!!
                          (cljs-time/hours (* day phase-change)))
                wake-time (cljs-time/plus
                           bed-time
                           (cljs-time/hours sleep-duration-hours))]]
      (cond-> {:bed-time  bed-time
               :day       day
               :t-min     (calculate-t-min wake-time)
               :wake-time wake-time}
        ;; if we want to use melatonin and it actually makes
        ;; sense (travelling eastwards), include it in the schedule
        (and melatonin-dose-mg
             (pos? melatonin-dose-mg)
             (neg? phase-change))
        (assoc :use-melatonin-at
               (calculate-melatonin-timing {:bed-time bed-time
                                            :melatonin-dose-mg melatonin-dose-mg}))

        ;; if we want to use a light box, include it here
        ;; TODO: base this on Tmin
        use-light-box?
        (assoc :use-light-box-at
               (if (neg? phase-change)
                 wake-time
                 (cljs-time/minus bed-time (cljs-time/hours
                                            light-box-evening-magic-number))))))))

(comment
  (generate-schedule {:bed-time-hour          23
                      :bed-time-minute        15
                      :time-zone-delta       6
                      :use-light-box?         true
                      :melatonin-dose-mg      3.0
                      :sleep-duration-hours   7.5})
  (generate-schedule {}))
