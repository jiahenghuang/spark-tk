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
package org.trustedanalytics.sparktk.frame

import java.util.{ ArrayList => JArrayList }

import com.typesafe.config.ConfigFactory
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.commons.lang.{ StringUtils => CommonsStringUtils }
import org.trustedanalytics.sparktk.frame.DataTypes.DataType
import org.trustedanalytics.sparktk.frame.internal.rdd.FrameRdd

import scala.reflect.runtime.universe._

/**
 * Column - this is a nicer wrapper for columns than just tuples
 * 列 - 这是一个更好的列包装不仅仅是元组
 * @param name the column name
 * @param dataType the type
 */
case class Column(name: String, dataType: DataType) {
  require(name != null, "column name is required")
  require(dataType != null, "column data type is required")
  require(name != "", "column name can't be empty")
  require(Column.isValidColumnName(name), "column name must be alpha-numeric with valid symbols")
}

object Column {
  /**
   * Create Column given column name and scala type
   * 创建列给定列名称和Scala类型
   * @param name Column name 列名称
   * @param dtype runtime type (using scala reflection) 运行时类型(使用scala反射)
   * @return Column
   */
  def apply(name: String, dtype: reflect.runtime.universe.Type): Column = {
    require(name != null, "column name is required")
    require(dtype != null, "column data type is required")
    require(name != "", "column name can't be empty")
    require(Column.isValidColumnName(name), "column name must be alpha-numeric with valid symbols")

    Column(name,
      dtype match {
          //这种类型是否符合给定的类型参数`that`？
        case t if t <:< definitions.IntTpe => DataTypes.int32
        case t if t <:< definitions.LongTpe => DataTypes.int64
        case t if t <:< definitions.FloatTpe => DataTypes.float32
        case t if t <:< definitions.DoubleTpe => DataTypes.float64
        case _ => DataTypes.string
      })
  }

  /**
   * Check if the column name is valid
   * 检查列名是否有效
   * @param str Column name
   * @return Boolean indicating if the column name was valid
    *         布尔值,指示列名是否有效
   */
  def isValidColumnName(str: String): Boolean = {
    for (c <- str.iterator) {
      //用于char时,其代表ascii码值0x20,即字符空格' '
      if (c <= 0x20)
        return false
    }
    true
  }
}

///**
// * Extra schema if this is a vertex frame
// */
//case class VertexSchema(columns: List[Column] = List[Column](), label: String, idColumnName: Option[String] = None) extends GraphElementSchema {
//  require(hasColumnWithType(GraphSchema.vidProperty, DataTypes.int64), "schema did not have int64 _vid column: " + columns)
//  require(hasColumnWithType(GraphSchema.labelProperty, DataTypes.str), "schema did not have string _label column: " + columns)
//  if (idColumnName != null) {
//    //require(hasColumn(vertexSchema.get.idColumnName), s"schema must contain vertex id column ${vertexSchema.get.idColumnName}")
//  }
//
//  /**
//   * If the id column name had already been defined, use that name, otherwise use the supplied name
//   * @param nameIfNotAlreadyDefined name to use if not defined
//   * @return the name to use
//   */
//  def determineIdColumnName(nameIfNotAlreadyDefined: String): String = {
//    idColumnName.getOrElse(nameIfNotAlreadyDefined)
//  }
//
//  /**
//   * Rename a column
//   * @param existingName the old name
//   * @param newName the new name
//   * @return the updated schema
//   */
//  override def renameColumn(existingName: String, newName: String): Schema = {
//    renameColumns(Map(existingName -> newName))
//  }
//
//  /**
//   * Renames several columns
//   * @param names oldName -> newName
//   * @return new renamed schema
//   */
//  override def renameColumns(names: Map[String, String]): Schema = {
//    val newSchema = super.renameColumns(names)
//    val newIdColumn = idColumnName match {
//      case Some(id) => Some(names.getOrElse(id, id))
//      case _ => idColumnName
//    }
//    new VertexSchema(newSchema.columns, label, newIdColumn)
//  }
//
//  override def copy(columns: List[Column]): VertexSchema = {
//    new VertexSchema(columns, label, idColumnName)
//  }
//
//  def copy(idColumnName: Option[String]): VertexSchema = {
//    new VertexSchema(columns, label, idColumnName)
//  }
//
//  override def dropColumn(columnName: String): Schema = {
//    if (idColumnName.isDefined) {
//      require(idColumnName.get != columnName, s"The id column is not allowed to be dropped: $columnName")
//    }
//
//    if (GraphSchema.vertexSystemColumnNames.contains(columnName)) {
//      throw new IllegalArgumentException(s"$columnName is a system column that is not allowed to be dropped")
//    }
//
//    super.dropColumn(columnName)
//  }
//
//}
//
///**
// * Extra schema if this is an edge frame
// * @param label the label for this edge list
// * @param srcVertexLabel the src "type" of vertices this edge connects
// * @param destVertexLabel the destination "type" of vertices this edge connects
// * @param directed true if edges are directed, false if they are undirected
// */
//case class EdgeSchema(columns: List[Column] = List[Column](), label: String, srcVertexLabel: String, destVertexLabel: String, directed: Boolean = false) extends GraphElementSchema {
//  require(hasColumnWithType(GraphSchema.edgeProperty, DataTypes.int64), "schema did not have int64 _eid column: " + columns)
//  require(hasColumnWithType(GraphSchema.srcVidProperty, DataTypes.int64), "schema did not have int64 _src_vid column: " + columns)
//  require(hasColumnWithType(GraphSchema.destVidProperty, DataTypes.int64), "schema did not have int64 _dest_vid column: " + columns)
//  require(hasColumnWithType(GraphSchema.labelProperty, DataTypes.str), "schema did not have string _label column: " + columns)
//
//  override def copy(columns: List[Column]): EdgeSchema = {
//    new EdgeSchema(columns, label, srcVertexLabel, destVertexLabel, directed)
//  }
//
//  override def dropColumn(columnName: String): Schema = {
//
//    if (GraphSchema.edgeSystemColumnNamesSet.contains(columnName)) {
//      throw new IllegalArgumentException(s"$columnName is a system column that is not allowed to be dropped")
//    }
//
//    super.dropColumn(columnName)
//  }
//}

