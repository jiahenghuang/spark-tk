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
package org.trustedanalytics.sparktk.frame.internal.ops.timeseries

import java.sql.Timestamp
import java.time.format.{ DateTimeFormatter }
import com.cloudera.sparkts.{ TimeSeriesRDD, DayFrequency, DateTimeIndex }
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{ Row, DataFrame, SQLContext }
import org.trustedanalytics.sparktk.frame.internal.constructors.Import
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.{ Column, DataTypes, FrameSchema }
import org.trustedanalytics.sparktk.testutils.TestingSparkContextWordSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import java.time.{ ZoneId, ZonedDateTime }

import scala.collection.mutable

class TimeSeriesTest extends TestingSparkContextWordSpec with Matchers {

  "TimeSeriesFunctions parseZonedDateTime" should {
    //返回分区日期时间提供日期时间序列
    "return ZonedDateTime for the DateTime provided" in {
      val dateTime = DateTime.parse("2016-01-05T12:15:55Z")
      val zonedDateTime = TimeSeriesFunctions.parseZonedDateTime(dateTime)
      assert(2016 == zonedDateTime.getYear)
      assert(1 == zonedDateTime.getMonthValue)
      assert(5 == zonedDateTime.getDayOfMonth)
      assert(12 == zonedDateTime.getHour)
      assert(15 == zonedDateTime.getMinute)
      assert(55 == zonedDateTime.getSecond)
      assert("Z" == zonedDateTime.getZone.toString)
    }
  }

  "TimeSeriesFunctions createDateTimeIndex" should {
    //如果提供了有效的日期/时间列表,则返回DateTimeIndex
    "return a DateTimeIndex if a valid list of date/times is provided" in {
      val dateTimeList = List(DateTime.parse("2016-01-01T12:00:0Z"), DateTime.parse("2016-01-03T12:00:00Z"), DateTime.parse("2016-01-05T12:00:00Z"))

      val dateTimeIndex = TimeSeriesFunctions.createDateTimeIndex(dateTimeList)
      dateTimeIndex.size shouldBe 3
      val x = 0
      for (x <- 0 until dateTimeIndex.size) {
        assertResult(dateTimeList(x).toString()) {
          dateTimeIndex.dateTimeAtLoc(x).format(DateTimeFormatter.ofPattern("YYYY-MM-dd'T'hh:mm:ss'.'SSSVV"))
        }
      }
    }
  }

  // Used for creating a frame of observations
  //用于创建一个观察框架
  def loadObservations(sqlContext: SQLContext, path: String): DataFrame = {
    val rowRdd = sqlContext.sparkContext.textFile(path).map { line =>
      val tokens = line.split('\t')
      val dt = ZonedDateTime.of(tokens(0).toInt, tokens(1).toInt, tokens(2).toInt, 0, 0, 0, 0,
        ZoneId.systemDefault())
      val symbol = tokens(3)
      val price = tokens(4).toDouble
      Row(Timestamp.from(dt.toInstant), symbol, price)
    }
    val fields = Seq(
      StructField("timestamp", TimestampType, true),
      StructField("symbol", StringType, true),
      StructField("price", DoubleType, true)
    )
    val schema = StructType(fields)
    sqlContext.createDataFrame(rowRdd, schema)
  }

