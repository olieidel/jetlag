(ns jetlag.views
  (:require [cljs-time.core :as cljs-time]
            [clojure.string :as str]
            [jetlag.events :as events]
            [jetlag.subs :as subs]
            [re-frame.core :as rf :refer [dispatch subscribe]]
            [reagent.core :as r]

            ;; material-ui, `:default` syntax is shadow-cljs specific
            ["@material-ui/core/AppBar" :default app-bar]
            ["@material-ui/core/Button" :default button]
            ["@material-ui/core/Card" :default card]
            ["@material-ui/core/CardContent" :default card-content]
            ["@material-ui/core/CardActions" :default card-actions]
            ["@material-ui/core/Checkbox" :default checkbox]
            ["@material-ui/core/Chip" :default chip]
            ["@material-ui/core/FormControl" :default form-control]
            ["@material-ui/core/FormControlLabel" :default form-control-label]
            ["@material-ui/core/Grid" :default grid]
            ["@material-ui/core/Input" :default input]
            ["@material-ui/core/InputLabel" :default input-label]
            ["@material-ui/core/MenuItem" :default menu-item]
            ["@material-ui/core/Select" :default select]
            ["@material-ui/core/TextField" :default text-field]
            ["@material-ui/core/Toolbar" :default toolbar]
            ["@material-ui/core/Typography" :default typography]

            ;; icons of material-ui
            ["@material-ui/icons/Alarm" :default alarm-icon]
            ["@material-ui/core/Paper" :default paper]
            ["@material-ui/icons/Place" :default place-icon]
            ["@material-ui/icons/WbSunny" :default sunny-icon]))


(defn- root-container [& children]
  (into
   [:<>
    [:> app-bar {:position "static"}
     [:> toolbar
      [:> typography {:variant "h6" :color "inherit"} "Jetlag Ninja"]]]]
   children))

(defn- header []
  [:<>
   [:> app-bar {:position "static"}
    [:> toolbar
     [:> typography {:variant "h6" :color "inherit"} "Jetlag Ninja"]]]
   [:> typography {:component "div"}
    [:div "Hello, World Traveller!"]]
   [:div "Ready to conquer your jetlag the next time you travel?"]])

(defn- target-value [event]
  (.. event -target -value))

(defn- parse-time-str [time-str]
  (let [[hour minute] (map js/parseInt (str/split time-str #":"))]
    {:hour   hour
     :minute minute}))

(defn- date-time->time-str [date-time]
  (letfn [(pad-zero [s] (cond->> s (< s 10) (str "0")))]
    (str
     (pad-zero (cljs-time/hour date-time))
     ":"
     (pad-zero (cljs-time/minute date-time)))))

(defn- form []
  (let [bed-time-str         (subscribe [::subs/bed-time-str])
        melatonin-dose-mg    (subscribe [::subs/melatonin-dose-mg])
        sleep-duration-hours (subscribe [::subs/sleep-duration-hours])
        time-zone-delta      (subscribe [::subs/time-zone-delta])
        use-light-box?       (subscribe [::subs/use-light-box?])]
    (fn []
      [:> paper {:style {:margin-top "8px"}}
       [:> grid {:container true
                 :spacing   16}
        [:> grid {:alignItems "center"
                  :container true
                  :direction  "row"
                  :display    "flex"
                  :justify    "center"
                  :xs         2}
         [:> place-icon {:style {:font-size 24}}]]
        [:> grid {:item true
                  :xs   10}
         [:> form-control {:style {:width "100%"}}
          [:> input-label {:htmlFor "time-zone-delta-helper"}
           "Time Zone of Destination"]
          (into
           [:> select {:input     (r/as-element [:> input {:name "time-zone-delta"
                                                           :id   "time-zone-delta-helper"}])
                       :on-change #(dispatch [::events/set-time-zone-delta
                                              (js/parseInt (target-value %))])
                       :value     @time-zone-delta}
            (mapcat (fn [i]
                      [^{:key (- i)}
                       [:> menu-item {:value (- i)}
                        (str i " hour" (if (> i 1) "s") " behind")]
                       ^{:key i}
                       [:> menu-item {:value i}
                        (str i " hour" (if (> i 1) "s") " ahead")]])
                    (range 1 24))])]]

        [:> grid {:alignItems "center"
                  :container true
                  :direction  "row"
                  :display    "flex"
                  :justify    "center"
                  :xs         2}
         [:> alarm-icon {:style {:font-size 24}}]]

        [:> grid {:item true
                  :xs   5}
         [:> text-field {:InputLabelProps #js {:shrink true}
                         :inputProps      #js {:step 600}
                         :label           "Bed Time"
                         :on-change       #(dispatch [::events/set-bed-time
                                                      (parse-time-str (target-value %))])
                         :style           {:width "100%"}
                         :type            "time"
                         :value           @bed-time-str}]]

        [:> grid {:item true
                  :xs   5}

         [:> form-control {:style {:width "100%"}}
          [:> input-label {:htmlFor "sleep-duration-helper"} "Sleep Duration"]
          (into
           [:> select {:input     (r/as-element [:> input {:name "sleep-duration"
                                                           :id   "sleep-duration-helper"}])
                       :on-change #(dispatch [::events/set-sleep-duration
                                              (target-value %)])
                       :value     @sleep-duration-hours}
            (for [i (range 4 10.5 0.5)]
              ^{:key i}
              [:> menu-item {:value i}
               (str i " hour" (if (> i 1) "s"))])])]]

        [:> grid {:alignItems "center"
                  :container true
                  :direction  "row"
                  :display    "flex"
                  :justify    "center"
                  :xs         2}
         [:> sunny-icon {:style {:font-size 24}}]]

        [:> grid {:item true
                  :xs   5}
         [:> form-control {:style {:width "100%"}}
          [:> input-label {:htmlFor "melatonin-dose"} "Melatonin Dose"]
          [:> select {:input     (r/as-element [:> input {:name "melatonin-dose"
                                                          :id   "melatonin-dose-helper"}])
                      :on-change #(dispatch [::events/set-melatonin-dose-mg
                                             (target-value %)])
                      :value     @melatonin-dose-mg}
           [:> menu-item {:value 0} "I won't take Melatonin"]
           [:> menu-item {:value 0.5} "0.5mg"]
           [:> menu-item {:value 3.0} "3.0mg"]]]]

        [:> grid {:item true
                  :xs   5}
         [:> form-control-label
          {:control (r/as-element [:> checkbox {:checked @use-light-box?
                                                :color   "primary"
                                                :on-change
                                                #(dispatch [::events/use-light-box?
                                                            (.. % -target -checked)]) }])
           :label   "Use a light box"
           :style   {:width "100%"}}]]]])))

