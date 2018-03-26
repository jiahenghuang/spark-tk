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
package org.trustedanalytics.sparktk.frame

import org.apache.spark.SparkContext
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ DataFrame, Row }
import org.json4s.JsonAST.JValue
import org.trustedanalytics.sparktk.frame.internal.ops.matrix._
import org.trustedanalytics.sparktk.frame.internal.{ BaseFrame, ValidationReport }
import org.trustedanalytics.sparktk.frame.internal.ops._
import org.trustedanalytics.sparktk.frame.internal.ops.binning.{ BinColumnTransformWithResult, HistogramSummarization, QuantileBinColumnTransformWithResult }
import org.trustedanalytics.sparktk.frame.internal.ops.join.{ JoinCrossSummarization, JoinInnerSummarization, JoinLeftSummarization, JoinOuterSummarization, JoinRightSummarization }
import org.trustedanalytics.sparktk.frame.internal.ops.sample.AssignSampleTransform
import org.trustedanalytics.sparktk.frame.internal.ops.exportdata._
import org.trustedanalytics.sparktk.frame.internal.ops.flatten.FlattenColumnsTransform
import org.trustedanalytics.sparktk.frame.internal.ops.groupby.GroupBySummarization
import org.trustedanalytics.sparktk.frame.internal.ops.RenameColumnsTransform
import org.trustedanalytics.sparktk.frame.internal.ops.sortedk.SortedKSummarization
import org.trustedanalytics.sparktk.frame.internal.ops.statistics.correlation.{ CorrelationMatrixSummarization, CorrelationSummarization }
import org.trustedanalytics.sparktk.frame.internal.ops.statistics.covariance.{ CovarianceMatrixSummarization, CovarianceSummarization }
import org.trustedanalytics.sparktk.frame.internal.ops.statistics.descriptives.{ CategoricalSummarySummarization, ColumnMedianSummarization, ColumnModeSummarization, ColumnSummaryStatisticsSummarization }
import org.trustedanalytics.sparktk.frame.internal.ops.statistics.quantiles.QuantilesSummarization
import org.trustedanalytics.sparktk.frame.internal.ops.topk.TopKSummarization
import org.trustedanalytics.sparktk.frame.internal.ops.unflatten.UnflattenColumnsTransform
import org.trustedanalytics.sparktk.frame.internal.rdd.{ FrameRdd, PythonJavaRdd }


class Frame(frameRdd: RDD[Row], frameSchema: Schema, validateSchema: Boolean = false) extends BaseFrame with Serializable
  //params named "frameRdd" and "frameSchema" because naming them "rdd" and "schema" masks the base members "rdd" and "schema" in this scope
  //params命名为“frameRdd”和“frameSchema”,因为将它们命名为“rdd”和“schema”掩盖了此范围内的基础成员“rdd”和“schema”
    with AddColumnsTransform
    with AppendFrameTransform
    //with AssignSampleTransform
    with BinColumnTransformWithResult
    with BoxCoxTransform
    //with CategoricalSummarySummarization
    with CollectSummarization
    //with ColumnMedianSummarization
   // with ColumnModeSummarization
    //with ColumnSummaryStatisticsSummarization
    with CopySummarization
    //with CorrelationMatrixSummarization
   // with CorrelationSummarization
    with CountSummarization
   // with CovarianceMatrixSummarization
   // with CovarianceSummarization 协方差
   // with DotProductTransform  乘积
    with DropColumnsTransform //从scheam中删除列
    with DropDuplicatesTransform //删除重复的列
    with DropRowsTransform   //删除重复的行
   // with EntropySummarization //
    with ExportToCsvSummarization //导出数据
    with ExportToHbaseSummarization
    with ExportToHiveSummarization
    with ExportToJdbcSummarization
    with ExportToJsonSummarization
    with FilterTransform //过滤数据
    with FlattenColumnsTransform
    with GroupBySummarization //分组数据
   // with HistogramSummarization  计数列的直方图
    with JoinCrossSummarization //笛卡尔连接
    with JoinInnerSummarization //内连接
    with JoinLeftSummarization //左链接
    with JoinOuterSummarization //外连接
    with JoinRightSummarization //右连接
    //with MatrixCovarianceMatrixTransform  协方差矩阵
   // with MatrixPcaTransform
    //with MatrixSvdTransform
  //  with QuantilesSummarization
   // with QuantileBinColumnTransformWithResult
    with RenameColumnsTransform //重命名列
    with ReverseBoxCoxTransform //列的反转
    with RowCountSummarization //frame中的行数
    with SaveSummarization //保存当前frame parquet
    with SortTransform  // 按一列或多列排序
    //with SortedKSummarization //
    //with TakeSummarization
    //with TopKSummarization
    with UnflattenColumnsTransform {

  val validationReport = init(frameRdd, frameSchema, validateSchema)

  def this(frameRdd: FrameRdd, validateSchema: Boolean) = {
    this(frameRdd.rdd, frameRdd.schema, validateSchema)
  }

  /**
   * Initialize the frame and call schema validation, if it's enabled.
   * 初始化框架并调用模式验证(如果已启用)
   * @param frameRdd RDD
   * @param frameSchema Schema
   * @param validateSchema Boolean indicating if schema validation should be performed.
    *                       布尔值,指示是否执行模式验证
   * @return ValidationReport, if the data is validated against the schema.
    *         ValidationReport,如果数据对模式进行了验证。
   */
  def init(frameRdd: RDD[Row], frameSchema: Schema, validateSchema: Boolean): Option[ValidationReport] = {
    var validationReport: Option[ValidationReport] = None

    // Infer the schema, if a schema was not provided
    //如果没有提供模式,使用推断模式
    val updatedSchema = if (frameSchema == null) {
      //使用推断模式
      SchemaHelper.inferSchema(frameRdd)
    }
    else
      frameSchema

    // Validate the data against the schema, if the validateSchema is enabled
    //如果启用了validateSchema,则根据模式验证数据
    val updatedRdd = if (validateSchema) {
      //据模式验证数据
      val schemaValidation = super.validateSchema(frameRdd, updatedSchema)
      if (schemaValidation.validationReport.numBadValues > 0)
        logger.warn(s"Schema validation found ${schemaValidation.validationReport.numBadValues} bad values.")
      validationReport = Some(schemaValidation.validationReport)
      schemaValidation.validatedRdd
    }
    else
      frameRdd

    super.init(updatedRdd, updatedSchema)

    validationReport
  }

  /**
   * (typically called from pyspark, with jrdd)
    * (通常从pyspark调用,使用jrdd)
   *
   * @param jrdd java array of Any
   * @param schema frame schema
   */
  def this(jrdd: JavaRDD[Array[Any]], schema: Schema) = {
    this(PythonJavaRdd.toRowRdd(jrdd.rdd, schema), schema)
  }

  private[frame] def this(sparktkFrameRdd: FrameRdd) = {
    this(sparktkFrameRdd, sparktkFrameRdd.schema)
  }

  /**
   * Construct a spark-tk Frame from a Spark DataFrame
    * 从Spark DataFrame构建一个spark-tk框架
   */
  def this(df: DataFrame) = {
    this(FrameRdd.toFrameRdd(df))
  }
}


