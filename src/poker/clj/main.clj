(ns poker.clj.main
  (:require [poker.clj.sente.router :as router]
            [org.httpkit.server :as http-kit]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [poker.clj.handler :as handler]))

(defonce    web-server_ (atom nil)) ; (fn stop [])
(defn  stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port (or port 0) ; 0 => Choose any available port
        ring-handler (var handler/app)

        [port stop-fn]
        (let [stop-fn (http-kit/run-server ring-handler {:port port})]
          [(:local-port (meta stop-fn)) (fn [] (stop-fn :timeout 100))])
        uri (format "http://localhost:%s/" port)]

    (infof "Web server is running at `%s`" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))

    (reset! web-server_ stop-fn)))

(defn -main []
  (router/start-router!)
  (start-web-server!))
