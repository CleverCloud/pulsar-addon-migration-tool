package com.clevercloud.tools

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import com.sksamuel.pulsar4s.akka.streams.sink
import com.sksamuel.pulsar4s._
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.impl.auth.AuthenticationToken

import java.nio.file.Paths
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

object Tool {
  implicit val system: ActorSystem = ActorSystem.create("QuickStart")
  implicit val ex: ExecutionContextExecutor = system.dispatchers.lookup("migration-dispatcher")

  implicit val schema: Schema[String] = Schema.STRING

  def main(args: Array[String]): Unit = {
    // the topics file
    /*
    it should be formatted as follow:

    You can retrieve it using pulsarctl/pulsar-admin or even the pulsar REST API.

    persistent://<tenant>/<namespace>/<topic>
    persistent://<tenant>/<namespace>/<topic>
    persistent://<tenant>/<namespace>/<topic>
    persistent://<tenant>/<namespace>/<topic>
    ...
     */
    val fileName = "/tmp/topics_list.txt"
    println(s"Will run with topics in file $fileName")
    val resultatF: Future[_] = run(fileName)
    Await.result(resultatF, Duration.Inf)
    println("replication over")
  }

  // the source addon pulsar client configuration
  val sourceClient: PulsarClient = PulsarClient(
    PulsarClientConfig(
      serviceUrl = "pulsar+ssl://c1-pulsar-clevercloud-customers.services.clever-cloud.com:2002",
      authentication = Some(new AuthenticationToken("<ADDON_PULSAR_TOKEN_SOURCE>"))
    )
  )

  // the sink addon pulsar client configuration
  val sinkClient: PulsarClient = PulsarClient(
    PulsarClientConfig(
      serviceUrl = "pulsar+ssl://c2-pulsar-clevercloud-customers.services.clever-cloud.com:2002",
      authentication = Some(new AuthenticationToken("<ADDON_PULSAR_TOKEN_SINK>"))
    )
  )

  def run(fileName: String): Future[_] = {
    // for each topic in file
    topicsSource(fileName).map { topic =>
      println(s"replicating $topic")

      // create the related reader
      val topicReader = sourceClient.reader[String](
        ReaderConfig(
          topic = topic,
          startMessage = Message(MessageId.earliest)
        )
      )

      // create topic's data source from related reader
      val topicReaderSource = Source.fromIterator(() => ReaderIterator(topicReader).iterator)

      // create related producer
      val topicSink = sinkClient.producer[String](ProducerConfig(topic))

      // create topic's data sink from related producer
      val topicProducerSink = sink(() => topicSink)

      val replicationStream = topicReaderSource.map { consumerMessage =>
        DefaultProducerMessage[String](
          key = consumerMessage.key,
          props = consumerMessage.props,
          value = consumerMessage.value,
          eventTime = Some(consumerMessage.eventTime)
        )
      }.runWith(topicProducerSink)

      // manage replication stream termination
      replicationStream.map { _ =>
        for {
          _ <- topicReader.closeAsync
          _ <- topicSink.closeAsync
        } yield {
          println(s"$topic has been replicated")
        }
      }.recover { e =>
        for {
          _ <- topicReader.closeAsync
          _ <- topicSink.closeAsync
        } yield {
          println(s"$topic FAILED its replication", e)
        }
      }
    }.run()
  }

  def topicsSource(fileName: String): Source[Topic, Future[IOResult]] = {
    val file = Paths.get(fileName)

    FileIO
      .fromPath(file)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 2048, allowTruncation = false).map(_.utf8String))
      .map(Topic)
  }

  // make reader an iterator for convenience
  case class ReaderIterator[String](reader: Reader[String]) extends Iterable[ConsumerMessage[String]] {

    override def iterator: Iterator[ConsumerMessage[String]] = new Iterator[ConsumerMessage[String]] {
      override def hasNext: Boolean = reader.hasMessageAvailable
      override def next(): ConsumerMessage[String] = reader.next
    }
  }
}
