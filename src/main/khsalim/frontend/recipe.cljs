(ns khsalim.frontend.recipe
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [clojure.edn :as edn]))
;; NOTE: could benifit from agents and add-watch function `https://clojuredocs.org/clojure.core/add-watch`
(goog-define VERBOSE true)

(defn read-data! []
  (-> js/document
      (.getElementById "data")
      .-text
      js/JSON.parse
      (js->clj :keywordize-keys true)))
(def current-page (read-data!) #_(get (read-data!) :page 0))

;; could merge with {current-page 0} or check if key is nil when advancing
;; I choose the second option
(defonce pages (atom (if-let [pages (edn/read-string (js/localStorage.getItem "pages"))]
                       pages
                       {current-page 0})))
(defonce counter (atom (get @pages current-page 0)))
(defonce finished (atom false))
(def carouselSlide (js/document.querySelector "[data-type=\"carousel-slide\"]"))
(def carouselImages (into [] (js/document.querySelectorAll "[data-type=\"img\"]")))

(def nextBtn (js/document.getElementById "nextBtn"))
(def prevBtn (js/document.getElementById "prevBtn"))

(def size (.-clientWidth (first carouselImages)))

(defn advance-carousel-event! [e]
  (do
    (when (< @counter (dec (count carouselImages)))
      (swap! counter inc)
      (if (= @counter (dec (count carouselImages)))
        (do (swap! pages dissoc current-page)
            (go (<! (http/post "/api/v1/statistic" {:edn-params {:recipe-id recipe-id
                                                                 :status :finished}}))))
        (swap! pages update current-page (fnil inc 0)))
      (js/localStorage.setItem "pages" (prn-str @pages)))
    (when VERBOSE
      (println "counter is " @counter)
      (println "pages is" @pages))
    (set! (.. carouselSlide -style -transform) (str "translateX(" (* size @counter) "px)"))))
(defn withdraw-carousel-event! [e]
  (do
    (when (> @counter 0)
      (swap! counter dec)
      (swap! pages assoc current-page @counter)
      (js/localStorage.setItem "pages" (prn-str @pages)))
    (when VERBOSE
      (println "counter is " @counter)
      (println "pages is" @pages))
    (set! (.. carouselSlide -style -transform) (str "translateX(" (* size @counter) "px)"))))

(defn ^:dev/after-load start []
  (when  VERBOSE
    (js/console.log "start")
    (js/console.log "data..." (read-data!))
    (js/console.log "adding events..."))
  (when VERBOSE
      (println "counter is " @counter)
      (println "pages in localStorage is" (js/localStorage.getItem "pages")))

  (.addEventListener nextBtn "click" advance-carousel-event!)
  (.addEventListener prevBtn "click" withdraw-carousel-event!)
  (set! (.. carouselSlide -style -transition) "transform 0.4s ease-in-out")
  (set! (.. carouselSlide -style -transform) (str "translateX(" (* size @counter) "px)"))
  )
(defn ^:dev/before-load stop []
  (when VERBOSE
    (js/console.log "stop you")
    (js/console.log "removing events..."))
  (.removeEventListener nextBtn "click" advance-carousel-event!)
  (.removeEventListener prevBtn "click" withdraw-carousel-event!)
  )
(defn init []
  (when VERBOSE
    (js/console.log "yeah baby I'm init!"))
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
