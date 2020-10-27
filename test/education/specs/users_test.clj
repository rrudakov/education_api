(ns education.specs.users-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [education.specs.users :as sut])
  (:import java.time.Instant))

(deftest id-test
  (doseq [id [23 999999999999999]]
    (testing (str "::id valid " id)
      (is (s/valid? ::sut/id id))))

  (doseq [id ["string" true]]
    (testing (str "::id invalid " id)
      (is (not (s/valid? ::sut/id id))))))

(deftest username-test
  (doseq [username ["s8" "rrudakov" "SomeName" "anyValidValue"]]
    (testing (str "::username valid " username)
      (is (s/valid? ::sut/username username))))

  (doseq [username ["s" "8sdfasdf" "with space" 1123 true "email@invalid.com"]]
    (testing (str "::username invalid " username)
      (is (not (s/valid? ::sut/username username))))))

(deftest password-test
  (doseq [password ["123456" "WItn@#$*()Special Characters"]]
    (testing (str "::password valid " password)
      (is (s/valid? ::sut/password password))))

  (doseq [password ["short" 123456 true]]
    (testing (str "::password invalid " password)
      (is (not (s/valid? ::sut/password password))))))

(deftest email-test
  (doseq [email ["some.email234@domain.org"]]
    (testing (str "::email valid " email)
      (is (s/valid? ::sut/email email))))

  (doseq [email ["some@email"
                 "anyvalue@"
                 "any123"
                 "@example.com"
                 "()email@valid.com"
                 "(*&^T)!@#$%%.com"
                 123456
                 true]]
    (testing (str "::email invalid " email)
      (is (not (s/valid? ::sut/email email))))))

(deftest role-test
  (doseq [role ["admin" "moderator" "guest"]]
    (testing (str "::role valid " role)
      (is (s/valid? ::sut/role role))))

  (doseq [role [:god :admin true]]
    (testing (str "::role invalid " role)
      (is (not (s/valid? ::sut/role role))))))

