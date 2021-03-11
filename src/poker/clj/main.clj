(ns poker.clj.main
  (:require [poker.clj.sente.router :as router]
            [poker.clj.rooms :as rooms]
            [org.httpkit.server :as http-kit]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [poker.clj.handler :as handler])
  (:gen-class))

(defonce web-server_ (atom nil)) ; (fn stop [])

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port (or port 9190) ; 0 => Choose any available port
        ring-handler (var handler/app)
        job (set-interval rooms/push-all-rooms 5000)
        [port stop-fn]
        (let [stop-fn (http-kit/run-server ring-handler {:port port})]
          [(:local-port (meta stop-fn)) (fn [] ((do (stop-fn :timeout 100) (future-cancel job))))])
        uri (format "http://localhost:%s/" port)]

    (infof "Web server is running at `%s`" uri)

    (reset! web-server_ stop-fn)))

(defn -main []
  (router/start-router!)
  (start-web-server!))
