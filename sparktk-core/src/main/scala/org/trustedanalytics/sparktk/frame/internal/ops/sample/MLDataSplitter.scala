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
package org.trustedanalytics.sparktk.frame.internal.ops.sample

import org.apache.spark.rdd._

import scala.reflect.ClassTag
import scala.util.Random

/**
 * Class that represents the entry content and label of a data point.
 * 表示数据点的条目内容和标签的类
 * @param label for this data point.为这个数据点
 * @param entry content for this data point. 这个数据点的内容
 */
case class LabeledLine[L: ClassTag, T: ClassTag](label: L, entry: T)

/**
 * Data Splitter for ML algorithms. It randomly labels an input RDD with user
 * specified percentage for each category.
  * 数据分配器用于ML算法,它为每个类别随机标记一个用户指定百分比的输入RDD
 *
 * TODO: this class doesn't really belong in the Engine but it is shared code that both frame-plugins and graph-plugins need access to
 *
 * @param percentages A double array stores percentages.
 * @param seed Random seed for random number generator.
 */
class MLDataSplitter(percentages: Array[Double], labels: Array[String], seed: Int) extends Serializable {

  require(percentages.forall(p => p > 0d), "MLDataSplitter: Some percentage numbers are negative or zero.")
  require(Math.abs(percentages.sum - 1.0d) < 0.000000001d, "MLDataSplitter: Sum of percentages does not equal  1.")
  require(labels.length == percentages.length, "Number of class labels differs from number of percentages given.")

  var cdf: Array[Double] = percentages.scanLeft(0.0d)(_ + _)
  cdf = cdf.drop(1)

  // clamp the final value to 1.0d so that we cannot get rare (but in big data, still possible!)
  //把最终的值调整到1.0d，这样我们就不会稀少了(但是在大数据中,仍然是可能的)
  // occurrences where the sample value falls between the gap of the summed input probabilities and 1.0d
  cdf(cdf.length - 1) = 1.0d

  /**
   * Randomly label each entry of an input RDD according to user specified percentage
   * for each category.
   * 根据每个类别的用户指定百分比随机标记输入RDD的每个条目。
   * @param inputRDD RDD of type T.
   */
  def randomlyLabelRDD[T: ClassTag](inputRDD: RDD[T]): RDD[LabeledLine[String, T]] = {
    // generate auxiliary (sample) RDD
    val auxiliaryRDD: RDD[(T, Double)] = inputRDD.mapPartitionsWithIndex({ case (i, p) => addRandomValues(seed, i, p) })

    val labeledRDD = auxiliaryRDD.map { p =>
      val (line, sampleValue) = p
      val label = labels.apply(cdf.indexWhere(_ >= sampleValue))
      LabeledLine(label, line)
    }
    labeledRDD
  }

  private def addRandomValues[T: ClassTag](seed: Int, index: Int, it: Iterator[T]): Iterator[(T, Double)] = {
    val pseudoRandomGenerator = new Random(seed + index)
    it.map(x => (x, pseudoRandomGenerator.nextDouble()))
  }
}
