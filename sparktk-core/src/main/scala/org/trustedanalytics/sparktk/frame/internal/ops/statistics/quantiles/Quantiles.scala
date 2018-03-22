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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics.quantiles

import org.trustedanalytics.sparktk.frame.internal.{ FrameState, FrameSummarization, BaseFrame }
import org.trustedanalytics.sparktk.frame.{ Column, FrameSchema, DataTypes, Frame }

trait QuantilesSummarization extends BaseFrame {
  /**
   * Calculate quantiles on the given column.
   * 计算给定列的分位数
   * @param column The name of the column to calculate quantiles off of.
    *               计算分位数列的名称
   * @param quantiles The quantile cutoffs being requested
    *                  分位数截止点被请求
   * @return A new frame with two columns (''float64''): requested quantiles and their respective values.
    *         具有两列（“float64”）的新框架：请求分位数及其各自的值。
   */
  def quantiles(column: String,
                quantiles: List[Double]): Frame = {

    execute(Quantiles(column, quantiles))
  }
}

case class Quantiles(column: String, quantiles: List[Double]) extends FrameSummarization[Frame] {
  require(quantiles.forall(x => x > 0.0d), "Quantile cutoffs must be positive")
  require(quantiles.forall(x => x <= 100.0d), "Quantile cutoffs must be less than equal to 100")

  override def work(state: FrameState): Frame = {
    val columnIndex = state.schema.columnIndex(column)

    // New schema for the quantiles frame
    //分位数框架的新模式
    val schema = FrameSchema(Vector(Column("Quantiles", DataTypes.float64), Column(column + "_QuantileValue", DataTypes.float64)))

    // return frame with quantile values
    //返回分位数值的frame
    new Frame(QuantilesFunctions.quantiles(state.rdd, quantiles, columnIndex, state.rdd.count()), schema)
  }
}

/**
 * Quantile composing element which contains element's index and its weight
  * 包含元素索引及其权重的分位数组合元素
 * @param index element index
 * @param quantileTarget the quantile target that the element can be applied to
 */
case class QuantileComposingElement(index: Long, quantileTarget: QuantileTarget)

