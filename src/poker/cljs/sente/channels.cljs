(ns poker.cljs.sente.channels
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"
                                  ?csrf-token
                                  {:type :ws})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

