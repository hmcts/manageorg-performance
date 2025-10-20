package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils._

object ApproveOrg {

  val AdminUrl = Environment.adminUrl
  val adminusers = csv("AdminOrgUsers.csv").circular

	val ApproveOrgHomepage = 

    group("AdminOrg_010_Homepage") {
      exec(http("AdminOrg_010_005_Homepage")
        .get(AdminUrl + "/")
        .headers(Environment.navigationHeader)
        .check(substring("base href")))

      .exec(http("AdminOrg_010_010_EnvConfig1")
        .get(AdminUrl + "/api/environment/config")
        .headers(Environment.getHeader))

      .exec(http("AdminOrg_010_015_EnvConfig2")
        .get(AdminUrl + "/api/environment/config")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*"))

      .exec(http("AdminOrg_010_020_UserDetails")
        .get(AdminUrl + "/api/user/details")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(status.in(200, 401)))

      .exec(http("AdminOrg_010_025_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*"))

      .exec(http("AdminOrg_010_030_LoadLogin")
        .get(AdminUrl + "/auth/login")
        .headers(Environment.navigationHeader)
        .header("sec-fetch-site", "same-origin")
        .check(css("input[name='_csrf']", "value").saveAs("csrfToken"))
        .check(regex("/oauth2/callback&amp;state=(.*)&amp;nonce=").saveAs("state"))
        .check(regex("nonce=(.*)&amp;response_type").saveAs("nonce")))

      .exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain("administer-orgs.#{env}.platform.hmcts.net").withSecure(true).saveAs("XSRFToken")))
    }

    .pause(Environment.thinkTime)

  val ApproveOrgLogin =

    feed(adminusers)

    .group("AdminOrg_020_Login") {
      exec(http("AdminOrg_020_005_Login")
        .post(Environment.idamURL + "/login?client_id=xuiaowebapp&redirect_uri=" + AdminUrl + "/oauth2/callback&state=#{state}&nonce=#{nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user&prompt=")
        .headers(Environment.navigationHeader)
        .header("sec-fetch-site", "same-origin")
        .formParam("username", "#{username}")
        .formParam("password", "#{password}")
        .formParam("selfRegistrationEnabled", "false")
        .formParam("mojLoginEnabled", "true")
        .formParam("_csrf", "#{csrfToken}")
        .check(substring("meta name")))

      .exec(http("AdminOrg_020_010_EnvConfig1")
        .get(AdminUrl + "/api/environment/config")
        .headers(Environment.getHeader)
        .check(substring("configEnv")))

      .exec(http("AdminOrg_020_015_EnvConfig2")
        .get(AdminUrl + "/api/environment/config")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("configEnv")))

      .exec(http("AdminOrg_020_020_UserDetails")
        .get(AdminUrl + "/api/user/details")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("idleTime")))

      .exec(http("AdminOrg_020_025_IsAuthenticated1")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_020_030_IsAuthenticated2")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_020_035_IsAuthenticated3")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_020_040_GetPendingOrgList")
        .post(AdminUrl + "/api/organisations?status=PENDING,REVIEW")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminOrgHomeSearch.json"))
        .check(substring("total_records")))
    }
      
    .pause(Environment.thinkTime)

  val SearchOrg = 

    exec(http("AdminOrg_030_SearchForOrg")
      .post(AdminUrl + "/api/organisations?status=PENDING,REVIEW")
      .headers(Environment.postHeader)
      .header("x-xsrf-token", "#{XSRFToken}")
      .body(ElFileBody("bodies/AdminOrgSearchOrg.json"))
      .check(jsonPath("$.organisations[0].organisationIdentifier").saveAs("OrgID"))
      .check(jsonPath("$.organisations[0].name").saveAs("OrgName"))
      .check(jsonPath("$.organisations[0].contactInformation[0].addressLine1").saveAs("addressLine1"))
      .check(jsonPath("$.organisations[0].contactInformation[0].addressLine2").optional.saveAs("addressLine2"))
      .check(jsonPath("$.organisations[0].contactInformation[0].townCity").saveAs("townCity"))
      .check(jsonPath("$.organisations[0].contactInformation[0].county").saveAs("county"))
      .check(jsonPath("$.organisations[0].pendingPaymentAccount[0]").optional.saveAs("PBA1"))
      .check(jsonPath("$.organisations[0].pendingPaymentAccount[1]").optional.saveAs("PBA2"))
      .check(jsonPath("$.organisations[0].pendingPaymentAccount[2]").optional.saveAs("PBA3"))
      .check(jsonPath("$.organisations[0].superUser.firstName").saveAs("firstName"))
      .check(jsonPath("$.organisations[0].superUser.lastName").saveAs("lastName"))
      .check(jsonPath("$.organisations[0].superUser.email").saveAs("email")))

    .pause(Environment.thinkTime)

  val ViewOrg = 

    group("AdminOrg_040_ViewOrg") {
      exec(http("AdminOrg_040_005_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_040_010_ViewOrg")
        .get(AdminUrl + "/api/organisations?organisationId=#{OrgID}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("organisationIdentifier")))

  /* These calls aren't used, as there are no users in the organisation

      .exec(http("AdminOrg_040_015_ViewOrg")
        .get(AdminUrl + "/api/organisations?usersOrgId=#{OrgID}")
        .headers(Environment.navigationHeader)
        .check(status.in(500, 304)))

      .exec(http("AdminOrg_040_020_ViewOrg")
        .get(AdminUrl + "/api/monitoring-tools")
        .headers(Environment.navigationHeader))
  */
      .doIf("#{PBA1.exists()}") {
        exec(http("AdminOrg_040_025_ViewPBAAccounts1")
          .get(AdminUrl + "/api/pbaAccounts/?accountNames=#{PBA1},#{PBA2},#{PBA3}")
          .headers(Environment.getHeader)
          .header("accept", "application/json, text/plain, */*")
          .check(substring("account_name").count.is(3)))

        .exec(http("AdminOrg_040_030_ViewPBAAccounts2")
          .get(AdminUrl + "/api/pbaAccounts/?accountNames=#{PBA1},#{PBA2},#{PBA3},#{PBA1},#{PBA2},#{PBA3}")
          .headers(Environment.getHeader)
          .header("accept", "application/json, text/plain, */*")
          .check(substring("account_name").count.is(6)))
      }

  /* These calls aren't used

      .exec(http("AdminOrg_040_035_ViewOrg")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.navigationHeader))

      .exec(http("AdminOrg_040_040_ViewOrg")
        .get(AdminUrl + "/api/organisations?organisationId=#{OrgID}")
        .headers(Environment.navigationHeader))

  */
    }

    .pause(Environment.thinkTime)

  val AddNewPBA = 

    exec(_.setAll(
      ("newPBA",Common.randomNumber(7))
    ))

    .group("AdminOrg_045_EditPBAs") {
      exec(http("AdminOrg_045_005_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_045_010_ViewOrg")
        .get(AdminUrl + "/api/organisations?organisationId=#{OrgID}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(jsonPath("$.status").is("PENDING")))
    }

    .pause(Environment.thinkTime)

    .group("AdminOrg_050_AddPBA") {
      exec(http("AdminOrg_050_005_UpdatePBAs")
        .put(AdminUrl + "/api/updatePba")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminAddPBA.json")))

      .exec(http("AdminOrg_050_010_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_050_015_ViewOrg")
        .get(AdminUrl + "/api/organisations?organisationId=#{OrgID}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(jsonPath("$.status").is("PENDING"))
        .check(substring("PBA#{newPBA}")))

  /* These calls aren't used, as there are no users in the organisation

      .exec(http("AdminOrg_050_020_AddPBA")
        .get(AdminUrl + "/api/organisations?usersOrgId=#{OrgID}")
        .headers(Environment.navigationHeader)
        .check(status.in(500, 304)))
  */
      
      .exec(http("AdminOrg_050_025_ViewPBAAccounts")
        .get(AdminUrl + "/api/pbaAccounts/?accountNames=#{PBA1},#{PBA2},#{PBA3},PBA#{newPBA}")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("account_name").count.is(4)))
      

  /* These calls aren't used
      .exec(http("AdminOrg_00_030_AddPBA")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.navigationHeader))

  */
    }

    .pause(Environment.thinkTime)

  val ApproveNewOrg =

    group("AdminOrg_055_Approval") {
      exec(http("AdminOrg_055_005_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))
    }

    .pause(Environment.thinkTime)

    .group("AdminOrg_060_ApproveOrg") {
      exec(http("AdminOrg_060_005_ApproveOrg")
        .put(AdminUrl + "/api/organisations/#{OrgID}")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminApproveOrg.json")))

      .exec(http("AdminOrg_060_008_MonitoringTools")
        .get(AdminUrl + "/api/monitoring-tools")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("InstrumentationKey")))

      .exec(http("AdminOrg_060_010_IsAuthenticated1")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_060_012_IsAuthenticated2")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_060_015_SearchForOrg")
        .post(AdminUrl + "/api/organisations?status=PENDING,REVIEW")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminOrgSearchOrg.json"))
        .check(jsonPath("$.total_records").is("0")))
    }

    .pause(Environment.thinkTime)

  val ApproveNewOtherOrg = 

    group("AdminOrg_055_Approval") {
      exec(http("AdminOrg_055_005_IsAuthenticated")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))
    }

    .pause(Environment.thinkTime)

    .group("AdminOrg_060_ApproveOrg") {
      exec(http("AdminOrg_060_005_ApproveOrg")
        .put(AdminUrl + "/api/organisations/#{OrgID}")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminApproveOtherOrg.json")))

      .exec(http("AdminOrg_060_008_MonitoringTools")
        .get(AdminUrl + "/api/monitoring-tools")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("InstrumentationKey")))

      .exec(http("AdminOrg_060_010_IsAuthenticated1")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_060_012_IsAuthenticated2")
        .get(AdminUrl + "/auth/isAuthenticated")
        .headers(Environment.getHeader)
        .header("accept", "application/json, text/plain, */*")
        .check(substring("true")))

      .exec(http("AdminOrg_060_015_SearchForOrg")
        .post(AdminUrl + "/api/organisations?status=PENDING,REVIEW")
        .headers(Environment.postHeader)
        .header("x-xsrf-token", "#{XSRFToken}")
        .body(ElFileBody("bodies/AdminOrgSearchOrg.json"))
        .check(jsonPath("$.total_records").is("0")))
    }

    .pause(Environment.thinkTime)

  val Logout =

    exec(http("AdminOrg_070_Logout")
      .get(AdminUrl + "/auth/logout")
      .headers(Environment.navigationHeader))

}