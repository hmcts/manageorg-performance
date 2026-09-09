package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils._
import utilities._
import xui._
import java.io.{BufferedWriter, FileWriter}
import xui.XuiHelper.IdamUrl

object ManageOrg {

  val feedManageOrgUserData = csv("PCSManageOrgUserData.csv").circular

	val LandingPage = 

    // exec(_.setAll(
    //   ("FirstName",Common.randomString(10)),
    //   ("LastName",Common.randomString(10)),
    //   ("RandDigits",Common.randomString(5).toUpperCase()),
    //   ("RandPBA1",Common.randomNumber(7)),
    //   ("RandPBA2",Common.randomNumber(7)),
    //   ("RandPBA3",Common.randomNumber(7)),
    //   "currentDate" -> Common.now.format(Common.patternDate),
    //   "currentTime" -> Common.now.format(Common.patternTime)
    // ))
// 
    group("Managerg_010_HomePage") {
      exec(http("ManageOrg_010_005_HomePage")
        .get("")
        .headers(Environment.navigationHeader)
        .check(substring("Manage organisation")))

      //.exec(getCookieValue(CookieKey("XSRF-TOKEN").withSecure(true).saveAs("XSRFToken")))

      .exec(http("ManageOrg_000_ConfigurationUI")
        .get("/external/configuration-ui/")
        .headers(Environment.getHeader)
        .check(substring("perftest")))

      // .exec(http("ManageOrg_010_015_ConfigurationUI2")
      //   .get("/external/configuration-ui")
      //   .headers(Environment.getHeader)
      //   .header("accept", "application/json, text/plain, */*")
      //   .check(substring("idamWeb")))

      .exec(http("ManageOrg_000_IsAuthenticated")
        .get("/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("false")))
 
      .exec(http("Managerg_020_HomePageAuthLogin")
        .get("/auth/login")
        .headers(Headers.navigationHeader)
        .check(css("input[name='_csrf']", "value").saveAs("csrf")))
    }

		.pause(Environment.thinkTime)

  val Login =

    feed(feedManageOrgUserData)

    .group("ManageOrg_020_LoginEnterEmail") {
      exec(http("ManageOrg_020_005_LoginEnterEmail")
        .post(IdamUrl + "/enter-email")
        .headers(Headers.navigationHeader)
        .formParam("email", "#{email}")
        .formParam("_csrf", "#{csrf}")
        .check(status.is(200)))
    }

    .group("ManageOrg_030_LoginEnterPassword") {
      exec(http("ManageOrg_030_005_LoginEnterPassword")
        .post(IdamUrl + "/enter-password")
        .headers(Headers.navigationHeader)
        .formParam("action", "_submit")
        .formParam("password", "#{password}")
        .formParam("_csrf", "#{csrf}")
        .check(status.is(200)))

      .exec(http("ManageOrg_000_ConfigurationUI")
        .get("/external/configuration-ui/")
        .headers(Headers.commonHeader)
        .header("accept", "*/*")
        .check(substring("perftest")))

      .exec(http("ManageOrg_000_IsAuthenticated")
        .get("/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("ManageOrg_000_UserDetails")
        .get("/api/user/details")
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{email}")))

      .exec(http("ManageOrg_000_IsAuthenticated")
        .get("/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("ManageOrg_000_OrgDetailsV1")
        .get("/api/organisation/v1")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{orgIdentifier}")))

      .exec(http("ManageOrg_000_Organisation")
        .get("/api/organisation")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{orgIdentifier}")))

      .exec(http("ManageOrg_000_LovRefData")
        .get("/external/getLovRefData?categoryId=OrgType&isChildRequired=Y")
        .headers(Headers.commonHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("SOLICITOR")))

      .exec(http("ManageOrg_000_IsAuthenticated")
        .get("/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exitHereIfFailed
    }

  val Users = 

    group("ManageOrg_050_Users") {
      exec(http("ManageOrg_050_005_Users")
        .get("/api/allUserListWithoutRoles")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{orgIdentifier}"))
        .check(jsonPath("$.organisationProfileIds[0]").saveAs("orgProfileId0"))
        .check(jsonPath("$.organisationProfileIds[1]").saveAs("orgProfileId1"))
        .check(jsonPath("$..userIdentifier").findAll.saveAs("allUserIds"))
        .check(status.is(200)))

      exec(http("ManageOrg_050_005_Users")
        .post("/api/retrieve-access-types")
        .headers(Environment.postHeader)
        .header("accept", "application/json, text/plain, */*")
        .body(StringBody("""{"organisationProfileIds":["#{orgProfileId0}","#{orgProfileId1}"]}""")).asJson
        .check(substring("#{orgIdentifier}"))
        .check(status.is(200)))
    }

  val ViewAndManageUsers = 

    //Select a random userId from the saved allUserIds
    exec(session => {
      val allUserIds = session("allUserIds").as[Seq[String]]
      val randomId = allUserIds(scala.util.Random.nextInt(allUserIds.size))
      session.set("selectedUserId", randomId)
      })

    .group("ViewAndManageUsers_060_SelectUser") {
      exec(http("ViewAndManageUsers_060_005_SelectUser")
        .get("/api/user-details?userId=#{selectedUserId}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{orgIdentifier}"))
        .check(status.is(200)))

      exec(http("ViewAndManageUsers_060_010_SelectUser")
        .put("/api/ogd-flow/update/d538e8d3-3798-4d65-a495-4e5ae9e119ee")
        .headers(Environment.postHeader)
        .header("accept", "application/json, text/plain, */*")
        .body(StringBody("""{"organisationProfileIds":["#{orgProfileId0}","#{orgProfileId1}"]}""")).asJson
        .check(substring("#{orgIdentifier}"))
        .check(status.is(200)))
    }

  val SubmitOrg = 

		exec(http("ManageOrg_020_SubmitNewOrgRegistration")
			.post("/external/register-org/register")
			.headers(Environment.postHeader)
      .header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/CreateNewOrg.json"))
      .check(jsonPath("$.organisationIdentifier").saveAs("orgId")))

    .pause(Environment.thinkTime)

    //Outputs the newly created Org ID and Org Name
    /*.exec {
      session =>
        val fw = new BufferedWriter(new FileWriter("NewOrgIDs.csv", true))
        try {
          fw.write(session("orgId").as[String] + "," + "perf" + "-" + session("currentDate").as[String] + "-" + session("currentTime").as[String] + "_" + session("RandDigits").as[String] + "\r\n")
        }
        finally fw.close()
        session
    }*/

  val SubmitOtherOrg = 

    exec(http("ManageOrg_020_SubmitOtherOrgRegistration")
			.post("/external/register-org-new/register")
			.headers(Environment.postHeader)
      .header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/CreateOtherOrg.json"))
      .check(jsonPath("$.organisationIdentifier").saveAs("orgId")))

    .pause(Environment.thinkTime)

}