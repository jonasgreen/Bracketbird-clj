(ns bracketbird.tournament-model-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(defn- gen-tn [tn-val-and-val-type]
  (merge tn-val-and-val-type {:tillaeg-nedslag/fortloebende false
                              :tillaeg-nedslag/aarsag       :begrundelse/rockerborg
                              :tillaeg-nedslag/type         :type/forurening}))

(deftest model
  (is (= 1500000
         (v/vur {:model/ev 1500000}
                :vurderingsmetode/model))))

(deftest kvm
  (is (= 1000000
         (v/vur {:model/ev 1500000
                 :kvm-pris 10000
                 :kvm      100}
                :vurderingsmetode/fastsaet-kvm-pris))))

(deftest model-nedslag-nominel
  (is (= 900000
         (v/vur {:model/ev        1000000
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      -100000})]}
                :vurderingsmetode/model))))

(deftest model-tillæg-nominel
  (is (= 1100000
         (v/vur {:model/ev        1000000
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      100000})]}
                :vurderingsmetode/model))))

(deftest model-nedslag-ratio
  (is (= 900000
         (v/vur {:model/ev        1000000
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/ratio
                                            :tillaeg-nedslag/value      -0.1})]}
                :vurderingsmetode/model))))

(deftest model-tillaeg-nedslag-nominel-ratio
  (is (= 920000
         (v/vur {:model/ev        1000000
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/ratio
                                            :tillaeg-nedslag/value      -0.1})
                                   (gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      20000})]}
                :vurderingsmetode/model))))

(deftest kvm-nedslag-nominel
  (is (= 1900000
         (v/vur {:model/ev        1000000
                 :kvm-pris        20000
                 :kvm             100
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      -100000})]}
                :vurderingsmetode/fastsaet-kvm-pris))))

(deftest kvm-tillæg-nominel
  (is (= 2100000
         (v/vur {:model/ev        1000000
                 :kvm-pris        20000
                 :kvm             100
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      100000})]}
                :vurderingsmetode/fastsaet-kvm-pris))))

(deftest kvm-nedslag-ratio
  (is (= 1800000
         (v/vur {:model/ev        1000000
                 :kvm-pris        20000
                 :kvm             100
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/ratio
                                            :tillaeg-nedslag/value      -0.1})]}
                :vurderingsmetode/fastsaet-kvm-pris))))

(deftest kvm-tillaeg-nedslag-nominel-ratio
  (is (= 1820000
         (v/vur {:model/ev        1000000
                 :kvm-pris        20000
                 :kvm             100
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/ratio
                                            :tillaeg-nedslag/value      -0.1})
                                   (gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      20000})]}
                :vurderingsmetode/fastsaet-kvm-pris))))

(deftest skøn
  (is (= 2100000
         (v/vur {:model/ev 1500000
                 :skoen    2100000}
                :vurderingsmetode/skoensmaessig-pris))))

(deftest skøn-ignorer-tillaeg-nedslag
  (is (= 2000000
         (v/vur {:model/ev        1500000
                 :skoen           2000000
                 :kvm-pris        10000
                 :kvm             100
                 :tillaeg-nedslag [(gen-tn {:tillaeg-nedslag/value-type :value-type/nominal
                                            :tillaeg-nedslag/value      -10000})
                                   (gen-tn {:tillaeg-nedslag/value-type :value-type/ratio
                                            :tillaeg-nedslag/value      -0.1})]}
                :vurderingsmetode/skoensmaessig-pris))))

(deftest udled-model

  (is (= (v/lean-model {:model           {:ev 2000000}
                        :tillaeg-nedslag []} :vurderingsmetode/model)
         {:model           {:ev 2000000}
          :tillaeg-nedslag []}))

  (is (= (v/lean-model {:model           {:ev 2000000}
                        :tillaeg-nedslag [{:tillaeg-nedslag/value-type   :value-type/nominal
                                           :tillaeg-nedslag/value        100000
                                           :tillaeg-nedslag/aarsag       :begrundelse/rockerborg
                                           :tillaeg-nedslag/fortloebende false
                                           :tillaeg-nedslag/type         :type/forurening}]}
                       :vurderingsmetode/model)
         {:model           {:ev 2000000}
          :tillaeg-nedslag [{:tillaeg-nedslag/value-type   :value-type/nominal
                             :tillaeg-nedslag/value        100000
                             :tillaeg-nedslag/aarsag       :begrundelse/rockerborg
                             :tillaeg-nedslag/fortloebende false
                             :tillaeg-nedslag/type         :type/forurening}]})))

