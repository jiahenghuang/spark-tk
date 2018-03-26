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
package org.trustedanalytics.sparktk.frame.internal
import org.apache.spark.mllib.linalg.{Vectors, Vector => MllibVector}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.catalyst.util.GenericArrayData
import org.trustedanalytics.sparktk.frame.DataTypes.DataType
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd
import org.trustedanalytics.sparktk.frame.{DataTypes, Schema}

import scala.collection.mutable.ArrayBuffer
import scala.xml.{NodeSeq, XML}

/**
 * This class wraps raw row data adding schema information - this allows for a richer easier to use API.
 * 这个类包装原始行数据添加schema信息 - 这使得更容易使用API更丰富,
 * Ideally, every row knows about its schema but this is inefficient when there are many rows.  As an
 * alternative, a single instance of this wrapper can be used to provide the same kind of API.
 * 理想情况下,每行都知道它的模式,但是当有很多行时,这是低效的,作为替代方案,可以使用此包装的单个实例来提供相同类型的API
 * @param schema the schema for the row
 */
class RowWrapper(override val schema: Schema) extends AbstractRow with Serializable {
  require(schema != null, "schema is required")

  private lazy val structType = FrameRdd.schemaToStructType(schema)
  //这个注解一般用于序列化的时候,标识某个字段不用被序列化
  @transient override var row: Row = null

  /**
   * Set the data in this wrapper
   * 在这个包装器中设置数据
   * @param row the data to set inside this Wrapper
    *            在这个Wrapper里面设置的数据
   * @return this instance
   */
  def apply(row: Row): RowWrapper = {
    this.row = row
    this
  }

  def apply(internalRow: InternalRow) = {
    //Row用Seq[Any]来表示values，GenericRow是Row的子类,用数组表示values。
    //Row支持数据类型包括Int, Long, Double, Float, Boolean, Short, Byte, String,
    //支持按序数(ordinal)读取某一个列的值,读取前需要做isNullAt(i: Int)的判断
    this.row = new GenericRow(internalRow.toSeq(structType).toArray)
    this
  }
}

/**
 * Most implementation belongs here so it can be shared with AbstractVertex and AbstractEdge
  * 大多数实现属于这里,所以它可以与AbstractVertex和AbstractEdge共享
 */
trait AbstractRow {

  val schema: Schema
  var row: Row

  /**
   * Determine whether the property exists
   * 确定属性是否存在
   * @param name name of the property 属性的名称
   * @return boolean value indicating whether the property exists
    *         表示属性是否存在的布尔值
   */
  def hasProperty(name: String): Boolean = {
    try {
      schema.columnIndex(name)
      true
    }
    catch {
      case e: Exception => false
    }
  }

  /**
   * Get property
   * 获取属性的值
   * @param columnName name of the property
   * @return property value
   */
  def value(columnName: String): Any = row(schema.columnIndex(columnName))

  /**
   * Get more than one value as a List
   * 获取多个值作为列表
   * @param columnNames the columns to get values for
   * @return the values for the columns 列的值
   */
  def values(columnNames: Seq[String] = schema.columnNames): Seq[Any] = {
    columnNames.map(columnName => value(columnName))
  }

  /**
   * Get property of boolean data type
   * 获取布尔数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def booleanValue(columnName: String): Boolean = row(schema.columnIndex(columnName)).asInstanceOf[Boolean]

  /**
   * Get property of integer data type
   * 获取整型数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def intValue(columnName: String): Int = DataTypes.toInt(row(schema.columnIndex(columnName)))

  /**
   * Get property of long data type
   * 获取长数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def longValue(columnName: String): Long = DataTypes.toLong(row(schema.columnIndex(columnName)))

  /**
   * Get property of float data type
   * 获取float数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def floatValue(columnName: String): Float = DataTypes.toFloat(row(schema.columnIndex(columnName)))

  /**
   * Get property of double data type
   * 获取double数据类型的属性的值
   * @param columnName name of the property
   * @return property value
   */
  def doubleValue(columnName: String): Double = DataTypes.toDouble(row(schema.columnIndex(columnName)))

  /**
   * Get property of string data type
   * 获取字符串数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def stringValue(columnName: String): String = DataTypes.toStr(row(schema.columnIndex(columnName)))

  /**
   * Get property of string data type
   * 获取字符串数据类型的属性
   * @param columnName name of the property
   * @return property value
   */
  def vectorValue(columnName: String): Vector[Double] = {
    row(schema.columnIndex(columnName)) match {
      case ga: GenericArrayData => ga.toDoubleArray().toVector
      case value => DataTypes.toVector()(value)
    }
  }

