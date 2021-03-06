package poc.persistence


object UtilActorSystem extends App {
  
  import akka.actor.{ActorSystem,Terminated}
  import akka.cluster.sharding.ShardRegion
  import scala.concurrent.Future
  import scala.util.{Success, Failure}
  import scala.concurrent.Promise
  import poc.persistence.write.OrderActorFunctional
  import poc.persistence.read.UserActorFunctional
  

  def terminate( implicit system : ActorSystem ) : Future[Terminated]= {
    
    import akka.cluster._
    import scala.concurrent.duration._
    
    UserActorFunctional.receiver( system ) ! ShardRegion.GracefulShutdown
    OrderActorFunctional.receiver( system ) ! ShardRegion.GracefulShutdown
    
    val delay = Duration.create(5, SECONDS)
    
    val promise : Promise[Terminated] = Promise()
    
    system.scheduler.scheduleOnce( delay ) {
        val cluster = Cluster.get( system )
        cluster.registerOnMemberRemoved( systemTerminate( promise, system ) )
        cluster.leave(cluster.selfAddress)
      } ( system.dispatcher )
      
    promise.future
      
  } 
  
  private def systemTerminate( promise : Promise[Terminated] , system : ActorSystem ) : Unit = {
    
    system.terminate().onComplete { 
      case Success(msg) =>
        promise.success( msg )
      case Failure(e) =>
        promise.failure( e )
      
    }( system.dispatcher )
    
  }
  
}


