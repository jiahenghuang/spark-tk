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
package org.trustedanalytics.sparktk.frame.internal.rdd

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.SparkException
import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.trustedanalytics.sparktk.frame.DataTypes
import org.trustedanalytics.sparktk.frame.internal.RowWrapper

import scala.beans.BeanInfo
import scala.xml.NodeSeq

class RowWrapperFunctions(self: RowWrapper) {

  /**
   * Convert Row into MLlib Dense Vector
    * 将行转换为MLlib密集向量
   */
  def valuesAsDenseVector(columnNames: Seq[String]): DenseVector = {
    val array = self.valuesAsDoubleArray(columnNames)
    new DenseVector(array)
  }

  /**
   * Convert Row into Breeze Dense Vector
    * 将行转换为Breeze密集向量
   */
  def valuesAsBreezeDenseVector(columnNames: Seq[String]): BDV[Double] = {
    val array = self.valuesAsDoubleArray(columnNames)
    new BDV[Double](array)
  }

  /**
   *  Convert Row into NodeSeq (i.e., Seq[Node])
   * 将行转换成NodeSeq（即Seq [Node]）
   * @param columnName Column name in frame holding xml string
    *                   在保存xml字符串的框架中的列名称
   * @param nodeName Name of the node to extract from column holding xml string
    *                 从包含xml字符串的列中提取的节点的名称
   * @return NodeSeq (i.e Seq[Node])
   */
  def valueAsXmlNodeSeq(columnName: String, nodeName: String): NodeSeq = {
    self.xmlNodeSeqValue(columnName, nodeName)
  }

  /**
   * Convert Row into LabeledPoint format required by MLLib
    * 将行转换为MLLib所需的LabeledPoint格式
   */
  def valuesAsLabeledPoint(featureColumnNames: Seq[String], labelColumnName: String): LabeledPoint = {
    val label = DataTypes.toDouble(self.value(labelColumnName))
    val vector = valuesAsDenseVector(featureColumnNames)
    new LabeledPoint(label, vector)
  }

  /**
   * Convert Row into LabeledPointWithFrequency format required for updates in MLLib code
    * 将行转换为MLLib代码更新所需的LabeledPointWithFrequency格式
   */
  def valuesAsLabeledPointWithFrequency(labelColumnName: String,
                                        featureColumnNames: Seq[String],
                                        frequencyColumnName: Option[String]): LabeledPointWithFrequency = {
    val label = DataTypes.toDouble(self.value(labelColumnName))
    val vector = valuesAsDenseVector(featureColumnNames)

    val frequency = frequencyColumnName match {
      case Some(freqColumn) => DataTypes.toDouble(self.value(freqColumn))
      case _ => 1d
    }
    new LabeledPointWithFrequency(label, vector, frequency)
  }
}
/**
 * Class that represents the features and labels of a data point.
  * 表示数据点的特征和标签的类
 *
 * Extension of MlLib's labeled points that supports a frequency column.
  * 支持频率列的MlLib标记点的扩展
 * The frequency column contains the frequency of occurrence of each observation.
  * 频率栏包含每个观察的发生频率
 *
 * @see org.apache.spark.mllib.regression.LabeledPoint
 * @param label Label for this data point.
 * @param features List of features for this data point.
 */
@BeanInfo
case class LabeledPointWithFrequency(label: Double, features: Vector, frequency: Double) {
  override def toString: String = {
    s"($label,$features,$frequency)"
  }
}

/**
 * Parser for [[LabeledPointWithFrequency]].
 */
object LabeledPointWithFrequency {
  /**
   * Parses a string resulted from `LabeledPointWithFrequency#toString` into
   * an [[LabeledPointWithFrequency]].
   *//*
  def parse(s: String): LabeledPointWithFrequency = {
    if (s.startsWith("(")) {
      NumericParser.parse(s) match {
        case Seq(label: Double, numeric: Any, frequency: Double) =>
          LabeledPointWithFrequency(label, parseNumeric(numeric), frequency)
        case other =>
          throw new SparkException(s"Cannot parse $other.")
      }
    }
    else { // dense format used before v1.0
      val parts = s.split(',')
      val label = java.lang.Double.parseDouble(parts(0))
      val features = Vectors.dense(parts(1).trim().split(' ').map(java.lang.Double.parseDouble))
      val frequency = java.lang.Double.parseDouble(parts(2))
      LabeledPointWithFrequency(label, features, frequency)
    }
  }*/
}
