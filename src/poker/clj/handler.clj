(ns poker.clj.handler
  (:require [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
            [compojure.core :refer :all]
            [compojure.route :as route]
			[hiccup.page :refer [html5 include-css include-js]]
            [poker.clj.sente.channels :refer [ring-ajax-get-or-ws-handshake ring-ajax-post]]
  			[ring.middleware.anti-forgery :as anti-forgery :refer [wrap-anti-forgery]]
			[ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "/css/site.css")])

(def loading-page
  [:div#app
   [:h2 "Loading..."]])

(defn landing-page []
  (html5
    (head)
    [:body {:class "body-container"}
      [:div#sente-csrf-token {:data-csrf-token (force anti-forgery/*anti-forgery-token*)}]
     loading-page
     (include-js "/main.js")]))

(defn landing-page-handler []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (landing-page)})

(defroutes app-routes
  (GET "/" [] (landing-page-handler))
  (GET "/room/:room-id" [] (landing-page-handler))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
))
