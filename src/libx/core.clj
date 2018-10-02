(ns libx.core
  (:require [libx.threading :refer [fuse-transforms]]))

;; (let [jsobj #js {"a" 1 "b" 2}]
;;  (destruct-js jsobj [a b]
;;    (console.log a b))
(defmacro destruct-js
  "Destructuring syntax for JavaScript objects"
  [obj keys & body]
  (let [bindings
        (->> keys
         (map name)
         (mapcat (fn [k] [(symbol k) `(.. ~obj ~(symbol (str "-" k)))]))
         (into []))]
    `(let ~bindings
      ~@body)))

;; (when-keys [{:keys [a b]} {:a 1 :b 2}]
;;   (println a b))
(defmacro when-keys
  "When all symbols in destructuring form are evaluated to true,
  evaluates body with those symbols bound to corresponding values in a map"
  [bindings & body]
  (let [forms (:keys (bindings 0))
        tst (bindings 1)
        nbinds (->> forms
                    (map (fn [s] `(~(keyword s) ~tst)))
                    (interleave forms)
                    (into []))]
    `(let ~nbinds
       (when (and ~@forms)
         ~@body))))

(defn -rewrite-for [bindings body]
    (let [[item coll] bindings]
          `(loop [[~item & xs#] ~coll
                              ret# []]
                    (if (seq xs#)
                               (recur xs# (conj ret# ~body))
                               (seq (conj ret# ~body))))))

(defmacro for
    "Rewrites simple form `for` into `loop`"
    [seq-exprs body-expr]
    (if (= 2 (count seq-exprs))
          (-rewrite-for seq-exprs body-expr)
          `(clojure.core/for ~seq-exprs ~body-expr)))

(defn -rewrite-doseq [bindings body]
    (let [[item coll] bindings]
          `(loop [[~item & xs#] ~coll]
                    (do ~@body)
                    (when (seq xs#)
                               (recur xs#)))))

(defmacro doseq
    "Rewrites simple form `doseq` into `loop`"
    [seq-exprs & body]
    (if (= 2 (count seq-exprs))
          (-rewrite-doseq seq-exprs body)
          `(clojure.core/for ~seq-exprs ~@body)))

(defmacro ->>
  "Combines collections transforms (map, filter, etc.) to eliminate intermediate collections"
  [x & forms]
  `(clojure.core/->> ~x ~@(fuse-transforms forms)))