(deftest roles-test
  (doseq [roles [["admin" "moderator" "guest"]
                 ["moderator" "admin"]
                 ["guest" "moderator"]
                 []]]
    (testing (str "::roles valid " roles)
      (is (s/valid? ::sut/roles roles))))

  (doseq [roles [[:admin :moderator :god]
                 [:admin :admin :guest]
                 #{"admin" "guest"}
                 '("admin")
                 #{"admin" :guest}
                 '("admin" "guest" "moderator")
                 [true false]
                 9999999
                 "string"
                 #{:admin :moderator :guest :someone-else}
                 [1 2 3 4]
                 #{}
                 '()]]
    (testing (str "::roles invalid " roles)
      (is (not (s/valid? ::sut/roles roles))))))

(deftest role-request-test
  (doseq [role ["admin" "moderator" "guest"]]
    (testing (str "::role-request valid " role)
      (is (s/valid? ::sut/role-request role))))

  (doseq [role [:admin :moderator :guest "god" 1234 false]]
    (testing (str "::role-request invalid " role)
      (is (not (s/valid? ::sut/role-request role))))))

(deftest roles-request-test
  (doseq [roles [["admin" "moderator" "guest"]
                 #{"admin" "guest"}
                 '("guest")]]
    (testing (str "::roles-request valid " roles)
      (is (s/valid? ::sut/roles-request roles))))

  (doseq [roles [["admin" "moderator" "god"]
                 ["admin" "guest" "guest"]
                 #{"admin" :guest}
                 '(true)
                 99999999
                 [1 2 3 4 5]
                 []
                 #{}
                 '()]]
    (testing (str "::roles-request invalid " roles)
      (is (not (s/valid? ::sut/roles-request roles))))))

(deftest created-on-test
  (testing "::created-on valid"
    (is (s/valid? ::sut/created_on (Instant/now))))

  (doseq [created-on ["2020-06-23T18:05:22.905106Z" "any_value" 1234567888 false]]
    (testing (str "::created-on invalid " created-on)
      (is (not (s/valid? ::sut/created_on created-on))))))

(deftest updated-on-test
  (testing "::updated-on valid"
    (is (s/valid? ::sut/updated_on (Instant/now))))

  (doseq [updated-on ["2020-06-23T18:05:22.905106Z" "any_value" 1234567888 false]]
    (testing (str "::updated-on invalid " updated-on)
      (is (not (s/valid? ::sut/updated_on updated-on))))))

(deftest token-test
  (testing "::token valid"
    (is (s/valid? ::sut/token "s")))

  (doseq [token [12345 true {} [:list :of :keywords]]]
    (testing (str "::token invalid " token)
      (is (not (s/valid? ::sut/token token)))))

  (testing "::token exercise"
    (is (= 10 (count (s/exercise ::sut/token))))))

(deftest user-create-request-test
  (doseq [req [{:username "valid" :password "123456" :email "email@example.org"}
               {:username "valid" :password "123456" :email "email@example.org" :extra "field"}]]
    (testing (str "::user-create-request valid " req)
      (is (s/valid? ::sut/user-create-request req))))

  (doseq [req [{:username "valid" :password "123456"}
               {:username "valid" :email "email@example.org"}
               {:password "123456" :email "email@example.org"}
               {:username "s" :password "12345" :email "email@example"}
               [1 2 3 4 5]
               {:some "value" :another true}
               "string"
               false
               888888888]]
    (testing (str "::user-create-request invalid " req)
      (is (not (s/valid? ::sut/user-create-request req))))))

(deftest user-update-request-test
  (doseq [req [{:roles ["admin" "moderator" "guest"]}
               {:roles ["admin"]}]]
    (testing (str "::user-update-request valid " req)
      (is (s/valid? ::sut/user-update-request req))))

  (doseq [req [{:roles ["admin" "admin"]}
               {:role ["admin"]}
               {:roles ["god"]}
               {:roles "admin"}
               ["list " "of " "values"]
               "just string"
               :keyword
               983745345
               #{true false}
               false]]
    (testing (str "::user-update-request invalid " req)
      (is (not (s/valid? ::sut/user-update-request req)))))

  (testing "::user-update exercise"
    (is (= 10 (count (s/exercise ::sut/user-update-request))))))

(deftest user-create-response-test
  (testing "::user-create-response is valid"
    (is (s/valid? ::sut/user-create-response {:id 222})))

  (testing "::user-create-request is invalid"
    (is (not (s/valid? ::sut/user-create-response {:id "23243"})))))

(deftest user-response-test
  (doseq [resp [{:id         23
                 :username   "rrudakov"
                 :email      "email@example.org"
                 :roles      ["admin" "guest"]
                 :created_on (Instant/now)
                 :updated_on (Instant/now)}
                {:id          23
                 :username    "rrudakov"
                 :email       "email@example.org"
                 :roles       ["admin" "guest"]
                 :created_on  (Instant/now)
                 :updated_on  (Instant/now)
                 :extra-field "ignored"}]]
    (testing (str "::user-response valid " resp)
      (is (s/valid? ::sut/user-response resp))))

  (doseq [resp [{:id         23
                 :username   "rrudakov"
                 :email      "email@example.org"
                 :roles      [:admin :guest]
                 :created_on (Instant/now)}
                {:id         23
                 :username   "rrudakov"
                 :email      "email@example.org"
                 :roles      [:admin :guest]
                 :updated_on (Instant/now)}
                {:id         23
                 :username   "rrudakov"
                 :email      "email@example.org"
                 :created_on (Instant/now)
                 :updated_on (Instant/now)}
                {:id         23
                 :username   "rrudakov"
                 :roles      [:admin :guest]
                 :created_on (Instant/now)
                 :updated_on (Instant/now)}
                {:id         23
                 :email      "email@example.org"
                 :roles      [:admin :guest]
                 :created_on (Instant/now)
                 :updated_on (Instant/now)}
                {:username   "rrudakov"
                 :email      "email@example.org"
                 :roles      [:admin :guest]
                 :created_on (Instant/now)
                 :updated_on (Instant/now)}
                {:id         "23"
                 :username   "r"
                 :email      "email.org"
                 :roles      [:god]
                 :created_on "time"
                 :updated_on "time"}
                {:some "invalid" :map true}
                true
                99938434
                ["vector" "of" "strings"]
                "whatever"]]
    (testing (str "::user-response invalid " resp)
      (is (not (s/valid? ::sut/user-response resp))))))

(deftest users-response-test
  (testing "::users-response valid"
    (is (s/valid? ::sut/users-response
                  [{:id         1
                    :username   "rr"
                    :email      "rr@example.org"
                    :roles      ["guest"]
                    :updated_on (Instant/now)
                    :created_on (Instant/now)}
                   {:id         2
                    :username   "rr2"
                    :email      "rr2@example.org"
                    :roles      ["moderator"]
                    :updated_on (Instant/now)
                    :created_on (Instant/now)
                    :extra      :field}])))

  (let [now (Instant/now)]
    (doseq [resp [[{:id         1
                    :username   "rr"
                    :email      "rr@example.org"
                    :roles      [:guest]
                    :updated_on now
                    :created_on now}
                   {:id         1
                    :username   "rr"
                    :email      "rr@example.org"
                    :roles      [:guest]
                    :updated_on now
                    :created_on now}]
                  [{:some "invalid"}]
                  {:not "vector"}
                  [1 2 3 4 5 6]
                  [true false]
                  88888888]]
      (testing (str "::users-response invalid " resp)
        (is (not (s/valid? ::sut/users-response resp)))))))

(deftest login-request-test
  (doseq [req [{:username "rr" :password "123456"}
               {:username "rrudakov" :password "123456" :extra-field "ignored"}]]
    (testing (str "::login-request valid " req)
      (is (s/valid? ::sut/login-request req))))

  (doseq [req [{:username "rrudakov"}
               {:password "123456"}
               {:username "r" :password "12"}
               {:username true :password "123456"}
               {:username 123456 :password "123456"}
               [:list :of :some :values]
               true
               9876354]]
    (testing (str "::login-request invalid " req)
      (is (not (s/valid? ::sut/login-request req)))))

  (testing "::login-request exercise"
    (is (= 10 (count (s/exercise ::sut/login-request))))))

(deftest token-response-test
  (testing "::token-response valid"
    (is (s/valid? ::sut/token-response {:token "any string here"})))

  (doseq [resp [{:t "valid string"}
                {:token {:value "invalid structure"}}
                {:token 123456}
                ["list" "of" "string"]
                true
                838383838383
                #{:set :of :values}]]
    (testing "::token-response invalid"
      (is (not (s/valid? ::sut/token-response resp)))))

  (testing "::token-response exercise"
    (is (= 10 (count (s/exercise ::sut/token-response))))))
