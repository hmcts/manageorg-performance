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

    group("Managerg_010_HomePage") {
      exec(http("ManageOrg_010_005_HomePage")
        .get("")
        .headers(Environment.navigationHeader)
        .check(substring("Manage organisation")))

      .exec(http("ManageOrg_000_ConfigurationUI")
        .get("/external/configuration-ui/")
        .headers(Environment.getHeader)
        .check(substring("perftest")))

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

    .pause(Environment.thinkTime)

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
      
    .pause(Environment.thinkTime)

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

      .exec(http("ManageOrg_050_005_Users")
        .post("/api/retrieve-access-types")
        .headers(Environment.postHeader)
        .header("accept", "application/json, text/plain, */*")
        .body(StringBody("""{"organisationProfileIds":["#{orgProfileId0}","#{orgProfileId1}"]}""")).asJson
        .check(substring("#{orgIdentifier}"))
        .check(status.is(200)))
    }
    
    .pause(Environment.thinkTime)

  val ViewAndManageUsers = 

    //Select a random userId from the saved allUserIds
    exec(session => {
      val allUserIds = session("allUserIds").as[Seq[String]]
      val randomId = allUserIds(scala.util.Random.nextInt(allUserIds.size))
      session.set("selectedUserId", randomId)})

    .exec(http("ViewAndManageUsers_060_SelectUser")
        .get("/api/user-details?userId=#{selectedUserId}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("#{orgIdentifier}"))
        .check(jsonPath("$.firstName").saveAs("userFirstName"))
        .check(jsonPath("$.lastName").saveAs("userLastName"))
        .check(jsonPath("$.email").saveAs("userEmail"))
        .check(jsonPath("$.userAccessTypes[*]").count.saveAs("accessTypesCount"))
        .check(status.is(200)))

    .pause(Environment.thinkTime)

    .exec(session => {
        val bodyFile = session("accessTypesCount").as[Int] match {
        case 2 => "bodies/UpdateUserDutyAdvisorRole.json"
        case _ => "bodies/UpdateUserGARoles.json" 
    }
    session.set("updateUserBody", bodyFile) 
    })
      
    .exec(http("ViewAndManageUsers_070_UpdateUser")
        .put("/api/ogd-flow/update/#{selectedUserId}")
        .headers(Environment.postHeader)
        .header("accept", "application/json, text/plain, */*")
        .body(ElFileBody("#{updateUserBody}"))
        .check(jsonPath("$.statusUpdateResponse.idamStatusCode").is("200")))

    .pause(Environment.thinkTime)

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