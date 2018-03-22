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
package org.trustedanalytics.sparktk.models

import org.apache.spark.ml.attribute.{ AttributeGroup, NumericAttribute, NominalAttribute }
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.DataFrame
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

class FrameFunctions(self: FrameRdd) extends Serializable {

  /**
   * Convert FrameRdd to Spark dataframe with feature vector and label
   * 使用特征向量和标签将FrameRdd转换为Spark dataframe
   * @param observationColumns List of observation column names 名称列表
   * @param labelColumn Label column name 标签列名称
   * @param outputFeatureVectorName Column name of output feature vector 输出特征向量的列名
   * @param categoricalFeatures Optional arity of categorical features. Entry (name -> k) indicates that
   *                            feature 'name' is categorical with 'k' categories indexed from 0:{0,1,...,k-1}
    *                            可选的分类特征,条目(名称 - > k)表示特征“名称”是分类的,“k”类别从0开始索引：{0,1，...，k-1}
   * @param labelNumClasses Optional number of categories in label column. If None, label column is continuous.
    *                        标签栏中的可选数量的类别,如果没有,标签列是连续的
   * @return Dataframe with feature vector and label
   */
  def toLabeledDataFrame(observationColumns: List[String],
                         labelColumn: String,
                         outputFeatureVectorName: String,
                         categoricalFeatures: Option[Map[String, Int]] = None,
                         labelNumClasses: Option[Int] = None): DataFrame = {
    require(labelColumn != null, "label column name must not be null")
    require(observationColumns != null, "feature column names must not be null")
    require(outputFeatureVectorName != null, "output feature vector name must not be null")
    require(labelNumClasses.isEmpty || labelNumClasses.get >= 2,
      "number of categories in label column must be greater than 1")

    val assembler = new VectorAssembler().setInputCols(observationColumns.toArray)
      .setOutputCol(outputFeatureVectorName)
    //获得转换所有列
    val featureFrame = assembler.transform(self.toDataFrame)
    /**
      * +---+------+------+------------+----------------+
        |age|weight|gender|has_diabetes|        features|
        +---+------+------+------------+----------------+
        | 20| 130.0|     1|           0|[20.0,130.0,1.0]|
        +---+------+------+------------+----------------+
      */
    featureFrame.show()
    // Identify categorical and numerical features
    //确定分类和数字特征
    val categoricalFeaturesMap = categoricalFeatures.getOrElse(Map.empty[String, Int])
    //indices返回所有有效索引值
    val featuresAttributes = observationColumns.indices.map { featureIndex =>
      //取出列名
      val featureName = observationColumns(featureIndex)
      //判断是否包含分类特征列
      if (categoricalFeaturesMap.contains(featureName)) {
        //NominalAttribute每个值代表某种类别、编码或状态，因此标称属性又被看做是分类的(categorical)
        //==categoricalFeaturesMap==2
        println("==categoricalFeaturesMap=="+categoricalFeaturesMap(featureName))
        NominalAttribute.defaultAttr.withIndex(featureIndex)
          //设置值,可选的分类特征,k”类别从0开始索引：{0,1，...，k-1}
          .withNumValues(categoricalFeaturesMap(featureName))
      }
      else {
        NumericAttribute.defaultAttr.withIndex(featureIndex)
      }
    }.toArray
    //=featuresAttributes==>{"type":"numeric","idx":0},{"type":"numeric","idx":1},{"type":"nominal","num_vals":2,"idx":2}
    println("=featuresAttributes==>"+featuresAttributes.mkString(","))
    val labelAttribute = if (labelNumClasses.isEmpty) {
      NumericAttribute.defaultAttr.withName(labelColumn)
    }
    else {
      NominalAttribute.defaultAttr.withName(labelColumn).withNumValues(labelNumClasses.get)
    }
    // Update frame metadata with categorical and numerical features
    //使用分类和数字特征更新框架元数据
    val featuresMetadata = new AttributeGroup(outputFeatureVectorName, featuresAttributes).toMetadata()
    val labelMetadata = labelAttribute.toMetadata()
    //{"ml_attr":{"attrs":{"nominal":[{"num_vals":2,"idx":2}]},"num_attrs":3}}
    println("AttributeGroup==>"+featuresMetadata)
    //NumericAttribute===>{"type":"nominal","num_vals":2,"name":"has_diabetes"}==={"ml_attr":{"type":"nominal","num_vals":2,"name":"has_diabetes"}}
    println("NumericAttribute===>"+labelAttribute+"==="+labelMetadata)
    //features===>{"ml_attr":{"attrs":{"nominal":[{"num_vals":2,"idx":2}]},"num_attrs":3}}====>features AS features#7
    //println(outputFeatureVectorName+"===>"+featuresMetadata+"====>"+featureFrame(outputFeatureVectorName).as(outputFeatureVectorName, featuresMetadata))
    //val tea=featureFrame(outputFeatureVectorName).as(outputFeatureVectorName, featuresMetadata)
/*    featureFrame.select(featureFrame(outputFeatureVectorName).as(outputFeatureVectorName, featuresMetadata),
      featureFrame(labelColumn).as(labelColumn, labelMetadata)).show()*/
    /**
       +----------------+------------+
      |        features|has_diabetes|
      +----------------+------------+
      |[20.0,130.0,1.0]|           0|
      +----------------+------------+
      */
    featureFrame.select(featureFrame(outputFeatureVectorName).as(outputFeatureVectorName, featuresMetadata),
      featureFrame(labelColumn).as(labelColumn, labelMetadata))
  }
}

object FrameImplicits {
  implicit def frameRddToFrameFunctions(frameRdd: FrameRdd) = new FrameFunctions(frameRdd)
}