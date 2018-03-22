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
package org.trustedanalytics.sparktk.models.classification.logistic_regression

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.{ Frame, DataTypes, Column, FrameSchema }
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec

class LogisticRegressionModelTest extends TestingSparkContextWordSpec with Matchers {

  // Test training data and schema
  val rows: Array[Row] = Array(new GenericRow(Array[Any](4.9, 1.4, 0)),
    new GenericRow(Array[Any](4.7, 1.3, 0)),
    new GenericRow(Array[Any](4.6, 1.5, 0)),
    new GenericRow(Array[Any](6.3, 4.9, 1)),
    new GenericRow(Array[Any](6.1, 4.7, 1)),
    new GenericRow(Array[Any](6.4, 4.3, 1)),
    new GenericRow(Array[Any](6.6, 4.4, 1)),
    new GenericRow(Array[Any](7.2, 6.0, 2)),
    new GenericRow(Array[Any](7.2, 5.8, 2)),
    new GenericRow(Array[Any](7.4, 6.1, 2)),
    new GenericRow(Array[Any](7.9, 6.4, 2)))
  val schema = new FrameSchema(List(Column("Sepal_Length", DataTypes.float32),
    Column("Petal_Length", DataTypes.float32),
    Column("Class", DataTypes.int32)))
  val obsColumns = List("Sepal_Length", "Petal_Length")
  val labelColumn = "Class"

  "LogisticRegressionModel train" should {
    //训练后返回一个LogisticRegressionModel
    "return a LogisticRegressionModel after training" in {
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, schema)
      val model = LogisticRegressionModel.train(frame, obsColumns, labelColumn, None, 3)

      model shouldBe a[LogisticRegressionModel]
    }
    //抛出无效训练参数的例外
    "throw exceptions for invalid training parameters" in {
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, schema)

      // empty observation column list 空观察列表
      intercept[IllegalArgumentException] {
        LogisticRegressionModel.train(frame, List(), labelColumn)
      }

      // invalid observation column
      intercept[IllegalArgumentException] {
        LogisticRegressionModel.train(frame, List("bogus"), labelColumn)
      }

      // invalid label column 无效标签栏
      intercept[IllegalArgumentException] {
        LogisticRegressionModel.train(frame, obsColumns, "bogus")
      }

      // empty label column 空标签列
      intercept[IllegalArgumentException] {
        LogisticRegressionModel.train(frame, obsColumns, "")
      }

      // invalid optimizer 无效的优化器
      intercept[IllegalArgumentException] {
        LogisticRegressionModel.train(frame, obsColumns, labelColumn, None, 3, "bogus")
      }
    }
  }

  "LogisticRegressionModel score" should {
    //在调用线性回归模型评分时返回预测
    "return predictions when calling the linear regression model score" in {
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, schema)
      val model = LogisticRegressionModel.train(frame, obsColumns, labelColumn, None, 3)

      // Test observation and label data for scoring
      ///测试观察和标签数据的评分
      val inputArray = Array[Any](4.9, 1.4)
      val classLabel = 0

      // Score and check the results 评分并检查结果
      val scoreResult = model.score(inputArray)
      assert(model.input().length == inputArray.length)
      assert(scoreResult.length == model.output().length)
      assert(scoreResult.slice(0, inputArray.length).sameElements(inputArray))
      scoreResult(2) match {
        case prediction: Int => assert(prediction == classLabel)
        case _ => throw new RuntimeException(s"Expected prediction to be a Int but is ${scoreResult(1).getClass.getSimpleName}")
      }
    }
    //为无效的分数参数抛出IllegalArgumentException
    "throw IllegalArgumentException for invalid score parameters" in {
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, schema)
      val model = LogisticRegressionModel.train(frame, obsColumns, labelColumn, None, 3)

      // Null/empty data arrays
      //空/空数据数组
      intercept[IllegalArgumentException] {
        model.score(null)
      }
      intercept[IllegalArgumentException] {
        model.score(Array[Any]())
      }

      // Invalid number of input values
      //输入值的数量无效
      intercept[IllegalArgumentException] {
        model.score(Array[Any](4.9, 1.4, 3.5))
      }

      // Invalid data types for the scoring input
      //评分输入的数据类型无效
      intercept[IllegalArgumentException] {
        model.score(Array[Any](4.9, "bogus"))
      }
    }
  }
}