(deftest keep-model-resultat
  (is (= (v/lean-model {:model           {:ev 2000000}
                        :kvm-pris        10000
                        :tillaeg-nedslag []}
                       :vurderingsmetode/fastsaet-kvm-pris)
         {:model           {:ev 2000000}
          :tillaeg-nedslag []
          :kvm-pris        10000}))

  (is (= (v/lean-model {:model           {:ev 2000000}
                        :skoen           1000000
                        :tillaeg-nedslag []}
                       :vurderingsmetode/skoensmaessig-pris)
         {:model  {:ev 2000000}
          :skoen 1000000})))

(deftest remove-non-selected
  (is (= (v/lean-model {:model           {:ev 2000000}
                        :kvm-pris        10000
                        :skoen           12345678
                        :tillaeg-nedslag []}
                       :vurderingsmetode/fastsaet-kvm-pris)
         {:model           {:ev 2000000}
          :tillaeg-nedslag []
          :kvm-pris        10000}))

  (is (= (v/lean-model {:model           {:ev 2000000}
                        :kvm-pris        10000
                        :skoen           1500000
                        :tillaeg-nedslag [{:tillaeg-nedslag/value-type :value-type/nominal
                                           :tillaeg-nedslag/value      100000}]}
                       :vurderingsmetode/skoensmaessig-pris)
         {:model {:ev 2000000}
          :skoen 1500000})))

(deftest remove-tillaeg-for-skoen
  (is (= (v/lean-model {:model           {:ev 2000000}
                        :kvm-pris        10000
                        :skoen           1500000
                        :tillaeg-nedslag [{:tillaeg-nedslag/value-type :value-type/nominal
                                           :tillaeg-nedslag/value      100000}]}
                       :vurderingsmetode/skoensmaessig-pris)
         {:model {:ev 2000000}
          :skoen 1500000})))




;------------ validate tillaeg-nedslag data structure -------------

(def valid-tn {:tillaeg-nedslag/type         :tillaeg-nedslag-typer/ejd-tag-kvalitet
               :tillaeg-nedslag/value-type   :value-type/nominal
               :tillaeg-nedslag/value        100
               :tillaeg-nedslag/fortloebende true
               :tillaeg-nedslag/aarsag       :tillaeg-nedslag-aarsager/besigtigelse})

(deftest validate-tn
  (is (v/is-valid-tn? valid-tn) "Valid tillæg-nedslag")
  (is (not (v/is-valid-tn? (assoc valid-tn :tillaeg-nedslag/value 0))) "Value = 0")
  (is (not (v/is-valid-tn? (dissoc valid-tn :tillaeg-nedslag/value-type))) "Missing value-type")

  (is (v/is-valid-tn? (assoc valid-tn
                        :tillaeg-nedslag/type :tillaeg-nedslag-typer/andet
                        :tillaeg-nedslag/anden-type "Badeværelse ser grimt ud")) "Valid tillæg-nedslag with anden-type")

  (is (not (v/is-valid-tn? (assoc valid-tn
                             :tillaeg-nedslag/type :tillaeg-nedslag-typer/andet
                             :tillaeg-nedslag/anden-type ""))) "Anden-type is empty")

  (is (not (v/is-valid-tn? (assoc valid-tn
                             :tillaeg-nedslag/type :tillaeg-nedslag-typer/andet
                             :tillaeg-nedslag/anden-type nil))) "Anden-type is nil")

  (is (v/is-valid-tn? (assoc valid-tn
                        :tillaeg-nedslag/type :tillaeg-nedslag-typer/andet
                        :tillaeg-nedslag/anden-type "Badeværelset er grimt"

                        :tillaeg-nedslag/aarsag :tillaeg-nedslag-aarsager/andet
                        :tillaeg-nedslag/anden-aarsag "Hørete det fra min onkel"
                        )) "Anden-type and anden-aarsag at the same time")

  (is (not (v/is-valid-tn? (assoc valid-tn
                             :tillaeg-nedslag/aarsag :tillaeg-nedslag-aarsager/besigtigelse
                             :tillaeg-nedslag/anden-aarsag "en anden aarsag")))
      "anden aarsag speficied when aarsag is not anden")

  (is (not (v/is-valid-tn? (assoc valid-tn
                             :tillaeg-nedslag/aarsag :tillaeg-nedslag-aarsager/besigtigelse
                             :tillaeg-nedslag/anden-aarsag nil)))
      "anden aarsag present when aarsag is not anden"))





