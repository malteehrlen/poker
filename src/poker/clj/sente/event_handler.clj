(ns poker.clj.sente.event-handler
  (:require 
            [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
            [poker.clj.sente.channels :refer [chsk-send! connected-uids]]
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
