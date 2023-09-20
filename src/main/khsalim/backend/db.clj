(ns khsalim.backend.db
  (:refer-clojure :exclude [derive])
  (:require [buddy.hashers :refer [derive verify]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time]
            [next.jdbc.result-set :as rs]
            [khsalim.backend.utils :refer [config]]))
;; https://github.com/kelvin-mai/clj-auth/blob/main/src/auth/db.clj
;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.847/doc/getting-started/tips-tricks#working-with-json-and-jsonb
;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.847/doc/getting-started?q=pool#connection-pooling

(defonce ephemeral-db (atom {}))

(def db-lite {:dbtype "sqlite" :dbname (if (get config :dev)
                                         "dev/data/db-lite.db"
                                         ;production
                                         "data/db-lite.db")})
(def ds (jdbc/get-datasource db-lite))

(defn log-message
  ([message] (log-message "generic" message))
  ([type message]
   (try
     (jdbc/execute! ds ["INSERT INTO logs(type,message,time_stamp) VALUES(?,?,datetime())" type message])
     (catch Exception _  (str "log-message failed with me " message)))))

(defn insert-refresh-token [ds [token user-id  :as record]]
  (try
    (jdbc/execute!
     ds
     (into ["UPDATE users SET refresh_token = ?, last_modified=datetime()  WHERE id=?;"] record))
    (catch Exception e (log-message "insert-refresh-token"(.getMessage e)))))
(defn create-authenticated-user
  "second argument is of the form [name email authorization-server authorization-server-refresh-token avatar-url]"
  [ds [name email authorization-server authorization-server-refresh-token avatar-url :as user-record]]
  (try
    (jdbc/execute-one!
     ds
     (into [(str "INSERT INTO users"
                 "(name,email,authorization_server,authorization_server_refresh_token,avatar_url,last_modified)"
                 "VALUES (?,?,?,?,?,datetime())"
                 "ON CONFLICT (email)"
                 "DO UPDATE SET (name,authorization_server,authorization_server_refresh_token,avatar_url,last_modified)"
                 "=(excluded.name,excluded.authorization_server,excluded.authorization_server_refresh_token,excluded.avatar_url,datetime())"
                 "WHERE authorization_server IS NULL OR authorization_server = excluded.authorization_server "
                 "RETURNING id,name,email")]
           user-record))
    (catch Exception e (log-message "create-auth-user" (.getMessage e)))))
(defn get-user-recipes [ds user-id]
  (try
    (jdbc/execute! ds ["SELECT \"recipe-id\",name,description FROM \"users-recipes\" WHERE \"user-id\"=?" user-id]
                   {:builder-fn rs/as-unqualified-lower-maps})
    (catch Exception e (log-message "select recipes" (.getMessage e))
           nil)))
(defn delete-recipe [ds user-id recipe-id]
  (try
    (jdbc/with-transaction [tx ds]
      (jdbc/execute! tx ["DELETE FROM \"users-recipes\" WHERE \"user-id\" = ? AND \"recipe-id\" = ? ;" user-id recipe-id])
      (jdbc/execute! tx ["DELETE FROM \"recipes-steps\" WHERE \"recipe-id\" = ? ;" recipe-id]))
    (catch Exception e (log-message "delete-recipe" (.getMessage e)))))
(defn insert-user-recipe [ds user-id recipe-id]
  (try
    (jdbc/execute!
     ds
     ["INSERT INTO \"users-recipes\" (\"user-id\",\"recipe-id\",last_modified) VALUES (?,?,datetime())" user-id recipe-id])
    (catch Exception e (log-message "add recipe" (.getMessage e)))))
(defn register-recipe-data [ds user-id recipe-id recipe-name recipe-description]
  (try
    (jdbc/execute! ds [(str "INSERT INTO \"users-recipes\" "
                            "(\"user-id\",\"recipe-id\",description,name,last_modified)"
                            "VALUES(?,?,?,?,datetime()"
                            "ON CONFLICT DO "
                            "UPDATE SET description=excluded.description,name=excluded.name")
                       recipe-name recipe-description
                       user-id recipe-id])
    (catch Exception e (log-message "register-recipe-data" (.getMessage e)))))
(defn register-recipe-statistic [ds recipe-id]
  (try
    (jdbc/execute! ds ["UPDATE \"users-recipes\" SET finish_count=finish_count+1, last_modified=datetime() WHERE \"recipe-id\"=?"
                       recipe-id])
    (catch Exception e (log-message "statistic" (.getMessage e)))))
