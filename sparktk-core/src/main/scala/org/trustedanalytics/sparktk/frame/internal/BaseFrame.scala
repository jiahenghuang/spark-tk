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
package org.trustedanalytics.sparktk.frame.internal

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ SQLContext, DataFrame, Row }

import org.slf4j.LoggerFactory
import org.trustedanalytics.sparktk.frame.Schema
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

import scala.util.{ Failure, Success }

trait BaseFrame {

  private var frameState: FrameState = null

  lazy val logger = LoggerFactory.getLogger("sparktk")

  /**
   * The content of the frame as an RDD of Rows.
    * 该frame的内容作为行的RDD
   */
  def rdd: RDD[Row] = if (frameState != null) frameState.rdd else null

  /**
   * Current frame column names and types.
    * 当前frame列名称和类型
   */
  def schema: Schema = if (frameState != null) frameState.schema else null

  /**
   * The content of the frame as a Spark DataFrame
    * 框架的内容为Spark DataFrame
   */
  def dataframe: DataFrame = if (frameState != null) {
    val frameRdd = new FrameRdd(schema, rdd)
    frameRdd.toDataFrame
  }
  else null

  /**
   * Validates the data against the specified schema. Attempts to parse the data to the column's data type.  If
   * it's unable to parse the data to the specified data type, it's replaced with null.
    *
   * 根据指定的模式验证数据,尝试将数据解析为列的数据类型,如果无法将数据解析为指定的数据类型,则将其替换为null。
    *
   * @param rddToValidate RDD of data to validate against the specified schema
    *                      数据的RDD根据指定的模式进行验证
   * @param schemaToValidate Schema to use to validate the data 架构来验证数据
   * @return RDD that has data parsed to the schema's data types 将数据解析为模式数据类型的RDD
   */
  protected def validateSchema(rddToValidate: RDD[Row], schemaToValidate: Schema): SchemaValidationReturn = {
    val columnCount = schemaToValidate.columns.length
    val schemaWithIndex = schemaToValidate.columns.zipWithIndex

    val badValueCount = rddToValidate.sparkContext.accumulator(0, "Frame bad values")

    val validatedRdd = rddToValidate.map(row => {
      if (row.length != columnCount)
        throw new RuntimeException(s"Row length of ${row.length} does not match the number of columns in the schema (${columnCount}).")

      val parsedValues = schemaWithIndex.map {
        case (column, index) =>
          column.dataType.parse(row.get(index)) match {
            case Success(value) => value
            case Failure(e) =>
              badValueCount += 1
              null
          }
      }

      Row.fromSeq(parsedValues)
    })

    // Call count() to force rdd map to execute so that we can get the badValueCount from the accumulator.
    //调用count()来强制执行rdd映射,以便我们可以从accumulator中获取badValueCount
    validatedRdd.count()

    SchemaValidationReturn(validatedRdd, ValidationReport(badValueCount.value))
  }

  private[sparktk] def init(rdd: RDD[Row], schema: Schema): Unit = {
    frameState = FrameState(rdd, schema)
  }

  protected def execute(transform: FrameTransform): Unit = {
    logger.info("Frame transform {}", transform.getClass.getName)
    frameState = transform.work(frameState)
  }

  protected def execute[T](summarization: FrameSummarization[T]): T = {
    logger.info("Frame summarization {}", summarization.getClass.getName)
    summarization.work(frameState)
  }

  protected def execute[T](transform: FrameTransformWithResult[T]): T = {
    logger.info("Frame transform (with result) {}", transform.getClass.getName)
    val r = transform.work(frameState)
    frameState = r.state
    r.result
  }
}

/**
 * Validation report for schema and rdd validation.
  * 模式和rdd验证的验证报告
 *
 * @param numBadValues The number of values that were unable to be parsed to the column's data type.
  *                     无法解析为列的数据类型的值的数量
 */
case class ValidationReport(numBadValues: Int)

/**
 * Value to return from the function that validates the data against schema.
 * 从验证数据的模式返回的值返回值
 * @param validatedRdd RDD of data has been casted to the data types specified by the schema.
  *                     数据的RDD已经被转换成模式指定的数据类型
 * @param validationReport Validation report specifying how many values were unable to be parsed to the column's
 *                         data type.
  *                         验证报告指定有多少个值无法被解析为列的数据类型
 */
case class SchemaValidationReturn(validatedRdd: RDD[Row], validationReport: ValidationReport)

trait FrameOperation extends Product {
  //def name: String
}

trait FrameTransform extends FrameOperation {
  def work(state: FrameState): FrameState
}

case class FrameTransformReturn[T](state: FrameState, result: T)

trait FrameTransformWithResult[T] extends FrameOperation {
  def work(state: FrameState): FrameTransformReturn[T]
}

trait FrameSummarization[T] extends FrameOperation {
  def work(state: FrameState): T
}

trait FrameCreation extends FrameOperation {
  def work(): FrameState
}