/**
 * Schema for a data frame. Contains the columns with names and data types.
  * 数据框的Schema,包含具有名称和数据类型的列
 *
 * @param columns the columns in the data frame 数据框中的列
 */
case class FrameSchema(columns: Seq[Column] = Vector[Column]()) extends Schema {

  override def copy(columns: Seq[Column]): FrameSchema = {
    new FrameSchema(columns)
  }

}

/**
 * Common interface for Vertices and Edges
  * 顶点和边的通用接口
 */
trait GraphElementSchema extends Schema {

  /** Vertex or Edge label
    * 顶点或边缘标签*/
  def label: String

}

object SchemaHelper {

  val config = ConfigFactory.load(this.getClass.getClassLoader)

  lazy val defaultInferSchemaSampleSize = config.getInt("trustedanalytics.sparktk.frame.schema.infer-schema-sample-size")

  /**
   * Appends letter to the conflicting columns
    * 将追加到冲突的列
   *
   * @param left list to which letter needs to be appended,需要附加哪个字母的列表
   * @param right list of columns which might conflict with the left list,可能与左侧列表冲突的列表列表
   * @param appendLetter letter to be appended
   * @return Renamed list of columns
   */
  def funcAppendLetterForConflictingNames(left: List[Column],
                                          right: List[Column],
                                          appendLetter: String,
                                          ignoreColumns: Option[List[Column]] = None): Seq[Column] = {

    var leftColumnNames = left.map(column => column.name)
    val rightColumnNames = right.map(column => column.name)
    val ignoreColumnNames = ignoreColumns.map(column => column.map(_.name)).getOrElse(List(""))

    left.map { column =>
      if (ignoreColumnNames.contains(column.name))
        column
      else if (right.map(i => i.name).contains(column.name)) {
        var name = column.name + "_" + appendLetter
        while (leftColumnNames.contains(name) || rightColumnNames.contains(name)) {
          name = name + "_" + appendLetter
        }
        leftColumnNames = leftColumnNames ++ List(name)
        Column(name, column.dataType)
      }
      else
        column
    }
  }

  /**
   * Join two lists of columns.
    * 加入两列的列表
   *
   * Join resolves naming conflicts when both left and right columns have same column names
    * 当左列和右列具有相同的列名时,加入将解决命名冲突
   *
   * @param leftColumns columns for the left side 左侧的列
   * @param rightColumns columns for the right side 右侧的列
   * @return Combined list of columns 列的组合列表
   */
  def join(leftColumns: Seq[Column], rightColumns: Seq[Column]): Seq[Column] = {

    val left = funcAppendLetterForConflictingNames(leftColumns.toList, rightColumns.toList, "L")
    val right = funcAppendLetterForConflictingNames(rightColumns.toList, leftColumns.toList, "R")

    left ++ right
  }

