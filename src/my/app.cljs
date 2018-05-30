(ns my.app
  (:require
    [my.run :refer [run-graph!]]
    [naga.rules :as r :refer-macros [r]]
    [naga.engine :as e]
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
   [:mary :gender :female]
   [:george :gender :male]])

(def program (r/create-program rules axioms))
(def store (first (e/run {:type :memory} program)))

(run-graph! store (js/document.querySelector "svg"))
