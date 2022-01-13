package com.clevercloud.tools

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import com.sksamuel.pulsar4s.akka.streams.sink
import com.sksamuel.pulsar4s.circe.circeSchema
import com.sksamuel.pulsar4s._
import io.circe.generic.auto._
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.impl.auth.AuthenticationToken

import java.nio.file.Paths
import scala.concurrent.{ ExecutionContextExecutor, Future }

object Tool {
  implicit val system: ActorSystem = ActorSystem.create("QuickStart")
  implicit val ex: ExecutionContextExecutor = system.dispatchers.lookup("my-dispatcher")

  // the data model parsed as json and sent as json
  case class Data(
      fieldA: String,
      fieldB: Int)
  implicit val schema: Schema[Data] = circeSchema[Data]

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
    run("mytopicfile.txt")
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

  def run(fileName: String): Unit = {
    // for each topic in file
    topicsSource(fileName).map { topic =>
      // create the related reader
      val topicReader = sourceClient.reader[Data](
        ReaderConfig(
          topic = topic,
          startMessage = Message(MessageId.earliest)
        )
      )

      // create topic's data source from related reader
      val topicReaderSource = Source.fromIterator(() => ReaderIterator(topicReader).iterator)

      // create related producer
      val topicSink = () => sinkClient.producer[Data](ProducerConfig(topic))

      // create topic's data sink from related producer
      val topicReaderSink = sink(topicSink)

      val replicationStream = topicReaderSource.map { consumerMessage => ProducerMessage[Data](consumerMessage.value) }
        .runWith(topicReaderSink)

      replicationStream.map { _ => println(s"$topic has been replicated") }.recover(e =>
        println(s"$topic FAILED its replication", e)
      )
    }
  }

  def topicsSource(fileName: String): Source[Topic, Future[IOResult]] = {
    val file = Paths.get(fileName)

    FileIO
      .fromPath(file)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 2048, allowTruncation = false).map(_.utf8String))
      .map(Topic)
  }

  // make reader an iterator for convenience
  case class ReaderIterator[Data](reader: Reader[Data]) extends Iterable[ConsumerMessage[Data]] {

    override def iterator: Iterator[ConsumerMessage[Data]] = new Iterator[ConsumerMessage[Data]] {
      override def hasNext: Boolean = reader.hasMessageAvailable
      override def next(): ConsumerMessage[Data] = reader.next
    }
  }
}
