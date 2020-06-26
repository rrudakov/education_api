(ns education.specs.roles-test
  (:require [education.specs.roles :as sut]
            [clojure.test :refer [testing deftest is]]
            [clojure.spec.alpha :as s]))

(deftest id-test
  (doseq [id [23 999999999999999]]
    (testing (str "::id valid " id)
      (is (s/valid? ::sut/id id))))

  (doseq [id ["string" true]]
    (testing (str "::id invalid " id)
      (is (not (s/valid? ::sut/id id))))))

(deftest name-test
  (doseq [name [:admin :moderator :guest]]
    (testing (str "::name valid " name)
      (is (s/valid? ::sut/name name))))

  (doseq [name [:god "admin" true]]
    (testing (str "::name invalid " name)
      (is (not (s/valid? ::sut/name name))))))

(deftest role-test
  (doseq [role [{:id 1 :name :admin}
                {:id 2 :name :moderator}
                {:id 3 :name :guest}
                {:id 4 :name :admin :another {:ignored "field"}}]]
    (testing (str "::role valid " role)
      (is (s/valid? ::sut/role role))))

  (doseq [role [{:id 1 :name "admin"}
                {:id "2" :name :moderator}
                {:id 3 :name :god}
                {:id 4}
                {:name :admin}]]
    (testing (str "::role invalid " role)
      (is (not (s/valid? ::sut/role role))))))

(deftest roles-response-test
  (testing "::roles-response valid"
    (is (s/valid? ::sut/roles-response
                  #{{:id 1 :name :admin}
                    {:id 2 :name :moderator}
                    {:id 3 :name :guest}})))

  (doseq [resp [[{:id 1 :name :admin}
                 {:id 2 :name :moderator}
                 {:id 3 :name :guest}]
                #{{:id 1}}
                #{{:name :admin}}
                #{{:id 1 :name "admin"}}
                #{{:id "1" :name :moderator}}]]
    (testing (str "::roles-response invalid " resp)
      (is (not (s/valid? ::sut/roles-response resp))))))
