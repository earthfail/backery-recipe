(ns khsalim.backend.db
  (:refer-clojure :exclude [derive])
  (:require [buddy.hashers :refer [derive verify]]))
;; https://github.com/kelvin-mai/clj-auth/blob/main/src/auth/db.clj
;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.847/doc/getting-started/tips-tricks#working-with-json-and-jsonb
;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.847/doc/getting-started?q=pool#connection-pooling

(defonce db-dev (atom {::users [{:email "salim@gmail.com", :password "1234"}]
		       ::recipes
		       [{:name "salim"
			 :page 0
                         :recipes [{:id 1 :img "test1.jpg" :description "نضع زيت الزيتون في المقلاة"}
                                   {:id 2 :img "test2.jpg" :description "نقطع الخضراوات على شكل مربعات"}]}]}))

(defn create-user [email name password]
  (let [hashed-password (derive password {:alg :bcrypt+blake2b-512 :iterations 12})
        created-user {:email email :name name :password hashed-password}]
    (swap! db-dev update ::users conj created-user)
    (dissoc created-user :password)))
(defn get-user-by-credentials [email password]
  (if-let [entry (first (filter #(= (:email %) email) (::users @db-dev)))]
    (verify password (:password entry))
    {:valid false
     :message "email doesn't exist"}))
(defn get-user-by-email [email]
  (if-let [entry (first (filter #(= (:email %) email) (::users @db-dev)))]
    (dissoc entry :password)))
(comment
  @db-dev
   {:valid false
        :update false}
  ;; (create-user "nana@gmail.com" "nana" "abcdef")
  (verify "abcsdef" "bcrypt+blake2b-512$cf60ee5915292ad7c99eeaae4962cfa3$12$ffddab2c4f89182875063e6086daf8a0aba201b39975423f")
  (derive "1234" {:alg :bcrypt+blake2b-512 :iterations 12})
  ;; => "bcrypt+blake2b-512$8d7ee2a7d6f33f6f59264d04b4e312f8$12$580e06b49a52fb68c1c43e9ec11ebe9f89c318e2412e04df"
  ;; => "bcrypt+blake2b-512$4953bdb8d25f9df16d364dc55bb0ec5d$10$d51d871f028100cdafeb3dea4e34c346a54013f872e02367"
  )
