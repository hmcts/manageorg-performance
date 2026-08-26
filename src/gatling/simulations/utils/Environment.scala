package utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Environment {

  val idamURL = "https://idam-web-public.#{env}.platform.hmcts.net"
  val IDAMUrl = "https://idam-api.#{env}.platform.hmcts.net"
  val idamAPI = "https://idam-api.#{env}.platform.hmcts.net"
  val BaseUrl = "https://manage-org.#{env}.platform.hmcts.net"
  val adminUrl = "https://administer-orgs.#{env}.platform.hmcts.net"
  val pcsUrl = "http://pcs-api-perftest.service.core-compute-perftest.internal"
  val xuiBaseURL = "https://manage-case.#{env}.platform.hmcts.net"
  val rpeUrl = "http://rpe-service-auth-provider-#{env}.service.core-compute-#{env}.internal"
  val paymentsUrl = "http://payment-api-#{env}.service.core-compute-#{env}.internal"

  val thinkTime = 7
  val constantthinkTime = 10 //7

  val HttpProtocol = http

  val navigationHeader = Map(
    "accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "accept-encoding" -> "gzip, deflate, br",
    "accept-language" -> "en-GB,en;q=0.9",
		"sec-fetch-dest" -> "document",
		"sec-fetch-mode" -> "navigate",
		"sec-fetch-site" -> "none",
		"sec-fetch-user" -> "?1",
		"upgrade-insecure-requests" -> "1")

  val getHeader = Map(
    "accept" -> "*/*",
    "accept-encoding" -> "gzip, deflate, br",
    "accept-language" -> "en-GB,en;q=0.9",
    "sec-fetch-dest" -> "empty",
    "sec-fetch-mode" -> "cors",
    "sec-fetch-site" -> "same-origin")

  val postHeader = Map(
    "accept" -> "application/json, text/plain, */*",
    "accept-encoding" -> "gzip, deflate, br",
    "accept-language" -> "en-GB,en;q=0.9",
    "content-type" -> "application/json",
    "sec-fetch-dest" -> "empty",
    "sec-fetch-mode" -> "cors",
    "sec-fetch-site" -> "same-origin")

}