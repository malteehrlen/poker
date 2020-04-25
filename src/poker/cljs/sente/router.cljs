(ns poker.cljs.sente.router
  (:require [poker.cljs.sente.event-handler :refer (event-msg-handler)]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]
			[poker.cljs.sente.channels :refer (ch-chsk)]))


(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
      (stop-router!)
      (reset! router_
              (sente/start-client-chsk-router!
                ch-chsk event-msg-handler)))
