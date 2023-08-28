(ns khsalim.backend.routers
  (:require
   [clojure.set :refer [map-invert]]
   [khsalim.backend.customer :refer [render-form]]
   [khsalim.backend.utils :as ku :refer [config cookie-header-middleware wrap-jwt-authentication auth-middleware create-token]]
   [khsalim.backend.db :as db :refer [db-dev]]
   
   [clj-http.client :as client]
   

   [reitit.ring.malli :as malli]

   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [reitit.core :as rc]
   
   [ring.util.response :as rur]
   [ring.middleware.cookies :refer [wrap-cookies]]
   ;; [ring.util.mime-type :as mime-type]
   [reitit.ring.middleware.dev]
   [reitit.ring.middleware.exception :refer [exception-middleware]]
   [reitit.ring.middleware.muuntaja :refer [format-middleware
                                            #_format-negotiate-middleware
                                            #_format-request-middleware
                                            #_format-response-middleware]]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   
   [reitit.ring.middleware.multipart :refer [multipart-middleware]]
   [selmer.parser :as selmer]
   [clojure.java.io :as io]))
;; (filter #(clojure.string/starts-with? (second %) "image") mime-type/default-mime-types)
(def mime-type->ext {"image/jpeg" "jpg" "image/png" "png"})
(declare dev-router)
(set! *warn-on-reflection* true)
(selmer/set-resource-path! (io/resource "templates"))

(def environment (get config :env :dev))
(def jwt-cookie-name "JWT_TOKEN")

(when (= environment :dev)
  (require '[clojure.java.javadoc :as jdoc]
           '[clojure.reflect :as reflect]
           '[clojure.inspector :as insp]
           '[clojure.pprint :as pp]
           '[clojure.repl :as repl])
  (jdoc/add-remote-javadoc "org.eclipse.jetty.server" "https://www.eclipse.org/jetty/javadoc/jetty-9/")
  :required)


(def datasource-middleware
  {:name ::datasource
   :wrap (fn datasource-wrap [handler]
           (fn ds-handler [req]
             (dissoc
              (handler
               (assoc req :ds db/ds))
              :ds)))})
(def echo-middleware
  {:name ::echo
   :wrap (fn echo-wrap [handler message]
           (fn echo-handler [req]
             (println (java.util.Date.) message)
             (def tmp req)
             (handler req)))})
(defn static-file
  ([file-name] (static-file file-name {}))
  ([file-name context-map]
   (if (= environment :dev)
     (fn [_] (rur/response (selmer/render-file (io/resource (str "pages/" file-name))  context-map)))
     (fn [_] (rur/file-response (str "public/" file-name))))))

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
  (let [email (get params "email")
        password (get params "password")]
    (if (and email password)
      (if (db/get-user-by-email email)
        {:status 200 :body "email already taken"}
        {:status 200 :body (str "created user" (db/create-user email (str (gensym "user")) password))})
      ;else
      {:status 400 :body "email or password messing. must be in a form urlencoded"})))
(defn confirm-user [{:keys [params] :as req}]
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
             :cookies {jwt-cookie-name {:value (create-token {:email email :id 1})
                                        :same-site :lax :domain (:uri req)}}})
          ;else
          (do
            (println "I'm here")
            {:status 400 :body message})))
      {:status 400 :body "email or password messing. must be in a form urlencoded to login"})))
(defn dashboard [_]
  (rur/response (selmer/render-file "dashboard.html"
                                    {:name "salim"})))
(defn recipe [{{:keys [recipe-id]} :path-params}]
  (let [recipe-id (try (Integer/parseInt recipe-id)
                       (catch Exception _ (db/log-message (str "recipe-id is not Integer it is " recipe-id)) 0))]
    (rur/response
     (selmer/render-file "recipe.html"
                         (get-in @db-dev [::db/recipes recipe-id])))))
(defn make-recipe [_]
  (if (= environment :dev)
    (rur/response (selmer/render-file (io/resource "pages/make.html") {}))
    (rur/file-response "public/make.html")))

(defn generate-img-url [_]
  (let [url (random-uuid)]
    (println url)
    (swap! db-dev update ::db/urls conj {:url url :date (java.util.Date.)})
    (rur/response {:url (str "http://localhost:3000/api/v1/img/" url)})))
(defn get-img-url [{{:keys [url]} :path-params}]
    (println "asked for url:" url)
  (rur/file-response (str "public/vault/" url)))
(defn post-img-url [{{:keys [url]} :path-params ,
                     {:strs [media]} :multipart-params}]
  (if (get mime-type->ext (get media :content-type))
    (let [file-name url]
      (println "posting to file:" file-name)

      (io/copy (get media :tempfile) (io/file "public/vault" file-name))
      (rur/created (str "/api/v1/img/" file-name)))
    ;;else
    (rur/bad-request "file type not supported")))

(defn sign-up [{{:strs [code]} :params
                ds :ds}]
  (let [result-body-json (ku/github-access-token code)]
    (if-let [atoken (get result-body-json "access_token")]
      (let [email (ku/github-user-email atoken)
            {:keys [name avatar-url]} (ku/github-user-info atoken)
            {:users/keys [id]} (db/create-authenticated-user
                                ds
                                [name email "github" (get result-body-json "refresh_token") avatar-url])
            [refresh-token token] (ku/create-tokens {:id id :email email :name name})]
        (db/insert-refresh-token ds [refresh-token id])
        (rur/set-cookie
         (rur/response {:message "Successfully authorized!"
                        :refresh-token refresh-token
                        :success true})
         jwt-cookie-name token))
      (rur/bad-request {:message "bad request to get token"
                        :success false}))))
