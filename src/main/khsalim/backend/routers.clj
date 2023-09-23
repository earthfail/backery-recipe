(ns khsalim.backend.routers
  (:require
   ;;[clojure.set :refer [map-invert]]
   ;;[khsalim.backend.customer :refer [render-form]]
   [khsalim.backend.utils :as ku :refer [config cookie-header-middleware wrap-jwt-authentication auth-middleware create-token]]
   [khsalim.backend.db :as db :refer [ephemeral-db]]

   [clj-http.client :as client]

   [reitit.ring.malli :as malli]

   [muuntaja.core :as m]
   [reitit.ring :as ring]
   [reitit.core :as rc]

   [ring.util.response :as rur]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.cors :refer [wrap-cors]]
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

(def dev (get config :dev true))
(def jwt-cookie-name "JWT_TOKEN")
(def vault-path (if dev
                  "dev/data/vault"
                  (get config :vault "dev/data/vault")))
(when dev
  (require '[clojure.java.javadoc :as jdoc]
           '[clojure.reflect :as reflect]
           '[clojure.inspector :as insp]
           '[clojure.pprint :as pp]
           '[clojure.repl :as repl])
  ;;(jdoc/add-remote-javadoc "org.eclipse.jetty.server" "https://www.eclipse.org/jetty/javadoc/jetty-9/")
  :required)

(def allow-origin [#"http://localhost:3000" #"http://dwave.local:3000"])
(def cors-middleware
  {:name ::cors
   :wrap #_wrap-cors
   (fn my-wrap-cors [h]
     (wrap-cors h
                :access-control-allow-origin allow-origin
                :access-control-allow-methods [:get :post :delete]
                :access-control-allow-credentials "true"))})
(def datasource-middleware
  {:name ::datasource
   :wrap (fn datasource-wrap [handler]
           (fn ds-handler [req]
             (dissoc
              (handler
               (assoc req :ds @#'db/ds))
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
   (if dev
     (fn [_] (rur/response (selmer/render-file (io/resource (str "pages/" file-name))  context-map)))
     (fn [_] (rur/file-response (str "public/" file-name))))))

(defn recipe [{ds :ds
               {:keys [recipe-id]} :path-params}]
  (let [recipe-id (try (Integer/parseInt recipe-id)
                       (catch Exception _ (db/log-message (str "recipe-id is not Integer it is " recipe-id)) 0))]
    (rur/response
     (selmer/render-file "recipe.html"
                         {:recipe-id recipe-id
                          :recipes (db/get-recipe-steps ds recipe-id)}))))

(defn make-recipe [{ds :ds
                    {:keys [id]} :identity}]
  (let [recipe-id (rand-int 100000)]
    #_(db/insert-user-recipe ds id recipe-id)
    (rur/response (selmer/render-file "make.html" {:recipe-id recipe-id}))))

(defn generate-img-url [_]
  (let [url (random-uuid)]
    (println url)
    (swap! ephemeral-db update ::db/urls conj {:url url :date (java.util.Date.)})
    (rur/response {:url (str "http://localhost:3000/api/v1/img/" url)})
    #_(rur/response {:url (str "/api/v1/img/" url)}))) ; http://localhost:3000
(defn get-img-url [{{:keys [url]} :path-params}]
  (println "asked for url:" url)
  (rur/file-response (str vault-path "/" url)))
(defn post-img-url [{{:keys [url]} :path-params ,
                     {:strs [media]} :multipart-params}]
  (if (get mime-type->ext (get media :content-type))
    (let [file-name url]
      (println "posting to file:" file-name)
      (io/copy (get media :tempfile) (io/file vault-path file-name))
      (rur/created (str "/api/v1/img/" file-name)))
    ;;else
    (rur/bad-request "file type not supported")))

(defn dashboard [{{:keys [id name avatar-url]} :identity
                  ds :ds}]
  (let [recipes (db/get-user-recipes ds id)
        linked-recipes (map (partial ku/insert-link "/recipes/cook/") recipes)]
    (rur/response
     (selmer/render-file "dashboard.html"
                         {:name name
                          :avatar-url avatar-url
                          :recipes linked-recipes}))))
(defn sign-up [{:keys [params ds]}]
  (let [{:strs [code]} params
        result-body-json (ku/github-access-token code)]
    (when dev
      (println "body-json " result-body-json))
    (if-let [atoken (get result-body-json "access_token")]
      (let [email (ku/github-user-email atoken)
            {:keys [name avatar-url]} (ku/github-user-info atoken)
            user-record [name email "github" (get result-body-json "refresh_token") avatar-url]]
        (when dev
          (println "email name avatar-url id atoken" email name avatar-url atoken))
        (if-let [{:users/keys [id]} (db/create-authenticated-user ds user-record)]
          (let [[refresh-token token] (ku/create-tokens {:id id :avatar-url avatar-url :name name})
                uri "/dashboard"]
            (when dev
              (println "good" id))
            (db/insert-refresh-token ds [refresh-token id])

            (rur/set-cookie
             (rur/response
              (selmer/render-file "signup.html" {:refresh-token refresh-token
                                                 :name name
                                                 :redirect-uri uri}))
             jwt-cookie-name token))
          ;;else
          (do
            (when dev
              (println "bad"))
            (rur/bad-request {:message "bad request to get id"
                              :success false}))))
      ;;else
      (rur/bad-request {:message "bad request to get token"
                        :success false}))))
(defn register-recipe-finished [{ds :ds
                                 {:keys [recipe-id status]} :body-params}]
  (if (not= :finished status)
    (rur/bad-request (str "not finished" recipe-id " got " status))
    (if (db/register-recipe-statistic ds recipe-id)
      (rur/response "added statistic")
      (rur/status (rur/response "sorry didn't succeed") 502))))
(defn register-recipe [{ds :ds
                        {:keys [id]} :identity
                        {:keys [recipe-id]} :path-params
                        {:keys [recipe-name recipe-description]} :body-params}]
  (db/register-recipe-data ds id recipe-id recipe-name recipe-description)
  (rur/response (str "added recipe data " id recipe-id recipe-name recipe-description)))
(defn upload-recipe [{ds :ds
                      {:keys [id]} :identity
                      {:keys [recipe-id]} :path-params,
                      {:keys [step description url media-type]} :body-params}]
  ;; (println "req recipe" req)
  (db/insert-recipe-step ds [recipe-id step description url media-type])

  (rur/response [recipe-id " test " step description url media-type]))
(defn delete-recipe [{ds :ds
                     {:keys [id]} :identity
                     {:keys [recipe-id]} :path-params}]
  (if (< 0 (get (first (db/delete-recipe ds id recipe-id)) :next.jdbc/update-count))
    (rur/response "deleted!!")
    (rur/status (rur/response "resource gone") 405)))

(defn echo [_]
  #_(rur/response
     (selmer/render-file "signup.html" {:refresh-token "abc"
                                        :name "seeeso"}))
  {:status 200, :body (str "salim khatib (me)" (java.util.Date.))})

(defn static-routes []
  [["/" {:name ::root
         :get (static-file "index.html" {:client-id (get-in config [:github :client-id])
                                         :redirect-uri (get-in config [:github :redirect-uri])})}]
   ["/index.html"
    {:name ::landing
     :get (static-file "index.html" {:client-id (get-in config [:github :client-id])
                                     :redirect-uri (get-in config [:github :redirect-uri])})}]
   (when dev
     ["/tmp.html" (static-file "tmp.html" {:client-id (get-in config [:github :client-id])
                                           :redirect-uri (get-in config [:github :redirect-uri])})])
   ["/assets/"
    ["img/*"  (ring/create-resource-handler {:root "img"})]
    ["js/*"  (ring/create-file-handler {:root "public/js"})]
    ["css/*" (ring/create-file-handler {:root "public/css"})]]])
(defn api-routes []
  [["/api"
    ["/v1" {:middleware [cors-middleware]}
     ["/recipe/:recipe-id" {:name ::order
                            :muuntaja m/instance
                            :middleware [datasource-middleware
                                         wrap-cookies
                                         [cookie-header-middleware jwt-cookie-name]
                                         format-middleware
                                         parameters-middleware]
                            ;:middleware
                            ;; [#_wrap-jwt-authentication
                            ;;  #_auth-middleware
                            ;;  #_format-middleware
                            ;;  #_[echo-middleware "order"]]
                            ;; :get customer-form
                            :patch {:handler register-recipe
                                    :middleware [(when dev
                                                   [echo-middleware "patch recipe"])]}
                            :post {:handler upload-recipe
                                   :middleware [(when dev
                                                  [echo-middleware "post recipe"])]}
                            :delete {:handler delete-recipe
                                     :middleware [(when dev
                                                    [echo-middleware "delete recipe"])]}}]
     ["/statistic" {:muuntaja m/instance
                    :middleware [datasource-middleware
                                 format-middleware]
                    :post register-recipe-finished}] ;; recipe.cljs
     ["/getUrl" {                       ;:name ::order
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
                            (when dev
                              [echo-middleware "signup endpoint!"])]
               :get sign-up}]])

(defn router-gen []
  (ring/router
   [(static-routes)

    ["/register" {:name ::register
                  :get (static-file "register.html")
                  :post {:handler echo #_add-user
                         :muuntaja m/instance
                         :middleware [parameters-middleware
                                      format-middleware
                                      exception-middleware
                                      (when dev
                                        [echo-middleware "adding user!"])]}}]

    ["/login" {:get echo
               :post {:handler echo #_confirm-user
                      :muuntaja m/instance
                      :middleware [parameters-middleware
                                   format-middleware
                                   exception-middleware]}}]
    ["/dashboard" {:get dashboard
                   :middleware [datasource-middleware
                                wrap-cookies
                                [cookie-header-middleware jwt-cookie-name]
                                (when dev
                                  [echo-middleware "daaaashboarrrrrd!"])]}]
    ["/recipes/cook/:recipe-id" {:name ::recipe
                                 :middleware [datasource-middleware]
                                 :get recipe}]
    ["/recipes/make" {:name ::make
                      :middleware [datasource-middleware
                                   wrap-cookies
                                   [cookie-header-middleware jwt-cookie-name]]
                      :get make-recipe}]
    (when dev
      ["/echo" {:name ::echo
              ;; :middleware [[cookie-header-middleware "user_jwt"] wrap-jwt-authentication auth-middleware] ; old

              ;; :parameters {:multipart [:map [:file malli/temp-file-part]]}
              ;; :middleware [datasource-middleware
              ;;              wrap-cookies
              ;;              parameters-middleware
              ;;              multipart-middleware
              ;;              [echo-middleware "echo"]]
              ;; :muuntaja m/instance

                :middleware [;cors-middleware
                             ;; [wrap-cors
                             ;;  :access-control-allow-credentials true
                             ;;  :access-control-allow-origin #"http://localhost:3000"
                             ;;  :access-control-allow-methods [:get]
                             ;;  :access-control-expose-headers "Etag"]
                             [echo-middleware "echoing"]]
                :get echo
                :post echo}])
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
  undoes all of reitit's great performance.
  see `https://github.com/lambdaisland/souk/blob/main/src/lambdaisland/souk/util/dev_router.clj`"
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
   ;; (if dev
   ;;   (dev-router @#'router-gen)
   ;;   prod-router)
   (dev-router (if dev
                 @#'router-gen
                 prod-router))
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
  (app {:request-method :get :uri "/signup" :query-string "d34fe88648147f014edb"})

  (app {:request-method :get :uri "/echo" :headers {"origin" "http://localhost:3000"}})
  ((ring/create-resource-handler {:root "img"
                                  :path "/"}) {:uri "/food.jpg"})
  (rur/resource-response "food.jpg" {:root "img"})

  (app {:request-method :get :uri "recipes/make" :cookie "JWT_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwiYXZhdGFyLXVybCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS8yMTI5NjQ0OD92PTQiLCJuYW1lIjoiZWFydGhmYWlsIn0.Czv61JsFLX5eICWPKfpQYrvVAzlVYyCMm7p_dhdEJro"})
  tmp
  res

  (fn wrap-cors-handler [req]
    (rur/header req :access-control-allow-credentials true)
    (wrap-cors h
               :access-control-allow-origin [#"http://localhost:3000" #"http://dwave.local:3000"]
               :access-control-allow-methods [:get :post]))

  (app {:request-method :get :uri "/echo"})

  ((wrap-cors (fn [_] {:status 200})
              :access-control-allow-credentials "true"
              :access-control-allow-origin #".*"
              :access-control-allow-methods [:get]
              :access-control-expose-headers "Etag")
   {:request-method :get
    :uri "/"
    :headers {"origin" "http://example.com"}})
  ((wrap-cors ((:wrap echo-middleware) echo "kkk")
              :access-control-allow-credentials true
              :access-control-allow-origin #".*"
              :access-control-allow-methods [:get]
              :access-control-expose-headers "Etag")
   {:request-method :get :uri "/" :headers {"origin" "http://example.com"}})

  ((wrap-cors identity
              :access-control-allow-origin [#"http://localhost:3000" #"http://dwave.local:3000"]
              :access-control-allow-methods [:get :post])
   {:request-method :get :uri "/echo"})

  (map (partial ku/insert-link "/recipes/cook/")
       (db/get-user-recipes db/ds 1))
  (let [{:user/keys [id]} {}]
    id)
  "ghu_9yrtq6FduvNd0ruJNzNYwNtYmCaB8g3WcfTx"

  ;;{"access_token":"ghu_3W2t6HiQoFPMtNwHG7igikOGzuJKpF12hEPu","expires_in":28800,"refresh_token":"ghr_cDKGUAjD9Wd01x1wrL3cW8PYWyyC3HSwYgq2ZNOHcoZ7dStSdlJGSGWUouluwvPFgtvys21z5YiW","refresh_token_expires_in":15897600,"token_type":"bearer","scope":""}
  (select-keys tmp [:server-name :server-port :scheme :request-method :uri :query-string])

  (-> tmp
      :params)
  ring.middleware.cookies/cookies-request

  (selmer/known-variables (slurp (io/resource "templates/recipe.html")))

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
(comment
  #_(defn add-user [{:keys [params] :as req}]
  ;; extract user and password
    (let [email (get params "email")
          password (get params "password")]
      (if (and email password)
        (if (db/get-user-by-email email)
          {:status 200 :body "email already taken"}
          {:status 200 :body (str "created user" (db/create-user email (str (gensym "user")) password))})
      ;else
        {:status 400 :body "email or password messing. must be in a form urlencoded"})))
#_(defn confirm-user [{:keys [params] :as req}]
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
)
