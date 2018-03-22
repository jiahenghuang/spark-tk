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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics.numericalstatistics

import org.apache.spark.rdd.RDD
/**
 * Statistics calculator for weighted numerical data. Data elements with non-positive weights are thrown out and do
 * not affect stastics (excepting the count of entries with non-postive weights).
 * 加权数值数据的统计计算器,具有非正面权重的数据元素被丢弃,并且不影响stastics(除了具有非正面权重的条目的计数)
 * @param dataWeightPairs RDD of pairs of  the form (data, weight)
 */
class NumericalStatistics(dataWeightPairs: RDD[(Option[Double], Option[Double])], usePopulationVariance: Boolean) extends Serializable {

  /*
   * Incoming weights and data are Doubles, but internal running sums are represented as BigDecimal to improve
   * numerical stability.
   * 传入的权重和数据是双精度的,但内部运算和用BigDecimal表示,以提高数值稳定性
   *
   * Values are recast to Doubles before being returned because we do not want to give the false impression that
   * we are improving precision. The use of BigDecimals is only to reduce accumulated rounding error while combing
   * values over many, many entries.
   *
   * 由于我们不想给我们提高精度的错误印象,所以在返回之前价值被重铸为双倍,BigDecimals的使用仅仅是为了减少积累的舍入误差,同时在许多条目上梳理数值。
   */

  private lazy val singlePassStatistics: FirstPassStatistics = StatisticsRddFunctions.generateFirstPassStatistics(dataWeightPairs)

  /**
   * The weighted mean of the data.
    * 数据的加权平均值
   */
  lazy val weightedMean: Double = singlePassStatistics.mean.toDouble

  /**
   * The weighted geometric mean of the data. NaN when a data element is <= 0,
   * 1 when there are no data elements of positive weight.
    * 数据的加权几何平均值,当数据元素<= 0时为NaN,当没有正数权重的数据元素时为1,
   */
  lazy val weightedGeometricMean: Double = {

    val totalWeight: BigDecimal = singlePassStatistics.totalWeight
    val weightedSumOfLogs: Option[BigDecimal] = singlePassStatistics.weightedSumOfLogs

    if (totalWeight > 0 && weightedSumOfLogs.nonEmpty)
      Math.exp((weightedSumOfLogs.get / totalWeight).toDouble)
    else if (totalWeight > 0 && weightedSumOfLogs.isEmpty) {
      Double.NaN
    }
    else {
      // this is the totalWeight == 0 case
      1.toDouble
    }
  }

  /**
   * The weighted variance of the data. NaN when there are <=1 data elements.
    * 数据的加权方差,NaN当有<= 1个数据元素时
   */
  lazy val weightedVariance: Double = {
    val weight: BigDecimal = singlePassStatistics.totalWeight
    if (usePopulationVariance) {
      (singlePassStatistics.weightedSumOfSquaredDistancesFromMean / weight).toDouble
    }
    else {
      if (weight > 1)
        (singlePassStatistics.weightedSumOfSquaredDistancesFromMean / (weight - 1)).toDouble
      else
        Double.NaN
    }
  }

  /**
   * The weighted standard deviation of the data. NaN when there are <=1 data elements of nonzero weight.
    * 数据的加权标准差,NaN当有<= 1个非零权重的数据元素时
   */
  lazy val weightedStandardDeviation: Double = Math.sqrt(weightedVariance)

  /**
   * Sum of all weights that are finite numbers  > 0.
    * 所有有限数> 0的权重之和
   */
  lazy val totalWeight: Double = singlePassStatistics.totalWeight.toDouble

  /**
   * The minimum value of the data. Positive infinity when there are no data elements of positive weight.
    * 数据的最小值,没有正面权重的数据元素时为正无穷大
   */
  lazy val min: Double = if (singlePassStatistics.minimum.isInfinity) Double.NaN else singlePassStatistics.minimum

  /**
   * The maximum value of the data. Negative infinity when there are no data elements of positive weight.
    * 数据的最大值,没有正数权重的数据元素时为负无穷大
   */
  lazy val max: Double = if (singlePassStatistics.maximum.isInfinity) Double.NaN else singlePassStatistics.maximum

  /**
   * The number of elements in the data set with weight > 0.
    * 数据集中元素的数量与权重> 0
   */
  lazy val positiveWeightCount: Long = singlePassStatistics.positiveWeightCount

  /**
   * The number of pairs that contained NaNs or infinite values for a data column or a weight column (if the weight column
    * 包含数据列或权重列的NaN或无限值的对数,如果权重列
   */
  lazy val badRowCount: Long = singlePassStatistics.badRowCount

  /**
   * The number of pairs that contained proper finite numbers for the data column and the weight column.
    * 数据列和权重列中包含适当有限数字的对的数量
   */
  lazy val goodRowCount: Long = singlePassStatistics.goodRowCount

  /**
   * The number of elements in the data set with weight <= 0.
    * 数据集中权重<= 0的元素数量
   */
  lazy val nonPositiveWeightCount: Long = singlePassStatistics.nonPositiveWeightCount

  /**
   * The lower limit of the 95% confidence interval about the mean. (Assumes that the distribution is normal.)
   * NaN when the total weight is 0.
    * 平均值的95％置信区间的下限,(假设分布是正常的)NaN当总重量为0时。
   */
  lazy val meanConfidenceLower: Double =

    if (positiveWeightCount > 1 && weightedStandardDeviation != Double.NaN)
      weightedMean - 1.96 * (weightedStandardDeviation / Math.sqrt(totalWeight))
    else
      Double.NaN

  /**
   * The lower limit of the 95% confidence interval about the mean. (Assumes that the distribution is normal.)
   * NaN when the total weight is 0.
    * 平均值的95％置信区间的下限,(假设分布是正常的)总重量为0时的NaN
   */
  lazy val meanConfidenceUpper: Double =
    if (totalWeight > 0 && weightedStandardDeviation != Double.NaN)
      weightedMean + 1.96 * (weightedStandardDeviation / Math.sqrt(totalWeight))
    else
      Double.NaN

}
