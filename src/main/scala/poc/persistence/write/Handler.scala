package poc.persistence.write

import akka.actor._

class Handler extends Actor with ActorLogging {

  def receive: Receive = {
    case msg: WithOrder => {
      getOrderChild(msg.idOrder) ! msg
      log.info("message with idOrder = {}", msg)
    }
    case s: String =>
      log.info(s)
  }

  def getOrderChild(id: String): ActorRef = context.child(id).getOrElse(context.actorOf(OrderActor.props(id), id))


}


