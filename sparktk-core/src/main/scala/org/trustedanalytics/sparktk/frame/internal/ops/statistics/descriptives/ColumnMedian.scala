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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics.descriptives

import org.trustedanalytics.sparktk.frame.DataTypes.DataType
import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }

trait ColumnMedianSummarization extends BaseFrame {
  /**
   * Calculate the (weighted) median of a column.
   * 计算一列的(加权)中位数
   * The median is the least value X in the range of the distribution so that the cumulative weight of values strictly
   * below X is strictly less than half of the total weight and the cumulative weight of values up to and including X
   * is greater than or equal to one-half of the total weight.
   *
   * All data elements of weight less than or equal to 0 are excluded from the calculation, as are all data elements
   * whose weight is NaN or infinite.  If a weight column is provided and no weights are finite numbers greater than 0,
   * None is returned.
   *
   * @param dataColumn The column whose median is to be calculated.
   * @param weightsColumn The column that provides weights (frequencies) for the median calculation.
   *                      Must contain numerical data.
   *                      Default is all items have a weight of 1.
   * @return The median of the values.<br>If a weight column is provided and no weights are finite numbers greater
   *         than 0, None is returned. The type of the median returned is the same as the contents of the data column,
   *         so a column of longs will result in a ''long'' median and a column of floats will result in a
   *         ''float'' median.
   */
  def columnMedian(dataColumn: String, weightsColumn: Option[String]): Option[ColumnMedianReturn] = {
    execute(ColumnMedian(dataColumn, weightsColumn))
  }
}

case class ColumnMedian(dataColumn: String, weightsColumn: Option[String]) extends FrameSummarization[Option[ColumnMedianReturn]] {
  require(dataColumn != null, "data column is required")

  override def work(state: FrameState): Option[ColumnMedianReturn] = {
    val columnIndex = state.schema.columnIndex(dataColumn)
    val valueDataType = state.schema.columnDataType(dataColumn)

    // run the operation and return results
    //运行操作并返回结果
    val weightsColumnIndexAndType: Option[(Int, DataType)] = weightsColumn match {
      case None =>
        None
      case Some(weightColumnName) =>
        Some((state.schema.columnIndex(weightsColumn.get), state.schema.columnDataType(weightsColumn.get)))
    }

    val ret = ColumnStatistics.columnMedian(columnIndex, valueDataType, weightsColumnIndexAndType, state.rdd)
    ret match {
      case None => None
      case Some(value) => Some(ColumnMedianReturn(value))
    }
  }
}

// This case class is because py4j can't serialize Any.

/**
 * The median value of the (possibly weighted) column. None when the sum of the weights is 0.
 * (可能是加权的)列的中值值,当权重的总和为0时没有。
 * If no weights are provided, all elements receive a uniform weight of 1.
 * 如果不提供权重,则所有元素的统一权重为1
 * If any element receives a weight that is NaN, infinite or <= 0, that element is thrown
 * out of the calculation.
  * 如果任何元素接收到一个权重,该值为m、无穷或< = 0,则抛出该元素。
 * @param value The median. None if the net weight of the column is 0.
 */
case class ColumnMedianReturn(value: Any)

