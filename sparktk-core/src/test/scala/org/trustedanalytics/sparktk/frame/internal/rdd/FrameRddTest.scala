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

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.types._
import org.scalatest.Matchers
import org.trustedanalytics.sparktk.frame.internal.FrameState
import org.trustedanalytics.sparktk.frame.{Column, DataTypes, FrameSchema}
import org.trustedanalytics.sparktk.testutils._

class FrameRddTest extends TestingSparkContextWordSpec with Matchers {

  "FrameRdd" should {

    /**
     * Method that accepts FrameState as a parameter (for testing implicit conversion).
      * 接受FrameState作为参数的方法(用于测试隐式转换)
     * @return Returns schema column column and rdd row count.
      *         返回模式列列和rdd行数
     */
    def frameStateColumnCount(frameState: FrameState): (Int, Long) = {
      (frameState.schema.columns.length, frameState.rdd.count())
    }

    /**
     * Method that accepts FrameRdd as a parameter (for testing implicit conversion)
      * 接受FrameRdd作为参数的方法（用于测试隐式转换）
     * @return Returns schema column column and rdd row count.
      *         返回模式列列和rdd行数
     */
    def frameRddColumnCount(frameRdd: FrameRdd): (Int, Long) = {
      (frameRdd.frameSchema.columns.length, frameRdd.count())
    }
    //隐式地在FrameState和FrameRdd之间进行转换
    "implicitly convert between FrameState and FrameRdd" in {
      val schema = FrameSchema(Vector(Column("num", DataTypes.int32), Column("name", DataTypes.string)))
      val rows = FrameRdd.toRowRDD(schema, sparkContext.parallelize((1 to 100).map(i => Array(i.toLong, i.toString))).repartition(3))

      val frameRdd = new FrameRdd(schema, rows)
      val frameState = FrameState(rows, schema)

      // Call both methods with FrameState
      //用FrameState调用两个方法
      assert(frameStateColumnCount(frameState) == (2, 100))
      assert(frameRddColumnCount(frameState) == (2, 100))

      // Call both methods with FrameRdd
      //用FrameRdd调用这两个方法
      assert(frameRddColumnCount(frameRdd) == (2, 100))
      assert(frameStateColumnCount(frameRdd) == (2, 100))
    }

    /**
     * Tests converting from a FrameRdd to a DataFrame and then back to a FrameRdd.
      * 测试从FrameRdd转换到DataFrame,然后返回到FrameRdd
     */
    //在FrameRdd和Spark DataFrame之间转换
    "converting between FrameRdd and Spark DataFrame" in {
      val schema = FrameSchema(Vector(Column("id", DataTypes.int32), Column("name", DataTypes.string), Column("bday", DataTypes.datetime)))
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](1, "Bob", "1950-05-12T03:25:21.123Z")),
        new GenericRow(Array[Any](2, "Susan", "1979-08-05T07:51:28.000Z")),
        new GenericRow(Array[Any](3, "Jane", "1986-10-17T11:45:00.000Z"))
      )
      val frameRDD = new FrameRdd(schema, sparkContext.parallelize(rows))

      // Convert FrameRDD to DataFrame
      //将FrameRDD转换为DataFrame
      val dataFrame = frameRDD.toDataFrame

      // Check the schema and note that the datetime column is represented as a long in the DataFrame
      //检查架构并注意datetime列在DataFrame中表示为long
      assert(dataFrame.schema.fields.sameElements(Array(StructField("id", IntegerType, true),
        StructField("name", StringType, true),
        StructField("bday", LongType, true))))

      // Add a column that converts the bday (LongType) to a timestamp column that uses the TimestampType
      //添加将bday（LongType）转换为使用TimestampType的时间戳列的列
 /*     val dfWithTimestamp = dataFrame.withColumn("timestamp", TimeSeriesFunctions.toTimestamp(dataFrame("bday")))
      assert(dfWithTimestamp.schema.fields.sameElements(Array(StructField("id", IntegerType, true),
        StructField("name", StringType, true),
        StructField("bday", LongType, true),
        StructField("timestamp", TimestampType, true))))

      // Convert DataFrame back to a FrameRDD
      //将DataFrame转换回FrameRDD
      val frameRddWithTimestamp = FrameRdd.toFrameRdd(dfWithTimestamp)

      // Check schema
      //检查模式
      val fields = frameRddWithTimestamp.schema.columns
      assert(frameRddWithTimestamp.schema.columnNames.sameElements(Vector("id", "name", "bday", "timestamp")))
      assert(frameRddWithTimestamp.schema.columnDataType("id") == DataTypes.int32)
      assert(frameRddWithTimestamp.schema.columnDataType("name") == DataTypes.string)
      assert(frameRddWithTimestamp.schema.columnDataType("bday") == DataTypes.int64)
      assert(frameRddWithTimestamp.schema.columnDataType("timestamp") == DataTypes.datetime)*/
    }
  }
}