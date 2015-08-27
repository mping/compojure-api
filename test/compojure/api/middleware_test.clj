(ns compojure.api.middleware-test
  (:require [compojure.api.middleware :refer :all]
            [midje.sweet :refer :all]
            [ring.util.http-response :refer [ok]]
            [ring.util.http-status :as status]
            ring.util.test))

(facts serializable?
  (tabular
    (fact
      (serializable? nil
                     {:body ?body
                      :compojure.api.meta/serializable? ?serializable?}) => ?res)
    ?body ?serializable? ?res
    5 true true
    5 false false
    "foobar" true true
    "foobar" false false

    {:foobar "1"} false true
    {:foobar "1"} true true
    [1 2 3] false true
    [1 2 3] true true

    (ring.util.test/string-input-stream "foobar") false false))

(facts "wrap-exceptions"
  (let [exception (proxy [RuntimeException] [] (printStackTrace []))
        exception-class (.getName (.getClass exception))
        failure (fn [_] (throw exception))
        success (fn [_] (ok "SUCCESS"))
        request irrelevant]

    (fact "passed through normal requests with deprecated exception-handler"
      ((wrap-exceptions success (support-deprecated-error-handler-config (merge (:exceptions api-middleware-defaults) {:exception-handler (constantly (ok "FAIL"))}))) request)
      => success)

    (fact "converts exceptions into safe internal server errors"
      ((wrap-exceptions failure (:error-handlers (:exceptions api-middleware-defaults))) request)
      => (contains {:status status/internal-server-error
                    :body (contains {:class exception-class
                                     :type "unknown-exception"})}))

    (fact "deprecated exception-handler still works"
      ((wrap-exceptions failure (support-deprecated-error-handler-config (merge (:exceptions api-middleware-defaults) {:exception-handler (constantly (ok "FAIL"))}))) request)
      => (ok "FAIL"))))
