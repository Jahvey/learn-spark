## ��� 
��������򵥵Ĵ���Ϊ�ο������𲽷���Spark

    val conf = new SparkConf()
                            .setAppName("Simple Application")  
                            .setMaster("spark://192.168.242.130:7077")  
    val sc = new SparkContext(conf)  
    val rdd = sc.parallelize(1 to 10000)
    //val rdd = sc.textFile()
    rdd.count()    

## SparkContext ���� 

### SparkConf

        val sc = new SparkContext(conf) 

SparkConf�ṩ������Ϣ�� SparkContext�������¹�����  

    def this() = this(new SparkConf())

>>�޲ε�SparkConf����ϵͳ�����ж�ȡ������Ϣ *Spark����ʱ�����Shell��������*

    if (loadDefaults) {
        // Load any spark.* system properties
        for ((k, v) <- System.getProperties.asScala if k.startsWith("spark.")) {
          settings(k) = v
        }
     }


### �������캯��
N�����캯�� ......


### �����ж�

����һ�����������ж�֮���



### LiveListenerBus
    // An asynchronous listener bus for Spark events
    private[spark] val listenerBus = new LiveListenerBus   //TODO LiveListenerBus
>> ����SparkListenerEvents����ע���SparkListener

   _jobProgressListener = new JobProgressListener(_conf)
    listenerBus.addListener(jobProgressListener)



### SparkEnv
����SparkEnv
    // Create the Spark execution environment (cache, map output tracker, etc)
    _env = createSparkEnv(_conf, isLocal, listenerBus)
    SparkEnv.set(_env)


###  SparkUI
SparkUI����
    
    // Initialize the Spark UI, registering all associated listeners
    private[spark] val ui: Option[SparkUI] =
    if (conf.getBoolean("spark.ui.enabled", true)) {
      Some(new SparkUI(this))
    } else {
      // For tests, do not enable the UI
      None
    }

    // Bind the UI before starting the task scheduler to communicate
    // the bound port to the cluster manager properly
    ui.foreach(_.bind())


### Scheduler����
- [ ] DAGScheduler,TaskScheduler,SchedulerBackend����֮ǰ�Ĺ�ϵ��
>>DAGScheduler����stage�ĵ��ȣ�TaskScheduler����task�ĵ��ȣ�SchedulerBackend��TaskScheduler�ĺ�ˣ���ͬ����ʽ�в�ͬʵ�֡�
����action���ӣ��ͻᴴ��һ��JOB��JOBͬ���ͻᱻ�ύ��DAGScheduler�DAGScheduler���JOB����Ϊ���stag��Ȼ��ÿ��stag����һ��taskSet(������ÿ��task),���taskSet�ύ��TaskScheduler�TaskScheduler���taskSet���ÿ��taskr�ύ��executor��ִ��(task�����㷨)

 val (sched, ts) = SparkContext.createTaskScheduler(this, master)
    _schedulerBackend = sched
    _taskScheduler = ts
    _dagScheduler = new DAGScheduler(this)
    _heartbeatReceiver.ask[Boolean](TaskSchedulerIsSet)

    // start TaskScheduler after taskScheduler sets DAGScheduler reference in DAGScheduler's
    // constructor
    _taskScheduler.start()

createTaskScheduler�и��ݲ�ͬ�Ĳ���Master URL����ʽ���ɲ�ͬ��SchedulerBackend��TaskScheduler�����

- [ ] Job����stage
- [ ] Task�����㷨

#### TaskSchedulerImpl

taskScheduler.start()

�˷���ʵ���ǵ�����SchedulerBackend.start()���������
��SchedulerBackend.start()��ͬ�Ĳ���ʽ����ʽ��ͬ��Local��ʽֻ�Ǵ�����Actor
����Ⱥ��ʽ������AppClient.start(),��Masterע�� **Master.RegisterApplication**

APPClient

  def tryRegisterAllMasters() {
      for (masterUrl <- masterUrls) {
        logInfo("Connecting to master " + masterUrl + "...")
        val actor = context.actorSelection(Master.toAkkaUrl(masterUrl))
        actor ! RegisterApplication(appDescription)
      }
    }


**AKKA��Actor��preStart()**

*������AKKA��ʹ�ã�SO��AKKA�ȸ�����* 



## MetricsSystem

  val metricsSystem = env.metricsSystem

  // The metrics system for Driver need to be set spark.app.id to app ID.
  // So it should start after we get app ID from the task scheduler and set spark.app.id.
  metricsSystem.start()


## RDD����

