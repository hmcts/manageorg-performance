package scenarios.api

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utilities.AzureKeyVault
import utils.Environment

object payments {

  val clientSecret = AzureKeyVault.loadClientSecret("ccpay-perftest", "paybubble-idam-client-secret", "PAYBUBBLE_CLIENT_SECRET")
  val clientId = "paybubble"
  val microservice = "xui_webapp"
  val pcsmicroservice = "pcs_api"

  val AddPCSPayment =

    exec(http("CCD_AuthLease")
      .post(Environment.rpeUrl + "/testing-support/lease")
      .body(StringBody(s"""{"microservice":"$microservice"}""")).asJson
      .check(regex("(.+)").saveAs("xui_webappAuthToken")))

      .exec(http("CCD_GetBearerToken")
        .post(Environment.idamAPI + "/o/token")
        .formParam("grant_type", "password")
        .formParam("username", "#{email}")
        .formParam("password", "#{password}")
        .formParam("client_id", clientId)
        .formParam("client_secret", clientSecret)
        .formParam("scope", "openid profile roles search-user")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .check(jsonPath("$.access_token").saveAs("access_tokenPayments")))

      .pause(Environment.constantthinkTime)

      .exec(http("PaymentAPI_GetCasePaymentOrders")
        .get(Environment.paymentsUrl + "/case-payment-orders?case_ids=#{caseId}")
        .header("Authorization", "Bearer #{access_tokenPayments}")
        .header("ServiceAuthorization", "#{xui_webappAuthToken}")
        .header("Content-Type","application/json")
        .check(jsonPath("$.content[0].orderReference").saveAs("caseIdPaymentRef")))

      .pause(Environment.constantthinkTime)

      .exec(http("CCD_AuthLease")
        .post(Environment.rpeUrl + "/testing-support/lease")
        .body(StringBody(s"""{"microservice":"$pcsmicroservice"}""")).asJson
        .check(regex("(.+)").saveAs("pcs_apiAuthToken")))

      .tryMax(2) {
        exec(http("API_PCS_AddPayment")
          .put(Environment.pcsUrl + "/payment-update")
          .header("Authorization", "Bearer #{access_tokenPayments}")
          .header("ServiceAuthorization", "#{pcs_apiAuthToken}")
          .header("Content-type", "application/json")
          .body(ElFileBody("pcsBodies/PCSAddPayment.json")))
      }

      .pause(Environment.constantthinkTime)

      .exec(session => session.removeAll())

}
