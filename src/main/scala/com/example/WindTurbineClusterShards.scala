import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by synerzip on 28/8/17.
  */


object WindTurbineClusterShards extends App {
  val port = args(0)

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [WindTurbineSimulator]"))
    .withFallback(ConfigFactory.load())

  implicit val system = ActorSystem.create("ClusterActorSystem", config)
  implicit val materializer = ActorMaterializer()

  val windTurbineShardRegion = ClusterSharding(system).start(
    typeName = "WindTurbineSupervisorShardRegion",
    entityProps = WindTurbineSupervisor.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = WindTurbineClusterConfig.extractEntityId,
    extractShardId = WindTurbineClusterConfig.extractShardId
  )

  sys.addShutdownHook {
    Await.result(system.whenTerminated, Duration.Inf)
  }
}