import WindTurbineSimulator.{ConnectionFailure, FailedUpgrade, Upgraded, WindTurbineSimulatorException}
import akka.actor.{Actor, ActorLogging, Props, Terminated}
import akka.http.scaladsl.model.StatusCode
import akka.io.Tcp.Connected
import akka.stream.ActorMaterializer

/**
  * Created by synerzip on 23/8/17.
  */
object WindTurbineSimulator {
  def props(id: String, endpoint: String)(implicit materializer: ActorMaterializer) =
    Props(classOf[WindTurbineSimulator], id, endpoint, materializer)

  case class WindTurbineSimulatorException (id:String) extends Exception
  final case object Upgraded
  final case object Connected
  final case object Terminated
  final case class ConnectionFailure(ex: Throwable)
  final case class FailedUpgrade(statusCode: StatusCode)
}

class WindTurbineSimulator(id: String, endpoint: String)
                          (implicit materializer: ActorMaterializer)
  extends Actor with ActorLogging {
  implicit private val system = context.system
  implicit private val executionContext = system.dispatcher

  val webSocket = WebSocketClient(id, endpoint, self)

  override def postStop() = {
    log.info(s"$id : Stopping WebSocket connection")
    webSocket.killSwitch.shutdown()
  }

  override def receive: Receive = {
    case Upgraded =>
      log.info(s"$id : WebSocket upgraded")
    case FailedUpgrade(statusCode) =>
      log.error(s"$id : Failed to upgrade WebSocket connection : $statusCode")
      throw WindTurbineSimulatorException(id)
    case ConnectionFailure(ex) =>
      log.error(s"$id : Failed to establish WebSocket connection $ex")
      throw WindTurbineSimulatorException(id)
    case Connected =>
      log.info(s"$id : WebSocket connected")
      context.become(running)
  }

  def running: Receive = {
    case Terminated =>
      log.error(s"$id : WebSocket connection terminated")
      throw WindTurbineSimulatorException(id)
  }
}