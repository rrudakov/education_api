(ns education.http.component-test
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [clojure.test :refer [deftest is testing]]
            [education.config :as config]
            [education.http.component :as sut]
            [education.http.routes :refer [api-routes]]
            [education.test-data :refer :all]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [spy.core :as spy]
            [com.stuartsierra.component :as component]))

(deftest map->WebServer-start-test
  (testing "Test creating new instance of web server"
    (let [auth-backend (config/auth-backend test-config)
          db           "database"
          srv          42]
      (with-redefs [config/auth-backend             (spy/stub auth-backend)
                    api-routes                      (spy/spy)
                    server/run-server               (spy/stub srv)
                    wrap-authorization              (spy/spy)
                    wrap-authentication             (spy/spy)
                    muuntaja.middleware/wrap-format (spy/spy)
                    wrap-cors                       (spy/spy)
                    wrap-defaults                   (spy/spy)]
        (let [server                (.start (sut/map->WebServer {:config test-config :db db}))
              [[_ run-server-args]] (spy/calls server/run-server)]
          (is (spy/called-once-with? api-routes db test-config))
          (is (= {:port (config/application-port test-config)} run-server-args))
          (is (spy/called-once-with? wrap-authentication nil auth-backend))
          (is (spy/called-once-with? wrap-authorization nil auth-backend))
          (is (spy/called-once-with? muuntaja.middleware/wrap-format nil))
          (is (spy/called-once? wrap-cors))
          (is (spy/called-once-with? wrap-defaults nil api-defaults))
          (is (= srv (:srv server)))
          (is (= db (:db server))))))))

(deftest map->WebServer-stop-test
  (testing "Test shutdown instance of web server"
    (let [db  "database"
          srv "server"]
      (with-redefs [config/auth-backend             (spy/spy)
                    api-routes                      (spy/spy)
                    server/run-server               (spy/stub (fn [& {:keys [timeout] :or {timeout 100}}] srv))
                    wrap-authorization              (spy/spy)
                    wrap-authentication             (spy/spy)
                    muuntaja.middleware/wrap-format (spy/spy)
                    wrap-cors                       (spy/spy)
                    wrap-defaults                   (spy/spy)]
        (let [server (-> {:config test-config :db db} sut/map->WebServer .start .stop)]
          (is (nil? (:srv server)))
          (is (nil? (:db server))))))))

(deftest new-webserver-test
  (testing "Test creation web server using new-webserver function"
    (with-redefs [component/using    (spy/spy)
                  sut/map->WebServer (spy/spy)]
      (let [_          (sut/new-webserver test-config)
            [[_ deps]] (spy/calls component/using)]
        (is (spy/called-once-with? sut/map->WebServer {:config test-config}))
        (is (= [:db] deps))))))
