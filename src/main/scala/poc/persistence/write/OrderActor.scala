package poc.persistence.write

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.persistence._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import poc.persistence.BaseObjectActor

import scala.language.postfixOps

sealed trait StateOrder

object StateOrder {

  case object NONE extends StateOrder

  case object INIT extends StateOrder

  case object OK extends StateOrder

  case object CANCELLED extends StateOrder

  case object IN_PROGRESS extends StateOrder

}

trait WithOrder {
  val idOrder: String
}

trait WithUser {
  val idUser: Long
}

package Commands {

  case class InitializeOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder

  case class CancelOrder(idMsg: Long, idOrder: String, idUser: Long) extends WithUser with WithOrder

}

sealed trait Event {
  val timeStamp: Long
}

package Events {

  case class OrderInitialized(timeStamp: Long, order: Commands.InitializeOrder) extends Event

  case class OrderCancelled(timeStamp: Long, order: Commands.CancelOrder) extends Event

}

object OrderActor extends BaseObjectActor {

  protected def props = Props(classOf[OrderActor])

  val name = "orders"

  // the input for the extractShardId function
  // is the message that the "handler" receives
  protected def extractShardId: ShardRegion.ExtractShardId = {
    case msg: WithUser =>
      (msg.idUser % 2).toString
  }

  // the input for th extractEntityId function
  // is the message that the "handler" receives
  protected def extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: WithOrder =>
      (msg.idOrder, msg)
  }

}

class OrderActor extends PersistentActor with ActorLogging with AtLeastOnceDelivery {

  import ShardRegion.Passivate

  import scala.concurrent.duration._
  context.setReceiveTimeout(120 seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = self.path.name


  val receiveCommand: Receive = {
    case o: Commands.InitializeOrder =>
      log.info("Received InitializeOrder command! . I am {}", self.path)
      persist(Events.OrderInitialized(System.nanoTime(), o)) { e =>
        onEvent(e)
        log.info("Persisted OrderInitialized event!")
           sender ! "Sucessfully persisted OrderInitialized "
      }

    case o: Commands.CancelOrder =>
      if (state == StateOrder.IN_PROGRESS) {
        log.info("Received CancelOrder command!. I am {}", self.path)
        persist(Events.OrderCancelled(System.nanoTime(), o)) { e =>
          onEvent(e)
          log.info("Persisted OrderCancelled event!")
           sender ! "Sucessfully persisted OrderCancelled "
        }

      } else {
        // Sometimes you may want to persist an event OrderCancellationRequestRejected
        log.info("Command rejected!")
        sender ! "Cannot cancel order if it is not in progress"
      }

    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop => context.stop(self)

  }
  var state: StateOrder = StateOrder.NONE

  def receiveRecover = {
    case e: Event =>
      log.info("Received an event I need to process for recovery")
      onEvent(e)
    case _ =>
  }

  def onEvent(e: Event) = {
    log.info("Changing internal state in response to an event!")
    e match {
      case e: Events.OrderInitialized =>
        state = StateOrder.IN_PROGRESS
      case e: Events.OrderCancelled =>
        state = StateOrder.CANCELLED
    }
  }

}

import akka.persistence.journal.{Tagged, WriteEventAdapter}

class OrderTaggingEventAdapter extends WriteEventAdapter {
  
  override def toJournal(event: Any): Any = event match {
    case e: Events.OrderInitialized =>
      //~ println("########## Event Adapter Works ############")
      Tagged(e, Set("byUser"))
    case e: Events.OrderCancelled =>
      //~ println("########## Event Adapter Works ############")
      Tagged(e, Set("byUser"))
  }

  override def manifest(event: Any): String = ""
}






