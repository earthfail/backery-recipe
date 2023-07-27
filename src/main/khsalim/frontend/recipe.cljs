(ns khsalim.frontend.recipe
  ;; (:require [helix.core :refer [defnc $]]
  ;;           [helix.hooks :as hooks]
  ;;           [helix.dom :as d]
  ;;           ["react-dom/client" :as rdom])
  )
(defn read-data! []
  (-> js/document
      (.getElementById "data")
      .-text
      js/JSON.parse
      (js->clj :keywordize-keys true)))
(defonce counter (atom 0))
(def carouselSlide (js/document.querySelector "[data-type=\"carousel-slide\"]"))
(def carouselImages (into [] (js/document.querySelectorAll "[data-type=\"img\"]")))

(def nextBtn (js/document.getElementById "nextBtn"))
(def prevBtn (js/document.getElementById "prevBtn"))

(def size (.-clientWidth (first carouselImages)))

(defn advance-carousel-event! [e]
  (do
    (if (< @counter (dec (count carouselImages)))
      (swap! counter inc))
    (set! (.. carouselSlide -style -transform) (str "translateX(" (* size @counter) "px)"))))
(defn withdraw-carousel-event! [e]
  (do
    (if (> @counter 0)
      (swap! counter dec))
    (set! (.. carouselSlide -style -transform) (str "translateX(" (* size @counter) "px)"))))

(defn ^:dev/after-load start []
  ;; (js/console.log "start")
  ;; (js/console.log "data..." (read-data!))
  ;; (js/console.log "adding events...")
  (.addEventListener nextBtn "click" advance-carousel-event!)
  (.addEventListener prevBtn "click" withdraw-carousel-event!)
  (set! (.. carouselSlide -style -transition) "transform 0.4s ease-in-out")
  )
(defn ^:dev/before-load stop []
  ;; (js/console.log "stop you")
  ;; (js/console.log "removing events...")
  (.removeEventListener nextBtn "click" advance-carousel-event!)
  (.removeEventListener prevBtn "click" withdraw-carousel-event!)
  )
(defn init []
  ;; (js/console.log "yeah baby I'm init!")
  #_(update-plot-data! true)
  (start))

(comment
  ;; helix
  ;; define components using the `defnc` macro
;; (defnc greeting
;;   "A component which greets a user."
;;   [{:keys [name]}]
;;   ;; use helix.dom to create DOM elements
;;   (d/div "Hello, " (d/strong name) "!" " you look fine today. did you work out?"))

;; (defnc app []
;;   #_(let [[state set-state] (hooks/use-state {:name "سليم خطيب" :recipes (read-data!)})]
;;     (d/div
;;       ;; create elements out of components
;;       ;; ($ greeting {:name (:name state)})
;;            (for [r (:recipes state)]
;;              ^{:key (:id r)} (d/div {:class ["w-full" "bg-cover" "h-[250px]"]
;;                                      :style {:background-image (str "url:('" "/assets/img/" (:img r) "')")} }
;;                        #_(d/img {:class ""
;;                                  :src (str "/assets/img/" (:img r))})
;;                        (d/span {:class "text-white bg-black"}
;;                                (:description r)))))))
  ;; (defonce root (rdom/createRoot (js/document.getElementById "app")))
;;   (defn ^:dev/after-load start []
;;   (js/console.log "start")
;;   ;; start your app with your favorite React renderer
;;   (.render root ($ app)))
;; (defn ^:dev/before-load stop []
;;   (js/console.log "stop"))
;; (defn init []
;;   (js/console.log "hiii init")
;;   (start))
)
(comment
  ;; reagent
  (read-data!)
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
