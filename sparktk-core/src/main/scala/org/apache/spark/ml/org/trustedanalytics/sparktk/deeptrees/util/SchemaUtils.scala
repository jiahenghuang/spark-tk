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

import org.apache.spark.sql.types.{ DataType, NumericType, StructField, StructType }

/**
 * Utils for handling schemas.
  * 工具处理模式
 */
private[spark] object SchemaUtils {

  // TODO: Move the utility methods to SQL.

  /**
   * Check whether the given schema contains a column of the required data type.
    * 检查给定模式是否包含所需数据类型的列
   * @param colName  column name 列名称
   * @param dataType  required column data type 所需的列数据类型
   */
  def checkColumnType(
    schema: StructType,
    colName: String,
    dataType: DataType,
    msg: String = ""): Unit = {
    val actualDataType = schema(colName).dataType
    val message = if (msg != null && msg.trim.length > 0) " " + msg else ""
    require(actualDataType.equals(dataType),
      s"Column $colName must be of type $dataType but was actually $actualDataType.$message")
  }

  /**
   * Check whether the given schema contains a column of one of the require data types.
    * 检查给定的模式是否包含其中一个require数据类型的列
   * @param colName  column name 列名称
   * @param dataTypes  required column data types 所需的列数据类型
   */
  def checkColumnTypes(
    schema: StructType,
    colName: String,
    dataTypes: Seq[DataType],
    msg: String = ""): Unit = {
    val actualDataType = schema(colName).dataType
    val message = if (msg != null && msg.trim.length > 0) " " + msg else ""
    require(dataTypes.exists(actualDataType.equals),
      s"Column $colName must be of type equal to one of the following types: " +
        s"${dataTypes.mkString("[", ", ", "]")} but was actually of type $actualDataType.$message")
  }

  /**
   * Check whether the given schema contains a column of the numeric data type.
    * 检查给定的模式是否包含数字数据类型的列
   * @param colName  column name 列名称
   */
  def checkNumericType(
    schema: StructType,
    colName: String,
    msg: String = ""): Unit = {
    val actualDataType = schema(colName).dataType
    val message = if (msg != null && msg.trim.length > 0) " " + msg else ""
    require(actualDataType.isInstanceOf[NumericType], s"Column $colName must be of type " +
      s"NumericType but was actually of type $actualDataType.$message")
  }

  /**
   * Appends a new column to the input schema. This fails if the given output column already exists.
    * 将新列添加到输入模式,如果给定的输出列已经存在,则失败
   * @param schema input schema 输入模式
   * @param colName new column name. If this column name is an empty string "", this method returns
   *                the input schema unchanged. This allows users to disable output columns.
    *                新的列名称,如果这个列名是一个空字符串“”,这个方法返回输入模式不变,这允许用户禁用输出列
   * @param dataType new column data type 新列数据类型
   * @return new schema with the input column appended 添加了输入列的新模式
   */
  def appendColumn(
    schema: StructType,
    colName: String,
    dataType: DataType,
    nullable: Boolean = false): StructType = {
    if (colName.isEmpty) return schema
    appendColumn(schema, StructField(colName, dataType, nullable))
  }

  /**
   * Appends a new column to the input schema. This fails if the given output column already exists.
    * 将新列添加到输入模式,如果给定的输出列已经存在,则失败
   * @param schema input schema 输入模式
   * @param col New column schema 新的列架构
   * @return new schema with the input column appended 添加了输入列的新模式
   */
  def appendColumn(schema: StructType, col: StructField): StructType = {
    require(!schema.fieldNames.contains(col.name), s"Column ${col.name} already exists.")
    StructType(schema.fields :+ col)
  }
}
