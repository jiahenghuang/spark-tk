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
package org.trustedanalytics.sparktk.frame.internal.constructors

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec
import org.trustedanalytics.sparktk.frame.{ FrameSchema, Frame, Column, DataTypes }

class FrameInitTest extends TestingSparkContextWordSpec {
  //Frame初始化
  "Frame init" should {
    //如果没有提供schema式,则使用的推断模式(int和string)
    "infer the schema (int and string) if no schema is provided" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](1, "one")),
        new GenericRow(Array[Any](2, "two")),
        new GenericRow(Array[Any](3, "three")),
        new GenericRow(Array[Any](4, "four")),
        new GenericRow(Array[Any](5, "five"))
      )
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null)
      assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.int32), Column("C1", DataTypes.string))))
    }
    //当有int,浮点数和字符串,混合推断模式
    "infer the schema when there is a mix of int, floats, and strings" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](1, 1, 2.5)),
        new GenericRow(Array[Any](2.2, "2", 2)),
        new GenericRow(Array[Any](3, "three", 7)),
        new GenericRow(Array[Any](4, 2.3, 8.5)),
        new GenericRow(Array[Any](5, "five", "seven"))
      )
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null)
      assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.float64),
        Column("C1", DataTypes.string),
        Column("C2", DataTypes.string))))
    }
    //如果没有提供模式,则使用向量推断模式
    "infer the schema with vectors, if no schema is provided" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](List[Int](1, 2, 3))),
        new GenericRow(Array[Any](List[Int](4, 5, 6))),
        new GenericRow(Array[Any](List[Int](7, 8, 9)))
      )
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null)
      assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.vector(3)))))
      val data = frame.take(frame.rowCount().toInt)
      for ((frameRow, originalRow) <- (data.zip(rows))) {
        assert(frameRow.equals(originalRow))
      }
    }
    //推断模式,缺少值
    "infer the schema, with missing values" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](1, 2, null)),
        new GenericRow(Array[Any](4, null, null)),
        new GenericRow(Array[Any](7, 8, null))
      )
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null)
      assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.int32),
        Column("C1", DataTypes.int32),
        Column("C2", DataTypes.int32))))
      val data = frame.take(frame.rowCount().toInt)
      assert(data.sameElements(rows))
    }
    //当vectors长度不一样的时候抛出一个异常
    "throw an exception when vectors aren't all the same length" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](List[Int](1, 2, 3))),
        new GenericRow(Array[Any](List[Int](4, 5, 6))),
        new GenericRow(Array[Any](List[Int](7, 8, 9, 10)))
      )
      val rdd = sparkContext.parallelize(rows)

      intercept[RuntimeException] {
        val frame = new Frame(rdd, null)
        assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.vector(3)))))
        val data = frame.take(frame.rowCount().toInt)
      }
    }
    //测试推断验证模式
    "test schema validation" in {
      val rows: Array[Row] = Array(
        new GenericRow(Array[Any](1)),
        new GenericRow(Array[Any](2)),
        new GenericRow(Array[Any](3)),
        new GenericRow(Array[Any](4)),
        new GenericRow(Array[Any](5.1))
      )
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null, true)
      assert(frame.schema == FrameSchema(Vector(Column("C0", DataTypes.float64))))
      // All values should be double
      //所有值都是Double类型值
      assert(frame.take(frame.rowCount.toInt).forall(row => row.get(0).isInstanceOf[Double]))
    }
    //如果启用验证,则如果超过前100行的数据与模式不匹配,则包含缺少的值
    "include missing values, if data past the first 100 rows does not match the schema, when validation is enabled" in {
      //创建一个指定重复数量的元素列表
      val intRows: Array[Row] = Array.fill(100) { new GenericRow(Array[Any](1)) }
      val floatRows: Array[Row] = Array.fill(20) { new GenericRow(Array[Any]("a")) }
      val rows: Array[Row] = intRows ++ floatRows
      val rdd = sparkContext.parallelize(rows)
      val frame = new Frame(rdd, null, validateSchema = true)
      val data = frame.take(frame.rowCount.toInt)
      // check for missing values
      data.slice(100, data.length).foreach(r => assert(r.get(0) == null))
      assert(frame.validationReport.isDefined == true)
      assert(20 == frame.validationReport.get.numBadValues)
    }
    //如果经过前100行的数据与模式不匹配(如果禁用验证),则不会出现异常
    "no exception if data past the first 100 rows does not match the schema, if validation is disabled" in {
      val intRows: Array[Row] = Array.fill(100) { new GenericRow(Array[Any](1)) }
      val floatRows: Array[Row] = Array.fill(20) { new GenericRow(Array[Any]("a")) }
      val rows: Array[Row] = intRows ++ floatRows
      val rdd = sparkContext.parallelize(rows)

      val frame = new Frame(rdd, null, validateSchema = false)
      frame.dataframe.show(10)
      assert(frame.take(frame.rowCount.toInt).length == frame.rowCount)
      assert(frame.validationReport.isDefined == false)
    }
    //为重复的列名引发异常
    "throw an exception for duplicate column names" in {
      intercept[IllegalArgumentException] {
        // duplicate column names should cause an exception
        //重复的列名应该会导致异常
        val schema = FrameSchema(List(Column("a", DataTypes.int32), Column("a", DataTypes.str)))
      }
    }
  }
}
