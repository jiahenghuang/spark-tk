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
package org.trustedanalytics.sparktk.frame.internal.ops.statistics.covariance

import breeze.numerics.abs
import org.trustedanalytics.sparktk.frame.DataTypes
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.apache.spark.mllib.linalg.{ Matrix }
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow

/**
 * Object for calculating covariance and the covariance matrix
  * 用于计算协方差和协方差矩阵的对象
 */

object CovarianceFunctions extends Serializable {

  /**
   * Compute covariance for exactly two columns
    * 计算恰好两列的协方差
   *
   * @param frameRdd input rdd containing all columns 输入包含所有列的rdd
   * @param dataColumnNames column names for which we calculate the covariance
    *                        我们计算协方差的列名
   * @return covariance wrapped in CovarianceReturn
    *         协方差包裹在CovarianceReturn中
   */
  def covariance(frameRdd: FrameRdd,
                 dataColumnNames: List[String]): Double = {

    // compute and return covariance
    //计算并返回协方差
    def rowMatrix: RowMatrix = new RowMatrix(frameRdd.toDenseVectorRdd(dataColumnNames))

    val covariance: Matrix = rowMatrix.computeCovariance()

    val dblVal: Double = covariance.toArray(1)

    dblVal
  }

  /**
   * Compute covariance for two or more columns
   * 计算两列或更多列的协方差
   * @param frameRdd input rdd containing all columns 输入包含所有列的rdd
   * @param dataColumnNames column names for which we calculate the covariance matrix 我们计算协方差矩阵的列名
   * @param outputVectorLength If specified, output results as a column of type 'vector(vectorOutputLength)'
    *                           如果指定，则输出结果作为类型为“vector（vectorOutputLength）”的列
   * @return the covariance matrix in a RDD[Rows] RDD中的协方差矩阵[Rows]
   */
  def covarianceMatrix(frameRdd: FrameRdd,
                       dataColumnNames: List[String],
                       outputVectorLength: Option[Long] = None): RDD[Row] = {

    def rowMatrix: RowMatrix = new RowMatrix(frameRdd.toDenseVectorRdd(dataColumnNames))

    val covariance: Matrix = rowMatrix.computeCovariance()
    val vecArray = covariance.toArray.grouped(covariance.numCols).toArray
    val formatter: Array[Any] => Array[Any] = outputVectorLength match {
      case Some(length) =>
        val vectorizer = DataTypes.toVector(length)_
        x => Array(vectorizer(x))
        case _ => identity
    }

    val arrGenericRow = vecArray.map(row => {
      val formattedRow: Array[Any] = formatter(row.map(x => x))
      new GenericRow(formattedRow)
    })

    frameRdd.sparkContext.parallelize(arrGenericRow)
  }
}
