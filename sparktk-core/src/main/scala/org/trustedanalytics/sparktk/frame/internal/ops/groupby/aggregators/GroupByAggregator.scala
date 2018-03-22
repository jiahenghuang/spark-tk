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

/**
 *  Trait for group-by aggregators
 *
 *  This trait defines methods needed to implement aggregators, e.g. counts and sums.
  *  这个特性定义了实现聚合器所需的方法,例如 数量和总和。
 *
 *  This implementation uses Spark's aggregateByKey(). aggregateByKey() aggregates the values of each key using
 *  an initial "zero value", an operation which merges an input value V into an aggregate value U,
 *  and an operation for merging two U's.
 *
 *  @see org.apache.spark.rdd.PairRDDFunctions#aggregateByKey
 */
trait GroupByAggregator extends Serializable {

  /**
   * The type that represents aggregate results. For example, Double for mean, and Long for distinct count values.
    * 表示聚合结果的类型。 例如:Double表示平均值,Long表示不同的计数值
   */
  type AggregateType

  /**
   * A type that represents the value to be aggregated. For example, 'ones' for counts, or column values for sums
    * 表示要汇总的值的类型。例如:用于计数的“ones”或用于总和的列值
   */
  type ValueType

  /**
   * The 'empty' or 'zero' or initial value for the aggregator
    * 聚合器的“空”或“零”或初始值
   */
  def zero: AggregateType

  /**
   * Map function that transforms column values in each row into the input expected by the aggregator
    * Map函数将每行中的列值转换为聚合器预期的输入
   *
   * For example, for counts, the map function would output 'one' for each map value.
    * 例如,对于计数，地图功能将为每个地图值输出“1”
   *
   * @param columnValue Column value
   * @param columnDataType Column data type
   * @return Input value for aggregator
   */
  def mapFunction(columnValue: Any, columnDataType: DataType): ValueType

  /**
   * Adds the output of the map function to the aggregate value
    * 将map函数的输出添加到聚合值中
   *
   * This function increments the aggregated value within a single Spark partition.
    * 此函数在单个Spark分区内增加聚合值
   *
   * @param aggregateValue Current aggregate value
   * @param mapValue Input value
   * @return Updated aggregate value
   */
  def add(aggregateValue: AggregateType, mapValue: ValueType): AggregateType

  /**
   * Combines two aggregate values
    * 组合两个聚合值
   *
   * This function combines aggregate values across Spark partitions.
    * 这个函数结合了整个Spark分区的聚合值
   * For example, adding two counts
   *
   * @param aggregateValue1 First aggregate value
   * @param aggregateValue2 Second aggregate value
   * @return Combined aggregate value
   */
  def merge(aggregateValue1: AggregateType, aggregateValue2: AggregateType): AggregateType

  /**
   * Returns the results of the aggregator
    * 返回聚合器的结果
   *
   * For some aggregators, this involves casting the result to a Scala Any type.
   * Other aggregators output an intermediate value, so this method computes the
   * final result. For example, the arithmetic mean is represented as a count and sum,
   * so this method computes the mean by dividing the count by the sum.
   *
   * @param result Results of aggregator (might be an intermediate value)
   * @return Final result
   */
  def getResult(result: AggregateType): Any = result

}