  "TimeSeriesFunctions createFrameFromTimeSeries" should {
    //从TimeSeriesRDD创建一个Frame
    "create a Frame from a TimeSeriesRDD" in {
      val sqlContext = new SQLContext(sparkContext)
      val dateTimeCol = "dates"
      val tsCol = "timestamp"
      val keyCol = "keys"
      val valCol = "values"
      val xCol = "temp"

      val inputData = Array(
        Array("2016-01-01T12:00:00Z", "a", 1.0, 88),
        Array("2016-01-01T12:00:00Z", "b", 2.0, 89),
        Array("2016-01-02T12:00:00Z", "a", Double.NaN, 100),
        Array("2016-01-02T12:00:00Z", "b", 3.0, 78),
        Array("2016-01-03T12:00:00Z", "a", 3.0, 72),
        Array("2016-01-03T12:00:00Z", "b", 4.0, 85),
        Array("2016-01-04T12:00:00Z", "a", 4.0, 87),
        Array("2016-01-04T12:00:00Z", "b", 5.0, 88),
        Array("2016-01-05T12:00:00Z", "a", Double.NaN, 88),
        Array("2016-01-05T12:00:00Z", "b", 6.0, 87),
        Array("2016-01-06T12:00:00Z", "a", 6.0, 86),
        Array("2016-01-06T12:00:00Z", "b", 7.0, 84)
      )

      // Create date/time index from interval
      //从区间创建日期/时间索引
      val dtIndex = DateTimeIndex.uniformFromInterval(ZonedDateTime.parse("2016-01-01T12:00:00Z"), ZonedDateTime.parse("2016-01-06T12:00:00Z"), new DayFrequency(1))

      // Try using ATK FrameScheme/FrameRdd wrappers
      //尝试使用ATK FrameScheme / FrameRdd包装
      val frameSchema = FrameSchema(Vector(Column(dateTimeCol, DataTypes.datetime), Column(keyCol, DataTypes.string), Column(valCol, DataTypes.float64), Column(xCol, DataTypes.int32)))
      val rowArrayRdd = sparkContext.parallelize(inputData)
      val frameRdd = FrameRdd.toFrameRdd(frameSchema, rowArrayRdd)
      var frameDataFrame = frameRdd.toDataFrame

      val testData = frameDataFrame.take(frameDataFrame.count().toInt)

      // Add a "timestamp" column using the Timestamp data type
      //使用时间戳记数据类型添加“时间戳记”列
      val toTimestamp = udf((t: Long) => new Timestamp(t))
      frameDataFrame = frameDataFrame.withColumn(tsCol, toTimestamp(frameDataFrame(dateTimeCol))).select(tsCol, keyCol, valCol)

      // Create a timeseries RDD
      //创建时间序列RDD
      val timeseriesRdd = TimeSeriesRDD.timeSeriesRDDFromObservations(dtIndex, frameDataFrame, tsCol, keyCol, valCol)
      //我们应该在时间序列rdd中每个键都有一行
      assert(2 == timeseriesRdd.count) // we should have one row per key in the timeseries rdd

      // Create frame from the timeseries RDD
      //从时间序列RDD创建frame
      var frame = TimeSeriesFunctions.createFrameFromTimeSeries(timeseriesRdd, keyCol, valCol)
      assert(2 == frame.rowCount())

      frame.sort(List((keyCol, true)))
      val frameData = frame.take(frame.rowCount().toInt)
      val expectedData = Array(
        Row("a", Array(1.0, Double.NaN, 3.0, 4.0, Double.NaN, 6.0)),
        Row("b", Array(2.0, 3.0, 4.0, 5.0, 6.0, 7.0))
      )

      // Get column indexes for the key and time series values
      //获取密钥和时间序列值的列索引
      val keyIndex = frame.schema.columnIndex(keyCol)
      val seriesIndex = frame.schema.columnIndex(valCol)

      // Check that the time series frame has the expected values
      //检查时间系列框架是否具有预期值
      for (row_i <- 0 until expectedData.length) {
        // Compare key 比较关键
        assert(expectedData(row_i).get(keyIndex) == frameData(row_i).get(keyIndex))
        // Compare time series values 比较时间系列值
        val expectedValues = expectedData(row_i).get(seriesIndex).asInstanceOf[Array[Double]]
        val frameValues = frameData(row_i).get(seriesIndex).asInstanceOf[mutable.WrappedArray[Double]].toArray
        assert(expectedValues.corresponds(frameValues) {
          _.equals(_)
        })
      }
    }
  }

  "TimeSeriesFunctions discoverKeyAndValueColumns" should {
    //返回有效时间系列模式的键和值列名称
    "return the key and value column names for a valid time series schema" in {
      val expectedKeyColumn = "Name"
      val expectedValueColumn = "TimeSeries"
      val frameSchema = FrameSchema(Vector(Column(expectedKeyColumn, DataTypes.string), Column(expectedValueColumn, DataTypes.vector(2))))

      val actualColumnNames = TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)

      assert(expectedKeyColumn == actualColumnNames.keyColumnName)
      assert(expectedValueColumn == actualColumnNames.valueColumnName)
    }
    //当架构不符合时间序列框架时抛出异常
    "throw an exception when the schema does not conform to a time series frame" in {
      // Frame with two string columns
      //与两个字符串列的框架
      var frameSchema = FrameSchema(Vector(Column("key1", DataTypes.string), Column("key2", DataTypes.string)))
      intercept[RuntimeException] {
        TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)
      }

