(ns my.app
  (:require
    ["dagre-d3" :as dagreD3]
    ["d3" :as d3]
    [clojure.string :as string]
    [naga.rules :as r :refer-macros [r]]
    [naga.engine :as e]
    [naga.store :as store]
    [asami.core :as mem]))

(def rules
  [(r "shared-parent" [?b :parent ?c] :- [?a :sibling ?b] [?a :parent ?c])
   (r "sibling->brother" [?a :brother ?b] :- [?a :sibling ?b] [?b :gender :male])
   (r "uncle" [?a :uncle ?c] :- [?a :parent ?b] [?b :brother ?c])
   (r "male-father" [?f :gender :male] :- [?a :father ?f])
   (r "female-father" [?f :gender :female] :- [?a :mother ?f])
   (r "parent-father" [?a :parent ?f] :- [?a :father ?f])
   (r "parent-mother" [?a :parent ?f] :- [?a :mother ?f])])

(def axioms
  [[:fred :sibling :barney]
   [:fred :mother :mary]
   [:mary :sibling :george]
   [:george :gender :male]])

(def program (r/create-program rules axioms))
(def store (first (e/run {:type :memory} program)))

(def g
  (.setGraph
    (new (-> dagreD3
             (.-graphlib)
             (.-Graph))
         #js {:multigraph true})
    #js {}))

(def entities (map first (store/query store '[?e] '[[?e ?p ?v]])))

(doseq [state entities]
  (.setNode g (name state) #js {:labelType "html"
                                :label (str "<div><strong>" (name state) "</strong>"
                                            ;; TODO: without negation, I can't figure out how to get ?v where ?v is not an ?e
                                            (string/join
                                              ","
                                              (map
                                                (fn [[p v]]
                                                  (str "<div>" p " " v "</div>"))
                                                (let [x (set entities)]
                                                  (filter
                                                    (fn [[_ v]]
                                                      (not (contains? x v)))
                                                    (store/query store
                                                                 '[?p ?v]
                                                                 [[state '?p '?v]])))))
                                            "</div>")}))

(doseq [[e a v] (store/query store '[?e ?a ?v] '[[?e ?a ?v]
                                                 [?v ?discard ?discardx]])]
  (prn e a v (str (name e) "-" (name v)))
  (.setEdge g (name e) (name v) #js {:label (name a)}
                                (str (name e) "-" (name a) "-" (name v))))

(def svg (.select d3 "svg"))
(def inner (.select svg "g"))
(def zoom (.on (.zoom d3)
               "zoom"
               (fn []
                 (.attr inner "transform" (.-transform (.-event d3))))))

(.call svg zoom)

(let [render (new (.-render dagreD3))]
  (render inner g))

(def initialScale 0.75)

(.call svg (.-transform zoom) (-> (.-zoomIdentity d3)
                                  (.translate
                                    (* (- (.attr svg "width")
                                          (.-width (.graph g)))
                                       (/ initialScale 2))
                                    20)
                                  (.scale initialScale)))

(.attr svg "height" (+ (* (.-height (.graph g)) initialScale) 40))
