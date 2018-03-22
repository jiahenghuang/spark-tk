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
package org.trustedanalytics.sparktk.frame.internal.ops.join

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.{ Column, DataTypes, FrameSchema }
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

class SparkJoinITest extends TestingSparkContextWordSpec with Matchers {
  // Test data has duplicate keys, matching and non-matching keys
  //测试数据具有重复的密钥,匹配和不匹配的密钥
  val idCountryCodes: List[Row] = List(
    new GenericRow(Array[Any](1, 354, "a")),
    new GenericRow(Array[Any](2, 91, "a")),
    new GenericRow(Array[Any](2, 100, "b")),
    new GenericRow(Array[Any](3, 47, "a")),
    new GenericRow(Array[Any](4, 968, "c")),
    new GenericRow(Array[Any](5, 50, "c")))

  val idCountryNames: List[Row] = List(
    new GenericRow(Array[Any](1, "Iceland", "a")),
    new GenericRow(Array[Any](1, "Ice-land", "a")),
    new GenericRow(Array[Any](2, "India", "b")),
    new GenericRow(Array[Any](3, "Norway", "a")),
    new GenericRow(Array[Any](4, "Oman", "c")),
    new GenericRow(Array[Any](6, "Germany", "c"))
  )

  val codeSchema = FrameSchema(Vector(
    Column("col_0", DataTypes.int32),
    Column("col_1", DataTypes.int32),
    Column("col_2", DataTypes.str)
  ))

  val countrySchema = FrameSchema(Vector(
    Column("col_0", DataTypes.int32),
    Column("col_1", DataTypes.str),
    Column("col_2", DataTypes.str)
  ))

  "joinRDDs" should {
    //内连接加入两个RDD
    "join two RDD with inner join" in {

      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.innerJoin(RddJoinParam(countryCode, Seq("col_0")),
        RddJoinParam(countryNames, Seq("col_0")), Some("inner"))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland", "a")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land", "a")),
        new GenericRow(Array[Any](2, 91, "a", "India", "b")),
        new GenericRow(Array[Any](2, 100, "b", "India", "b")),
        new GenericRow(Array[Any](3, 47, "a", "Norway", "a")),
        new GenericRow(Array[Any](4, 968, "c", "Oman", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量连接两个RDD和内部连接
    "join two RDD with inner join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0"))

