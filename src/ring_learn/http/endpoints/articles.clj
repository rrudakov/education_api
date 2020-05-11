(ns ring-learn.http.endpoints.articles
  (:require [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [ring-learn.database.articles :as articlesdb]
            [ring-learn.http.restructure :refer [require-roles]]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response :refer [created no-content not-found ok]]
            [schema.core :as s]))

;; Model definitions
(s/defschema ArticleCreateRequest
  "Request for creating new article."
  {:user_id s/Int
   :title s/Str
   :body s/Str
   :featured_image s/Str
   :is_main_featured s/Bool})

(s/defschema ArticleUpdateRequest
  "Request for updating existing article."
  {:title (s/maybe s/Str)
   :body (s/maybe s/Str)
   :featured_image (s/maybe s/Str)
   :is_main_featured (s/maybe s/Bool)})

(s/defschema ArticleShortResponse
  "Article response short object."
  {:id s/Int
   :user_id s/Int
   :title s/Str
   :featured_image s/Str
   :updated_on s/Inst})

(s/defschema ArticleFullResponse
  "Article response full object."
  {:id s/Int
   :user_id s/Int
   :title s/Str
   :body s/Str
   :featured_image s/Str
   :created_on s/Inst
   :updated_on s/Inst})

;; Converters
(defn to-short-article-response
  "Convert database article to article response."
  [{:articles/keys [id user_id title featured_image updated_on]}]
  {:id id
   :user_id user_id
   :title title
   :featured_image featured_image
   :updated_on updated_on})

(defn to-full-article-response
  "Convert database article to full article response."
  [{:articles/keys [id user_id title body featured_image created_on updated_on]}]
  {:id id
   :user_id user_id
   :title title
   :body body
   :featured_image featured_image
   :created_on created_on
   :updated_on updated_on})

;; Handlers
(defn- create-article-handler
  "Create new article handler."
  [db article]
  (let [new-id (str (articlesdb/add-article db article))]
    (created (str "/articles/" new-id) {:id new-id})))

(defn- update-article-handler
  "Update existing article handler."
  [db article-id article]
  (do
    (articlesdb/update-article db article-id article)
    (no-content)))

(defn- get-all-articles-handler
  "Get all recent articles handler."
  [{:keys [db limit user-id]}]
  (let [articles (if (nil? user-id)
                   (articlesdb/get-all-articles db limit)
                   (articlesdb/get-user-articles db user-id limit))]
    (->> articles
         (map to-short-article-response)
         ok)))

(defn- get-article-by-id-handler
  "Get article by `article-id` handler."
  [db article-id]
  (let [article (articlesdb/get-article-by-id db article-id)]
    (if (nil? article)
      (not-found {:message (str "Article with id " article-id " not found!")})
      (ok (to-full-article-response article)))))

(defn- get-last-main-featured-article-handler
  "Get last main featured article handler."
  [db]
  (let [mf-article (articlesdb/get-last-featured-article db)]
    (if (nil? mf-article)
      (not-found {:message "Main featured article was not found!"})
      (ok (to-short-article-response mf-article)))))

(defn- delete-article-handler
  "Delete article by `article-id` handler."
  [db article-id]
  (do
    (articlesdb/delete-article db article-id)
    (no-content)))

;; Define routes
(defn articles-routes
  "Define routes for articles endpoint."
  [db]
  (context "" []
    :tags ["articles"]
    (POST "/articles" []
      :middleware [[require-roles #{:moderator}]]
      :body [article (describe ArticleCreateRequest "New article to be created")]
      :return (describe s/Int "ID of created article")
      :summary "Create new article"
      (create-article-handler db article))
    (PATCH "/articles/:id" []
      :middleware [[require-roles #{:moderator}]]
      :body [article (describe ArticleUpdateRequest "Request body with updated fields")]
      :path-params [id :- (describe s/Int "Specify article ID")]
      :summary "Update existing article by article ID"
      (update-article-handler db id article))
    (GET "/articles" []
      :return (describe [ArticleShortResponse] "List of short article objects")
      :query-params [{limit :-  (describe s/Int "Limit maximum articles") 100}
                     {user_id :- (describe (s/maybe s/Int) "Return only articles for this author") nil}]
      :summary "Get list of latest articles"
      (get-all-articles-handler {:db db :limit limit :user-id user_id}))
    (GET "/articles/:id" []
      :return (describe ArticleFullResponse "Full article object")
      :path-params [id :- (describe s/Int "Specify article ID")]
      :summary "Get full article by article ID"
      (get-article-by-id-handler db id))
    (GET "/articles/featured/main" []
      :return (describe ArticleShortResponse "Main featured article object")
      :summary "Get main featured article"
      (get-last-main-featured-article-handler db))
    (DELETE "/articles/:id" []
      :middleware [[require-roles #{:moderator}]]
      :path-params [id :- (describe s/Int "Specify article ID")]
      :return nil
      :summary "Delete article by ID"
      (delete-article-handler db id))))