      // Frame with two vector columns
      //与两个向量列的框架
      frameSchema = FrameSchema(Vector(Column("value1", DataTypes.vector(1)), Column("value2", DataTypes.vector(2))))

      intercept[RuntimeException] {
        TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)
      }

      // Frame with only one column
      //只有一列的Frame
      frameSchema = FrameSchema(Vector(Column("key", DataTypes.str)))

      intercept[RuntimeException] {
        TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)
      }

      // Frame with three columns
      //只有三列的帧
      frameSchema = FrameSchema(Vector(Column("key", DataTypes.str), Column("value", DataTypes.vector(5)), Column("other", DataTypes.string)))

      intercept[RuntimeException] {
        TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)
      }

      // Frame with non-string/vector types
      //与非字符串/矢量类型的Frame
      frameSchema = FrameSchema(Vector(Column("key", DataTypes.int32), Column("timeseries", DataTypes.vector(4))))

      intercept[RuntimeException] {
        TimeSeriesFunctions.discoverKeyAndValueColumns(frameSchema)
      }
    }
  }

  "getYandXFromFrame" should {
    //如果y或x列不是数字,则抛出异常
    "throw an exception if the y or x columns are not numerical" in {
      val schema = FrameSchema(Vector(Column("float_value", DataTypes.float32), Column("str_value", DataTypes.string), Column("int_value", DataTypes.int32)))
      val rows = sparkContext.parallelize((1 to 10).map(i => Array(i, i.toString, i)))
      val rdd = FrameRdd.toFrameRdd(schema, rows)

      intercept[IllegalArgumentException] {
        // We should get an exception when y is a string
        //当y是一个字符串时,我们应该得到一个异常
        TimeSeriesFunctions.getYAndXFromFrame(rdd, "str_value", List("int_value"))
      }

      intercept[IllegalArgumentException] {
        // We should get an exception when an x column is a string
        //当x列是一个字符串时,我们应该得到一个异常
        TimeSeriesFunctions.getYAndXFromFrame(rdd, "int_value", List("float_value", "str_value"))
      }
    }
    //当列不存在时抛出异常
    "throw an exception when a column does not exist" in {
      val schema = FrameSchema(Vector(Column("float_value", DataTypes.float32), Column("str_value", DataTypes.string), Column("int_value", DataTypes.int32)))
      val rows = sparkContext.parallelize((1 to 10).map(i => Array(i, i.toString, i)))
      val rdd = FrameRdd.toFrameRdd(schema, rows)

      intercept[IllegalArgumentException] {
        // We should get an exception when the y column does not exist
        //当y列不存在时,我们应该得到一个异常
        TimeSeriesFunctions.getYAndXFromFrame(rdd, "bogus", List("int_value"))
      }

      intercept[IllegalArgumentException] {
        // We should get an exception when an x column does not exist
        //当x列不存在时,我们应该得到一个异常
        TimeSeriesFunctions.getYAndXFromFrame(rdd, "int_value", List("float_value", "bogus"))
      }
    }
    //当列是数字数据类型时返回x和y值
    "return x and y values when columns are numerical data types" in {
      val schema = FrameSchema(Vector(Column("name", DataTypes.string), Column("float32_value", DataTypes.float32), Column("float64_value", DataTypes.float64), Column("int_value", DataTypes.int32)))
      val rows = sparkContext.parallelize((1 to 10).map(i => Array(i.toString, i, i * 2, i * 3)))
      val rdd = FrameRdd.toFrameRdd(schema, rows)

      val (y, x) = TimeSeriesFunctions.getYAndXFromFrame(rdd, "float32_value", List("float64_value", "int_value"))
      for (i <- 1 until 10) {
        assert(y(i - 1) == i)
        assert(x(i - 1, 0) == (i * 2))
        assert(x(i - 1, 1) == (i * 3))
      }
    }
  }
}
