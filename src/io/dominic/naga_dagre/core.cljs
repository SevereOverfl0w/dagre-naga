(ns io.dominic.naga-dagre.core
  (:require
    ["dagre-d3" :as dagreD3]
    ["d3" :as d3]
    [goog.object :as gobj]
    [naga.store :as store]))

(defn run-graph!
  [store svg-node]
  (let [g (.setGraph
            (new (-> dagreD3
                     (.-graphlib)
                     (.-Graph))
                 #js {:multigraph true})
            #js {})
        entities (map first (store/query store '[?e] '[[?e ?p ?v]]))]

    (doseq [state entities]
      (.setNode g (name state) #js {:labelType "html"
                                    :label (str "<div><strong>" (name state) "</strong>"
                                                ;; TODO: without negation, I can't figure out how to get ?v where ?v is not an ?e
                                                (apply str
                                                  (map
                                                    (fn [[p v]]
                                                      (str "<div>" p " " v "</div>"))
                                                    (let [x (set entities)]
                                                      (->> (store/query store
                                                                        '[?p ?v]
                                                                        [[state '?p '?v]])
                                                           (filter (fn [[_ v]] (not (contains? x v))))
                                                           (remove (comp #{:db/ident :db/id} first))))))
                                                "</div>")}))

    (doseq [[e a v] (store/query store '[?e ?a ?v] '[[?e ?a ?v]
                                                     [?v ?discard ?discardx]])]
      (.setEdge g (name e) (name v) #js {:label (name a)}
                                    (str (name e) "-" (name a) "-" (name v))))

    (gobj/set svg-node "innerHTML" "<g />")
    (let [svg (.select d3 "svg")
          inner (.select svg "g")
          zoom (.on (.zoom d3) "zoom"
                    (fn []
                      (.attr inner "transform" (.-transform (.-event d3)))))]
      (.call svg zoom)
      (let [render (new (.-render dagreD3))
            initialScale 0.75]
        (render inner g)
        (.call svg (.-transform zoom) (-> (.-zoomIdentity d3)
                                          (.translate
                                            (* (- (.attr svg "width")
                                                  (.-width (.graph g)))
                                               (/ initialScale 2))
                                            20)
                                          (.scale initialScale)))

        (.attr svg "height" (+ (* (.-height (.graph g)) initialScale) 40))))))
