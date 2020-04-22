(ns poker.clj.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [taoensso.sente :as sente]
            [hiccup.page :refer [include-js include-css html5]]
			[ring.util.response :as response]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
  			[ring.middleware.session :refer [wrap-session]]
			[ring.middleware.anti-forgery :as anti-forgery :refer [wrap-anti-forgery]]
            [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
            [taoensso.encore :as encore :refer (have have?)]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [taoensso.sente :as sente]
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
	  wrap-anti-forgery
	  wrap-session
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params
))

(defonce broadcast-enabled?_ (atom true))

(defn start-example-broadcaster!
      "As an example of server>user async pushes, setup a loop to broadcast an
      event to all connected users every 10 seconds"
      []
      (let [broadcast!
            (fn [i]
                (let [uids (:any @connected-uids)]
                     (debugf "Broadcasting server>user: %s uids" (count uids))
                     (doseq [uid uids]
                            (chsk-send! uid
                                        [:some/broadcast
                                         {:what-is-this "An async broadcast pushed from server"
                                          :how-often "Every 10 seconds"
                                          :to-whom uid
                                          :i i}]))))]

           (go-loop [i 0]
                    (<! (async/timeout 10000))
                    (when @broadcast-enabled?_ (broadcast! i))
                    (recur (inc i)))))

;;;; Sente event handlers

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id                                               ; Dispatch on event-id
          )

(defn event-msg-handler
      "Wraps `-event-msg-handler` with logging, error catching, etc."
      [{:as ev-msg :keys [id ?data event]}]
      (-event-msg-handler ev-msg)                           ; Handle event-msgs on a single thread
      ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
      )

(defmethod -event-msg-handler
           :default                                         ; Default/fallback case (no other matching handler)
           [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
           (let [session (:session ring-req)
                 uid (:uid session)]
                (debugf "Unhandled event: %s" event)
                (when ?reply-fn
                      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler :example/toggle-broadcast
           [{:as ev-msg :keys [?reply-fn]}]
           (let [loop-enabled? (swap! broadcast-enabled?_ not)]
                (?reply-fn loop-enabled?)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
	(debugf "starting router")
      (stop-router!)
      (reset! router_
              (sente/start-server-chsk-router!
                ch-chsk event-msg-handler)))
