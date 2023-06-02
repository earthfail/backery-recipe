(ns khsalim.backend.customer
  (:require [selmer.parser :as parser]
            [clojure.java.io :as io]))
(defn render-form [info]
  (parser/render-file (io/resource "templates/customer.html") info))

(comment
  
  parser/render-template
  parser/render-file
  parser/templates
  (p/render "Hello {{name}}!" {:name "mee"})
  (spit "public/index.html"
        (p/render-file (io/resource "templates/customer.html")
                       {:name "سليم" :merchant "افنان"
                        :items ["2/06 12:30" "2/06 13:00" "2/07 8:00"]}))
  (p/render "{{d|date:shortDate}}" {:d (java.util.Date.)})
  :rfc
  )
