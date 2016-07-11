package dbraynard.nidum.httpservice

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

object ClusterBoot {


  def bootProxyRegions(clusterSystem: ActorSystem)
    : ActorRef = {


    //start a region proxy for the processor
    val regionProxyForProcessor = ClusterSharding(clusterSystem).startProxy(
      typeName = FibProcessor.shardName,
      role = None,
      extractEntityId = FibProcessor.idExtractor,
      extractShardId = FibProcessor.shardResolver
    )

    regionProxyForProcessor

  }

  def bootRegion(clusterSystem: ActorSystem) : ActorRef ={

    //start a region for the processor

    val cs = ClusterSharding(clusterSystem)

    val regionForProcessor: ActorRef = cs.start(
      FibProcessor.shardName,
      FibProcessor.props,
      ClusterShardingSettings(clusterSystem),
      FibProcessor.idExtractor,
      FibProcessor.shardResolver)

    regionForProcessor

  }


}
