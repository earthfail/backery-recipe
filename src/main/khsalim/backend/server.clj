(ns khsalim.backend.server
  (:gen-class)
  (:require
   [khsalim.backend.customer :refer [render-form]]
   [khsalim.backend.utils :refer [cookie-header-middleware wrap-jwt-authentication auth-middleware create-token]]
   [khsalim.backend.db :as db :refer [db-dev]]
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
   ;; [reitit.ring.middleware.multipart :refer [multipart-middleware]]
   ))



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
(defn add-user [{:keys [params] :as req}]
  ;; extract user and password
  (println "adding usessssr!")
  (println (java.util.Date.))
  (def tmp req)
  (let [email (get params "email")
        password (get params "password")]
    (if (and email password)
      (if (db/get-user-by-email email)
        {:status 200 :body "email already taken"}
        {:status 200 :body (str "created user" (db/create-user email (str (gensym "user")) password))})
      ;else
      {:status 400 :body "email or password messing. must be in a form urlencoded"})))
(defn confirm-user [{:keys [params] :as req}]
  (println "loggin user!")
  (println (java.util.Date.))
  (def tmp req)
  (let [email (get params "email")
        password (get params "password")]
    (if (and email password)
      (let [{:keys [valid update message] :or {valid false}}
            (db/get-user-by-credentials email password)]
        (if valid
          (if update
            (rur/header (rur/redirect "/" :see-other)
                        "message" "password needs updating")
            {:status 200 :body "everything is okay"
             :cookies {"user_jwt" {:value (create-token {:email email :id 1})
                                   :same-site :lax :domain (:uri req)}}})
          ;else
          (do
            (println "I'm here")
            {:status 400 :body message})))
      {:status 400 :body "email or password messing. must be in a form urlencoded to login"})))
(defn echo [req]
  (println "doing echo!!")
  (println (java.util.Date.))
  (def tmp req)
  {:status 200, :body {:id "abc" :name "kkk"}})
;; from https://github.com/lambdaisland/souk/blob/main/src/lambdaisland/souk/util/dev_router.clj
(defn dev-router
  "Given a function which builds a reitit router, returns a router which rebuilds
  the router on every call. This makes sure redefining routes in a REPL works as
  expected. Should only every be used in development mode, since it completely
  undoes all of reitit's great performance."
  [new-router]
  (reify rc/Router
    (router-name [_] (rc/router-name (new-router)))
    (routes [_] (rc/routes (new-router)))
    (compiled-routes [_] (rc/compiled-routes (new-router)))
    (options [_] (rc/options (new-router)))
    (route-names [_] (rc/route-names (new-router)))
    (match-by-path [_ path] (rc/match-by-path (new-router) path))
    (match-by-name [_ name] (rc/match-by-name (new-router) name))
    (match-by-name [_ name path-params] (rc/match-by-name (new-router) name path-params))))

(def static-routes
  [["/" {:name ::root
         :get (fn [_] (rur/file-response "public/index.html"))}]
   ["/index.html" #_(java.io.File. "public/index.html")
    {:name ::landing
     :get (fn [_] (rur/file-response "public/index.html"))}]
   ["/assets/*" (ring/create-file-handler)]])
(def api-routes
  ["/api"
   ["/v1"
    ["/recipe/:order-id" :name ::order
      :middleware [wrap-jwt-authentication auth-middleware]
      :get customer-form
     :post confirm-choice]]])
(def router-gen
  (fn [] (ring/router
          [static-routes
           ["/register" {:name ::register
                         :get (fn [_] (rur/file-response "public/register.html"))
                         :post {:handler add-user
                                :muuntaja m/instance
                                :middleware [parameters-middleware
                                             format-middleware
                                             exception-middleware]}}]
           ["/login" {
                      :get echo
                      :post {:handler confirm-user
                             :muuntaja m/instance
                             :middleware [parameters-middleware
                                          format-middleware
                                          exception-middleware]}}]
           ["/echo" {:name ::echo
                     :middleware [[cookie-header-middleware "user_jwt"] wrap-jwt-authentication auth-middleware]
                     :get echo
                     :post echo}]
           api-routes]
          #_{:data {:muuntaja m/instance
                    :middleware [parameters-middleware
                                 format-middleware
                                 exception-middleware]}
             :exception pretty/exception})))
(def app
  (ring/ring-handler
   ;; ring/router the value
   (dev-router router-gen)
   (ring/routes
    #_(ring/redirect-trailing-slash-handler)
    (ring/create-default-handler))))

(def service-map
  {:port 3001
   :legacy-return-value? false})
(defn start-dev []
  (reset! server-dev
          (jetty/run-jetty #'app {:port 3000, :join? false :host "0.0.0.0"}))
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
  (app {:request-method :get, :uri "/"})
  (app {:request-method :get, :uri "/register"})
  (app {:request-method :post, :uri "/register"
        :body {:a 1 :b 2}})
  (-> app
      ring/get-router
      (rc/match-by-path "/login"))
  (app {:request-method :post :uri "/login"})
  (app {:request-method :get, :uri "/assets/favicon.png"})
  (app {:request-method :get, :uri "/"})

  :rfc)
;; => nil

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
