(ns khsalim.backend.utils
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :refer [jws]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]))
;; buddy.auth.backends
;; jwt
;; [no.nsd.clj-jwt :as clj-jwt]
(def jwt-secret "f9d63a08-cb4f-4796-8869-8487bd29c29b")
;; {:secret jwt-secret :on-error println}
(def buddy-backend (jws {:secret jwt-secret}))
(defn wrap-jwt-authentication
  [handler]
  (wrap-authentication handler buddy-backend))
(defn auth-middleware
  [handler]
  (fn new-handler [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :headers {} :body {:error "Unauthorized"}})))
(defn create-token [payload]
  (jwt/sign payload @#'jwt-secret))

(comment
  jwt-secret
  buddy-backend
  (create-token {:name "salim" :id "3"})
  (jwt/unsign "eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoic2FsaW0iLCJpZCI6IjMifQ.DrFsXMKgngQGzs-Be14UhJKRVMm-CO2I_Qfy2Hr75G8"
              jwt-secret)
  (try
    (-> "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.vHq9-Yi6NjVoyRppY0SVAAq2FTNedm34-ZbLz7Jf92k"
        (jwt/unsign jwt-secret))
    (catch Exception e (str "caught exception: " (.getMessage e) (type e))))

  @#'jwt-secret)
  :rfc
