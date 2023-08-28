(ns khsalim.backend.utils
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :refer [jws]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clj-http.client :as client]
   [cheshire.core :as json]
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]

   [ring.util.response :as rur]))
;; buddy.auth.backends
;; jwt
;; [no.nsd.clj-jwt :as clj-jwt]

(def config (read-config (io/resource "config.edn")))

(when (= (get config :env) :dev)
  (require '[clojure.java.javadoc :as jdoc]
             '[clojure.reflect :as reflect]
             '[clojure.inspector :as insp]
             '[clojure.pprint :as pp]
             '[clojure.repl :as repl])
  (jdoc/add-remote-javadoc "org.eclipse.jetty.server" "https://www.eclipse.org/jetty/javadoc/jetty-9/")
  :required)

(def static-files-root (if (= (get config :env :dev) :dev)
                         "resources/pages"
                         "public"))
(def jwt-secret (get-in config [:jwt-secret 0]))
(def jwt-refresh-secret (get-in config [:jwt-refresh-secret]))
;; {:secret jwt-secret :on-error println}
(def buddy-backend (jws {:secret @#'jwt-secret}))

(defn wrap-jwt-authentication
  "check `authorization` header in http request for the expression \"Token [token]\""
  [handler]
  (wrap-authentication handler @#'buddy-backend))
;; check :identity key in request
(defn auth-middleware
  "check `:identity` key in `request`"
  [handler]
  (fn auth-handler [request]
    (if (authenticated? request)
      (handler request)
      (rur/status (rur/file-response "error.html"
                                     {:root static-files-root})
                  401))))
(defn create-token [payload]
  (jwt/sign payload @#'jwt-secret))
(defn create-refresh-token [payload]
  (jwt/sign payload @#'jwt-refresh-secret))
(def create-tokens (juxt create-refresh-token create-token))

(defn cookie-header-middleware [handler cookie-name]
  (fn cookie-auth-handler [request]
    ((-> handler
         auth-middleware
         wrap-jwt-authentication)
     (assoc-in request [:headers "authorization"]
               (str "Token " (get-in request [:cookies cookie-name :value]))))))
(defn github-access-token [code]
  (let [result (client/post "https://github.com/login/oauth/access_token"
                            {:accept :json
                             :form-params {:client_id (get-in config [:github :client-id])
                                           :client_secret (get-in config [:github :client-secret])
                                           :code code}})]
    (def res result)
    (->
     result
     (get :body "")
     json/parse-string)))
(defn github-user-email [access-token]
  (->>
   (client/get "https://api.github.com/user/emails"
               {:headers {"Accept" "application/vnd.github+json"
                          "X-Github-Api.Version" "2022-11-28"}
                :oauth-token access-token
                :as :json})
   :body
   (filter :primary)
   first
   :email))
(defn github-user-info [access-token]
  (let [info (-> (client/get "https://api.github.com/user"
                             {:headers {"Accept" "application/vnd.github+json"
                                        "X-Github-Api.Version" "2022-11-28"}
                              :oauth-token access-token
                              :as :json})
                 :body)]
    {:name (get info :login)
     :avatar-url (get info :avatar_url)}))

(comment

  (github-user-email "ghu_kFg6DzkfhhumRecL8aAktnw2PbzOpw1I9ne7")
  (github-user-info "ghu_kFg6DzkfhhumRecL8aAktnw2PbzOpw1I9ne7")

  rur/set-cookie

  jwt-secret
  buddy-backend
  (create-token {:name "salim" :id "3"})

  (jwt/unsign "eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.ncgLP5Ovsr6xYTdot5l7d3uO_kxVVw-D3TLg8ivDCxk"
              jwt-secret)
  (try
    (-> "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"
        (jwt/unsign jwt-secret))
    (catch Exception e (str "caught exception: " (.getMessage e) (type e))))

  @#'jwt-secret
  :rfc)
