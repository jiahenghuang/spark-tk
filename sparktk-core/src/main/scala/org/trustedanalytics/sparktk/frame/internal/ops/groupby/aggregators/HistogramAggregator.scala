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
package org.trustedanalytics.sparktk.frame.internal.ops.groupby.aggregators

import org.trustedanalytics.sparktk.frame.DataTypes.DataType
import org.trustedanalytics.sparktk.frame.DataTypes
import org.trustedanalytics.sparktk.frame.internal.ops.binning.DiscretizationFunctions

/**
 * Aggregator for computing histograms using a list of cutoffs.
 * 使用截断列表计算直方图的聚合器
 * The histogram is a vector containing the percentage of observations found in each bin
  * 直方图是一个包含在每个箱中发现的观测百分比的向量
 */
case class HistogramAggregator(cutoffs: List[Double], includeLowest: Option[Boolean] = None, strictBinning: Option[Boolean] = None) extends GroupByAggregator {
  require(cutoffs.size >= 2, "At least one bin is required in cutoff array")
  require(cutoffs == cutoffs.sorted, "the cutoff points of the bins must be monotonically increasing")

  /** An array that aggregates the number of elements in each bin
    * 一个数组,聚合每个bin中元素的数量 */
  override type AggregateType = Array[Double]

  /** The bin number for a column value
    * 列值的bin编号*/
  override type ValueType = Int

  /** The 'empty' or 'zero' or default value for the aggregator
    * 聚合器的“空”或“零”或默认值 */
  override def zero = Array.ofDim[Double](cutoffs.size - 1)

  /**
   * Get the bin index for the column value based on the cutoffs
    * 根据截止值获取列值的bin索引
   *
   * Strict binning is disabled so values smaller than the first bin are assigned to the first bin,
   * and values larger than the last bin are assigned to the last bin.
    * 严格装箱被禁用,因此小于第一个箱的值被分配给第一个箱,大于最后一个箱的值被分配给最后一个箱。
   */
  override def mapFunction(columnValue: Any, columnDataType: DataType): ValueType = {
    if (columnValue != null) {
      DiscretizationFunctions.binElement(DataTypes.toDouble(columnValue),
        cutoffs,
        lowerInclusive = includeLowest.getOrElse(true),
        strictBinning = strictBinning.getOrElse(false))
    }
    else -1
  }

  /**
   * Increment the count for the bin corresponding to the bin index
    * 增加与bin索引对应的bin的计数
   */
  override def add(binArray: AggregateType, binIndex: ValueType): AggregateType = {
    if (binIndex >= 0) binArray(binIndex) += 1
    binArray
  }

  /**
   * Sum two binned lists.
    * 总结两个分箱列表
   */
  override def merge(binArray1: AggregateType, binArray2: AggregateType) = {
    (binArray1, binArray2).zipped.map(_ + _)
  }

  /**
   * Return the vector containing the percentage of observations found in each bin
    * 返回包含在每个bin中找到的观测百分比的向量
   */
  override def getResult(binArray: AggregateType): Any = {
    val total = binArray.sum
    if (total > 0) DataTypes.toVector()(binArray.map(_ / total)) else binArray
  }
}