(defn get-recipe-steps [ds recipe-id]
  (try
    (jdbc/execute! ds ["SELECT step,description,media,\"media-type\" FROM \"recipes-steps\" WHERE \"recipe-id\"=? ORDER BY step ASC" recipe-id]
                   {:builder-fn rs/as-unqualified-lower-maps})
    (catch Exception e (log-message "get recipe-steps" (.getMessage e))
           nil)))
(defn insert-recipe-step [ds [recipe-id step description media media-type :as recipe-steps]]
  (try
    (jdbc/execute!
     ds
     (into ["INSERT INTO \"recipes-steps\" (\"recipe-id\",step,description,media,\"media-type\",last_modified) VALUES (?,?,?,?,?,datetime())"]
           recipe-steps))
    (catch Exception e (log-message "insert recipe step" (.getMessage e)))))
(comment
  @ephemeral-db
  ;; next.jdbc
  (def ds2 (jdbc/get-datasource db-lite))

  ;; (jdbc/execute! ds2 ["insert into users(name) values(\"salim\"),(\"mariam\")"])
  (jdbc/execute! ds ["select * from users"])
  (jdbc/execute! ds ["select * from \"users-recipes\""]
                 {:builder-fn rs/as-unqualified-lower-maps})
  (jdbc/execute! ds ["select * from \"recipes-steps\""])
  (jdbc/execute! ds ["select * from \"logs\""])

  (create-authenticated-user ds ["name" "email" "githubdub" "refresh-token" "avatar-url"])
  (create-authenticated-user ds ["salim4" "surrlim@gmail.com" "github" "adfa_vvvv"])

  (insert-refresh-token ds ["abc" 37])

  (delete-recipe ds "1" "21438")

  (->
   (jdbc/execute! ds ["select * from \"logs\""])
   first
   :logs/time_stamp)
  
  (java.util.Date/parse "2023/08/28 16:51")
  
  (jdbc/execute! ds ["SELECT * from users where id= :id" {"id" 1}])

  :rfc)

(comment
  @ephemeral-db
  {:valid false
   :update false}
  ;; (create-user "nana@gmail.com" "nana" "abcdef")
  (verify "abcsdef" "bcrypt+blake2b-512$cf60ee5915292ad7c99eeaae4962cfa3$12$ffddab2c4f89182875063e6086daf8a0aba201b39975423f")
  (derive "1234" {:alg :bcrypt+blake2b-512 :iterations 12})
  ;; => "bcrypt+blake2b-512$8d7ee2a7d6f33f6f59264d04b4e312f8$12$580e06b49a52fb68c1c43e9ec11ebe9f89c318e2412e04df"
  ;; => "bcrypt+blake2b-512$4953bdb8d25f9df16d364dc55bb0ec5d$10$d51d871f028100cdafeb3dea4e34c346a54013f872e02367"
  :rfc)

(comment

  (get-recipe-steps ds 20674)
  ;;[{:step 1, :description "ويندوز", :media "/api/v1/img/aaf4c2da-224f-477e-aa75-a699f5f42558", :media-type "img"} {:step 0, :description "قدس", :media "/api/v1/img/7f1f6ca7-62a4-4b14-96ed-47a53fd50b58", :media-type "img"}]
  (defn create-user [email name password]
    (let [hashed-password (derive password {:alg :bcrypt+blake2b-512 :iterations 12})
          created-user {:email email :name name :password hashed-password}]
      (swap! ephemeral-db update ::users conj created-user)
      (dissoc created-user :password)))
  (defn get-user-by-credentials [email password]
    (if-let [entry (first (filter #(= (:email %) email) (::users @ephemeral-db)))]
      (verify password (:password entry))
      {:valid false
       :message "email doesn't exist"}))
  (defn get-user-by-email [email]
    (if-let [entry (first (filter #(= (:email %) email) (::users @ephemeral-db)))]
      (dissoc entry :password)))

  (defn get-user-recipes [ds user-id]
    (jdbc/execute! ds ["SELECT \"recipe-id\",description as name FROM \"users-recipes\" WHERE \"user-id\"=?" user-id] {:builder-fn rs/as-unqualified-lower-maps}))

  (defn get-recipe-steps [ds recipe-id]
    (jdbc/execute! ds ["SELECT step,description,media,\"media-type\" FROM \"recipes-steps\" WHERE \"recipe-id\"=?" recipe-id]
                   {:builder-fn rs/as-unqualified-lower-maps})))
