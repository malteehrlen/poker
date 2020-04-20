(ns poker.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [taoensso.sente :as sente]
            [hiccup.page :refer [include-js include-css html5]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :as http-kit]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

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
     loading-page
     (include-js "/main.js")]))

(defn landing-page-handler []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (landing-page)})

(defroutes app-routes
  (GET "/" [] (landing-page-handler))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)))
