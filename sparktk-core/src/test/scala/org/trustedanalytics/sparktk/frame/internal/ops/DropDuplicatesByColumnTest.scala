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
package org.trustedanalytics.sparktk.frame.internal.ops

import org.scalatest.Matchers
import org.apache.spark.sql.Row
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.{ Column, DataTypes, FrameSchema }
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec

class DropDuplicatesByColumnITest extends TestingSparkContextWordSpec with Matchers {
  //删除重复的列
  "dropDuplicatesByColumn" should {
    //只保留列的子集的唯一行
    "keep only unique rows for subset of columns" in {

      //setup test data
      val favoriteMovies = List(
        Row("John", 1, "Titanic"),
        Row("Kathy", 2, "Jurassic Park"),
        Row("John", 1, "The kite runner"),
        Row("Kathy", 2, "Toy Story 3"),
        Row("Peter", 3, "Star War"))

      val schema = FrameSchema(Vector(
        Column("name", DataTypes.string),
        Column("id", DataTypes.int32),
        Column("movie", DataTypes.string)))

      val rdd = sparkContext.parallelize(favoriteMovies)
      val frameRdd = new FrameRdd(schema, rdd)

      frameRdd.count() shouldBe 5

      //remove duplicates identified by column names
      //删除由列名标识的重复项
      val duplicatesRemoved = frameRdd.dropDuplicatesByColumn(Vector("name", "id")).collect()

      val expectedResults = Array(
        Row("John", 1, "Titanic"),
        Row("Kathy", 2, "Jurassic Park"),
        Row("Peter", 3, "Star War")
      )

      duplicatesRemoved should contain theSameElementsAs (expectedResults)
    }
    //只保留唯一的行
    "keep only unique rows" in {

      //setup test data
      //
      val favoriteMovies = List(
        Row("John", 1, "Titanic"),
        Row("Kathy", 2, "Jurassic Park"),
        Row("Kathy", 2, "Jurassic Park"),
        Row("John", 1, "The kite runner"),
        Row("Kathy", 2, "Toy Story 3"),
        Row("Peter", 3, "Star War"),
        Row("Peter", 3, "Star War"))

      val schema = FrameSchema(Vector(
        Column("name", DataTypes.string),
        Column("id", DataTypes.int32),
        Column("movie", DataTypes.string)))

      val rdd = sparkContext.parallelize(favoriteMovies)
      val frameRdd = new FrameRdd(schema, rdd)

      frameRdd.count() shouldBe 7

      //remove duplicates identified by column names
      //删除由列名标识的重复项
      val duplicatesRemoved = frameRdd.dropDuplicatesByColumn(Vector("name", "id", "movie")).collect()

      val expectedResults = Array(
        Row("John", 1, "Titanic"),
        Row("John", 1, "The kite runner"),
        Row("Kathy", 2, "Jurassic Park"),
        Row("Kathy", 2, "Toy Story 3"),
        Row("Peter", 3, "Star War")
      )

      duplicatesRemoved should contain theSameElementsAs (expectedResults)
    }
    //对无效的列名称抛出IllegalArgumentException
    "throw an IllegalArgumentException for invalid column names" in {
      intercept[IllegalArgumentException] {
        //setup test data
        val favoriteMovies = List(
          Row("John", 1, "Titanic"),
          Row("Kathy", 2, "Jurassic Park"),
          Row("John", 1, "The kite runner"),
          Row("Kathy", 2, "Toy Story 3"),
          Row("Peter", 3, "Star War"))

        val schema = FrameSchema(Vector(
          Column("name", DataTypes.string),
          Column("id", DataTypes.int32),
          Column("movie", DataTypes.string)))

        val rdd = sparkContext.parallelize(favoriteMovies)
        val frameRdd = new FrameRdd(schema, rdd)
        frameRdd.dropDuplicatesByColumn(Vector("name", "invalidCol1", "invalidCol2")).collect()
      }

    }
  }
}