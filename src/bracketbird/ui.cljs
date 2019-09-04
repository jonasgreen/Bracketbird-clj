(ns bracketbird.ui
  (:require [bracketbird.state :as state]))



(defn gui [hook ctx]
  (let [r (state/subscribe [:renders hook])
        reactions (reduce (fn [m h] (assoc m h (state/hook h ctx))) {:values (state/hook hook ctx (:values @r))} (:reactions @r))]

    (fn [hook ctx]
      (println "render " hook)
      (let [{:keys [render]} @r]
        (if render
          [(render) ctx (reduce-kv (fn [m k v] (assoc m k (deref v))) {} reactions)]
          [:div (str "No render: " hook ctx)])))))