  /**
   * Get the row as NodeSeq
   * 获取该行作为NodeSeq
   * @param columnName Column name in frame holding xml string 列中的列名称保存xml字符串
   * @param nodeName Name of the node to extract from column holding xml string 从保存xml字符串的列中提取节点的名称
   * @return NodeSeq (i.e Seq[Node])
   */
  def xmlNodeSeqValue(columnName: String, nodeName: String): NodeSeq = {
    val result = row(schema.columnIndex(columnName)).toString
    XML.loadString(result) \ nodeName
  }

  /**
   * True if value for this column is null.
   * 如果此列的值为null，则为true
   * (It is non-intuitive but SparkSQL seems to allow null primitives).
    * 这是非直观的,但SparkSQL似乎允许null原语
   */
  def isNull(columnName: String): Boolean = row.isNullAt(schema.columnIndex(columnName))

  /**
   * Set a value in a column - validates the supplied value is the correct type
   *在列中设置一个值 - 验证提供的值是正确的类型
   * @param name the name of the column to set
   * @param value the value of the column
   */
  def setValue(name: String, value: Any): Row = {
    validate(name, value)
    setValueIgnoreType(name, value)
  }

  /**
   * Set all of the values for an entire row with validation
   * 使用验证设置整个行的所有值
   * @param values the values to set
   * @return the row
   */
  def setValues(values: Array[Any]): Row = {
    validate(values)
    setValuesIgnoreType(values)
  }

  /**
   * Validate the supplied value matches the schema for the supplied columnName.
   * 验证提供的值是否匹配提供的columnName的模式
   * @param name column name
   * @param value the value to check
   */
  private def validate(name: String, value: Any): Unit = {
    if (!schema.columnDataType(name).isType(value)) {
      val dataType = DataTypes.dataTypeOfValueAsString(value)
      throw new IllegalArgumentException(s"setting property $name with value $value with an incorrect data type: $dataType")
    }
  }

  private def validate(values: Array[Any]): Unit = {
    require(values.length == schema.columns.length, "number of values must match the number of columns")
    values.zip(schema.columns).foreach { case (value, column) => validate(column.name, value) }
  }

  /**
   * Set the value in a column - don't validate the type
   * 在列中设置值 - 不验证类型
   * @param name the name of the column to set
   * @param value the value of the column
   */
  private def setValueIgnoreType(name: String, value: Any): Row = {
    val position = schema.columnIndex(name)
    val content = row.toSeq.toArray
    content(position) = value
    //TODO: what is the right way to introduce GenericMutableRow?
    row = new GenericRow(content)
    row
  }

  /**
   * Set all of the values for a row - don't validate type
   * 设置一行的所有值 - 不要验证类型
   * @param values all of the values
   * @return the row
   */
  private def setValuesIgnoreType(values: Array[Any]): Row = {
    //TODO: what is the right way to introduce GenericMutableRow?
    row = new GenericRow(values)
    row
  }

  /**
   * Add a property onto the end of this row.
   * 在此行的末尾添加一个属性
   * Since this property isn't part of the current schema, no name is supplied.
   * 由于此属性不是当前模式的一部分,因此不提供名称。
   * This method changes the schema of the underlying row.
   *
   * @param value the value of the new column
   * @return the row (with a different schema)
   */
  def addValue(value: Any): Row = {
    val content = row.toSeq.toArray :+ value
    //TODO: what is the right way to introduce GenericMutableRow?
    row = new GenericRow(content)
    row
  }

  /**
   * Add the value if the column name doesn't exist, otherwise set the existing column
   * 如果列名不存在则添加该值,否则设置现有列
   * Note this method may change the schema of the underlying row
   */
  def addOrSetValue(name: String, value: Any): Row = {
    if (!hasProperty(name)) {
      addValue(value)
    }
    else {
      setValueIgnoreType(name, value)
    }
  }

  /**
   * Convert the supplied column from the current type to the supplied dataType
   * 将提供的列从当前类型转换为提供的数据类型
   * This method changes the schema of the underlying row.
   *
   * @param columnName column to change
   * @param dataType new data type to convert existing values to
   * @return the modified row (with a different schema)
   */
  def convertType(columnName: String, dataType: DataType): Row = {
    setValueIgnoreType(columnName, DataTypes.convertToType(value(columnName), dataType))
  }

  /**
   * Get underlying data for this row
   * 获取此行的基础数据
   * @return the actual row
   */
  def data: Row = row

