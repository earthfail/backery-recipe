(ns khsalim.backend.core
  (:require [clojure.java.io :as io]
            [khsalim.backend.server :as server])
  (:gen-class))

(defn -main []
  (server/start-prod)
  (println "working.....zzz"))
