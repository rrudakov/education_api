(ns education.system-test
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [clojure.test :refer [deftest is testing]]
            [compojure.api.api :refer [api-defaults]]
            [education.config :as config]
            [education.http.routes :as routes :refer [api-routes]]
            [education.system :as sut]
            [education.test-data :refer [test-config]]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [next.jdbc.connection :as connection]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [spy.core :as spy])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defprotocol DummyConnection
  (close [this]))

(deftest integrant-config-test
  (testing "Test integrant config init"
    (with-redefs [config/config (spy/stub test-config)]
      (let [config (ig/init (sut/system-config :prod) [:system/config])]
        (is (= test-config (:system/config config)))
        (is (spy/called-once-with? config/config :prod))))))

(deftest integrant-handler-test
  (testing "Test integrant handler init"
    (let [auth-backend (config/auth-backend test-config)
          db           "database"
          routes-stub  "routes"]
      (with-redefs [config/config       (spy/stub test-config)
                    config/auth-backend (spy/stub auth-backend)
                    connection/->pool   (spy/stub db)
                    routes/api-routes   (spy/spy)
                    wrap-authorization  (spy/spy)
                    wrap-authentication (spy/spy)
                    wrap-format         (spy/spy)
                    wrap-cors           (spy/spy)
                    wrap-defaults       (spy/stub routes-stub)]
        (let [handler (-> (ig/init (sut/system-config :prod) [:handler/run-app])
                          :handler/run-app)]
          (is (spy/called-once-with? api-routes db test-config))
          (is (spy/called-once-with? wrap-authentication nil auth-backend))
          (is (spy/called-once-with? wrap-authorization nil auth-backend))
          (is (spy/called-once-with? muuntaja.middleware/wrap-format nil))
          (is (spy/called-once? wrap-cors))
          (is (spy/called-once-with? wrap-defaults nil api-defaults))
          (is (= routes-stub handler)))))))

(deftest integrant-http-kit-test
  (testing "Test integrant http-kit init"
    (let [handler     "handler"
          server-stub "srv"]
      (with-redefs [config/config       (spy/stub test-config)
                    config/auth-backend (spy/spy)
                    connection/->pool   (spy/spy)
                    routes/api-routes   (spy/spy)
                    wrap-authorization  (spy/spy)
                    wrap-authentication (spy/spy)
                    wrap-format         (spy/spy)
                    wrap-cors           (spy/spy)
                    wrap-defaults       (spy/stub handler)
                    server/run-server   (spy/stub server-stub)]
        (let [srv (-> (ig/init (sut/system-config :prod) [:adapter/http-kit])
                      :adapter/http-kit)]
          (is (= server-stub srv))
          (is (spy/called-once-with? server/run-server handler
                                     {:port (config/application-port test-config)}))))))

  (testing "Test integrant http-kit halt"
    (let [db-pool     (reify DummyConnection (close [this] :db-pool))
          handler     "handler"
          server-stub (spy/mock (fn [& _] "server"))]
      (with-redefs [config/config       (spy/stub test-config)
                    config/auth-backend (spy/spy)
                    connection/->pool   (spy/stub db-pool)
                    routes/api-routes   (spy/spy)
                    wrap-authorization  (spy/spy)
                    wrap-authentication (spy/spy)
                    wrap-format         (spy/spy)
                    wrap-cors           (spy/spy)
                    wrap-defaults       (spy/stub handler)
                    server/run-server   (spy/stub server-stub)]
        (let [system (ig/init (sut/system-config :prod) [:adapter/http-kit])]
          (ig/halt! system)
          (is (spy/called-once-with? server-stub :timeout 100)))))))

(deftest integrant-database-test
  (testing "Test integrant database init"
    (with-redefs [config/config     (spy/stub test-config)
                  connection/->pool (spy/stub :db-pool)]
      (let [db (-> (ig/init (sut/system-config :prod) [:database.sql/connection])
                   :database.sql/connection)]
        (is (= :db-pool db))
        (is (spy/called-once-with? connection/->pool
                                   ComboPooledDataSource
                                   (config/db-spec test-config))))))

  (testing "Test integrant database halt"
    (let [close-connection (spy/spy)
          db-pool          (reify DummyConnection (close [this] (close-connection this)))]
      (with-redefs [config/config     (spy/stub test-config)
                    connection/->pool (spy/stub db-pool)]
        (let [system (ig/init (sut/system-config :prod) [:database.sql/connection])]
          (ig/halt! system)
          (is (spy/called-once-with? close-connection (:database.sql/connection system))))))))
