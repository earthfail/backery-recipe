(ns build
  (:require
   [clojure.tools.build.api :as b]
   [asset-minifier.core :as am]
   [selmer.parser :as selmer]
   ;; [selmer.util :as sl]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def version "0.0.2")
(def app-name "backery-recipe")
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder app-name version))

(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder))
  (b/delete {:path "minified"})
  (println (format "minified folder \"%s\" removed" "minified"))
  (b/delete {:path "tmp"})
  (println "tmp folder removed"))

(defn compile-css [_]
  (doseq [css-file (.listFiles (io/file (io/resource "styles")))]
    (println "compile " (.getPath css-file))
    (b/process {:command-args ["npx" "tailwindcss" "--input" (.getPath css-file) "--output" (str "public/css/" (.getName css-file))]
                :env {"PROJ_ENV" "production"}})))
(defn compile-cljs [{builds :builds}]
  (let [builds (if builds builds
                   (with-open [f (io/reader "shadow-cljs.edn")]
                     (-> f
                         (java.io.PushbackReader.)
                         edn/read
                         (get :builds)
                         keys)))]
    (println "builds are" builds)
    (b/process {:command-args (into
                               ["npx" "shadow-cljs" "release"]
                               (map str builds))})))
(defn compile-html
  "Given a `context-map` for `selmer.parser/render-file`,takes html files in resources/pages
  and output them to public folder"
  [context-map]
  ;; (println "selmer custom path is " sl/*custom-resource-path*)
  (doseq [html-file (.list (io/file (io/resource "pages")))]
    (println "compile " html-file)
    (spit (str "public/" html-file)
          (selmer/render-file (str "pages/" html-file) context-map))))
(defn minify-html [_]
  (println "minify html templates...")
  (am/minify-html "resources/templates" "minified/templates" {:enabled true :remove-comments true :remove-multi-spaces true :compress-css true})
  (println "minify html files...")
  (am/minify-html "public" "minified/pages" {:enabled true :remove-comments true :remove-multi-spaces true :compress-css true})
  )

(defn minify [_]
  (minify-html nil)
  ;; minify css and js
  (println "minify css...")
  (compile-css nil)
  )
(defn uber [{:keys [css html template cljs clean?], :or {css false html false template false cljs false clean? true}}]
  (clean nil)
  ; (println "compile cljs to js...")
  ; (compile-cljs nil)
  ; (b/delete {:path "public/js/cljs-runtime"})
  ; (b/delete {:path "public/js/manifest.edn"})
  (b/copy-dir {:src-dirs ["resources"]
               :target-dir "tmp/resources"})

  (when css
    (compile-css nil))
  (when cljs
    (compile-cljs nil))

  (when template
    (compile-html {}))
  (when html
    (do
      (minify-html nil)
      (b/copy-dir {:src-dirs ["minified/templates"]
                   :target-dir "tmp/resources/templates"})
      (b/copy-dir {:src-dirs ["minified/pages"]
                   :target-dir "public"})))
  
  (b/copy-dir {:src-dirs ["tmp/resources"]
               :target-dir jar-content})

  (when clean?
    (println "deleting tmp and minified")
    (b/delete {:path "tmp"})
    (b/delete {:path "minified"}))

  (println "compile clj...")
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

