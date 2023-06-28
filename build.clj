(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def version "0.0.1")
(def app-name "backery-recipe")
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder app-name version))

(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder)))
(defn uber [_]
  (clean nil)

  (b/copy-dir {:src-dirs ["resources" "public"]
               :target-dir jar-content})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir jar-content})
  (b/uber {:class-dir jar-content
           :uber-file uber-file-name
           :basis basis
           ;; :main 'khsalim.backend.server
           :main 'khsalim.backend.core
           })
  (println (format "Uber file created \"%s\"" uber-file-name)))