(defn- inverse-hint []
  (let [consider-inverse-schedule? (subscribe [::subs/consider-inverse-schedule?])]
    (fn []
      (when @consider-inverse-schedule?
        [:> card {:style {:margin-top "12px"}}
         [:> card-content
          [:> typography {:component "p"}
           "Consider adapting \"the other way around the world\" -
           you'll save time!"]]
         [:> card-actions
          [:> button {:color "secondary"
                      :on-click #(dispatch [::events/invert-time-zone-delta])
                      :size "small"}
           "Good idea! Let's do it!"]]]))))

(defn- plan []
  (let [schedule (subscribe [::subs/schedule])]
    (fn []
      (when @schedule
        [:> grid {:container true
                  :spacing   16
                  :style     {:margin-top "12px"}}
         (for [{:keys [bed-time day t-min use-light-box-at
                       use-melatonin-at wake-time]} @schedule
               :let
               [extra-item-count (cond-> 0
                                   use-light-box-at inc
                                   use-melatonin-at inc)
                small-width (case extra-item-count
                              0 6
                              1 4
                              2 3)]]
           ^{:key day}
           [:> grid {:item true
                     :lg   3
                     :md   4
                     :sm   6
                     :xs   12}
            [:> card {:style {:width "100%"}}
             [:> card-content
              [:> chip {:color "primary"
                        :label (str "Day " day)}]

              [:> grid {:container true}

               [:> grid {:item true
                         :sm   small-width
                         :xs   6}
                [:> grid {:container true}
                 [:> grid {:item true
                           :xs   12}
                  [:> typography {:component "h2"
                                  :variant   "headline"}
                   (date-time->time-str bed-time)]]
                 [:> grid {:item true
                           :xs   12}
                  [:> typography {:color "textSecondary"}
                   "Bed Time"]]]]

               [:> grid {:item true
                         :sm   small-width
                         :xs   6}
                [:> grid {:container true}
                 [:> grid {:item true
                           :xs   12}
                  [:> typography {:component "h2"
                                  :variant   "headline"}
                   (date-time->time-str wake-time)]]
                 [:> grid {:item true
                           :xs   12}
                  [:> typography {:color "textSecondary"}
                   "Wake Time"]]]]

               (when use-melatonin-at
                 [:> grid {:item true
                           :sm   small-width
                           :xs   6}
                  [:> grid {:container true}
                   [:> grid {:item true
                             :xs   12}
                    [:> typography {:component "h2"
                                    :variant   "headline"}
                     (date-time->time-str use-melatonin-at)]]
                   [:> grid {:item true
                             :xs   12}
                    [:> typography {:color "textSecondary"}
                     "Take Melatonin"]]]])

               (when use-light-box-at
                 [:> grid {:item true
                           :sm   small-width
                           :xs   6}
                  [:> grid {:container true}
                   [:> grid {:item true
                             :xs   12}
                    [:> typography {:component "h2"
                                    :variant   "headline"}
                     (date-time->time-str use-light-box-at)]]
                   [:> grid {:item true
                             :xs   12}
                    [:> typography {:color "textSecondary"}
                     "Use Light Box"]]]])]]]])]))))

(defn main []
  [root-container
   [form]
   [inverse-hint]
   [plan]])