(defn echo [_]
  {:status 200, :body (str "salim khatib (me)" (java.util.Date.))})

(defn static-routes []
  [["/" {:name ::root
         :get (static-file "index.html")}]
   ["/index.html" #_(java.io.File. "public/index.html")
    {:name ::landing
     :get (static-file "index.html")}]
   ["/tmp.html" (static-file "tmp.html" {:client-id (get-in config [:github :client-id])
                                         :redirect-uri (get-in config [:github :redirect-uri])})]
   ["/assets/*" (ring/create-file-handler)]])
(defn api-routes []
  [["/api"
    ["/v1"
     ["/recipe/:order-id" {:name ::order
                           :middleware [wrap-jwt-authentication
                                        auth-middleware
                                        [echo-middleware "order"]]
                           :get customer-form
                           :post confirm-choice}]
     ["/getUrl" {;:name ::order
                                        ;:middleware [wrap-jwt-authentication auth-middleware]
                 :muuntaja m/instance
                 :middleware [format-middleware]
                 :get generate-img-url}]
     ["/img/:url" ;; mock s3 storage
      {:get get-img-url
       :post {:handler post-img-url
              :parameters {:multipart [:map [:file malli/temp-file-part]]}
              :middleware [parameters-middleware
                           multipart-middleware]
              :muuntaja m/instance}}]]]
   ["/signup" {:name ::signup
               :muuntaja m/instance
               :middleware [datasource-middleware
                            wrap-cookies
                            format-middleware
                            parameters-middleware
                            [echo-middleware "signup endpoint!"]]
               :get sign-up}]])

(defn router-gen []
  (ring/router
   [(static-routes)

    ["/register" {:name ::register
                  :get (static-file "register.html")
                  :post {:handler add-user
                         :muuntaja m/instance
                         :middleware [parameters-middleware
                                      format-middleware
                                      exception-middleware
                                      [echo-middleware "adding user!"]]}}]

    ["/login" {:get echo
               :post {:handler confirm-user
                      :muuntaja m/instance
                      :middleware [parameters-middleware
                                   format-middleware
                                   exception-middleware]}}]
    ["/echo" {:name ::echo
              ;; :middleware [[cookie-header-middleware "user_jwt"] wrap-jwt-authentication auth-middleware] ; old
              :parameters {:multipart [:map [:file malli/temp-file-part]]}
              :middleware [datasource-middleware
                           wrap-cookies
                           parameters-middleware
                           multipart-middleware
                           [echo-middleware "echo"]]
              :muuntaja m/instance
              :get echo
              :post echo}]
    ["/dashboard" {:get dashboard
                   :middleware [datasource-middleware
                                wrap-cookies
                                [cookie-header-middleware jwt-cookie-name]
                                [echo-middleware "daaaashboarrrrrd!"]]}]
    ["/recipes/cook/:recipe-id" {:name ::recipe
                                 :get recipe}]
    ["/recipes/make" {:name ::make
                      :get (static-file "recipe.html")}]
    (api-routes)]
   {:reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs}
   #_{:data {:muuntaja m/instance
             :middleware [parameters-middleware
                          format-middleware
                          exception-middleware]}
      :exception pretty/exception}))

(def prod-router (constantly (router-gen)))
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

(def app
  (ring/ring-handler
   ;; ring/router the value
   (dev-router router-gen)
   ;; (router-gen)
   (ring/routes
    #_(ring/redirect-trailing-slash-handler)
    (ring/create-default-handler))))

(comment
  (app {:request-method :get :uri "/echo"})
  (app {:request-method :get :uri "/index.html"})
  (app {:request-method :get :uri "/"})
  (app {:request-method :get :uri "/dashboard"})
  (app {:request-method :get :uri "/recipes/make"})
  (app {:request-method :get :uri "/recipes/cook/0"})
  (app {:request-method :get :uri "/tmp.html"})
  (app {:request-method :get :uri "/api/v1/getUrl"})
  (app {:request-method :get :uri "/assets/img/test1.jpg"})
  (app {:request-method :get :uri "/signup" :query-string "code=1ac9f0aa84915b6aef69"})
  tmp
  res
  
  "ghu_9yrtq6FduvNd0ruJNzNYwNtYmCaB8g3WcfTx"

  ;;{"access_token":"ghu_3W2t6HiQoFPMtNwHG7igikOGzuJKpF12hEPu","expires_in":28800,"refresh_token":"ghr_cDKGUAjD9Wd01x1wrL3cW8PYWyyC3HSwYgq2ZNOHcoZ7dStSdlJGSGWUouluwvPFgtvys21z5YiW","refresh_token_expires_in":15897600,"token_type":"bearer","scope":""}
  (select-keys tmp [:server-name :server-port :scheme :request-method :uri :query-string])

  (-> tmp
      :params)
  ring.middleware.cookies/cookies-request
  :rfc)
(comment
  ;; pomegranate
  (require '[cemerick.pomegranate :as pom]
           '[cemerick.pomegranate.aether :as aether])
  (pom/add-dependencies :coordinates '[[clj-http/clj-http "3.12.3"]]
                        :repositories (merge aether/maven-central
                                             {"clojars" "https://clojars.org/repo"}))
  (require '[clj-http.client :as client])
  
  (client/post "https://github.com/login/oauth/access_token" {:accept :json
                                             :form-params {:client_id "1"
                                                           :client_secret "2"
                                                           :code "a"
                                                           :as :json}})
  :rfc)
