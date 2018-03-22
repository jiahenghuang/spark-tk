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
package org.trustedanalytics.sparktk.models.recommendation.collaborative_filtering

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.{ Column, DataTypes, Frame, FrameSchema }
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec

class CollaborativeFilteringModelTest extends TestingSparkContextWordSpec with Matchers {

  val schema = FrameSchema(List(
    Column("source", DataTypes.int32),
    Column("dest", DataTypes.int32),
    Column("weight", DataTypes.float64)
  ))

  val data: List[Row] = List(
    new GenericRow(Array[Any](1, 3, .5)),
    new GenericRow(Array[Any](1, 4, .6)),
    new GenericRow(Array[Any](1, 5, .7)),
    new GenericRow(Array[Any](2, 5, .1))
  )

  "CollaborativeFiltering recommend" should {
    //抛出一个无效用户标识的异常
    "throw an exception for an invalid user id" in {
      val rdd = sparkContext.parallelize(data)
      val frame = new Frame(rdd, schema)

      // train model
      val trainedModel = CollaborativeFilteringModel.train(frame, "source", "dest", "weight")

      // user id that didn't exist in training
      //在训练中不存在的用户标识
      val userId = 3

      val ex = intercept[IllegalArgumentException] {
        trainedModel.recommend(userId, numberOfRecommendations = 1, recommendProducts = true)
      }
      assert(ex.getMessage.contains(s"requirement failed: No users found with id = ${userId}."))
    }
    //抛出一个无效产品ID的异常
    "throw an exception for an invalid product id" in {
      val rdd = sparkContext.parallelize(data)
      val frame = new Frame(rdd, schema)

      // train model
      val trainedModel = CollaborativeFilteringModel.train(frame, "source", "dest", "weight")

      // product id that didn't exist in in training
      val productId = 7

      val ex = intercept[IllegalArgumentException] {
        trainedModel.recommend(productId, numberOfRecommendations = 1, recommendProducts = false)
      }
      assert(ex.getMessage.contains(s"requirement failed: No products found with id = ${productId}."))
    }
    //抛出一个无效数量的建议例外
    "throw an exception for an invalid number of recommendations" in {
      val rdd = sparkContext.parallelize(data)
      val frame = new Frame(rdd, schema)

      // train model
      val trainedModel = CollaborativeFilteringModel.train(frame, "source", "dest", "weight")

      // Recommend requesting a negative number of recommendations
      //建议请求负数的建议
      val ex = intercept[IllegalArgumentException] {
        trainedModel.recommend(1, numberOfRecommendations = -1, recommendProducts = true)
      }
      assert(ex.getMessage.contains(s"requirement failed: numberOfRecommendations number be greater than 0."))
    }
  }

  "CollaborativeFiltering scoring" should {
    //预测用户对产品的评分
    "predict rating of a user for a product" in {
      val rdd = sparkContext.parallelize(data)
      val frame = new Frame(rdd, schema)

      // train model
      val trainedModel = CollaborativeFilteringModel.train(frame, "source", "dest", "weight")

      // input array with user and product integers
      //输入数组与用户和产品整数
      val user = 1
      val product = 4
      val inputArray = Array[Any](user, product)
      assert(trainedModel.input().length == inputArray.length)

      // score
      val result = trainedModel.score(inputArray)
      assert(result.length == trainedModel.output().length)
      assert(result(0) == user)
      assert(result(1) == product)
      result(2) match {
        case rating: Double => {
          assertAlmostEqual(rating, 0.04852774194163523, 0.002)
        }
        case _ => throw new RuntimeException("Expected Double from collaborative filtering scoring")
      }
    }
    //为无效评分参数抛出IllegalArgumentExceptions
    "throw IllegalArgumentExceptions for invalid scoring parameters" in {
      val rdd = sparkContext.parallelize(data)
      val frame = new Frame(rdd, schema)

      // train model
      val trainedModel = CollaborativeFilteringModel.train(frame, "source", "dest", "weight")

      intercept[IllegalArgumentException] {
        trainedModel.score(null)
      }

      intercept[IllegalArgumentException] {
        trainedModel.score(Array[Any]("user", "product"))
      }

      intercept[IllegalArgumentException] {
        trainedModel.score(Array[Any](1, 2, 3))
      }
    }
  }

}
