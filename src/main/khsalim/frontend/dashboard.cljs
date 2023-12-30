(ns khsalim.frontend.dashboard
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [lambdaisland.dom-types]))
(goog-define VERBOSE false)
(goog-define APP_URL "localhost:3000")


(def recipes (-> js/document
                 (.getElementById "data")
                 .-text
                 js/JSON.parse
                 (js->clj :keywordize-keys true)))

(def clipboards-btns (js/document.querySelectorAll ".clipboard-btn"))
(def checkbox-btns (js/document.querySelectorAll ".checkbox-btn"))
(def delete-btn (js/document.getElementById "delete-btn"))
(def filter-box (js/document.getElementById "filter-name"))
(defn recipe-link->clipboard [e]
  (try 
    (let [el (.-currentTarget e)
          recipe-link (.-recipeLink ^DOMStringMap (.-dataset el))]
      (js/navigator.clipboard.writeText (str APP_URL recipe-link))
      (when VERBOSE (js/console.log "checked clipboard")
            (js/console.log (.querySelector el "svg > use")))
      (set! (.. (.querySelector el "svg > use") -href -baseVal)  "#checked-clipboard")
      (js/setTimeout
       #(do
          (when VERBOSE (js/console.log "return to clipboard"))
          (set! (.. (.querySelector el "svg > use") -href -baseVal)  "#clipboard")
          (.blur el))
       1300))
    (catch :default e (js/console.log "couldn't copy" e))))
(defn toggle-checkmark [e]
  (let [el (.-currentTarget e)
        state (.-state ^DOMStringMap (.-dataset el))
        checkbox (.querySelector el "svg > use")]
    (when VERBOSE
      (js/console.log el checkbox "hi there" "state" state)
      (js/console.log (.getAttribute el "aria-checked"))
      )
    (if (= state "unchecked")
      (do
        (.setAttribute el "aria-checked" true)
        (set! el -value "on")
        (set! (.-state ^DOMStringMap (.-dataset el)) "checked"))
      (do
        (.setAttribute el "aria-checked" false)
        (set! el -value "off")
        (set! (.-state ^DOMStringMap (.-dataset el)) "unchecked")
          ))
    (.. checkbox -classList (toggle "hidden"))))
(defn delete-recipe [tr-element]
  (let [recipe-id (.-recipeId ^DOMStringMap (.-dataset tr-element))]
    (when VERBOSE
      (js/console.log recipe-id))
    (go
      (if (:success (<! (http/delete (str "/api/v1/recipe/" recipe-id))))
        (.remove tr-element)
        ;;else
        (js/console.log "couldn't delete recipe" recipe-id)))))
(defn delete-recipes [_]
  (when VERBOSE
    #_(js/console.log el)
    (js/console.log (filterv #(= (.-value %) "on") checkbox-btns))
    (doseq [tr-ele (map #(.. % -parentElement -parentElement)
                        (filter #(= (.-value %) "on") checkbox-btns))]
      (delete-recipe tr-ele))))
(defn filter-recipes [e]
  (let [el (.-currentTarget e)
        filter-string (.-value el)
        selector (if (empty? filter-string)
                   "tr[data-recipe-name]"
                   (str "tr[data-recipe-name*=\"" filter-string "\"]"))]
    (when VERBOSE
      (js/console.log "filter-string" filter-string)
      (js/console.log selector)
      (js/console.log (.querySelectorAll
                       (js/document.getElementById "table-body")
                       selector)))
    (doseq [tr-ele (.querySelectorAll
                    (js/document.getElementById "table-body") "tr")]
      (.. tr-ele -classList (add "hidden")))
    (doseq [tr-ele (.querySelectorAll
                    (js/document.getElementById "table-body")
                    selector)]
      (.. tr-ele -classList (remove "hidden")))))
(defn loggin-event [_]
  (js/console.log "recipes "recipes)
  (js/console.log "link" APP_URL))
(defn ^:dev/after-load start []
  (when VERBOSE
    (js/console.log "start")
    (js/console.dir recipes))
  (when VERBOSE
    (let [logging-el (doto (js/document.createElement "button")
                       (set! -innerHTML "logging 3")
                       (set! -id "logging-btn")
                       (.addEventListener "click" loggin-event))
          el-style (.-style logging-el)]
      (doto el-style
        (set! -position "fixed")
        (set! -top "100px")
        (set! -left "10px"))
      (.appendChild js/document.body logging-el)))
  (doseq [clip clipboards-btns]
    (.addEventListener clip "click" recipe-link->clipboard))
  (doseq [checkbox checkbox-btns]
    (.addEventListener checkbox "click" toggle-checkmark))
  (.addEventListener delete-btn "click" delete-recipes)
  (.addEventListener filter-box "input" filter-recipes)
  )
(defn ^:dev/before-load stop []
  (when VERBOSE
    (js/console.log "stop"))
  (when VERBOSE
    (.removeEventListener (js/document.getElementById "logging-btn") "click" loggin-event)
    (js/console.log "logbtn" (js/document.getElementById "logging-btn"))
    (.remove (js/document.getElementById "logging-btn")))
  (doseq [clip clipboards-btns]
    (.removeEventListener clip "click" recipe-link->clipboard))
  (doseq [checkbox checkbox-btns]
    (.removeEventListener checkbox "click" toggle-checkmark))
  (.removeEventListener delete-btn "click" delete-recipes)
  (.removeEventListener filter-box "input" filter-recipes))
(defn init []
  (js/console.log "hiii init")
  (start))


