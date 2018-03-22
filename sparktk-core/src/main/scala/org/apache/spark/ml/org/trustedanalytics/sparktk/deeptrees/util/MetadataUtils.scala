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
package org.apache.spark.ml.org.trustedanalytics.sparktk.deeptrees.util

import org.apache.spark.ml.attribute._
import org.apache.spark.mllib.linalg.VectorUDT
import org.apache.spark.sql.types.StructField

import scala.collection.immutable.HashMap

/**
 * Helper utilities for algorithms using ML metadata
  * 用于使用ML元数据的算法的辅助工具
 */
private[spark] object MetadataUtils {

  /**
   * Examine a schema to identify the number of classes in a label column.
    * 检查模式以确定标签列中的类的数量
   * Returns None if the number of labels is not specified, or if the label column is continuous.
    * 如果没有指定标签数量,或者标签列是连续的,则返回None
   */
  def getNumClasses(labelSchema: StructField): Option[Int] = {
    Attribute.fromStructField(labelSchema) match {
      case binAttr: BinaryAttribute => Some(2)
      case nomAttr: NominalAttribute => nomAttr.getNumValues
      case _: NumericAttribute | UnresolvedAttribute => None
    }
  }

  /**
   * Examine a schema to identify categorical (Binary and Nominal) features.
    * 检查模式以识别分类(二进制和名义)功能
   *
   * @param featuresSchema  Schema of the features column.功能列的架构
   *                        If a feature does not have metadata, it is assumed to be continuous.
    *                        如果某个功能没有元数据,则认为它是连续的
   *                        If a feature is Nominal, then it must have the number of values
   *                        specified.
    *                        如果某个功能是名义上的,那么它必须具有指定的数量
   * @return  Map: feature index to number of categories.
   *          The map's set of keys will be the set of categorical feature indices.
   */
  def getCategoricalFeatures(featuresSchema: StructField): Map[Int, Int] = {
    val metadata = AttributeGroup.fromStructField(featuresSchema)
    if (metadata.attributes.isEmpty) {
      HashMap.empty[Int, Int]
    }
    else {
      metadata.attributes.get.zipWithIndex.flatMap {
        case (attr, idx) =>
          if (attr == null) {
            Iterator()
          }
          else {
            attr match {
              case _: NumericAttribute | UnresolvedAttribute => Iterator()
              case binAttr: BinaryAttribute => Iterator(idx -> 2)
              case nomAttr: NominalAttribute =>
                nomAttr.getNumValues match {
                  case Some(numValues: Int) => Iterator(idx -> numValues)
                  case None => throw new IllegalArgumentException(s"Feature $idx is marked as" +
                    " Nominal (categorical), but it does not have the number of values specified.")
                }
            }
          }
      }.toMap
    }
  }

  /**
   * Takes a Vector column and a list of feature names, and returns the corresponding list of
   * feature indices in the column, in order.
    * 获取“矢量”列和要素名称列表,并按顺序返回列中的相应列表“获取矢量”列和要素索引列表。
   * @param col  Vector column which must have feature names specified via attributes
    *             矢量列必须具有通过属性指定的特征名称
   * @param names  List of feature names 特征名称列表
   */
  def getFeatureIndicesFromNames(col: StructField, names: Array[String]): Array[Int] = {
    require(col.dataType.isInstanceOf[VectorUDT], s"getFeatureIndicesFromNames expected column $col"
      + s" to be Vector type, but it was type ${col.dataType} instead.")
    val inputAttr = AttributeGroup.fromStructField(col)
    names.map { name =>
      require(inputAttr.hasAttr(name),
        s"getFeatureIndicesFromNames found no feature with name $name in column $col.")
      inputAttr.getAttr(name).index.get
    }
  }
}
