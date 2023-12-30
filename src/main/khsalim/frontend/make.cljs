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
(goog-define VERBOSE false)

;; https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file#unique_file_type_specifiers
;; https://developer.mozilla.org/en-US/docs/Web/API/File_API/Using_files_from_web_applications

(def recipe-id (-> js/document
                   (.getElementById "data")
                   .-text
                   js/JSON.parse
                   (js->clj :keywordize-keys true)))

(defnc preview-list [{:keys [files more? add-picture set-files]}]
  (d/ol {:class "preview"}
        (when more?
          (d/svg {:stroke "black"
                  :style {:width "200px" :height "100px"
                          :background "rgba(200,0,0,0.7)"}
                  :class "rounded-lg"
                  :on-click add-picture}
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
        switch-to-edit (constantly :edit)
        add-picture (fn add-picture [e]
                      (when VERBOSE
                        (js/console.log "clicking file by +"))
                      (.click (js/document.getElementById "media-file")))]
    (<>
     (d/div
      {:class "pb-4"}
      (d/label {:for "recipe-name"
                :class "py-2"} "اسم الوصفة")
      (d/input {:id "recipe-name" :type "text"
                :on-change (fn name-event [e]
                             (set-name (constantly (.. e -target -value))))})
      (d/label {:for "recipe-description"} "شرح عن الوصفة")
      (d/input {:id "recipe-description" :type "text"
                :on-change (fn name-event [e]
                             (set-description (constantly (.. e -target -value))))}))
     (d/div
      (d/label {:for "media-file"
                :class "sr-only"} "حمل صورة")
      (d/input {:name "media" :id "media-file" :type "file"
                :accept "image/*" :capture "enviroment"
                :class "sr-only"
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
      (d/label {:for "description"} (let [working-step (if (= :add status)
                                                         (inc total)
                                                         total)]
                                      (if (= working-step 1)
                                        "شرح الخطوة الاولى"
                                        (if (= working-step 2)
                                          "شرح الخطوة الثانية"
                                          (str "شرح الخطوة رقم " working-step)))))
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
      (d/div {:class "flex justify-between py-4 px-8 gap-8"}
             (when (= :edit status)
               (d/button {:class "w-full border-gray-300 rounded-lg shadow-md focus:border-accent focus:ring-accent"
                          :id "add-picture"
                          :on-click #(do
                                       (when VERBOSE (js/console.log "add-picture"))
                                       (.click (js/document.getElementById "media-file"))
                                       (let [top-image (peek files-list)]
                                         (when (and (contains? top-image :file)
                                                    (contains? top-image :desc)
                                                    (contains? top-image :url))
                                           (set! (js/document.getElementById "description") -value "")
                                           (set-status (constantly :add)))))}
                         "اضف صورة جديدة"))
             (when-not (zero? total)
               (d/button {:class "w-full border-gray-300 rounded-lg shadow-md focus:border-accent focus:ring-accent"
                          :id "upload"
                          :on-click #(do
                                       (when VERBOSE
                                         (js/console.log "uploadding !!!!!"))
                                       (go
                                         (if (:success
                                              (<! (http/patch
                                                   (str "/api/v1/recipe/" recipe-id)
                                                   {:edn-params {:recipe-name name
                                                                 :recipe-description description}})))
                                           (do (when VERBOSE
                                                 (js/console.log "successfull name and descritption"))
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
                                                         (let [response (<! (http/post
                                                                             (str "/api/v1/recipe/" recipe-id)
                                                                             {:edn-params {:step step
                                                                                           :description desc :url url :media-type "img"}}))]
                                                           (js/console.log response)))
                                                 ;; else
                                                     (do
                                                       (js/console.log "could not upload image!!"))))))
                                           ;;else
                                           (do
                                             (js/console.log "could not register name and description" recipe-id name description)))
                                         (set-files-list (constantly (list)))
                                         (set-status (constantly :add))
                                         (set-total (constantly 0))))}
                         "انشر الصور"))))
     ($ preview-list {:files files-list :more? (= :add status) :add-picture add-picture
                      :set-files set-files-list})
     (when VERBOSE
       (d/button {:id "logging"
                  :style {:position "fixed" :top "10px" :left "5px"}
                  :on-click #(do
                               (js/console.log status)
                               (js/console.dir files-list)
                               (js/console.log name description)
                               (js/console.log total))}
                 "console log")))))

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