  /**
   * Resolve name conflicts (alias) in frame before sending it to spark data frame join
    * 在发送到Spark数据帧连接之前,解决frame中的名称冲突（别名）
   * @param left Left Frame
   * @param right Right Frame
   * @param joinColumns List of Join Columns (common to both frames)
   * @return Aliased Left and Right Frame
   */
  def resolveColumnNamesConflictForJoin(left: FrameRdd, right: FrameRdd, joinColumns: List[Column]): (FrameRdd, FrameRdd) = {
    val leftColumns = left.schema.columns.toList
    val rightColumns = right.schema.columns.toList
    val renamedLeftColumns = funcAppendLetterForConflictingNames(leftColumns, rightColumns, "L", Some(joinColumns))
    val renamedRightColumns = funcAppendLetterForConflictingNames(rightColumns, leftColumns, "R", Some(joinColumns))

    def createRenamingColumnMap(f: FrameRdd, newColumns: Seq[Column]): FrameRdd = {
      val oldColumnNames = f.schema.columnNames
      val newColumnNames = newColumns.map(_.name)
      f.selectColumnsWithRename(oldColumnNames.zip(newColumnNames).toMap)
    }

    (createRenamingColumnMap(left, renamedLeftColumns), createRenamingColumnMap(right, renamedRightColumns))
  }

  def checkValidColumnsExistAndCompatible(leftFrame: FrameRdd, rightFrame: FrameRdd, leftColumns: Seq[String], rightColumns: Seq[String]) = {
    //Check left join column is compatiable with right join column
    (leftColumns zip rightColumns).foreach {
      case (leftJoinCol, rightJoinCol) => require(DataTypes.isCompatibleDataType(
        leftFrame.schema.columnDataType(leftJoinCol),
        rightFrame.schema.columnDataType(rightJoinCol)),
        "Join columns must have compatible data types")
    }
  }
  /**
   *  Creates Frame schema from input parameters
   * 从输入参数创建框架模式
   * @param dataColumnNames list of column names for the output schema
    *                        list of column names for the output schema
   * @param dType uniform data type to be applied to all columns if outputVectorLength is not specified
    *              如果未指定outputVectorLength,则统一数据类型将应用于所有列
   * @param outputVectorLength optional parameter for a vector datatype
   * @return a new FrameSchema
   */
  def create(dataColumnNames: Seq[String],
             dType: DataType,
             outputVectorLength: Option[Long] = None): FrameSchema = {
    val outputColumns = outputVectorLength match {
      case Some(length) => List(Column(dataColumnNames.head, DataTypes.vector(length)))
      case _ => dataColumnNames.map(name => Column(name, dType))
    }
    FrameSchema(outputColumns.toVector)
  }

  def pythonToScala(pythonSchema: JArrayList[JArrayList[String]]): Schema = {
    import scala.collection.JavaConverters._
    val columns = pythonSchema.asScala.map { item =>
      val list = item.asScala.toList
      require(list.length == 2, "Schema entries must be tuples of size 2 (name, dtype)")
      Column(list.head, DataTypes.toDataType(list(1)))
    }.toVector
    FrameSchema(columns)
  }

  def scalaToPython(scalaSchema: FrameSchema): JArrayList[JArrayList[String]] = {
    val pythonSchema = new JArrayList[JArrayList[String]]()
    scalaSchema.columns.map {
      case Column(name, dtype) =>
        val c = new JArrayList[String]()
        c.add(name)
        c.add(dtype.toString)
        pythonSchema.add(c)
    }
    pythonSchema
  }

