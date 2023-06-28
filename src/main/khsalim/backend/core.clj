(ns khsalim.backend.core
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (println (slurp (io/resource "input.css")))
  (println "register" (slurp (io/resource "register.html")))
  (println "working.....zzz"))
