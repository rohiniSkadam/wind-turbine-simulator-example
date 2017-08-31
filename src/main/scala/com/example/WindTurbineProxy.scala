import WindTurbineClusterShards.system
import WindTurbineSupervisor.StartSimulator
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.model.StatusCode
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by synerzip on 28/8/17.
  */
object WindTurbineProxy extends App {
  val port = args(0)

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [WindTurbineProxy]"))
    .withFallback(ConfigFactory.load())

  implicit val system = ActorSystem.create("ClusterActorSystem", config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val windTurbineShardRegionProxy: ActorRef = ClusterSharding(system).startProxy(
    typeName = "WindTurbineSupervisorShardRegion",
    role = None,
    extractEntityId = WindTurbineClusterConfig.extractEntityId,
    extractShardId = WindTurbineClusterConfig.extractShardId
  )



  Source(1 to 100000)
    .throttle(
      elements = 100,
      per = 1 second,
      maximumBurst = 100,
      mode = ThrottleMode.shaping
    )
    .map { _ =>
      val id = java.util.UUID.randomUUID.toString
      windTurbineShardRegionProxy ! EntityEnvelope(id, StartSimulator)
    }
    .runWith(Sink.ignore)

  sys.addShutdownHook {
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
