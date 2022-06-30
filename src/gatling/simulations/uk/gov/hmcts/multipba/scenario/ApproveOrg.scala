package uk.gov.hmcts.multipba.scenario

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import uk.gov.hmcts.multipba.util._

object ApproveOrg {

  val AdminUrl = Environment.adminUrl

	val ApproveOrgHomepage = 

    //Login screen
    exec(http("request_0")
      .get(Environment.adminUrl + "/")
      .headers(Environment.commonHeader))

    .exec(http("request_1")
      .get(Environment.adminUrl + "/api/environment/config")
      .headers(Environment.commonHeader))

    .exec(http("request_2")
      .get(Environment.adminUrl + "/api/environment/config")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_3")
      .get(Environment.adminUrl + "/api/user/details")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_4")
      .get(Environment.adminUrl + "/auth/login")
      .headers(Environment.commonHeader)
      .header("accept-encoding", "gzip, deflate, br")
      .header("accept", "application/json, text/plain, */*")
      .check(css("input[name='_csrf']", "value").saveAs("csrfToken"))
      .check(regex("callback&state=(.*)&nonce=").saveAs("state"))
      .check(regex("&nonce=(.*)&response_type").saveAs("nonce")))

    // .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain(adminDomain).saveAs("XSRFToken")))
    .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain("administer-orgs.perftest.platform.hmcts.net").saveAs("XSRFToken")))
    // .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain(AdminUrl.replace("https://", "")).saveAs("XSRFToken")))

    .pause(Environment.thinkTime)

  val ApproveOrgLogin =

    //Login
    exec(http("request_6")
      .post(Environment.idamURL + "/login?client_id=xuiaowebapp&redirect_uri=" + Environment.adminUrl + "/oauth2/callback&state=${state}&nonce=${nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user&prompt=")
      .headers(Environment.commonHeader)
      .formParam("username", "vmuniganti@mailnesia.com")
      .formParam("password", "Monday01")
      .formParam("save", "Sign in")
      .formParam("selfRegistrationEnabled", "false")
      .formParam("mojLoginEnabled", "true")
      .formParam("_csrf", "${csrfToken}"))

    .exec(http("request_7")
      .get(Environment.adminUrl + "/api/environment/config")
      .headers(Environment.commonHeader))

    .exec(http("request_8")
      .get(Environment.adminUrl + "/api/environment/config")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_9")
      .get(Environment.adminUrl + "/api/user/details")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_10")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_13")
      .post(Environment.adminUrl + "/api/organisations?status=PENDING,REVIEW")
      .headers(Environment.commonHeader)
      .header("content-type", "application/json")
      .header("x-xsrf-token", "${XSRFToken}")
      .body(ElFileBody("bodies/AdminOrgHomeSearch.json")))
      
    .pause(Environment.thinkTime)

  val SearchOrg = 

    //Search org
    exec(http("request_15")
      .post(Environment.adminUrl + "/api/organisations?status=PENDING,REVIEW")
      .headers(Environment.commonHeader)
      .header("content-type", "application/json")
      .header("x-xsrf-token", "${XSRFToken}")
      .body(ElFileBody("bodies/AdminOrgSearchOrg.json"))
      .check(jsonPath("$.organisations[0].organisationIdentifier").saveAs("OrgID")))

    .pause(Environment.thinkTime)

  val ViewOrg = 

    //View org
    exec(http("request_16")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_17")
      .get(Environment.adminUrl + "/api/organisations?organisationId=${OrgID}")
      .headers(Environment.commonHeader)
      .header("content-type", "application/json")
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_18")
      .get(Environment.adminUrl + "/api/organisations?usersOrgId=${OrgID}")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*")
      .check(status.is(500)))

    .exec(http("request_19")
      .get(Environment.adminUrl + "/api/monitoring-tools")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_20")
      .get(Environment.adminUrl + "/api/pbaAccounts/?accountNames=PBA1042AAA,PBA1042AAC,PBA1042AAB")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_21")
      .get(Environment.adminUrl + "/api/pbaAccounts/?accountNames=PBA1042AAA,PBA1042AAC,PBA1042AAB,PBA1042AAA,PBA1042AAC,PBA1042AAB")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_22")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_23")
      .get(Environment.adminUrl + "/api/organisations?organisationId=${OrgID}")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .pause(11)

    //Add new PBA
    .exec(http("request_24")
      .put(Environment.adminUrl + "/api/updatePba")
      .headers(Environment.commonHeader)
      .header("x-xsrf-token", "${XSRFToken}")
      .body(RawFileBody("approveOrg1_0024_request.txt")))

    .exec(http("request_25")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_26")
      .get(Environment.adminUrl + "/api/organisations?organisationId=${OrgID}")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_27")
      .get(Environment.adminUrl + "/api/organisations?usersOrgId=${OrgID}")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*")
      .check(status.is(500)))

    .exec(http("request_28")
      .get(Environment.adminUrl + "/api/pbaAccounts/?accountNames=PBA1042AAD,PBA1042AAA,PBA1042AAC,PBA1042AAB")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .pause(15)

    .exec(http("request_29")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .pause(7)

    //Approve Org
    .exec(http("request_30")
      .put(Environment.adminUrl + "/api/organisations/${OrgID}")
      .headers(Environment.commonHeader)
      .header("x-xsrf-token", "${XSRFToken}")
      .body(RawFileBody("approveOrg1_0030_request.txt")))

    .exec(http("request_31")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_33")
      .post(Environment.adminUrl + "/api/organisations?status=PENDING,REVIEW")
      .headers(Environment.commonHeader)
      .header("x-xsrf-token", "${XSRFToken}")
      .body(RawFileBody("approveOrg1_0033_request.txt")))

    .pause(5)

    //View orgs
    .exec(http("request_34")
      .get(Environment.adminUrl + "/auth/isAuthenticated")
      .headers(Environment.commonHeader)
      .header("accept", "application/json, text/plain, */*"))

    .exec(http("request_37")
      .post(Environment.adminUrl + "/api/organisations?status=PENDING,REVIEW")
      .headers(Environment.commonHeader)
      .header("x-xsrf-token", "${XSRFToken}")
      .body(RawFileBody("approveOrg1_0037_request.txt")))

    //Logout
    .exec(http("request_38")
      .get(Environment.adminUrl + "/auth/logout")
      .headers(Environment.commonHeader))

}