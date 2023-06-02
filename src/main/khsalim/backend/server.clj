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
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]))

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
(defn echo [req]
  (println "doing echo!!")
  (println (java.util.Date.))
  (def tmp req)
  {:status 200, :body {:id "abc" :name "kkk"}})

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:name ::landing
           :get (constantly (rur/file-response "public/index.html"))}]
     ["/assets/*" (ring/create-file-handler)]
     ["/login" {:middleware [wrap-jwt-authentication auth-middleware]
                :get echo
                :post echo}]
     ["/register" {:name ::register
                   :get echo
                   :post echo}]
     ["/echo" {:name ::echo
               :get echo}]
     ["/api"
      ["/v1"
       ["/order/:order-id" {:name ::order
                            :middleware [wrap-jwt-authentication auth-middleware]
                            :get customer-form
                            :post confirm-choice}]]]]
    {:data {:muuntaja m/instance
            :middleware [parameters-middleware
                         format-middleware
                         exception-middleware]}
     :exception pretty/exception})
   (ring/routes
    (ring/redirect-trailing-slash-handler)
    (ring/create-default-handler))))

(def service-map
  {:port 3001
   :legacy-return-value? false})

;; (defn start []
;;   (run-server app service-map))

(defn start-dev []
  (reset! server-dev
          (jetty/run-jetty #'app {:port 3000, :join? false}))
  ;; to stop run (.stop server)
  (println "server running in port 3000"))

(defn start []
  (println "starting server at port" (service-map :port))
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
  ;; (start-dev)
  (restart)
  ;; nix-env -iA nixpkgs.emacs
  (-> app
      (ring/get-router)
      (rc/match-by-path "/api/v1/order/123?sss=456"))
  
  (-> app
      ring/get-router
      (rc/match-by-path "/")
      keys) #_(:template :data :result :path-params :path)
  (-> app
      ring/get-router
      (rc/match-by-path "/")
      :result
      keys)
  (app {:request-method :get, :uri "/"})
  (app {:request-method :get, :uri "/api/v1/order/123"})
  (-> ((auth-middleware customer-form)
       {:request-method :get, :uri "/api/v1/order/123" :path-params {:order-id "123"}, :identity (create-token {:name "salim" :id "3"})})) ;; this works!!!
  (->> app
       (ring/get-router)
       (rc/routes)
       (map first))
  (keys tmp)
  (-> (buddy.auth.middleware/authenticate-request tmp (list backend.utils/buddy-backend)))

  (app {:request-method :get, :uri "/register", :headers {"Authorization" "Token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"}})
  @server
  ;; for authorization we should have "authorization" header with

  (some->> (buddy.auth.http/-get-header {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"}} "authorization")
           (re-find (re-pattern (str "^" "Token" " (.+)$")))
           second)
  #_(defn- parse-header
      [request token-name]
      (some->> (buddy.auth.http/-get-header request "authorization")
               (re-find (re-pattern (str "^" token-name " (.+)$")))
               (second)))
  #_(parse-header {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"}} "Token")
  (some->> {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.DrFsXMKgngQGzs-Be14UhJKRVMm-CO2I_Qfy2Hr75G8"}}
           (buddy.auth.protocols/-parse backend.utils/buddy-backend)
           (buddy.auth.protocols/-authenticate backend.utils/buddy-backend {}))

  (buddy.auth.middleware/authenticate-request {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.DrFsXMKgngQGzs-Be14UhJKRVMm-CO2I_Qfy2Hr75G8"}} (list backend.utils/buddy-backend))
  (buddy.auth.middleware/authentication-request {:request-method :get, :uri "/register", :headers {"authorization" "Token eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.DrFsXMKgngQGzs-Be14UhJKRVMm-CO2I_Qfy2Hr75G8"}} backend.utils/buddy-backend)

  (buddy.auth.middleware/authentication-request {:request-method :get, :uri "/register", :headers {"authorization" (str "Token " (backend.utils/create-token {:name "me" :id 69}))}} backend.utils/buddy-backend)
  :rfc)
;; => nil
