(ns education.http.coercion
  (:require [schema.core]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [clojure.walk :as walk]
            [compojure.api.coercion.core :as cc]
            [spec-tools.openapi.core :as openapi]
            [compojure.api.common :as common]
            [clojure.set :as set])
  (:import (clojure.lang IPersistentMap)
           (schema.core RequiredKey OptionalKey)
           (spec_tools.core Spec)
           (spec_tools.data_spec Maybe)))

(def string-transformer
  (st/type-transformer
   st/string-transformer
   st/strip-extra-keys-transformer
   {:name :string}))

(def json-transformer
  (st/type-transformer
   st/json-transformer
   st/strip-extra-keys-transformer
   {:name :json}))

(defn default-transformer
  ([] (default-transformer :default))
  ([name] (st/type-transformer {:name name})))

(defprotocol Specify
  (specify [this name]))

(extend-protocol Specify
  IPersistentMap
  (specify [this name]
    (-> (->>
         (walk/postwalk
          (fn [x]
            (if (and (map? x) (not (record? x)))
              (->> (for [[k v] (dissoc x schema.core/Keyword)
                         :let [k (cond
                                   ;; Schema required
                                   (instance? RequiredKey k)
                                   (ds/req (schema.core/explicit-schema-key k))

                                   ;; Schema options
                                   (instance? OptionalKey k)
                                   (ds/opt (schema.core/explicit-schema-key k))

                                   :else
                                   k)]]
                     [k v])
                   (into {}))
              x))
          this)
         (ds/spec name))
      (dissoc :name)))

  Maybe
  (into-spec [this name]
    (ds/spec name this))

  Spec
  (specify [this _] this)

  Object
  (specify [this _]
    (st/create-spec {:spec this})))

(def memoized-specify
  (common/fifo-memoize #(specify %1 (keyword "spec" (name (gensym "")))) 1000))

(defn maybe-memoized-specify
  [spec]
  (if (keyword? spec)
    (specify spec nil)
    (memoized-specify spec)))

(defn stringify-pred [pred]
  (str (if (instance? clojure.lang.LazySeq pred)
         (seq pred)
         pred)))

(defmulti coerce-response? identity :default ::default)
(defmethod coerce-response? ::default [_] true)

(defn get-apidocs-internal
  [_ _ {:keys [parameters responses] :as info}]
  (cond-> (dissoc info :parameters :responses)
    (and parameters
         (seq (set/intersection #{:path :query :header :cookie}
                                (set (keys parameters)))))
    (assoc ::openapi/parameters
           (into (empty parameters)
                 (for [[in spec] parameters
                       :when     (not= in :body)
                       :when     (not= in :formData)]
                   [in (maybe-memoized-specify spec)])))

    (and parameters
         (contains? parameters :body))
    (assoc :requestBody
           {::openapi/content
            {"default" (maybe-memoized-specify (:body parameters))}})

    (and parameters
         (contains? parameters :formData))
    (assoc :requestBody
           {::openapi/content
            {"multipart/form-data" (maybe-memoized-specify (:file (:formData parameters)))}})

    responses
    (assoc
     :responses
     (into {}
           (for [[code response] responses]
             [code (-> response
                       (assoc ::openapi/content {"default" (some-> (:schema response maybe-memoized-specify))})
                       (dissoc :schema))])))))

(defrecord SpecCoercion [name options]
  cc/Coercion
  (get-name [_] name)

  (get-apidocs
    [x y info]
    (get-apidocs-internal x y info))

  (make-open [_ spec] spec)

  (encode-error [_ error]
    (let [problems (-> error :problems ::s/problems)]
      (-> error
          (update :spec (comp str s/form))
          (assoc :problems (mapv #(update % :pred stringify-pred) problems)))))

  (coerce-request [_ spec value type format _]
    (let [spec (maybe-memoized-specify spec)
          type-options (options type)]
      (if-let [transformer (or (get (get type-options :formats) format)
                               (get type-options :default))]
        (let [coerced (st/coerce spec value transformer)]
          (if (s/valid? spec coerced)
            coerced
            (let [conformed (st/conform spec coerced transformer)]
              (if (s/invalid? conformed)
                (let [problems (st/explain-data spec coerced transformer)]
                  (cc/map->CoercionError
                   {:spec spec
                    :problems problems}))
                (s/unform spec conformed)))))
        value)))

  (accept-response? [_ spec]
    (boolean (coerce-response? spec)))

  (coerce-response [this spec value type format request]
    (cc/coerce-request this spec value type format request)))

(def default-options
  {:body {:default (default-transformer)
          :formats {"application/json" json-transformer
                    "application/msgpack" json-transformer
                    "application/x-yaml" json-transformer}}
   :string {:default string-transformer}
   :response {:default (default-transformer)
              :formats {"application/json" (default-transformer :json)
                        "application/msgpack" (default-transformer :json)
                        "application/x-yaml" (default-transformer :json)}}})

(defn create-coercion [options]
  (->SpecCoercion :openapi options))

(def default-coercion (create-coercion default-options))

(defmethod cc/named-coercion :openapi [_] default-coercion)