  private def mergeType(dataTypeA: DataType, dataTypeB: DataType): DataType = {
    val numericTypes = List[DataType](DataTypes.float64, DataTypes.float32, DataTypes.int64, DataTypes.int32)

    if (dataTypeA.equalsDataType(dataTypeB))
      dataTypeA
    else if (dataTypeA == DataTypes.string || dataTypeB == DataTypes.string)
      DataTypes.string
    else if (numericTypes.contains(dataTypeA) && numericTypes.contains(dataTypeB)) {
      if (numericTypes.indexOf(dataTypeA) > numericTypes.indexOf(dataTypeB))
        dataTypeB
      else
        dataTypeA
    }
    else if (dataTypeA.isVector && dataTypeB.isVector) {
      throw new RuntimeException(s"Vectors must all be the same length (found vectors with length " +
        s"${dataTypeA.asInstanceOf[DataTypes.vector].length} and ${dataTypeB.asInstanceOf[DataTypes.vector].length}).")
    }
    else
      // Unable to merge types, default to use a string
      //无法合并类型,默认使用字符串
      DataTypes.string
  }
  //合并数据类型
  private def mergeTypes(dataTypesA: Vector[DataType], dataTypesB: Vector[DataType]): Vector[DataType] = {
    if (dataTypesA.length != dataTypesB.length)
      throw new RuntimeException(s"Rows are not the same length (${dataTypesA.length} and ${dataTypesB.length}).")
    //zipWithIndex 返回对偶元组列表,第二个组成部分是元素下标
    dataTypesA.zipWithIndex.map {
      case (dataTypeA, index) => mergeType(dataTypeA, dataTypesB(index))
    }
  }
  //推动数据类型
  private def inferDataTypes(row: Row): Vector[DataType] = {
    row.toSeq.map(value => {
      value match {
        case null => DataTypes.int32
        case i: Int => DataTypes.int32
        case l: Long => DataTypes.int64
        case f: Float => DataTypes.float32
        case d: Double => DataTypes.float64
        case s: String => DataTypes.string
        case l: List[_] => DataTypes.vector(l.length)
        case a: Array[_] => DataTypes.vector(a.length)
        case s: Seq[_] => DataTypes.vector(s.length)
      }
    }).toVector
  }

  /**
   * Looks at the first x number of rows (depending on the sampleSize) to determine the schema of the data set.
   * 查看前1行(取决于样本大小)来确定数据集的schema模式
   * @param data RDD of data
   * @return Schema inferred from the data
   */
  def inferSchema(data: RDD[Row], sampleSize: Option[Int] = None, columnNames: Option[List[String]] = None): Schema = {

    val sampleSet = data.take(math.min(data.count.toInt, sampleSize.getOrElse(defaultInferSchemaSampleSize)))
    //foldLeft是从左开始计算,然后往右遍历,初始化参数和返回值的参数类型必须相同。
    val dataTypes = sampleSet.foldLeft(inferDataTypes(sampleSet.head)) {
      case (v: Vector[DataType], r: Row) => mergeTypes(v, inferDataTypes(r))
    }

    val schemaColumnNames = columnNames.getOrElse(List[String]())

    val columns = dataTypes.zipWithIndex.map {
      case (dataType, index) =>
        val columnName = if (schemaColumnNames.length > index) schemaColumnNames(index) else "C" + index
        Column(columnName, dataType)
    }

    FrameSchema(columns)
  }

  /**
   * Return if list of schema can be merged. Throw exception on name conflicts
    * 返回模式列表是否可以合并,抛出名称冲突的异常
   * @param schema List of schema to be merged
   * @return true if schemas can be merged, false otherwise
   */
  def validateIsMergeable(schema: Schema*): Boolean = {
    def merge(schema_l: Schema, schema_r: Schema): Schema = {
      require(schema_l.columnNames.intersect(schema_r.columnNames).isEmpty, "Schemas have conflicting column names." +
        s" Please rename before merging. Left Schema: ${schema_l.columnNamesAsString} Right Schema: ${schema_r.columnNamesAsString}")
      FrameSchema(schema_l.columns ++ schema_r.columns)
    }
    schema.reduce(merge)
    true
  }

}

/**
 * Schema for a data frame. Contains the columns with names and data types.
  * 数据框架的模式,包含具有名称和数据类型的列
 */
trait Schema {

  val columns: Seq[Column]
  //列不能为空
  require(columns != null, "columns must not be null")
  require({
    val distinct = columns.map(_.name).distinct
    distinct.length == columns.length
    //无效的模式,列名不能重复
  }, s"invalid schema, column names cannot be duplicated: $columns")
  //列名的名称索引
  private lazy val namesToIndices: Map[String, Int] = (for ((col, i) <- columns.zipWithIndex) yield (col.name, i)).toMap

  def copy(columns: Seq[Column]): Schema
  //获得所有列的名称
  def columnNames: Seq[String] = {
    columns.map(col => col.name)
  }

  /**
   * True if this schema contains the supplied columnName
    * 如果schema包含提供的列名columnName,则为true
   */
  def hasColumn(columnName: String): Boolean = {
    namesToIndices.contains(columnName)
  }

