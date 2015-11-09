(ns applepie.core
  (:use [compojure.route :only [files not-found files]]
        [org.httpkit.timer :only [schedule-task]]
        [org.httpkit.server :only [run-server with-channel on-close on-receive send!]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [ring.util.response :only [redirect file-response resource-response content-type]]))

(defn log-handler [request]
  (with-channel request channel
                (on-close channel (fn [status] (println "channel closed: " status)))
                (on-receive channel (fn [data] ;; echo it back
                                      (send! channel data)))
                (loop []
                  (Thread/sleep 10000)
                  (schedule-task 1000 (send! channel "hello!" false)) ; false => don't close after send
                  (recur))))

(defroutes all-routes
           ;           (GET "/" [] (file-response "index.html" {:root "public"}))
           (GET "/" [] (content-type (resource-response "index.html" {:root "public"}) "text/html"))
           (GET "/ws" [] log-handler)
           (files "/static/")                         ;; static file url prefix /static, in `public` folder
           (not-found "<p>Page not found.</p>"))      ;; all other, return 404





(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(stop-server)
(reset! server (run-server (site #'all-routes) {:port 8080}))

