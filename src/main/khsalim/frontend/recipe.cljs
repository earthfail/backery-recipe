(ns khsalim.frontend.recipe
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            ))
(defn read-data! []
  (-> js/document
      (.getElementById "data")
      (.-innerHTML)
      js/JSON.parse
      js->clj))
;; define components using the `defnc` macro
(defnc greeting
  "A component which greets a user."
  [{:keys [name]}]
  ;; use helix.dom to create DOM elements
  (d/div "Hello, " (d/strong name) "!" " you look fine today. did you work out?"))

(defnc app []
  (let [[state set-state] (hooks/use-state {:name "Helix User"})]
    (d/div
     (d/h1 "Welcome!")
      ;; create elements out of components
      ($ greeting {:name (:name state)})
      (d/input {:value (:name state)
                :on-change #(set-state assoc :name (.. % -target -value))})
      (d/div {:class ""} "Hi Dave"))))
(defonce root (rdom/createRoot (js/document.getElementById "app")))

(defn ^:dev/after-load start []
  (js/console.log "start")
  ;; start your app with your favorite React renderer
  (.render root ($ app)))
(defn ^:dev/before-load stop []
  (js/console.log "stop"))
(defn init []
  (-> js/document
      (.getElementById "data")
      (.-innerHTML))
  (js/console.log "hiii init")
  (start))

(comment
  ;; Reagent
  (defn root [state]
    [:div.view #_{:class "container is-fluid columns box"}
     [months-menu state]
     [:div.chart [chart state]]])
;; [:div#view {:class "container is=fluid columns box"}
;;    [:div#app {:class "column"}]
;;    [:div#plot {:class "column is-two-thirds"}]]
  (defn ^:dev/after-load start []
    (js/console.log "start")
    (when-let [el (gdom/getElement "root")]
      (rdom/render [root plot-data] el)))
  (defn ^:dev/before-load stop []
    (js/console.log "stop you"))
  (defn init []
    (js/console.log "yeah baby I'm init!")
    #_(update-plot-data! true)
    (start)))