  /**
   * True if this schema contains all of the supplied columnNames
    * 如果此架构schema包含所有提供的columnNames,则为true
   */
  def hasColumns(columnNames: Seq[String]): Boolean = {
    //forall测试列表中所有元素的谓词是否成立
    columnNames.forall(hasColumn)
   }

  //如果此架构包含所有提供的columnNames,则为true
  def hasColumns(columnNames: Iterable[String]): Boolean = {
    //forall测试列表中所有元素的谓词是否成立
    columnNames.forall(hasColumn)
  }

  /**
   * True if this schema contains the supplied columnName with the given dataType
    * 如果此schema包含给定数据类型和列名columnName,则为true
   */
  def hasColumnWithType(columnName: String, dataType: DataType): Boolean = {
    hasColumn(columnName) && column(columnName).dataType.equalsDataType(dataType)
  }

  /**
   * Validate that the list of column names provided exist in this schema
   * throwing an exception if any does not exist.
    * 验证此schema中提供的列名列表是否存在引发异常(如果有)
   */
  def validateColumnsExist(columnNames: Iterable[String]): Iterable[String] = {
    columnIndices(columnNames.toSeq)
    columnNames
  }

  /**
   * Validate that all columns are of numeric data type
    * 验证所有列是数字数据类型
   */
  def requireColumnsOfNumericPrimitives(columnNames: Iterable[String]) = {
    columnNames.foreach(columnName => {
      require(hasColumn(columnName), s"column $columnName was not found")
      require(columnDataType(columnName).isNumerical, s"column $columnName should be of type numeric")
    })
  }

  /**
   * Validate that a column exists, and has the expected data type
    * 验证列是否存在,并具有预期的数据类型
   */
  def requireColumnIsType(columnName: String, dataType: DataType): Unit = {
    require(hasColumn(columnName), s"column $columnName was not found")
    require(columnDataType(columnName).equalsDataType(dataType), s"column $columnName should be of type $dataType")
  }

  /**
   * Validate that a column exists, and has the expected data type by supplying a custom checker, like isVectorDataType
    * 通过提供自定义检查程序(如isVectorDataType)来验证列是否存在,并具有预期的数据类型
   */
  def requireColumnIsType(columnName: String, dataTypeChecker: DataType => Boolean): Unit = {
    require(hasColumn(columnName), s"column $columnName was not found")
    val colDataType = columnDataType(columnName)
    require(dataTypeChecker(colDataType), s"column $columnName has bad type $colDataType")
  }
  //列为是否数值类型
  def requireColumnIsNumerical(columnName: String): Unit = {
    //获得列的类型
    val colDataType = columnDataType(columnName)
    require(colDataType.isNumerical, s"Column $columnName was not numerical. Expected a numerical data type, but got $colDataType.")
  }

  /**
   * Either single column name that is a vector or a list of columns that are numeric primitives
   * that can be converted to a vector.
   * 可以是向量的单列名称,也可以转换为向量的原始数字列的列表
   * List cannot be empty
   */
  def requireColumnsAreVectorizable(columnNames: Seq[String]): Unit = {
    //单个矢量列或需要一个或多个数字列
    require(columnNames.nonEmpty, "single vector column, or one or more numeric columns required")
    //数据列名称不能为空
    columnNames.foreach { c => require(CommonsStringUtils.isNotEmpty(c), "data columns names cannot be empty") }
    if (columnNames.size > 1) {
      requireColumnsOfNumericPrimitives(columnNames)
    }
    else {
      val colDataType = columnDataType(columnNames.head)
      require(colDataType.isVector || colDataType.isNumerical, s"column ${columnNames.head} should be of type numeric")
    }
  }

  /**
   * Column names as comma separated list in a single string
    * 列名称以逗号分隔列表中的单个字符串
   * (useful for error messages, etc)
   */
  def columnNamesAsString: String = {
    columnNames.mkString(", ")
  }
  //
  override def toString: String = columns.mkString(", ")

  // TODO: add a rename column method, since renaming columns shows up in Edge and Vertex schema it is more complicated

  /**
   * get column index by column name
   * 按列名获取列索引
   * Throws exception if not found, check first with hasColumn()
    * 如果找不到则抛出异常,先用hasColumn()检查
   *
   * @param columnName name of the column to find index
   */
  def columnIndex(columnName: String): Int = {
    //返回当前序列中第一个满足p条件的元素的索引,指定从from索引处开始
    val index = columns.indexWhere(column => column.name == columnName, 0)
    if (index == -1)
      throw new IllegalArgumentException(s"Invalid column name $columnName provided, please choose from: " + columnNamesAsString)
    else
      index
  }

