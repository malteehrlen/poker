(ns poker.cljs.components.room-component
  (:require poker.clj.localstorage
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(println ?csrf-token)

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" 
	   ?csrf-token
       {:type :ws})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

;;;; Sente event handlers

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id                                               ; Dispatch on event-id
          )

(defn event-msg-handler
      "Wraps `-event-msg-handler` with logging, error catching, etc."
      [{:as ev-msg :keys [id ?data event]}]
      (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
           :default                                         ; Default/fallback case (no other matching handler)
           [{:as ev-msg :keys [event]}]
           (println "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
           [{:as ev-msg :keys [?data]}]
           (let [[old-state-map new-state-map] (have vector? ?data)]
                (if (:first-open? new-state-map)
                  (println "Channel socket successfully established!: %s" new-state-map)
                  (println "Channel socket state change: %s" new-state-map))))

(defmethod -event-msg-handler :chsk/recv
           [{:as ev-msg :keys [?data]}]
           (println "Push event from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
           [{:as ev-msg :keys [?data]}]
           (let [[?uid ?csrf-token ?handshake-data] ?data]
                (println "Handshake: %s" ?data)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
      (stop-router!)
      (reset! router_
              (sente/start-client-chsk-router!
                ch-chsk event-msg-handler)))


(defn members-list [props]
  [:div.panel [:p (:user-id props)] [:p "member 2"]])

(defn vote-controls [props]
  [:div.panel [:button "0"] [:button "1"] [:button "2"] [:button "3"] [:button "?"]])

(defn message [props]
  [:p "its a tie! Fight to the death"])

(defn history [props]
  [:div.panel [:p  "1"] [:p "2"]])

(defn room-component []
(let [user-id (localstorage.get-item :user-id)]
(if (clojure.string/blank? user-id)
[:p "user-id not set"]
(do (start-router!) (fn []
  [:div.room
  [members-list {:user-id user-id}]
  [vote-controls]
  [message]
  [history]])
))))
