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

import org.trustedanalytics.sparktk.frame.DataTypes
import org.trustedanalytics.sparktk.frame.DataTypes.DataType
/**
 * Counter used to compute the arithmetic mean incrementally.
  * 计数器用于逐渐计算算术平均值
 */
case class MeanCounter(count: Long, sum: Double) {
  require(count >= 0, "Count should be greater than zero")
}

/**
 *  Aggregator for incrementally computing the mean column value using Spark's aggregateByKey()
 * 使用Spark的aggregateByKey（）增量计算平均列值的聚合器
 *  @see org.apache.spark.rdd.PairRDDFunctions#aggregateByKey
 */
case class MeanAggregator() extends GroupByAggregator {

  /** Type for aggregate values that corresponds to type U in Spark's aggregateByKey()
    * 在Spark的aggregateByKey（）中键入对应于类型U的聚合值*/
  override type AggregateType = MeanCounter

  /** Output type of the map function that corresponds to type V in Spark's aggregateByKey() */
  override type ValueType = Double

  /** The 'empty' or 'zero' or default value for the aggregator */
  override def zero: MeanCounter = MeanCounter(0L, 0d)

  /**
   * Converts column value to Double
   */
  override def mapFunction(columnValue: Any, columnDataType: DataType): ValueType = {
    if (columnValue != null)
      DataTypes.toDouble(columnValue)
    else
      Double.NaN
  }

  /**
   * Adds map value to incremental mean counter
    * 将map值添加到增量平均计数器
   */
  override def add(mean: AggregateType, mapValue: ValueType): AggregateType = {
    if (mapValue.isNaN) { // omit value from calculation
      //TODO: Log to IAT EventContext once we figure out how to pass it to Spark workers
      println(s"WARN: Omitting NaNs from mean calculation in group-by")
      mean
    }
    else {
      val sum = mean.sum + mapValue
      val count = mean.count + 1L
      MeanCounter(count, sum)
    }
  }

  /**
   * Merge two mean counters
    * 合并两个平均计数器
   */
  override def merge(mean1: AggregateType, mean2: AggregateType) = {
    val count = mean1.count + mean2.count
    val sum = mean1.sum + mean2.sum
    MeanCounter(count, sum)
  }
  override def getResult(mean: AggregateType): Any = if (mean.count > 0) {
    mean.sum / mean.count
  }
  else {
    null //TODO: Re-visit when data types support Inf and NaN
  }

}