  /**
   * Retrieve list of column index based on column names
   * 根据列的名列检索索引的列表
   * @param columnNames input column names 输入列名称
   */
  def columnIndices(columnNames: Seq[String]): Seq[Int] = {
    if (columnNames.isEmpty)
      //indices返回当前序列索引集合
      columns.indices.toList
    else {
      columnNames.map(columnName => columnIndex(columnName))
    }
  }

  /**
   * Copy a subset of columns into a new Schema
   * 将列的一个子集复制到一个新的Schema中
   * @param columnNames the columns to keep 要保留的列
   * @return the new Schema
   */
  def copySubset(columnNames: Seq[String]): Schema = {
    val indices = columnIndices(columnNames)
    //列的子集
    val columnSubset = indices.map(i => columns(i)).toVector
    copy(columnSubset)
  }

  /**
   * Produces a renamed subset schema from this schema
   * 从该模式生成重命名的子集模式
   * @param columnNamesWithRename rename mapping 重名称的Map
   * @return new schema
   */
  def copySubsetWithRename(columnNamesWithRename: Map[String, String]): Schema = {
    //保存的顺序的列的名称
    val preservedOrderColumnNames = columnNames.filter(name => columnNamesWithRename.contains(name))
    copySubset(preservedOrderColumnNames).renameColumns(columnNamesWithRename)
  }

  /**
   * Union schemas together, keeping as much info as possible.
   * 联合schemas在一起,保持尽可能多的信息
   * Vertex and/or Edge schema information will be maintained for this schema only
   * 顶点和/或边缘模式信息将仅针对该模式维护
   * Column type conflicts will cause error
    * 列类型冲突将导致错误
   */
  def union(schema: Schema): Schema = {
    // check for conflicts 检查冲突列
    val newColumns: Seq[Column] = schema.columns.filterNot(c => {
      hasColumn(c.name) && {
        require(hasColumnWithType(c.name, c.dataType), s"columns with same name ${c.name} didn't have matching types"); true
      }
    })
    //
    val combinedColumns = this.columns ++ newColumns
    copy(combinedColumns)
  }

  /**
   * get column datatype by column name
   * 给列名称获取列的数据类型
   * @param columnName name of the column
   */
  def columnDataType(columnName: String): DataType = {
    column(columnName).dataType
  }

  /**
   * Get all of the info about a column - this is a nicer wrapper than tuples
   * 获取关于列的所有信息 - 这是比元组更好的包装
   * @param columnName the name of the column 列的名称
   * @return complete column info 完整的列信息
   */
  def column(columnName: String): Column = {
    val i = namesToIndices.getOrElse(columnName, throw new IllegalArgumentException(s"No column named $columnName choose between: $columnNamesAsString"))
    columns(i)
  }

  /**
   * Convenience method for optionally getting a Column.
   * 方便的方法可以选择一个列
   * This is helpful when specifying a columnName was optional for the user.
    * 这对于为用户指定columnName是可选的时很有用
   *
   * @param columnName the name of the column
   * @return complete column info, if a name was provided 填写列信息,如果提供了名称
   */
  def column(columnName: Option[String]): Option[Column] = {
    columnName match {
      case Some(name) => Some(column(name))
      case None => None
    }
  }

  /**
   * Select a subset of columns.
   * 选择一个列的子集
   * List can be empty.
   */
  def columns(columnNames: Iterable[String]): Iterable[Column] = {
    //column获取关于列的所有信息 - 这是比元组更好的包装
    columnNames.map(column)
  }

  /**
   * Validates a Map argument used for renaming schema, throwing exceptions for violations
   * 验证重命名模式的Map参数,违规的抛出异常
   * @param names victimName -> newName
   */
  def validateRenameMapping(names: Map[String, String], forCopy: Boolean = false): Unit = {
    if (names.isEmpty)
      throw new IllegalArgumentException(s"Empty column name map provided.  At least one name is required")
    val victimNames = names.keys.toList
    //验证列是否存在
    validateColumnsExist(victimNames)
    //新的列名
    val newNames = names.values.toList
    //新的列名有重复列名抛出异常
    if (newNames.size != newNames.distinct.size) {
      //无效的新列名称不唯一
      throw new IllegalArgumentException(s"Invalid new column names are not unique: $newNames")
    }
    if (!forCopy) {
      //
      val safeNames = columnNamesExcept(victimNames)
      for (n <- newNames) {
        if (safeNames.contains(n)) {
          //
          throw new IllegalArgumentException(s"Invalid new column name '$n' collides with existing names which are not being renamed: $safeNames")
        }
      }
    }
  }

