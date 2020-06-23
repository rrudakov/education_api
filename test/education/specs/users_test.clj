(ns education.specs.users-test
  (:require [education.specs.users :as sut]
            [clojure.test :refer [testing deftest is]]
            [clojure.spec.alpha :as s]))

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
  (doseq [role [:admin :moderator :guest]]
    (testing (str "::role valid " role)
      (is (s/valid? ::sut/role role))))

  (doseq [role [:god "admin" true]]
    (testing (str "::role invalid " role)
      (is (not (s/valid? ::sut/role role))))))

(deftest roles-test
  (doseq [roles [[:admin :moderator :guest]
                 #{:admin :guest}
                 '(:admin)
                 []]]
    (testing (str "::roles valid " roles)
      (is (s/valid? ::sut/roles roles))))

  (doseq [roles [[:admin :moderator :god]
                 [:admin :admin :guest]
                 ["admin" :guest]
                 [1 2 3 4]]]
    (testing (str "::roles invalid " roles)
      (is (not (s/valid? ::sut/roles roles))))))
