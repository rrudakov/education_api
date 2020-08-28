(ns education.http.endpoints.articles
  (:require [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [education.database.articles :as articlesdb]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.articles :as specs]
            [education.specs.error :as err]
            [ring.util.http-response :as status]))

;; Converters
(defn to-short-article-response
  "Convert database article to article response."
  [{:articles/keys [id user_id title featured_image updated_on description]}]
  {:id             id
   :user_id        user_id
   :title          title
   :featured_image featured_image
   :updated_on     updated_on
   :description    (or description articlesdb/default-article-description)})

(defn to-full-article-response
  "Convert database article to full article response."
  [{:articles/keys [id user_id title body featured_image created_on updated_on description]}]
  {:id             id
   :user_id        user_id
   :title          title
   :description    (or description articlesdb/default-article-description)
   :body           body
   :featured_image featured_image
   :created_on     created_on
   :updated_on     updated_on})

;; Handlers
(defn- create-article-handler
  "Create new article handler."
  [db article]
  (fn [{:keys [identity]}]
    (let [user   (:user identity)
          new-id (str (articlesdb/add-article db user article))]
      (status/created (str "/articles/" new-id) {:id new-id}))))

(defn- update-article-handler
  "Update existing article handler."
  [db article-id article]
  (fn [{:keys [identity]}]
    (if (articlesdb/can-update? db (:user identity) article-id)
      (case (articlesdb/update-article db article-id article)
        1 (status/no-content)
        0 (status/not-found {:message const/not-found-error-message})
        (status/internal-server-error {:message const/server-error-message}))
      (status/forbidden {:message const/no-access-error-message}))))

(defn- get-all-articles-handler
  "Get all recent articles handler."
  [{:keys [db limit user-id]}]
  (let [articles (if (nil? user-id)
                   (articlesdb/get-all-articles db limit)
                   (articlesdb/get-user-articles db user-id limit))]
    (->> articles
         (mapv to-short-article-response)
         status/ok)))

(defn- get-latest-full-articles-handler
  "Get latest full articles handler."
  [db number]
  (let [articles (articlesdb/get-latest-full-sized-articles db number)]
    (->> articles
         (mapv to-full-article-response)
         status/ok)))

(defn- get-article-by-id-handler
  "Get article by `article-id` handler."
  [db article-id]
  (let [article (articlesdb/get-article-by-id db article-id)]
    (if (nil? article)
      (status/not-found {:message const/not-found-error-message})
      (status/ok (to-full-article-response article)))))

(defn- get-last-main-featured-article-handler
  "Get last main featured article handler."
  [db]
  (let [mf-article (articlesdb/get-last-featured-article db)]
    (if (nil? mf-article)
      (status/not-found {:message const/not-found-error-message})
      (status/ok (to-short-article-response mf-article)))))

(defn- get-last-featured-articles-handler
  "Get last featured articles handler."
  [db limit]
  (->> (articlesdb/get-last-featured-articles db limit)
       (mapv to-short-article-response)
       status/ok))

(defn- delete-article-handler
  "Delete article by `article-id` handler."
  [db article-id]
  (fn [{:keys [identity]}]
    (if (articlesdb/can-update? db (:user identity) article-id)
      (case (articlesdb/delete-article db article-id)
        1 (status/no-content)
        0 (status/not-found {:message const/not-found-error-message})
        (status/internal-server-error {:message const/server-error-message}))
      (status/forbidden {:message const/no-access-error-message}))))

;; Define routes
(defn articles-routes
  "Define routes for articles endpoint."
  [db]
  (context "" []
    :tags ["articles"]
    (POST "/articles" []
      :middleware [[require-roles #{:guest}]]
      :body [article ::specs/article-create-request]
      :return ::specs/id
      :summary "Create new article"
      :responses {401 {:description "Not authorized!"
                       :schema      ::err/error-response}
                  403 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (create-article-handler db article))
    (PATCH "/articles/:id" []
      :middleware [[require-roles #{:guest}]]
      :body [article ::specs/article-update-request]
      :path-params [id :- ::specs/id]
      :summary "Update existing article by article ID"
      :responses {401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (update-article-handler db id article))
    (GET "/articles" []
      :return ::specs/articles-short
      :query-params [{limit :- ::specs/limit 100}
                     {user_id :- ::specs/user_id nil}]
      :summary "Get list of latest articles"
      (get-all-articles-handler {:db db :limit limit :user-id user_id}))
    (GET "/articles/latest" []
      :return ::specs/articles-full
      :query-params [limit :- ::specs/limit]
      :summary "Get latest full articles"
      (get-latest-full-articles-handler db limit))
    (GET "/articles/:id" []
      :return ::specs/article-full
      :path-params [id :- ::specs/id]
      :summary "Get full article by article ID"
      (get-article-by-id-handler db id))
    (GET "/articles/featured/main" []
      :return ::specs/article-short
      :summary "Get main featured article"
      (get-last-main-featured-article-handler db))
    (GET "/articles/featured/latest" []
      :return ::specs/articles-short
      :query-params [limit :- ::specs/limit]
      :summary "Get latest featured articles list"
      (get-last-featured-articles-handler db limit))
    (DELETE "/articles/:id" []
      :middleware [[require-roles #{:guest}]]
      :path-params [id :- ::specs/id]
      :return {}
      :summary "Delete article by ID"
      :responses {401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (delete-article-handler db id))))
