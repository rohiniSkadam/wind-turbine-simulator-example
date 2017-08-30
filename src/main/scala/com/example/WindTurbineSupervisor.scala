import WindTurbineSupervisor.StartClient
import akka.actor.{Actor, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import scala.concurrent.duration._


object WindTurbineSupervisor {
  final case object StartSimulator
  final case class StartClient(id: String)
  def props(implicit materializer: ActorMaterializer) =
    Props(classOf[WindTurbineSupervisor], materializer)
}

class WindTurbineSupervisor(implicit materializer: ActorMaterializer) extends Actor {
  override def preStart(): Unit = {
    self ! StartClient(self.path.name)
  }

  def receive: Receive = {
    case StartClient(id) =>
      val supervisor = BackoffSupervisor.props(
        Backoff.onFailure(
          WindTurbineSimulator.props(id, "Config.endpoint"),
          childName = id,
          minBackoff = 1.second,
          maxBackoff = 30.seconds,
          randomFactor = 0.2
        )
      )
      context.actorOf(supervisor, name = s"$id-backoff-supervisor")
      context.become(running)
  }

  def running: Receive = Actor.emptyBehavior
}