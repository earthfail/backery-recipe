(ns khsalim.backend.server
  (:gen-class)
  (:require
   [khsalim.backend.customer :refer [render-form]]
   [khsalim.backend.utils :refer [wrap-jwt-authentication auth-middleware create-token]]
   ;; prod server
   [org.httpkit.server :refer [run-server server-stop!]]
   ;; dev server
   [ring.adapter.jetty :as jetty]
   [reitit.dev.pretty :as pretty]

   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [reitit.core :as rc]

   [ring.util.response :as rur]
   [reitit.ring.middleware.exception :refer [exception-middleware]]
   [reitit.ring.middleware.muuntaja :refer [format-middleware
                                            #_format-negotiate-middleware
                                            #_format-request-middleware
                                            #_format-response-middleware]]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   [reitit.ring.middleware.multipart :refer [multipart-middleware]]))

(defonce db-dev (atom {"123" {:merchant "افنان"
                              :customer "سليم"
                              :items ["1999-01-01" "2023-02-19"]}}))

(defonce server (atom nil))
(defonce server-dev (atom nil))
(defn customer-form [{{:keys [order-id]} :path-params}]
  (if-let [info (get @db-dev order-id)]
    (rur/response (render-form info))
    ;;else
    (rur/not-found (str order-id " not found"))))
(defn confirm-choice [{{:keys [order-id]} :path-params
                       headers :headers
                       uri :uri
                       server-name :server-name
                       server-port :server-port}]
  (println "headers " headers)
  (println "order-id " order-id)
  (if (= (get headers "hhh") "jjj")
    (do
      (swap! db-dev update order-id update :touched inc)
      (rur/created (str server-name ":" server-port uri) "url made"))
    (rur/bad-request "bad request")))
(defn add-user [{:keys [body] :as req}]
  ;; extract user and password
  (println "adding user!")
  (println (java.util.Date.))
  (def tmp req)
  )
(defn echo [req]
  (println "doing echo!!")
  (println (java.util.Date.))
  (def tmp req)
  {:status 200, :body {:id "abc" :name "kkk"}})

(def static-routes
  [["/" {:name ::root
     :get (constantly (rur/file-response "public/index.html"))}]
   ["/index.html" #_(java.io.File. "public/index.html")
    {:name ::landing
     :get (constantly (rur/file-response "public/index.html"))}]
   ["/assets/*" (ring/create-file-handler)]])
(def api-routes
  ["/api"
   ["/v1"
    ["/order/:order-id" {:name ::order
                         :middleware [wrap-jwt-authentication auth-middleware]
                         :get customer-form
                         :post confirm-choice}]]])
(def app
  (ring/ring-handler
   (ring/router
    [static-routes
     ["/register" {:name ::register
                   :get echo
                   :post {:handler add-user
                          :parameters :multipart
                          :muuntaja m/instance
                          :middleware [parameters-middleware
                                       format-middleware
                                       multipart-middleware
                                       exception-middleware]}}]
     ["/login" {:middleware [wrap-jwt-authentication auth-middleware]
                :get echo
                :post echo}]
     ["/echo" {:name ::echo
               :get echo
               :post echo}]
     api-routes]
    #_{:data {:muuntaja m/instance
              :middleware [parameters-middleware
                           format-middleware
                           exception-middleware]}
       :exception pretty/exception})
   (ring/routes
    #_(ring/redirect-trailing-slash-handler)
    (ring/create-default-handler))))
;; from https://cljdoc.org/d/metosin/reitit/0.7.0-alpha4/doc/basics/route-data#customizing-expansion
(extend-type java.io.File
  rc/Expand
  (expand [file _] ; second part is options
    {:handler (constantly (slurp file))}))

(def service-map
  {:port 3001
   :legacy-return-value? false})
(defn start-dev []
  (reset! server-dev
          (jetty/run-jetty #'app {:port 3000, :join? false}))
  ;; to stop run (.stop server)
  (println "jetty server running in port 3000"))
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

(comment
  (start-dev)
  ;; (restart)
  ;; setup for development
  (do
    (require '[clojure.java.javadoc :as jdoc]
             '[clojure.reflect :as reflect]
             '[clojure.inspector :as insp]
             '[clojure.pprint :as pp]
             '[clojure.repl :as repl])
    (jdoc/add-remote-javadoc "org.eclipse.jetty.server" "https://www.eclipse.org/jetty/javadoc/jetty-9/")
    (set! *print-length* nil)
    (set! *print-level* 4)
    ;; (jdoc/javadoc (:body tmp))
    (defn table-reflect [val]
      (->> (reflect/reflect val) :members (sort-by :name) (pp/print-table [:name :flags :parameter-types :return-type]))))

  (org.eclipse.jetty.server.Server/getVersion) ;"9.4.48.v20220622"
  tmp ; C-c M-i cider inspect
  #_{ "n" "emailasdfas", "pss" "passwdcvvccvvc", "button" "send" }
  
  ;; C-c C-d C-d cider-doc
  (-> app
      ring/get-router
      ;; (rc/options)
      ;; (rc/routes)
      ;; (rc/match-by-name ::register {:a 1 :b 2})
      (rc/match-by-path "/register")
      )
  (app {:request-method :post, :uri "/register"
        :body {:a 1 :b 2}})
  (app {:request-method :get, :uri "/assets/favicon.png"})
  (app {:request-method :get, :uri "/"})
  rc/router
  (rur/file-response "public/index.html")
  :rfc)

(comment
  ;; just for documentation
  ;; for authorization we should have "authorization" header with
  (app {:request-method :get, :uri "/"})
  (app {:request-method :get, :uri "/api/v1/order/123"})
  (-> ((auth-middleware customer-form)
       {:request-method :get, :uri "/api/v1/order/123" :path-params {:order-id "123"}, :identity (create-token {:name "salim" :id "3"})})) ;; this works!!!
  (app {:request-method :get, :uri "/register", :headers {"Authorization" "Token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"}})
  (buddy.auth.middleware/authentication-request {:request-method :get, :uri "/register", :headers {"authorization" (str "Token " (backend.utils/create-token {:name "me" :id 69}))}} backend.utils/buddy-backend)
  (some->> {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.DrFsXMKgngQGzs-Be14UhJKRVMm-CO2I_Qfy2Hr75G8"}}
           (buddy.auth.protocols/-parse backend.utils/buddy-backend)
           (buddy.auth.protocols/-authenticate backend.utils/buddy-backend {}))
  (some->> (buddy.auth.http/-get-header {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"}} "authorization")
           (re-find (re-pattern (str "^" "Token" " (.+)$")))
           second)
  )
