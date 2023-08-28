(ns khsalim.backend.server
  (:gen-class)
  (:require
   [khsalim.backend.routers :as krouters :refer [app]]
   
   ;; [org.httpkit.server :refer [run-server server-stop!]]
   [ring.adapter.jetty :as jetty]
   ))
;;  Startup: /usr/local/bin/clojure -Sdeps \{\:deps\ \{nrepl/nrepl\ \{\:mvn/version\ \"1.0.0\"\}\ cider/cider-nrepl\ \{\:mvn/version\ \"0.31.0\"\}\ cider/piggieback\ \{\:mvn/version\ \"0.5.2\"\}\}\ \:aliases\ \{\:cider/nrepl\ \{\:main-opts\ \[\"-m\"\ \"nrepl.cmdline\"\ \"--middleware\"\ \"\[cider.nrepl/cider-middleware\,cider.piggieback/wrap-cljs-repl\]\"\]\}\}\} -M:cider/nrepl

;; WARNING: CIDER 1.8.0-snapshot requires cider-nrepl 0.31.0, but you're currently using cider-nrepl 0.22.4. The version mismatch might break some functionality! (More information)
;; WARNING: update-vals already refers to: #'clojure.core/update-vals in namespace: cider.nrepl.inlined-deps.orchard.v0v5v3.orchard.misc, being replaced by: #'cider.nrepl.inlined-deps.orchard.v0v5v3.orchard.misc/update-vals
;; WARNING: update-keys already refers to: #'clojure.core/update-keys in namespace: cider.nrepl.inlined-deps.orchard.v0v5v3.orchard.misc, being replaced by: #'cider.nrepl.inlined-deps.orchard.v0v5v3.orchard.misc/update-keys

(defonce server (atom nil))
(defonce server-dev (atom nil))



(def service-map
  {:port 3000
   :legacy-return-value? false
   :join? false})
(defn start-dev []
  (reset! server-dev
          (jetty/run-jetty #'app {:port 3000, :join? false :host "0.0.0.0"}))
  ;; to stop run (.stop server)
  (println "jetty server running in port 3000"))


(comment
  
  (start-dev)
  ;; (restart)
  ;; setup for development
  @server-dev
  (.stop @server-dev)
  ;; (.start @server-dev)
  (do
    (require '[clojure.java.javadoc :as jdoc]
             '[clojure.reflect :as reflect]
             '[clojure.inspector :as insp]
             '[clojure.pprint :as pp]
             '[clojure.repl :as repl])
    (jdoc/add-remote-javadoc "org.eclipse.jetty.server" "https://www.eclipse.org/jetty/javadoc/jetty-9/")
    ;;(set! *print-length* nil)
    ;;(set! *print-level* 4)
    ;; (jdoc/javadoc (:body tmp))
    (defn table-reflect [val]
      (->> (reflect/reflect val) :members (sort-by :name) (pp/print-table [:name :flags :parameter-types :return-type]))))

  (org.eclipse.jetty.server.Server/getVersion) ;"9.4.48.v20220622"
  tmp                                          ; C-c M-i cider inspect
  #_{"n" "emailasdfas", "pss" "passwdcvvccvvc", "button" "send"}
  @server
  ;; C-c C-d C-d cider-doc
  (-> app
      ring/get-router
      ;; (rc/options)
      ;; (rc/routes)
      ;; (rc/match-by-name ::register {:a 1 :b 2})
      (rc/match-by-path "/api/v1/getUrl"))
  (app {:request-method :get, :uri "/"})
  (app {:request-method :get, :uri "/register"})
  (app {:request-method :post, :uri "/register"
        :body {:a 1 :b 2}})
  (-> app
      ring/get-router
      (rc/match-by-path "/login"))
  (-> app
      ring/get-router
      (rc/match-by-path "/echo"))
  (app {:request-method :post :uri "/login"})
  (app {:request-method :get, :uri "/assets/favicon.png"})
  (app {:request-method :get, :uri "/"})

  (app {:request-method :get :uri "/recipes/cook/0"})
  (app {:request-method :get, :uri "/api/v1/getUrl"}) ;; { :url "http:/localhost:3000/api/v1/img/072730dd-ae83-43ee-99f2-90163c7815b9" }
  (app {:request-method :get, :uri "/api/v1/img/072730dd-ae83-43ee-99f2-90163c7815b9"})

  (app {:request-method :get, :uri "/echo"})
  :rfc)

(comment
  ;; call Var 
  #'service-map
  (.fn  #'service-map)
  (.invoke #'service-map :port)
:rfc)
(comment
  ;; http-kit server functions
  (defn start []
    (println "starting httpkit server at port" (service-map :port))
    (reset! server
            (run-server app service-map service-map)))
  (defn stop []
    (when-not (nil? @server)
      (println "stop server")
      (server-stop! @server)
      #_(@server :timeout 100)
      (reset! server nil)))
  (defn restart []
    (println "restart server")
    (stop)
    (start))
  )
(comment
  ;; just for documentation
  ;; for authorization we should have "authorization" header with
  
  (-> ((auth-middleware customer-form)
       {:request-method :get, :uri "/api/v1/order/123" :path-params {:order-id "123"}, :identity (create-token {:name "salim" :id "3"})})) ;; this works!!!
  
  (buddy.auth.middleware/authentication-request {:request-method :get, :uri "/register", :headers {"authorization" (str "Token " (backend.utils/create-token {:name "me" :id 69}))}} backend.utils/buddy-backend)
  
  )

;; from https://cljdoc.org/d/metosin/reitit/0.7.0-alpha4/doc/basics/route-data#customizing-expansion
;; (extend-type java.io.File
;;   rc/Expand
;;   (expand [file _] ; second part is options
;;     {:handler (constantly (slurp file))}))
