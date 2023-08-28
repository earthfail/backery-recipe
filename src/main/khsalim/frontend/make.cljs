(ns khsalim.frontend.make
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [helix.core :refer [defnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   ["react-dom/client" :as rdom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   )
  ;; (:require [clojure.edn :as edn])
  )
(goog-define VERBOSE true)


(def btn (js/document.getElementById "btn"))
(defn upload-image! [e]
  (go (let [response (<! (http/get "/api/v1/getUrl"))
            url (get-in response [:body :url])
            file (-> js/document
                     (.getElementById "media-file")
                     .-files
                     first)
            response (<! (http/post url {:multipart-params [["media" file]]}))]
        (js/console.dir e)
        (prn response))))


(defnc app []
  (d/div  (d/label {:for "media-file"} "hey")
          (d/input {:name "media" :id "media-file" :type "file"
                    :accept "image/*" :capture "enviroment"
                    :hidden true}
                   )
          (d/label {:for "description"} "beey")
          (d/input {:type "text" :id "description"})
          (d/button {:id "btn"} "add")))

(defonce root (rdom/createRoot (js/document.getElementById "app")))
(defn ^:dev/after-load start []
  (when  VERBOSE
    (js/console.log "start"))
  ;; (.addEventListener btn "click" upload-image!)
  (when VERBOSE
    (js/console.log "added events..."))
  
  (.render root ($ app))
  )
(defn ^:dev/before-load stop []
  (when VERBOSE
    (js/console.log "stop you"))
  ;; (.removeEventListener btn "click" upload-image!)
  (when VERBOSE
    (js/console.log "removed events...")))
(defn init []
  (when VERBOSE
    (js/console.log "yeah baby I'm init!"))
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
;;   (read-data!)
;;   ;; Reagent
;;   (defn root [state]
;;     [:div.view #_{:class "container is-fluid columns box"}
;;      [months-menu state]
;;      [:div.chart [chart state]]])
;; ;; [:div#view {:class "container is=fluid columns box"}
;; ;;    [:div#app {:class "column"}]
;; ;;    [:div#plot {:class "column is-two-thirds"}]]
;;   (defn ^:dev/after-load start []
;;     (js/console.log "start")
;;     (when-let [el (gdom/getElement "root")]
;;       (rdom/render [root plot-data] el)))
;;   (defn ^:dev/before-load stop []
;;     (js/console.log "stop you"))
;;   (defn init []
;;     (js/console.log "yeah baby I'm init!")
;;     #_(update-plot-data! true)
;;     (start))
  )
