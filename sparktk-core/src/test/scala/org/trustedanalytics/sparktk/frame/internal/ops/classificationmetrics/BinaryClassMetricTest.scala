/**
 *  Copyright (c) 2016 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.sparktk.frame.internal.ops.classificationmetrics

import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.internal.rdd.ScoreAndLabel

class BinaryClassMetricTest extends TestingSparkContextWordSpec with Matchers {
  // posLabel = 1
  // tp = 1
  // tn = 2
  // fp = 0
  // fn = 1
  val inputListBinary = List(
    ScoreAndLabel(0, 0),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(0, 0),
    ScoreAndLabel(0, 1))

  val inputListBinaryChar = List(
    ScoreAndLabel("no", "no"),
    ScoreAndLabel("yes", "yes"),
    ScoreAndLabel("no", "no"),
    ScoreAndLabel("no", "yes"))

  val inputListBinary2 = List(
    ScoreAndLabel(0, 0),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(1, 0),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(0, 0),
    ScoreAndLabel(1, 0),
    ScoreAndLabel(0, 0),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(0, 0),
    ScoreAndLabel(0, 1),
    ScoreAndLabel(1, 0),
    ScoreAndLabel(1, 1),
    ScoreAndLabel(1, 0))

  // tp + tn = 2
  val inputListMulti = List(
    ScoreAndLabel(0, 0),
    ScoreAndLabel(2, 1),
    ScoreAndLabel(1, 2),
    ScoreAndLabel(0, 0),
    ScoreAndLabel(0, 1),
    ScoreAndLabel(1, 2))

  val inputListMultiChar = List(
    ScoreAndLabel("red", "red"),
    ScoreAndLabel("blue", "green"),
    ScoreAndLabel("green", "blue"),
    ScoreAndLabel("red", "red"),
    ScoreAndLabel("red", "green"),
    ScoreAndLabel("green", "blue"))

  "accuracy measure" should {
    //为二进制分类器计算正确的值
    "compute correct value for binary classifier" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val accuracy = binaryClassMetrics.accuracy()
      accuracy shouldEqual 0.75
    }
    //使用字符串标签计算二进制分类器的正确值
    "compute correct value for binary classifier with string labels" in {
      val rdd = sparkContext.parallelize(inputListBinaryChar)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yes")
      val accuracy = binaryClassMetrics.accuracy()
      accuracy shouldEqual 0.75
    }
    //计算二进制分类器2的正确值
    "compute correct value for binary classifier 2" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val accuracy = binaryClassMetrics.accuracy()
      val diff = (accuracy - 0.6428571).abs
      diff should be <= 0.0000001
    }
  }

  "precision measure" should {
    //为二进制分类器计算正确的值
    "compute correct value for binary classifier" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val precision = binaryClassMetrics.precision()
      precision shouldEqual 1.0
    }
    //使用字符串标签计算二进制分类器的正确值
    "compute correct value for binary classifier with string labels" in {
      val rdd = sparkContext.parallelize(inputListBinaryChar)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yes")
      val precision = binaryClassMetrics.precision()
      precision shouldEqual 1.0
    }
    //计算二进制分类器2的正确值
    "compute correct value for binary classifier 2" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val precision = binaryClassMetrics.precision()
      val diff = (precision - 0.5555555).abs
      diff should be <= 0.0000001
    }
    //如果标签列中不存在posLabel,则返回二进制分类器的值为0
    "return 0 for binary classifier if posLabel does not exist in label column" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yoyoyo")
      val precision = binaryClassMetrics.precision()
      precision shouldEqual 0.0
    }
  }

  "recall measure" should {
    //为二进制分类器计算正确的值
    "compute correct value for binary classifier" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val recall = binaryClassMetrics.recall()
      recall shouldEqual 0.5
    }
    //使用字符串标签计算二进制分类器的正确值
    "compute correct value for binary classifier with string labels" in {
      val rdd = sparkContext.parallelize(inputListBinaryChar)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yes")
      val recall = binaryClassMetrics.recall()
      recall shouldEqual 0.5
    }
    //计算二进制分类器2的正确值
    "compute correct value for binary classifier 2" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1)
      val recall = binaryClassMetrics.recall()
      val diff = (recall - 0.8333333).abs
      diff should be <= 0.0000001
    }
    //如果标签列中不存在posLabel,则返回二进制分类器的值为0
    "return 0 for binary classifier if posLabel does not exist in label column" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yoyoyo")
      val recall = binaryClassMetrics.recall()
      recall shouldEqual 0.0
    }
  }

  "f measure" should {
    //为beta = 0.5计算二进制分类器的正确值
    "compute correct value for binary classifier for beta = 0.5" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 0.5)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.8333333).abs
      diff should be <= 0.0000001
    }
    //为beta = 0.5计算二进制分类器2的正确值
    "compute correct value for binary classifier 2 for beta = 0.5" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 0.5)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.5952380).abs
      diff should be <= 0.0000001
    }
    //为beta = 1计算二进制分类器的正确值
    "compute correct value for binary classifier for beta = 1" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 1)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.6666666).abs
      diff should be <= 0.0000001
    }
    //使用字符串标签为beta = 1计算二进制分类器的正确值
    "compute correct value for binary classifier for beta = 1 with string labels" in {
      val rdd = sparkContext.parallelize(inputListBinaryChar)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yes", 1)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.6666666).abs
      diff should be <= 0.0000001
    }
    //为beta = 1计算二进制分类器2的正确值
    "compute correct value for binary classifier 2 for beta = 1" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 1)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.6666666).abs
      diff should be <= 0.0000001
    }
    //计算beta = 2的二进制分类器的正确值
    "compute correct value for binary classifier for beta = 2" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 2)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.5555555).abs
      diff should be <= 0.0000001
    }
    //为beta = 2计算二进制分类器2的正确值
    "compute correct value for binary classifier 2 for beta = 2" in {
      val rdd = sparkContext.parallelize(inputListBinary2)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, 1, 2)
      val fmeasure = binaryClassMetrics.fmeasure()
      val diff = (fmeasure - 0.7575757).abs
      diff should be <= 0.0000001
    }
    //如果标签列中不存在posLabel，则返回二进制分类器的值为0
    "return 0 for binary classifier if posLabel does not exist in label column" in {
      val rdd = sparkContext.parallelize(inputListBinary)

      val binaryClassMetrics = new BinaryClassMetrics(rdd, "yoyoyo", 1)
      val fmeasure = binaryClassMetrics.fmeasure()
      fmeasure shouldEqual 0.0
    }
  }
}
