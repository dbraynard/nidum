package dbraynard.nidum.httpservice



object NativeServiceDto {

  final case class FibResultDto(serviceId:String,input:Int,output:String,response:String="FibResultDto")

  final case class ServiceError(serviceId:String,msg:String,response:String = "ServiceError")


  final case class FibInputDto(
                                serviceId: String,
                                input: Int,
                                scheduledEndDateTime: String)


  final case class FibServiceStartedDto(
                                     serviceId: String,
                                     persistenceId: String,
                                     timestamp: Long)





}