  /**
   * Get all of the info about a column - this is a nicer wrapper than tuples
   * 获取关于列的所有信息 - 这是比元组更好的包装
   * @param columnIndex the index for the column
   * @return complete column info
   */
  def column(columnIndex: Int): Column = columns(columnIndex)

  /**
   * Add a column to the schema
   * 向模式添加一列
   * @param column New column
   * @return a new copy of the Schema with the column added
    *         添加了列的Schema的新副本
   */
  def addColumn(column: Column): Schema = {
    if (columnNames.contains(column.name)) {
      throw new IllegalArgumentException(s"Cannot add a duplicate column name: ${column.name}")
    }
    //:+方法用于在尾部追加元素,+:方法用于在头部追加元素
    copy(columns = columns :+ column)
  }

  /**
   * Add a column to the schema
   * 向模式添加一列
   * @param columnName name 列的名称
   * @param dataType the type for the column.列是数据类型
   * @return a new copy of the Schema with the column added
    *       添加了列模式的新副本
   */
  def addColumn(columnName: String, dataType: DataType): Schema = {
    addColumn(Column(columnName, dataType))
  }

  /**
   * Add a column if it doesn't already exist.
   * 添加一个列,如果它不存在
   * Throws error if column name exists with different data type
    * 如果列名以不同的数据类型存在,则会引发错误
   */
  def addColumnIfNotExists(columnName: String, dataType: DataType): Schema = {
    //判断列的名称存在
    if (hasColumn(columnName)) {
      //验证列的数据类型float64
      requireColumnIsType(columnName, DataTypes.float64)
      this
    }
    else {
      addColumn(columnName, dataType)
    }
  }

  /**
   * Returns a new schema with the given columns appended.
    * 返回附加了给定列的新模式
   */
  def addColumns(newColumns: Seq[Column]): Schema = {
    //用于连接两个集合
    copy(columns = columns ++ newColumns)
  }

  /**
   * Add column but fix the name automatically if there is any conflicts with existing column names.
    * 添加列,但如果与现有列名称存在冲突,则自动修复名称
   */
  def addColumnFixName(column: Column): Schema = {
    this.addColumn(this.getNewColumnName(column.name), column.dataType)
  }

  /**
   * Add columns but fix the names automatically if there are any conflicts with existing column names.
    * 添加列,但如果与现有列名称存在冲突,则自动修复名称
   */
  def addColumnsFixNames(columnsToAdd: Seq[Column]): Schema = {
    var schema = this
    for {
      column <- columnsToAdd
    } {
      schema = schema.addColumnFixName(column)
    }
    schema
  }

  /**
   * Remove a column from this schema
   * 从这个模式中删除一列
   * @param columnName the name to remove
   * @return a new copy of the Schema with the column removed
   */
  def dropColumn(columnName: String): Schema = {
    copy(columns = columns.filterNot(column => column.name == columnName))
  }

  /**
   * Remove a list of columns from this schema
   * 从该模式中删除列表的列
   * @param columnNames the names to remove,删除列的名称
   * @return a new copy of the Schema with the columns removed
    *         删除了列的模式的新副本
   */
  def dropColumns(columnNames: Iterable[String]): Schema = {
    var newSchema = this
    if (columnNames != null) {
      columnNames.foreach(columnName => {
        newSchema = newSchema.dropColumn(columnName)
      })
    }
    newSchema
  }

  /**
   * Drop all columns with the 'ignore' data type.
   * 删除“ignore”数据类型的所有列,
   * The ignore data type is a slight hack for ignoring some columns on import.
    * 忽略数据类型对于忽略导入中的某些列是一个小小的破解
   */
  def dropIgnoreColumns(): Schema = {
    dropColumns(columns.filter(col => col.dataType.equalsDataType(DataTypes.ignore)).map(col => col.name))
  }

  /**
   * Remove columns by the indices
   * 通过索引删除列
   * @param columnIndices the indices to remove,删除的删除列
   * @return a new copy of the Schema with the columns removed
   */
  def dropColumnsByIndex(columnIndices: Seq[Int]): Schema = {
    //filterNot返回所有假设条件返回false的元素组成的新集合
    val remainingColumns = columns.zipWithIndex.filterNot {
      case (col, index) =>
        columnIndices.contains(index)
    }.map { case (col, index) => col }

    copy(remainingColumns)
  }

