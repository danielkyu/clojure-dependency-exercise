;; Owner: yu.danielk@gmail.com
;;
;; Test cases that validate logic.

(ns app.test.core
  (:require [app.core :as core]
            [clojure.test :refer :all]))

(deftest ->source-map
  (is (= {"A" {:name "A"}
          "B" {:name "B"}
          "C" {:name "C"}}
         (core/->source-map [{:name "C"}
                             {:name "B"}
                             {:name "A"}]))))

(deftest is-dependencies-loaded?
  (testing "Dependencies are loaded"
    (is (core/is-dependencies-loaded? #{} {:name "A" :dependencies #{}}))
    (is (core/is-dependencies-loaded? #{"B" "C"} {:name "A" :dependencies #{}}))
    (is (core/is-dependencies-loaded? #{"B" "C" "D"} {:name "A" :dependencies #{"B" "C"}})))
  (testing "Dependencies are not loaded"
    (is (not (core/is-dependencies-loaded? #{} {:name "A" :dependencies #{"B"}})))
    (is (not (core/is-dependencies-loaded? #{"B" "C"} {:name "A" :dependencies #{"D"}})))
    (is (not (core/is-dependencies-loaded? #{"B" "C" "D"}
                                           {:name "A" :dependencies #{"B" "E"}})))))

(deftest find-loadable-sources
  (testing "Loadable sources exist"
    (is (= [["A" {:name "A" :dependencies #{}}]
            ["B" {:name "B" :dependencies #{}}]]
           (core/find-loadable-sources {"A" {:name "A" :dependencies #{}}
                                        "B" {:name "B" :dependencies #{}}
                                        "C" {:name "C" :dependencies #{"A"}}}
                                       #{}))))
  (testing "No loadable sources were found"
    (is (empty? (core/find-loadable-sources {"A" {:name "A" :dependencies #{"D"}}
                                             "B" {:name "B" :dependencies #{"A"}}
                                             "C" {:name "C" :dependencies #{"B" "D"}}}
                                            #{"E" "F"})))))

(deftest get-source-list
  (testing "Nil/empty source list"
    (is (= {:failed-sources #{}
            :loadable-sources []}
           (core/get-source-list nil)
           (core/get-source-list []))))
  (testing "Multiple independent sources"
    (is (= {:failed-sources #{}
            :loadable-sources [["B" {:name "B" :dependencies #{}}]
                               ["C" {:name "C" :dependencies #{}}]
                               ["A" {:name "A" :dependencies #{}}]]}
           (-> [{:name "A" :dependencies #{}}
                {:name "B" :dependencies #{}}
                {:name "C" :dependencies #{}}]
               (core/get-source-list)))))
  (testing "Chained dependencies"
    (is (= {:failed-sources #{}
            :loadable-sources [["C" {:name "C" :dependencies #{}}]
                               ["B" {:name "B" :dependencies #{"C"}}]
                               ["A" {:name "A" :dependencies #{"B"}}]]}
           (-> [{:name "A" :dependencies #{"B"}}
                {:name "B" :dependencies #{"C"}}
                {:name "C" :dependencies #{}}]
               (core/get-source-list)))))
  (testing "Unreachable sources"
    (is (= {:failed-sources #{"D" "E"}
            :loadable-sources [["C" {:name "C" :dependencies #{}}]
                               ["B" {:name "B" :dependencies #{"C"}}]
                               ["A" {:name "A" :dependencies #{"B"}}]]}
           (-> [{:name "A" :dependencies #{"B"}}
                {:name "B" :dependencies #{"C"}}
                {:name "C" :dependencies #{}}
                {:name "D" :dependencies #{"F"}}
                {:name "E" :dependencies #{"A" "D"}}]
               (core/get-source-list))))
    (is (= {:failed-sources #{"A" "B" "C"}
            :loadable-sources [["E" {:name "E" :dependencies #{}}]
                               ["F" {:name "F" :dependencies #{"E"}}]]}
           (-> [{:name "A" :dependencies #{"D"}}
                {:name "B" :dependencies #{"A"}}
                {:name "C" :dependencies #{"B" "D"}}
                {:name "E" :dependencies #{}}
                {:name "F" :dependencies #{"E"}}]
               (core/get-source-list)))))
  (testing "Duplicate sources with the same name"
    (is (= {:failed-sources #{"A"}
            :loadable-sources []}
           (-> [{:name "A" :dependencies #{}}
                {:name "A" :dependencies #{"B"}}]
               (core/get-source-list))))
    (is (= {:failed-sources #{}
            :loadable-sources [["A" {:name "A" :dependencies #{}}]]}
            (-> [{:name "A" :dependencies #{"B"}}
                 {:name "A" :dependencies #{}}]
                (core/get-source-list)))))
  (testing "Example from coding exercise"
    (is (= {:failed-sources #{}
            :loadable-sources [["sourceF" {:name "sourceF"
                                           :dependencies #{}}]
                               ["sourceD" {:name "sourceD"
                                           :dependencies #{}}]
                               ["sourceE" {:name "sourceE"
                                           :dependencies #{}}]
                               ["sourceB" {:name "sourceB"
                                           :dependencies #{"sourceE" "sourceF"}}]
                               ["sourceC" {:name "sourceC"
                                     :dependencies #{"sourceD" "sourceF"}}]
                               ["sourceA" {:name "sourceA"
                                     :dependencies #{"sourceC" "sourceD" "sourceB"}}]]}
           (-> [{:name "sourceA"
                 :dependencies #{"sourceB" "sourceC" "sourceD"}}
                {:name "sourceB"
                 :dependencies #{"sourceE" "sourceF"}}
                {:name "sourceC"
                 :dependencies #{"sourceD" "sourceF"}}
                {:name "sourceD"
                 :dependencies #{}}
                {:name "sourceE"
                 :dependencies #{}}
                {:name "sourceF"
                 :dependencies #{}}]
               (core/get-source-list))))))
