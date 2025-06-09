package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils._
import java.io.{BufferedWriter, FileWriter}

object CreateOrg {

	val CreateNewOrg = 

    exec(_.setAll(
      ("FirstName",Common.randomString(10)),
      ("LastName",Common.randomString(10)),
      ("RandDigits",Common.randomString(5).toUpperCase()),
      ("RandPBA1",Common.randomNumber(7)),
      ("RandPBA2",Common.randomNumber(7)),
      ("RandPBA3",Common.randomNumber(7)),
      "currentDate" -> Common.now.format(Common.patternDate),
      "currentTime" -> Common.now.format(Common.patternTime)
    ))

    .group("CreateOrg_010_HomePage") {
      exec(http("CreateOrg_010_005_RegisterHomePage")
        .get("/register-org/register")
        .headers(Environment.navigationHeader)
        .check(substring("base href")))

      .exec(getCookieValue(CookieKey("XSRF-TOKEN").withSecure(true).saveAs("XSRFToken")))

      .exec(http("CreateOrg_010_010_ConfigurationUI1")
        .get("/external/configuration-ui/")
        .headers(Environment.getHeader)
        .check(substring("idamWeb")))

      .exec(http("CreateOrg_010_015_ConfigurationUI2")
        .get("/external/configuration-ui")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("idamWeb")))
    }

		.pause(Environment.thinkTime)

  val SubmitOrg = 

		exec(http("CreateOrg_020_SubmitNewOrgRegistration")
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

    exec(http("CreateOrg_020_SubmitOtherOrgRegistration")
			.post("/external/register-org-new/register")
			.headers(Environment.postHeader)
      .header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/CreateOtherOrg.json"))
      .check(jsonPath("$.organisationIdentifier").saveAs("orgId")))

    .pause(Environment.thinkTime)

}