  /**
   * Convert data type for a column
   * 转换列的数据类型
   * @param columnName the column to change,改变的列
   * @param updatedDataType the new data type for that column,新的数据列的类型
   * @return the updated Schema
   */
  def convertType(columnName: String, updatedDataType: DataType): Schema = {
    val col = column(columnName)
    val index = columnIndex(columnName)
    //列的类型更新updated
    copy(columns = columns.updated(index, col.copy(dataType = updatedDataType)))
  }

  /**
   * Convert data type for a column
    * 转换列的数据类型
   * @param columnIndex index of the column to change
   * @param updatedDataType the new data type for that column
   * @return the updated Schema
   */
  def convertType(columnIndex: Int, updatedDataType: DataType): Schema = {
    val col = column(columnIndex)
    copy(columns = columns.updated(columnIndex, col.copy(dataType = updatedDataType)))
  }

  /**
   * Rename a column
   * 重命名一列
   * @param existingName the old name 旧的列名
   * @param newName the new name 新的列名
   * @return the updated schema 更新的schema
   */
  def renameColumn(existingName: String, newName: String): Schema = {
    //existingName旧的列名,newName新的列名
    renameColumns(Map(existingName -> newName))
  }

  /**
   * Renames several columns
   * 重命名几个列
   * @param names oldName -> newName
   * @return new renamed schema
   */
  def renameColumns(names: Map[String, String]): Schema = {
    validateRenameMapping(names)
    copy(columns = columns.map({
      //从名称列
      case found if names.contains(found.name) => found.copy(name = names(found.name))
      case notFound => notFound.copy()
    }))
  }

  /**
   * Re-order the columns in the schema.
   * 重新排列schema中的列
   * No columns will be dropped.  Any column not named will be tacked onto the end.
   * 没有列将被丢弃,任何未命名的列将被添加到最后
   * @param columnNames the names you want to occur first, in the order you want
    *                    你想要按照你想要的顺序先出现的名字
   * @return the updated schema
   */
  def reorderColumns(columnNames: Vector[String]): Schema = {
    validateColumnsExist(columnNames)
    val reorderedColumns = columnNames.map(name => column(name))
    val additionalColumns = columns.filterNot(column => columnNames.contains(column.name))
    //++ 该方法用于连接两个集合
    copy(columns = reorderedColumns ++ additionalColumns)
  }

  /**
   * Get the list of columns except those provided
   * 获取列列表,但提供的列除外
   * @param columnNamesToExclude columns you want to filter 要过滤的列
   * @return the other columns, if any
   */
  def columnsExcept(columnNamesToExclude: Seq[String]): Seq[Column] = {
    this.columns.filter(column => !columnNamesToExclude.contains(column.name))
  }

  /**
   * Get the list of column names except those provided
   * 获取列名,但提供的列名除外
   * @param columnNamesToExclude column names you want to filter 要过滤的列名称
   * @return the other column names, if any
   */
  def columnNamesExcept(columnNamesToExclude: Seq[String]): Seq[String] = {
    for { c <- columns if !columnNamesToExclude.contains(c.name) } yield c.name
  }

  /**
   * Convert the current schema to a FrameSchema.
   * 将当前模式转换为FrameSchema
   * This is useful when copying a Schema whose internals might be a VertexSchema
   * or EdgeSchema but you need to make sure it is a FrameSchema.
    * 这在复制内部可能是VertexSchema的Schema时非常有用或EdgeSchema,但你需要确保它是一个FrameSchema
   */
  def toFrameSchema: FrameSchema = {
    if (isInstanceOf[FrameSchema]) {
      this.asInstanceOf[FrameSchema]
    }
    else {
      new FrameSchema(columns)
    }
  }

  /**
   * create a column name that is unique, suitable for adding to the schema
   * (subject to race conditions, only provides unique name for schema as
   * currently defined)
    * 创建一个唯一的列名称,适合添加到schema(根据竞争条件,只为当前定义的模式提供唯一的名称)
   *
   * @param candidate a candidate string to start with, an _N number will be
   *                  append to make it unique
    *                  候选字符串的开始,一个_n数量将追加到使它与众不同
   * @return unique column name for this schema, as currently defined
    *         此schema的唯一列名,如目前定义的
   */
  def getNewColumnName(candidate: String): String = {
    var newName = candidate
    var i: Int = 0
    while (columnNames.contains(newName)) {
      newName = candidate + s"_$i"
      i += 1
    }
    newName
  }

}