>>A Resilient Distributed Dataset (RDD), the basic abstraction in Spark. Represents an immutable, partitioned collection of elements that can be operated on in parallel. This class contains the basic operations available on all RDDs, such as `map`, `filter`, and `persist`

RDD  && Partition
����RDD   SparkContext.textFile,parallelize ��.......


  override def getPartitions: Array[Partition] = {
    val slices = ParallelCollectionRDD.slice(data, numSlices).toArray
    slices.indices.map(i => new ParallelCollectionPartition(id, i, slices(i))).toArray
  }

����Partition

[Job �߼�ִ��ͼ](https://github.com/JerryLead/SparkInternals/blob/master/markdown/2-JobLogicalPlan.md)

## Actionִ��


RDD:
  def count(): Long = sc.runJob(this, Utils.getIteratorSize _).sum


SpackContext
    runjon(.)
    runjon(..)
    runjon(...)
    .....

dagScheduler.runJob(rdd, cleanedFunc, partitions, callSite, allowLocal,
      resultHandler, localProperties.get)   


DAGScheduler

  def runJob[T, U: ClassTag](
      rdd: RDD[T],
      func: (TaskContext, Iterator[T]) => U,
      partitions: Seq[Int],
      callSite: CallSite,
      allowLocal: Boolean,
      resultHandler: (Int, U) => Unit,
      properties: Properties = null)
  {
    val start = System.nanoTime
    val waiter = submitJob(rdd, func, partitions, callSite, allowLocal, resultHandler, properties)
    waiter.awaitResult() match {
      case JobSucceeded => {
        logInfo("Job %d finished: %s, took %f s".format
          (waiter.jobId, callSite.shortForm, (System.nanoTime - start) / 1e9))
      }
      case JobFailed(exception: Exception) =>
        logInfo("Job %d failed: %s, took %f s".format
          (waiter.jobId, callSite.shortForm, (System.nanoTime - start) / 1e9))
        throw exception
    }
  }





����ṹ

������ηֽ�
������κϲ�
������ε���

TaskScheduler
SchedulerBackend
ExecutorBackend
Executor


ÿ�� Worker �ϴ���һ�����߶�� ExecutorBackend ���̡�ÿ�����̰���һ�� Executor���󣬸ö������һ���̳߳أ�ÿ���߳̿���ִ��һ�� task


## Actionִ��










## DAGScheduler




SparkContext.runJob   






## TODO  
### broadcast



### ���� 
#### Scala

#### AKKA

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorSystem, ActorLogging, Actor}

/**
 * Created by migle on 2014/10/28.
 */
object LocalAKKA {

  case class HiMsg(msg: String)

  case class GoodMsg(msg: String)

  val system = ActorSystem("testakka")

  class MsgActor extends Actor with ActorLogging {
    override def preStart(): Unit = {
      println("pre start:" + self.path)
    }

    override def receive: Receive = {
      case HiMsg(msg) => log.warning(" msg:" + msg + " FROM " + sender.path + " TO  " + self.path)
        //����һ���µ�Actor,ע�����ַ�ʽ������Actor��·��
        //val actor2 = system.actorOf(Props[MsgActor], name = "actor2")
        val actor2  = context.actorOf(Props[MsgActor], name = "actor2")

        actor2 ! new GoodMsg("good news")

      case GoodMsg(msg) => log.warning(" msg:" + msg + " FROM " + sender.path + " TO  " + self.path)
        //�������߷���һ����Ϣ��������Ϻ�ر�ϵͳ
        sender ! "$$$$$$$!"
        // ��Ϣ������Ϻ�Ż�ر�ϵͳ
        system.shutdown()

      case _ => log.warning("unknow msg:" + " FROM " + sender.path + " TO  " + self.path)
    }

    override def postStop(): Unit = {
      println("post stop:" + self.path)
    }

  }

  def main(args: Array[String]) {
    //val system = ActorSystem("testakka")

    //ͬһ�����actor�����ֲ����ظ����Ҳ�����$��ͷ 
    //Actors are automatically started asynchronously when created
    val actor1 = system.actorOf(Props[MsgActor], name = "actor1")
    //val actor2 = system.actorOf(Props[MsgActor],name="actor2")
    //actor1 ! "hi! Msg Actor"

    actor1 ! new HiMsg("hi!")
    //system.shutdown()
    system.awaitTermination()
  }
}



//TODO
RemoteAKKA







���һ��Actor�ж�����ԣ�����ͨ�����·�ʽ������ֵ��
�����ʵ�����Ϣ
�ŵ����캯����
��дpreStart����


http://my.oschina.net/jingxing05/blog/287462


http://www.jianshu.com/p/bc1e9755143e




