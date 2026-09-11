package scenarios

import ccd._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.api.payments
import utils._
import utilities._
import ccd.CcdHelper.authenticate
import xui._
import xui.XuiHelper.xuiUrl

object PCSDefendant {

	val feedPCSDefendantData = csv("PCSDefendantUserData.csv").circular
	val feedPCSCWUserData = csv("PCSCWUserData.csv").circular
	val feedPCSHousingUserData = csv("PCSHousingUserData.csv").circular

	val NOC = 

		feed(feedPCSDefendantData)
			.exec(session => session
				.set("defendantEmail", session("email").as[String])
				.set("defendantPassword", session("password").as[String])
				//.set("caseId", "1788986961104250") // For debugging
				.set("caseType", "PCS"))

		/*=============================================
		Manage case landing page
		===============================================*/

		.exec(XuiHelper.Homepage)
		.pause(Environment.thinkTime)

		/*=============================================
		Login
		===============================================*/

		.exec(XuiHelper.Login("#{defendantEmail}", "#{defendantPassword}"))
		.pause(Environment.thinkTime)

		/*=============================================
		Select NoC
		===============================================*/

		.exec(http("XUI_010_DefendantNOCQuestions")
        	.get(xuiUrl + "/api/noc/nocQuestions?caseId=#{caseId}")
        	.headers(Environment.getHeader)
        	.header("accept", "application/json, text/plain, */*")
        	.check(substring("Enter client first name")))

		.pause(Environment.thinkTime)

		.exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain(xuiUrl.replace("https://", "")).withSecure(true).saveAs("XSRFToken")))

		/*=============================================
		Validate NoC Answers
		===============================================*/

		.exec(http("XUI_020_DefendantNOCValidateQuestions")
			.post(xuiUrl + "/api/noc/validateNoCQuestions")
			.headers(Environment.postHeader)
      		.header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/pcsBodies/PCSValidateNoCQuestions.json"))
     		.check(substring("Notice of Change answers verified successfully")))
		
		.pause(Environment.thinkTime)

		/*=============================================
		Submit NoC
		===============================================*/

		.exec(http("XUI_030_DefendantNOC")
			.post(xuiUrl + "/api/noc/submitNoCEvents")
			.headers(Environment.postHeader)
      		.header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/pcsBodies/PCSSubmitNoC.json"))
     		.check(substring("APPROVED")))

		.pause(Environment.thinkTime)

	val ViewCaseList = 

		/*=============================================
		Retrieve cases for user
		===============================================*/

		group("XUI_040_DefendantViewCaseList") {
		  exec(http("XUI_040_005_DefendantWorkBasketInputs")
        	.get(xuiUrl + "/data/internal/case-types/PCS/work-basket-inputs")
        	.headers(Environment.getHeader)
        	.header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-workbasket-input-details.v2+json;charset=UTF-8")
			.header("accept-encoding","gzip, deflate, br, zstd")
			.header("content-type","application/json")
			.header("experimental","true")
        	.check(substring("/PCS/work-basket-inputs")))

		  .exec(http("XUI_040_010_DefendantSearchCases")
			.post(xuiUrl + "/data/internal/searchCases?ctid=PCS&use_case=WORKBASKET&view=WORKBASKET&page=1")
			.headers(Environment.postHeader)
			.header("accept", "application/json")
			.body(StringBody("""{"size":"25"}""")).asJson
			.check(jsonPath("$.results[*].case_id").findAll.saveAs("caseIds"))
			.check(status.is(200)))
		}
		
		.pause(Environment.thinkTime)

	val ViewCase = 

		 //Select a random userId from the saved allUserIds
		exec(session => {
			val allCaseIds = session("caseIds").as[Seq[String]]
			val randomId = allCaseIds(scala.util.Random.nextInt(allCaseIds.size))
			session.set("selectedCaseId", randomId)
		})

		/*=============================================
		Open a random case from the caselist
		===============================================*/

		.exec(http("XUI_050_DefendantViewCase")
        	.get(xuiUrl + "/data/internal/cases/#{selectedCaseId}")
        	.headers(Environment.getHeader)
        	.header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json")
        	.header("accept-encoding","gzip, deflate, br, zstd")
			.header("content-type","application/json")
			.header("experimental","true")
			.check(substring("Possession Case Type"))
			.check(status.is(200)))
		
		.pause(Environment.thinkTime)

	val MakeAnApplication = 

		/*=============================================
		Upload Witness Statement Document
		===============================================*/

		feed(feedPCSCWUserData)
		.exec(session => session
			.set("cwEmail", session("email").as[String])
			.set("cwPassword", session("password").as[String]))

		.exec(CcdHelper.uploadDocumentToCdam("#{defendantEmail}", "#{defendantPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "DUMMY_WITNESS_STATEMENT.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("WitnessStatementDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("WitnessStatementDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Make an Application as the defendant Solicitor
		===============================================*/

		.exec(CcdHelper.addCaseEvent("#{defendantEmail}", "#{defendantPassword}", CcdCaseTypes.PCS_PCS, "#{caseId}", "makeAnApplication", "bodies/pcsBodies/PCSMakeAnApplication.json", additionalTriggerChecks = Seq(
				jsonPath("$.case_details.case_data.currentRepresentedPartyId").saveAs("representedPartyId")
		)))
		.pause(Environment.thinkTime)
}