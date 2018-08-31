(ns example.server
  (:require [io.pedestal.http]
            [clojure.core.async :as a]
            [reitit.pedestal :as pedestal]
            [reitit.http :as http]
            [reitit.ring :as ring]))

(defn interceptor [x]
  {:enter (fn [ctx] (println ">>" x) ctx)
   :leave (fn [ctx] (println "<<" x) ctx)})

(def routing-interceptor
  (pedestal/routing-interceptor
    (http/router
      ["/api"
       {:interceptors [[interceptor :api]
                       [interceptor :apa]]}

       ["/sync"
        {:interceptors [[interceptor :sync]]
         :get {:interceptors [[interceptor :get]]
               :handler (fn [_]
                          (println "handler")
                          {:status 200,
                           :body "pong"})}}]

       ["/async"
        {:interceptors [[interceptor :async]]
         :get {:interceptors [[interceptor :get]]
               :handler (fn [_]
                          (a/go
                            (println "handler")
                            {:status 200,
                             :body "pong"}))}}]]
      {:data {:interceptors [[interceptor :router]]}})
    (ring/create-default-handler)
    {:interceptors [[interceptor :top]]}))

(defonce server (atom nil))

(defn start []
  (when @server
    (io.pedestal.http/stop @server)
    (println "server stopped"))
  (-> {:env :prod
       :io.pedestal.http/routes []
       :io.pedestal.http/resource-path "/public"
       :io.pedestal.http/type :jetty
       :io.pedestal.http/port 3000}
      (merge {:env :dev
              :io.pedestal.http/join? false
              :io.pedestal.http/allowed-origins {:creds true :allowed-origins (constantly true)}})
      (pedestal/default-interceptors routing-interceptor)
      io.pedestal.http/dev-interceptors
      io.pedestal.http/create-server
      io.pedestal.http/start
      (->> (reset! server)))
  (println "server running in port 3000"))

(comment
  (start))