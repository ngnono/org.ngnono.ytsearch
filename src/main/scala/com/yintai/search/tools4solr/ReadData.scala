package com.yintai.search.tools4solr

import com.github.seratch.scalikesolr._
import java.net.URL
import com.github.seratch.scalikesolr.request.query.{MaximumRowsReturned, StartRow, Query}
import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer
import java.io.{PrintWriter, FileWriter, File}
import java.text.SimpleDateFormat


/**
 * Created with IntelliJ IDEA.
 * User: ngnono
 * Date: 13-9-11
 * Time: 下午4:24
 * To change this template use File | Settings | File Templates.
 */
class StatusSearchCheck(serverPath: String, productServerPath: String) extends SolrRequest(serverPath) {

  val productClient = Solr.httpServer(new URL(productServerPath)).newClient()

  def run() {

    val data = new ArrayBuffer[StatusSearchModel]

    data += new StatusSearchModel()



    get(data, 1, 1000)

    //    println(data.length)
    //    for (d <- data)
    //      println(d.pinyin)

    //data.append(new StatusSearchModel("", "这ge也能搜到ma？", 100, 10))


    val checkedData = check(data)


    saveExec(data, checkedData)

    if (data.length == checkedData.length) {
      println("org data.length = zero data.length,del stop.....")
    }
    else if (checkedData.length == 0) {
      println("zero data.length=0")
    }
    else {
      del(checkedData)
    }
  }


  def del(data: ArrayBuffer[StatusSearchModel]) {

    println("del zero data.length=" + data.length)
    val list = data.map(v => v.word).toList

    val request = new DeleteRequest(uniqueKeysToDelete = list, writerType = WriterType.JavaBinary)
    val response = client.doDeleteDocuments(request)
    client.doCommit(new UpdateRequest())

  }

  def get(data: ArrayBuffer[StatusSearchModel], startNum: Int, rows: Int) {
    var start = startNum
    var loop = true
    while (loop) {
      val request = new QueryRequest(writerType = WriterType.JavaBinary, query = Query("*:*"), startRow = StartRow(start = start), maximumRowsReturned = MaximumRowsReturned(rows = rows))
      val response = client.doQuery(request)
      val numFound = response.response.numFound

      loop = if (numFound < 0) false else true
      start += 1
      val page = new PageModel(start, rows, numFound)
      loop = if (page.isPaged) true else false

      //println("start:" + start + ",numFound:" + numFound + ",isPaged:" + page.isPaged)

      val result = parseResponse(response)
      data.appendAll(result)
      //println(data.length)
    }
  }

  def check(data: ArrayBuffer[StatusSearchModel]): ArrayBuffer[StatusSearchModel] = {
    val start = 0
    val rows = 0

    var checkedData = new ArrayBuffer[StatusSearchModel]
    for (d <- data) {
      var numFound = 0
      if (!d.word.isEmpty) {
        val request = new QueryRequest(writerType = WriterType.JavaBinary, query = Query("text:" + d.word), startRow = StartRow(start = start), maximumRowsReturned = MaximumRowsReturned(rows = rows))
        val response = productClient.doQuery(request)
        numFound = response.response.numFound
      }


      //      if (numFound == 0)
      //        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>word:" + d.word + ",numfound:" + numFound)

      //println("word:" + d.word + ",numfound:" + numFound)

      d.search_result = numFound

      //checkedData.append()

      if (numFound == 0) {
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>word:" + d.word + ",numfound:" + d.search_result)

        checkedData.append(d)
      }



      //println("<<<<d:", checkedData.length)

    }

    checkedData
  }


  def saveExec(orgData: ArrayBuffer[StatusSearchModel], checked: ArrayBuffer[StatusSearchModel]) {
    val org = "output/org/"
    val zero = "output/zero/"
    val have = "output/have/"

    save(checked, zero)
    save(orgData -- checked, have)
    save(orgData, org)

  }

  def save(data: ArrayBuffer[StatusSearchModel], fileParentPath: String) {

    println("datacount:" + data.length + ",path:" + fileParentPath)
    val fileName = () => {
      val dt = new java.util.Date(System.currentTimeMillis())
      val fmt = new SimpleDateFormat("yyyyMMddHHmmss")

      fmt.format(dt) + ".txt"
    }

    val filePath = FileUtil.getCurrentDirectory + "/" + fileParentPath
    val fileFullName = filePath + fileName()

    println(fileFullName)


    val fileData = () => {
      val sb = new StringBuilder()
      for (d <- data) {
        sb.append(d.word)
        sb.append(",")
        sb.append(d.pinyin)
        sb.append(",")
        sb.append(d.search_count)
        sb.append(",")
        sb.append(d.search_result)
        sb.append(System.getProperty("line.separator"))
      }

      sb.toString()
    }

    FileUtil.mkDirs(filePath)
    FileUtil.createFile(fileFullName, true)
    FileUtil.writeToFile(fileFullName, fileData())
  }
}


object FileUtil {
  def getCurrentDirectory = new File(".").getCanonicalPath


  def using[A <: {def close() : Unit}, B](param: A)(f: A => B): B = try {
    f(param)
  } finally {
    param.close()
  }

  def createFile(fileName: String, isDelAndCreate: Boolean) = {
    val f = new File(fileName)
    if (isDelAndCreate)
      delFile(fileName)

    f.createNewFile()

  }

  def writeToFile(fileName: String, data: String) = using(new FileWriter(fileName)) {
    fileWriter => fileWriter.write(data)
  }

  def appendToFile(fileName: String, textData: String) = using(new FileWriter(fileName, true)) {
    fileWriter => using(new PrintWriter(fileWriter)) {
      printWriter => printWriter.println(textData)
    }
  }

  def existsFile(fileName: String): Boolean = {
    val f = new File(fileName)
    return f.exists()
  }

  def delFile(fileName: String) = {

    val f = new File(fileName)

    f.deleteOnExit()

  }

  def mkDirs(filePath: String) = {
    val p = new File(filePath)
    if (!p.exists())
      p.mkdirs()
  }

}


object StringUtils {
  val Empty = ""
}

class PageModel(page: Int, size: Int, count: Int, pageCount: Int) {
  @BeanProperty
  val pageIndex: Int = page

  @BeanProperty
  val pageSize: Int = size

  @BeanProperty
  val totalCount: Int = count

  @BeanProperty
  val totalPage: Int = totalCount / pageSize + 1

  @BeanProperty
  val isPaged: Boolean = if (pageIndex <= totalPage) true else false

  def this(page: Int, size: Int, total: Int) = {

    this(
      if (page <= 0) 1 else page, if (size < 0) 0 else size, if (total < 0) 0 else total, 0
    )
  }
}

class StatusSearchModel(
                         var pinyin: String = StringUtils.Empty,
                         var word: String = StringUtils.Empty,
                         var search_count: Int = 0,
                         var search_result: Int = 0
                         ) {
  def this() = {
    this(StringUtils.Empty, StringUtils.Empty, 0, 0)
  }
}


class SolrRequest(serverUrl: String) {
  var client = Solr.httpServer(new URL(serverUrl)).newClient()

  def parseResponse(response: QueryResponse): ArrayBuffer[StatusSearchModel] = {
    val result = new ArrayBuffer[StatusSearchModel]

    response.response.documents foreach {
      case doc => {
        val model = doc.bind[StatusSearchModel](classOf[StatusSearchModel])
        result.append(model)
      }
    }

    result
  }
}