  /**
   * Create a new row from the data of the columns supplied
    * 根据提供的列的数据创建一个新行
   */
  def valuesAsRow(columnNames: Seq[String] = schema.columnNames): Row = {
    val content = valuesAsArray(columnNames)
    new GenericRow(content)
  }

  /**
   * Select several property values from their names
   *从他们的名字中选择几个属性值
   * @param names the names of the properties to put into an array
   * @param flattenInputs If true, flatten vector data types 如果为true,则展开向量数据类型
   * @return values for the supplied properties
   */
  def valuesAsArray(names: Seq[String] = schema.columnNames, flattenInputs: Boolean = false): Array[Any] = {
    val arrayBuf = new ArrayBuffer[Any]()

    schema.columnIndices(names).map(i => {
      schema.column(i).dataType match {
          //变长数组追加++=
          //+=
        case DataTypes.vector(length) => if (flattenInputs) arrayBuf ++= DataTypes.toVector(length)(row(i)) else arrayBuf += row(i)
        case _ => arrayBuf += row(i)
      }
    })

    arrayBuf.toArray
  }

  /**
   * Select several property values from their names as an array of doubles
   * 从名称中选择几个属性值作为双精度数组
   * @param names the names of the properties to put into an array
   * @param flattenInputs If true, flatten vector data types 压扁向量数据类型
   * @return array of doubles with values for the supplied properties 所提供属性值的双精度数组
   */
  def valuesAsDoubleArray(names: Seq[String] = schema.columnNames, flattenInputs: Boolean = false): Array[Double] = {
    valuesAsArray(names, flattenInputs).map(value => DataTypes.toDouble(value))
  }

  /**
   * Values of the row as a Seq[Any]
    * 行的值作为Seq [Any]
   */
  def toSeq: Seq[Any] = {
    row.toSeq
  }

  /**
   * Values of the row as an Array[Any]
    * 该行作为数组的值[任意]
   */
  def toArray: Array[Any] = {
    row.toSeq.toArray
  }
  //转换成稠密向量
  def toDenseVector(featureColumnNames: Seq[String]): MllibVector = {
    Vectors.dense(valuesAsDoubleArray(featureColumnNames))
  }

  def toWeightedDenseVector(featureColumnNames: Seq[String], columnWeights: Array[Double]): MllibVector = {
    val values = valuesAsDoubleArray(featureColumnNames)
    val scaledValues = values.zip(columnWeights).map { case (x, y) => x * y }
    Vectors.dense(scaledValues)
  }

  /**
   * Create a new row matching the supplied schema adding/dropping columns as needed.
    * 根据需要创建与提供的模式添加/删除列匹配的新行
   *
   * @param updatedSchema the new schema to match 要匹配的新模式
   * @return the row matching the new schema 新模式匹配的行
   */
  def valuesForSchema(updatedSchema: Schema): Row = {
    val content = new Array[Any](updatedSchema.columns.length)
    for (columnName <- updatedSchema.columnNames) {
      // todo: add graph?
      //      if (columnName == GraphSchema.labelProperty && updatedSchema.isInstanceOf[GraphElementSchema]) {
      //        content(updatedSchema.columnIndex(columnName)) = updatedSchema.asInstanceOf[GraphElementSchema].label
      //      }
      //      else
      if (schema.hasColumnWithType(columnName, updatedSchema.columnDataType(columnName))) {
        content(updatedSchema.columnIndex(columnName)) = value(columnName)
      }
      else if (schema.hasColumn(columnName)) {
        content(updatedSchema.columnIndex(columnName)) = DataTypes.convertToType(value(columnName), updatedSchema.columnDataType(columnName))
      }
      else {
        // it is non-intuitive but even primitives can be null with Rows
        //它是非直观的,但即使原始数据对于行也可以为空
        content(updatedSchema.columnIndex(columnName)) = null
      }
    }
    new GenericRow(content)
  }

  /**
   * Create a new empty row
    * 创建一个新的空行
   */
  def create(): Row = {
    //TODO: what is the right way to introduce GenericMutableRow?
    val content = new Array[Any](schema.columns.length)
    row = new GenericRow(content)
    row
  }

  /**
   * Create a row with values
   * 创建一个包含值的行
   * @param content the values
   * @return the row
   */
  def create(content: Array[Any]): Row = {
    create()
    setValues(content)
  }

  //  def create(vertex: GBVertex): Row = {
  //    create()
  //    vertex.properties.foreach(prop => setValue(prop.key, prop.value))
  //    row
  //  }
}

