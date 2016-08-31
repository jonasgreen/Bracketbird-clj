(ns bracketbird.pages.error-page)


(defn render [ctx]
  (println "render-error-page" ctx)
  [:div "error-page" (str ctx)]


  )
