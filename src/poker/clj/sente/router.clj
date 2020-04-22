(ns poker.clj.sente.router (:require
  [poker.clj.sente.channels :refer [ch-chsk]]
  [poker.clj.sente.event-handler :refer (event-msg-handler)]
  [taoensso.sente :as sente]
  [org.httpkit.server :as http-kit]
  [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
  [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))


(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
	(debugf "starting router")
      (stop-router!)
      (reset! router_
              (sente/start-server-chsk-router!
                ch-chsk event-msg-handler)))
