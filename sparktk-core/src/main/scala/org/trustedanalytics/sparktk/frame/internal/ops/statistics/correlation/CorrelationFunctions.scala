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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics.correlation

import breeze.numerics._
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.apache.spark.mllib.linalg.{ Matrix }
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow

/**
 * Object for calculating correlation and the correlation matrix
  * 用于计算相关性和相关矩阵的对象
 */

object CorrelationFunctions extends Serializable {

  /**
   * Compute correlation for exactly two columns
   * 计算恰好两列的相关性
   * @param frameRdd input rdd containing all columns
    *                 输入包含所有列的rdd
   * @param dataColumnNames column names for which we calculate the correlation
    *                        我们计算相关性的列名
   * @return correlation wrapped in DoubleValue 关联包裹在DoubleValue中
   */
  def correlation(frameRdd: FrameRdd,
                  dataColumnNames: List[String]): Double = {
    // compute correlation
    //计算相关性
    val correlation: Matrix = Statistics.corr(frameRdd.toDenseVectorRdd(dataColumnNames))

    val dblVal: Double = correlation.toArray(1)

    dblVal
  }

  /**
   * Compute correlation for two or more columns
   * 计算两列或更多列的相关性
   * @param frameRdd input rdd containing all columns 输入包含所有列的rdd
   * @param dataColumnNames column names for which we calculate the correlation matrix
    *                        我们计算相关矩阵的列名
   * @return the correlation matrix in a RDD[Rows]
    *         RDD [行]中的相关矩阵
   */
  def correlationMatrix(frameRdd: FrameRdd,
                        dataColumnNames: List[String]): RDD[Row] = {

    val correlation: Matrix = Statistics.corr(frameRdd.toDenseVectorRdd(dataColumnNames))
    val vecArray = correlation.toArray.grouped(correlation.numCols).toArray
    val arrGenericRow = vecArray.map(row => {
      val temp: Array[Any] = row.map(x => x)
      new GenericRow(temp)
    })

    frameRdd.sparkContext.parallelize(arrGenericRow)
  }
}
