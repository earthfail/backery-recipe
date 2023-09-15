(ns khsalim.frontend.make
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [helix.core :refer [defnc $ <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   ["react-dom/client" :as rdom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [lambdaisland.dom-types]

   )
  ;; (:require [clojure.edn :as edn])
  )
(goog-define VERBOSE true)

;; https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file#unique_file_type_specifiers
;; https://developer.mozilla.org/en-US/docs/Web/API/File_API/Using_files_from_web_applications

(def recipe-id (-> js/document
                   (.getElementById "data")
                   .-text
                   js/JSON.parse
                   (js->clj :keywordize-keys true)))

(def btn (js/document.getElementById "btn"))
(defn upload-image2! [e]
  (go (let [response (<! (http/get "/api/v1/getUrl"))
            url (get-in response [:body :url])
            file (-> js/document
                     (.getElementById "media-file")
                     .-files
                     first)
            response (<! (http/post url {:multipart-params [["media" file]]}))]
        (js/console.dir e)
        (prn response))))

#_(def files-state
  "each file in files-list is a map with keys :desc :file :url.
  :file - is a File object
  :url - URL object created by URL.createObjectURL(file) on the associated file object
  :desc - description"
  (hooks/use-state []))
;; (def status-state (hooks/use-state :edit))
(defnc preview-list [{:keys [files more?]}]
  (d/ol {:class "preview"}
        (when more?
          (d/svg {:stroke "black"
                  :style {:width "200px" :height "100px" :background "rgba(200,0,0,0.7)"}}
                 ($ "use" {:href "#plus" :stroke "white"})))
        (for [{:keys [desc url file]} files]
          (d/li {:key url}
                (d/span {:class "desc"} desc)
                (d/img {:class "window" :src url :file file})))))


(defnc app []
  (let [[files-list set-files-list]
        ^{:doc "each file in files-list is a map with keys:
  :file - is a File object
  :url - URL object created by URL.createObjectURL(file) on the associated file object
  :desc - description
  :step - index of the step in recipe"} (hooks/use-state (list)) ;files-state
        [status set-status] (hooks/use-state :add) ;status-state
        [name set-name] (hooks/use-state "")
        [description set-description] (hooks/use-state "")
        [total set-total] (hooks/use-state 0)
        switch-to-edit (constantly :edit)]
    (<>
     (d/label {:for "recipe-name"} "اسم الوصفة")
     (d/input {:id "recipe-name" :type "text"
               :on-change (fn name-event [e]
                            (set-name (constantly (.. e -target -value))))})
     (d/label {:for "recipe-description"} "شرح عن الوصفة")
     (d/input {:id "recipe-description" :type "text"
               :on-change (fn name-event [e]
                            (set-description (constantly (.. e -target -value))))})
     (d/label {:for "media-file"} "حمل صورة")
         (d/input {:name "media" :id "media-file" :type "file"
                   :accept "image/*" :capture "enviroment"
                   :class "sr-only"
                   ;; :hidden true
                   :on-change (fn in-event [e]
                                (do
                                  (.stopPropagation e)
                                  (.preventDefault e)
                                  (let [file-obj (first (.. e -target -files))
                                        file-url (js/URL.createObjectURL file-obj)]
                                    (when VERBOSE
                                      (js/console.log "obj" file-obj
                                                      "url" file-url))
                                    (case status
                                      :edit 
                                      (when-let [top-image (peek files-list)]
                                        (when VERBOSE
                                          (js/console.log "editing image"))
                                        (when-let [url (get top-image :url)]
                                          (js/URL.revokeObjectURL url))
                                        (set-files-list pop)
                                        (set-files-list conj (assoc top-image
                                                                    :file file-obj
                                                                    :url file-url)))
                                      :add (do
                                             (when VERBOSE
                                               (js/console.log "adding image"))
                                             (set-total inc)
                                             (set-files-list conj {:file file-obj
                                                                   :url file-url
                                                                   :step total})
                                             (set-status switch-to-edit))))))})
         (d/label {:for "description"} "اكتب الخطوة")
         (d/input {:type "text" :id "description"
                   :on-change #(do
                                 (when-let [desc (.. % -target -value)]
                                   (case status
                                     :edit (when-let [top-image (peek files-list)]
                                             (when VERBOSE
                                               (js/console.log "edit description"))
                                             (set-files-list pop)
                                             (set-files-list conj (assoc top-image
                                                                         :desc desc)))
                                     :add (do
                                            (when VERBOSE
                                              (js/console.log "add description"))
                                            (set-total inc)
                                            (set-files-list conj {:desc desc
                                                                  :step total})
                                            
                                            (set-status switch-to-edit)))))})
         (d/button {:id "add-picture"
                    :on-click #(do
                                 (let [top-image (peek files-list)]
                                   (when (and (contains? top-image :file)
                                              (contains? top-image :desc)
                                              (contains? top-image :url))
                                     (set! (js/document.getElementById "description") -value "")
                                     (set-status (constantly :add)))))}
                   "اضف صورة جديدة")
         (d/button {:id "upload"
                    :on-click #(do
                                 (js/console.log "uploadding !!!!!")
                                 (go
                                   (if (:success
                                        (<! (http/patch
                                             (str "/api/v1/recipe/" recipe-id)
                                             {:edn-params {:recipe-name name
                                                           :recipe-description description}})))
                                     (doseq [{:keys [file desc step]
                                              obj-url :url} files-list]
                                       (let [response (<! (http/get "/api/v1/getUrl"))
                                             url (get-in response [:body :url])
                                             response (<! (http/post url {:multipart-params [["media" file]]}))]
                                         (when VERBOSE
                                           (js/console.log "upload to " url)
                                           (js/console.log "file " file
                                                           "description " desc))
                                         (prn response)
                                         (js/console.dir "res " response)
                                         (if (:success response)
                                           (do (js/URL.revokeObjectURL obj-url)
                                               (let [response (<! (http/post (str "/api/v1/recipe/" recipe-id)
                                                                             {:edn-params {:step step
                                                                                           :description desc :url url :media-type "img"}}))]
                                                 (js/console.log response)))
                                           ;; else
                                           (do
                                             (js/console.log "could not upload image!!")))
                                         ))
                                     ;;else
                                     (do
                                       (js/console.log "could not register name and description" recipe-id name description)))
                                     (set-files-list (constantly (list)))
                                     (set-status (constantly :add))
                                     ))}
                   "انشر الصور")
         (d/button {:id "logging"
                    :on-click #(do
                                 (js/console.log "shoudl be uploading files")
                                 (js/console.log status)
                                 (js/console.dir files-list)
                                 (js/console.log name description)
                                 )
                    } "console log")
         ($ preview-list {:files files-list :more? (= status :add)}))))

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
    (js/console.log "removing events and urls...")
    #_(doseq [{:keys [url]} @file-list]
      (js/console.log "removeing" url)
      #_(js/URL.revokeObjectURL url)))
  
  )
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