      val resultFrame = JoinRddFunctions.innerJoin(leftJoinParam, rightJoinParam, Some("inner"))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland", "a")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land", "a")),
        new GenericRow(Array[Any](2, 91, "a", "India", "b")),
        new GenericRow(Array[Any](2, 100, "b", "India", "b")),
        new GenericRow(Array[Any](3, 47, "a", "Norway", "a")),
        new GenericRow(Array[Any](4, 968, "c", "Oman", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //用左连接加入两个RDD
    "join two RDD with left join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.leftJoin(RddJoinParam(countryCode, Seq("col_0")),
        RddJoinParam(countryNames, Seq("col_0")), false)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland", "a")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land", "a")),
        new GenericRow(Array[Any](2, 91, "a", "India", "b")),
        new GenericRow(Array[Any](2, 100, "b", "India", "b")),
        new GenericRow(Array[Any](3, 47, "a", "Norway", "a")),
        new GenericRow(Array[Any](4, 968, "c", "Oman", "c")),
        new GenericRow(Array[Any](5, 50, "c", null, null))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量连接两个带有左连接的RDD
    "join two RDD with left join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0"))

      // Test join wrapper function
      //测试连接包装函数
      val resultFrame = JoinRddFunctions.leftJoin(leftJoinParam, rightJoinParam, true)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland", "a")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land", "a")),
        new GenericRow(Array[Any](2, 91, "a", "India", "b")),
        new GenericRow(Array[Any](2, 100, "b", "India", "b")),
        new GenericRow(Array[Any](3, 47, "a", "Norway", "a")),
        new GenericRow(Array[Any](4, 968, "c", "Oman", "c")),
        new GenericRow(Array[Any](5, 50, "c", null, null))
      )

      results should contain theSameElementsAs expectedResults
    }
    //加入两个RDD
    "join two RDD with right join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.rightJoin(
        RddJoinParam(countryCode, Seq("col_0")),
        RddJoinParam(countryNames, Seq("col_0")), false)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_0_R", DataTypes.int32),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](354, "a", 1, "Iceland", "a")),
        new GenericRow(Array[Any](354, "a", 1, "Ice-land", "a")),
        new GenericRow(Array[Any](91, "a", 2, "India", "b")),
        new GenericRow(Array[Any](100, "b", 2, "India", "b")),
        new GenericRow(Array[Any](47, "a", 3, "Norway", "a")),
        new GenericRow(Array[Any](968, "c", 4, "Oman", "c")),
        new GenericRow(Array[Any](null, null, 6, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量以右连接加入两个RDD
    "join two RDD with right join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0"))

      val resultFrame = JoinRddFunctions.rightJoin(leftJoinParam, rightJoinParam, true)

      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_0_R", DataTypes.int32),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](354, "a", 1, "Iceland", "a")),
        new GenericRow(Array[Any](354, "a", 1, "Ice-land", "a")),
        new GenericRow(Array[Any](91, "a", 2, "India", "b")),
        new GenericRow(Array[Any](100, "b", 2, "India", "b")),
        new GenericRow(Array[Any](47, "a", 3, "Norway", "a")),
        new GenericRow(Array[Any](968, "c", 4, "Oman", "c")),
        new GenericRow(Array[Any](null, null, 6, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //加入两个外连接的RDD
    "join two RDD with outer join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(countryCode, Seq("col_0")),
        RddJoinParam(countryNames, Seq("col_0")))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland", "a")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land", "a")),
        new GenericRow(Array[Any](2, 91, "a", "India", "b")),
        new GenericRow(Array[Any](2, 100, "b", "India", "b")),
        new GenericRow(Array[Any](3, 47, "a", "Norway", "a")),
        new GenericRow(Array[Any](4, 968, "c", "Oman", "c")),
        new GenericRow(Array[Any](5, 50, "c", null, null)),
        new GenericRow(Array[Any](6, null, null, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
  }
  //复合加入RDD
  "compositeJoinRDDs" should {
    //内连接加入两个RDD
    "join two RDD with inner join" in {

      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.innerJoin(RddJoinParam(countryCode, Seq("col_0", "col_2")),
        RddJoinParam(countryNames, Seq("col_0", "col_2")), Some("inner"))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land")),
        new GenericRow(Array[Any](2, 100, "b", "India")),
        new GenericRow(Array[Any](3, 47, "a", "Norway")),
        new GenericRow(Array[Any](4, 968, "c", "Oman"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量连接两个RDD和内部连接
    "join two RDD with inner join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0", "col_2"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0", "col_2"))

      val resultFrame = JoinRddFunctions.innerJoin(leftJoinParam, rightJoinParam, Some("inner"))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land")),
        new GenericRow(Array[Any](2, 100, "b", "India")),
        new GenericRow(Array[Any](3, 47, "a", "Norway")),
        new GenericRow(Array[Any](4, 968, "c", "Oman"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //用左连接加入两个RDD
    "join two RDD with left join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.leftJoin(RddJoinParam(countryCode, Seq("col_0", "col_2")),
        RddJoinParam(countryNames, Seq("col_0", "col_2")), false)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land")),
        new GenericRow(Array[Any](2, 91, "a", null)),
        new GenericRow(Array[Any](2, 100, "b", "India")),
        new GenericRow(Array[Any](3, 47, "a", "Norway")),
        new GenericRow(Array[Any](4, 968, "c", "Oman")),
        new GenericRow(Array[Any](5, 50, "c", null))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量连接两个带有左连接的RDD
    "join two RDD with left join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0", "col_2"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0", "col_2"))

      // Test join wrapper function
      //测试连接包装函数
      val resultFrame = JoinRddFunctions.leftJoin(leftJoinParam, rightJoinParam, true)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land")),
        new GenericRow(Array[Any](2, 91, "a", null)),
        new GenericRow(Array[Any](2, 100, "b", "India")),
        new GenericRow(Array[Any](3, 47, "a", "Norway")),
        new GenericRow(Array[Any](4, 968, "c", "Oman")),
        new GenericRow(Array[Any](5, 50, "c", null))
      )

      results should contain theSameElementsAs expectedResults
    }
    //加入两个RDD
    "join two RDD with right join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.rightJoin(
        RddJoinParam(countryCode, Seq("col_0", "col_2")),
        RddJoinParam(countryNames, Seq("col_0", "col_2")), false)
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_1_L", DataTypes.int32),
        Column("col_0_R", DataTypes.int32),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](354, 1, "Iceland", "a")),
        new GenericRow(Array[Any](354, 1, "Ice-land", "a")),
        new GenericRow(Array[Any](100, 2, "India", "b")),
        new GenericRow(Array[Any](47, 3, "Norway", "a")),
        new GenericRow(Array[Any](968, 4, "Oman", "c")),
        new GenericRow(Array[Any](null, 6, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //使用广播变量以右连接加入两个RDD
    "join two RDD with right join using broadcast variable" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val leftJoinParam = RddJoinParam(countryCode, Seq("col_0", "col_2"))
      val rightJoinParam = RddJoinParam(countryNames, Seq("col_0", "col_2"))

      val resultFrame = JoinRddFunctions.rightJoin(leftJoinParam, rightJoinParam, true)

      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_1_L", DataTypes.int32),
        Column("col_0_R", DataTypes.int32),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](354, 1, "Iceland", "a")),
        new GenericRow(Array[Any](354, 1, "Ice-land", "a")),
        new GenericRow(Array[Any](100, 2, "India", "b")),
        new GenericRow(Array[Any](47, 3, "Norway", "a")),
        new GenericRow(Array[Any](968, 4, "Oman", "c")),
        new GenericRow(Array[Any](null, 6, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
    //加入两个外连接的RDD
    "join two RDD with outer join" in {
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(countryCode, Seq("col_0", "col_2")),
        RddJoinParam(countryNames, Seq("col_0", "col_2")))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", "Iceland")),
        new GenericRow(Array[Any](1, 354, "a", "Ice-land")),
        new GenericRow(Array[Any](2, 91, "a", null)),
        new GenericRow(Array[Any](2, 100, "b", "India")),
        new GenericRow(Array[Any](3, 47, "a", "Norway")),
        new GenericRow(Array[Any](4, 968, "c", "Oman")),
        new GenericRow(Array[Any](5, 50, "c", null)),
        new GenericRow(Array[Any](6, null, "c", "Germany"))
      )

      results should contain theSameElementsAs expectedResults
    }
  }
  //外部连接空左RDD
  "outer join with empty left RDD" should {
    //保留正确的RDD的结果
    "preserve the result from the right RDD" in {
      val emptyIdCountryCodes = List.empty[Row]
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(emptyIdCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(countryCode, Seq("col_0")), RddJoinParam(countryNames, Seq("col_0")))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, null, null, "Iceland", "a")),
        new GenericRow(Array[Any](1, null, null, "Ice-land", "a")),
        new GenericRow(Array[Any](2, null, null, "India", "b")),
        new GenericRow(Array[Any](3, null, null, "Norway", "a")),
        new GenericRow(Array[Any](4, null, null, "Oman", "c")),
        new GenericRow(Array[Any](6, null, null, "Germany", "c"))
      )

      results should contain theSameElementsAs expectedResults
    }
  }

  "compositeOuter join with empty left RDD" should {
    //保留正确的RDD的结果
    "preserve the result from the right RDD" in {
      val emptyIdCountryCodes = List.empty[Row]
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(emptyIdCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(idCountryNames))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(countryCode, Seq("col_0", "col_2")), RddJoinParam(countryNames, Seq("col_0", "col_2")))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, null, "a", "Iceland")),
        new GenericRow(Array[Any](1, null, "a", "Ice-land")),
        new GenericRow(Array[Any](2, null, "b", "India")),
        new GenericRow(Array[Any](3, null, "a", "Norway")),
        new GenericRow(Array[Any](4, null, "c", "Oman")),
        new GenericRow(Array[Any](6, null, "c", "Germany"))
      )

      results should contain theSameElementsAs expectedResults
    }
  }
  //外部连接与空右RDD
  "outer join with empty right RDD" should {
    //保留左RDD的结果
    "preserve the result from the left RDD" in {
      val emptyIdCountryNames = List.empty[Row]
      val countryCode = new FrameRdd(codeSchema, sparkContext.parallelize(idCountryCodes))
      val countryNames = new FrameRdd(countrySchema, sparkContext.parallelize(emptyIdCountryNames))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(countryCode, Seq("col_0")),
        RddJoinParam(countryNames, Seq("col_0")))
      val results = resultFrame.collect()

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32),
        Column("col_1_L", DataTypes.int32),
        Column("col_2_L", DataTypes.str),
        Column("col_1_R", DataTypes.str),
        Column("col_2_R", DataTypes.str)
      ))

      val expectedResults = List(
        new GenericRow(Array[Any](1, 354, "a", null, null)),
        new GenericRow(Array[Any](2, 91, "a", null, null)),
        new GenericRow(Array[Any](2, 100, "b", null, null)),
        new GenericRow(Array[Any](3, 47, "a", null, null)),
        new GenericRow(Array[Any](4, 968, "c", null, null)),
        new GenericRow(Array[Any](5, 50, "c", null, null))
      )

      results should contain theSameElementsAs expectedResults
    }
  }
  //外连接大型RDD
  "outer join large RDD" should {
    //生成RDD包含来自两个RDD的所有元素
    "generate RDD contains all element from both RDD" in {
      val oneToOneHundredThousand: List[Row] = (1 to 100000).map(i => {
        new GenericRow(Array[Any](i))
      }).toList

      val fiftyThousandToOneFiftyThousands: List[Row] = (50001 to 150000).map(i => {
        new GenericRow(Array[Any](i))
      }).toList

      val inputSchema = FrameSchema(Vector(Column("col_0", DataTypes.int32)))

      val rddOneToMillion = new FrameRdd(inputSchema, sparkContext.parallelize(oneToOneHundredThousand))
      val rddFiveHundredThousandsToOneFiftyThousands = new FrameRdd(inputSchema,
        sparkContext.parallelize(fiftyThousandToOneFiftyThousands))

      val resultFrame = JoinRddFunctions.outerJoin(RddJoinParam(rddOneToMillion, Seq("col_0")),
        RddJoinParam(rddFiveHundredThousandsToOneFiftyThousands, Seq("col_0")))

      resultFrame.frameSchema.columns should equal(List(
        Column("col_0_L", DataTypes.int32)
      ))
      resultFrame.count shouldBe 150000
    }
  }

}
