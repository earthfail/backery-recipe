(ns build
  (:require
    [clojure.tools.build.api :as b]
    [asset-minifier.core :as am]
    [clojure.java.io :as io]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def version "0.0.1")
(def app-name "backery-recipe")
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder app-name version))

(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn compile-css [_]
  (doseq [css-file (.list (io/file "styles"))]
    (println "compile " css-file)
    (b/process {:command-args ["npx" "tailwindcss" "--input" (str "styles/" css-file) "--output" (str "public/css/" css-file)]
		:env {"PROJ_ENV" "production"}})))
(defn compile-cljs [_]
  (b/process {:command-args ["npx" "shadow-cljs" "release" "recipe" "register"]}))
(defn minify-html [_]
  (println "minify html templates...")
  (am/minify-html "dev/resources/templates" "resources/templates" {:enabled true :remove-comments true :remove-multi-spaces true :compress-css true})
  (println "minify html files...")
  (am/minify-html "dev/resources/html" "public" {:enabled true :remove-comments true :remove-multi-spaces true :compress-css true})
)
(defn minify [_]
  (minify-html nil)
  ;; minify css and js
  (println "minify css...")
  (compile-css nil)
  )
(defn uber [_]
  (clean nil)
  ; (println "compile cljs to js...")
  ; (compile-cljs nil)
  ; (b/delete {:path "public/js/cljs-runtime"})
  ; (b/delete {:path "public/js/manifest.edn"})

  ; (println "minify html+css")
  ; (minify nil)
  
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

