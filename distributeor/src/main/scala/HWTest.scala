import com.asiainfo.Conf
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Created by migle on 2016/8/26.
 *  测试在HW集群上是否能正常的运行且能读取到kafka中的数据
 */
object HWTest {
  def main(args: Array[String]) {

    if(args.length<1){
      System.err.println("please input topic")
      System.exit(-1)
    }
    //一个topic启一个app

    if(!(args(0).equals(Conf.consume_topic_netpay)||args(0).equals(Conf.consume_topic_order)||args(0).equals(Conf.consume_topic_usim))){
      System.err.println(s"only support three topics: ${Conf.consume_topic_netpay}\ ${Conf.consume_topic_order}  \ ${Conf.consume_topic_usim}")
      System.exit(-1)
    }

    val consumerFrom = Set(args(0))
    val sparkConf = new SparkConf().setAppName("AiQcdEvent") //.setMaster("local[2]") //.setMaster("spark://vm-centos-00:7077")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    val topicMap = consumerFrom.map((_, 2)).toMap
    val messages = KafkaUtils.createStream(ssc,Conf.zkhosts,Conf.groupid,topicMap);
    //KafkaUtils.createDirectStream()  在华为的平台上会报错估计是版本兼容问题
    val data = messages.map(x => x._2)
    data.print()
    ssc.start();
    ssc.awaitTermination();
  }
}