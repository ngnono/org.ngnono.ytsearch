import com.github.seratch.scalikesolr._
import com.yintai.search.tools4solr.StatusSearchCheck
import java.net.URL
import org.rogach.scallop.ScallopConf


/**
 * Created with IntelliJ IDEA.
 * User: ngnono
 * Date: 13-9-11
 * Time: 下午1:38
 * To change this template use File | Settings | File Templates.
 */
object Program {
  def main(args: Array[String]) {
    //    helloSolr()
    val conf = new Conf(args)
    println(conf.host())
    val host = conf.host()
    val run = new StatusSearchCheck(host + "/solr/searchstatics", host + "/solr/product")
    run.run()
  }

  def helloSolr() {
    val serverStr = "http://10.32.34.117:8080/solr/searchstatics"
    val client = Solr.httpServer(new URL(serverStr)).newClient()
    val request = new QueryRequest(writerType = WriterType.JavaBinary, query = com.github.seratch.scalikesolr.request.query.Query("*:*"))
    val response = client.doQuery(request)
    println(response.responseHeader)
    println(response.response)
    response.response.documents foreach {
      case doc => {
        println(doc.get("pinyin").toString())
        println(doc.get("word").toString())
        println(doc.get("search_count").toIntOrElse(0))
        println(doc.get("search_result").toIntOrElse(0))
      }
    }
  }

}


class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val host = opt[String](required = true,descr = "input solr host e:http://10.32.34.117:8080")
}
