(ns poker.clj.sente.event-handler
  (:require
   [poker.clj.rooms :refer (join-room apply-vote drop-user)]
   [poker.clj.sente.channels :refer [connected-uids chsk-send!]]
   [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

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

(defmulti event
  "Multimethod to handle Sente `event-msg`s"
  :id                                               ; Dispatch on event-id
  )

(defn wrapped-event
  "Wraps `event` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (event ev-msg)                           ; Handle event-msgs on a single thread
      ;; (future (event ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod event
  :default                                         ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event :example/toggle-broadcast
  [{:as ev-msg :keys [?reply-fn]}]
  (let [loop-enabled? (swap! broadcast-enabled?_ not)]
    (?reply-fn loop-enabled?)))

(defmethod event :chsk/uidport-open [{:keys [uid client-id]}]
  (println "New connection:" uid client-id))

(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (drop-user uid)
  (println "Disconnected:" uid))

(defmethod event :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake:" ?data)))

(defmethod event :chsk/ws-ping [_])

(defmethod event :poker/join-room [{:as ev-msg :keys [?data]}]
  (join-room (:uid ev-msg) (:roomname ?data) (:username ?data))
  (println (format "%s joined room %s" (:username ?data) (:roomname ?data))))

(defmethod event :poker/leave [{:keys [uid]}]
  (drop-user uid)
  (println "Disconnected:" uid))

(defmethod event :poker/vote [{:as ev-msg :keys [?data]}]
  (apply-vote (:uid ev-msg) (:roomname ?data) (:username ?data))
  (println (format "got vote %s from %s" (:vote ?data) (:?uid ?data))))

(defmethod event :poker/request-reveal [{:as ev-msg :keys [?data]}]
  (println (format "%s requested a reveal" (:vote ?data) (:?uid ?data))))
