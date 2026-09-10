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
// import ccd.CcdCaseTypes

object PCSDefendant {

	val feedPCSDefendantData = csv("PCSDefendantUserData.csv").circular
	val feedPCSCWUserData = csv("PCSCWUserData.csv").circular
	val feedPCSHousingUserData = csv("PCSHousingUserData.csv").circular

	val NOC = 

		feed(feedPCSDefendantData)
			.exec(session => session
				.set("defendantEmail", session("email").as[String])
				.set("defendantPassword", session("password").as[String])
				.set("caseId", "1788990169492996") // For debugging
				.set("caseType", "PCS"))

		.exec(XuiHelper.Homepage)

		.pause(Environment.thinkTime)

		.exec(XuiHelper.Login("#{defendantEmail}", "#{defendantPassword}"))

		.pause(Environment.thinkTime)

		.exec(http("XUI_020_DefendantNOCQuestions")
        	.get(xuiUrl + "/api/noc/nocQuestions?caseId=#{caseId}")
        	.headers(Environment.getHeader)
        	.header("accept", "application/json, text/plain, */*")
        	.check(substring("Enter client first name")))

		.pause(Environment.thinkTime)

		.exec(getCookieValue(CookieKey("XSRF-TOKEN").withDomain(xuiUrl.replace("https://", "")).withSecure(true).saveAs("XSRFToken")))

		//.exec(authenticate("#{defendantEmail}", "#{defendantEmail}", CcdCaseTypes.PCS_PCS.microservice, CcdCaseTypes.PCS_PCS.clientId))

		.exec(http("XUI_030_DefendantNOCValidateQuestions")
			.post(xuiUrl + "/api/noc/validateNoCQuestions")
			.headers(Environment.postHeader)
      		.header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/pcsBodies/PCSValidateNoCQuestions.json"))
     		.check(substring("Notice of Change answers verified successfully")))
		
		.pause(Environment.thinkTime)

		.exec(http("XUI_040_DefendantNOC")
			.post(xuiUrl + "/api/noc/submitNoCEvents")
			.headers(Environment.postHeader)
      		.header("x-xsrf-token", "#{XSRFToken}")
			.body(ElFileBody("bodies/pcsBodies/PCSSubmitNoC.json"))
     		.check(substring("APPROVED")))

		.pause(Environment.thinkTime)

	val ViewCaseList = 

		group("XUI_060_ViewCaseList") {
		  exec(http("XUI_060_005_DefendantWorkBasketInputs")
        	.get(xuiUrl + "/data/internal/case-types/PCS/work-basket-inputs")
        	.headers(Environment.getHeader)
        	.header("accept", "application/json, text/plain, */*")
        	.check(substring("/PCS/work-basket-inputs")))

		  .exec(http("XUI_060_010_SearchCases")
        	.post(xuiUrl + "/data/internal/searchCases?ctid=PCS&use_case=WORKBASKET&view=WORKBASKET&page=1")
        	.headers(Environment.postHeader)
        	.header("accept", "application/json, text/plain, */*")
			.body(StringBody("""{"size":"25"}""")).asJson
			.check(jsonPath("$.results.case_id").findAll.saveAs("caseIds"))
        	.check(substring("/PCS/work-basket-inputs")))
		}
		.pause(Environment.thinkTime)

	val ViewCase = 

		 //Select a random userId from the saved allUserIds
		exec(session => {
			val allCaseIds = session("caseIds").as[Seq[String]]
			val randomId = allCaseIds(scala.util.Random.nextInt(allCaseIds.size))
			session.set("selectedCaseId", randomId)
		})

		.exec(http("XUI_070_DefendantViewCase")
        	.get(xuiUrl + "/data/internal/cases/#{selectedCaseId}")
        	.headers(Environment.getHeader)
        	.header("accept", "application/json, text/plain, */*")
        	.check(substring("/PCS/work-basket-inputs")))
		
		.pause(Environment.thinkTime)


		

		//.exec(CcdHelper.addCaseEvent("#{defendantEmail}", "#{defendantEmail}", CcdCaseTypes.PCS_PCS, "#{caseId}", "caseworkerNoticeOfChange", "bodies/pcsBodies/PCSSubmitNOC.json"))	
		/*.exec(CcdHelper.createCase("#{housingEmail}", "#{housingPassword}", CcdCaseTypes.PCS_PCS, "createPossessionClaim", "bodies/pcsBodies/PCSCreateCase.json"))
		.feed(feedPCSCWUserData)
			.exec(session => session
				.set("cwEmail", session("email").as[String])
				.set("cwPassword", session("password").as[String]))
	
		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("TenancyDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("TenancyDocumentHash")
		)))
		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("NoticeDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("NoticeDocumentHash")
		)))
		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("RentArrearsDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("RentArrearsDocumentHash")
		)))
		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("RentStatementDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("RentStatementDocumentHash")
		)))
		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("TenancyAgreementDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("TenancyAgreementDocumentHash")
		)))
		.exec(CcdHelper.addCaseEvent("#{housingEmail}", "#{housingPassword}", CcdCaseTypes.PCS_PCS, "#{caseId}", "resumePossessionClaim", "bodies/pcsBodies/PCSSubmitClaim.json"))
		.exec(payments.AddPCSPayment)
		.exec(_.set("pastDate", DateUtils.getDatePast("yyyy-MM-dd", days = 10)))
		.exec(CcdHelper.addCaseEvent("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS, "#{caseId}", "addCaseReviewDate", "bodies/pcsBodies/PCSAddReviewDate.json"))
	*/}
		//.feed(feedPCSCWUserData)
//		.exec(CcdHelper.uploadDocumentToCdam("#{email}", "#{password}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
//			jsonPath("$.documents[0]._links.self.href").saveAs("GADocumentURL"),
//			jsonPath("$.documents[0].hashToken").saveAs("GADocumentHash")
//		)))
//		.exec(CcdHelper.addCaseEvent("#{email}", "#{password}", CcdCaseTypes.PCS_PCS, "#{caseId}", "enterGenApp", "pcsBodies/PCSEnterGeneralApplication.json"))
				//.exec(CcdHelper.addCaseEvent("#{email}", "#{password}", CcdCaseTypes.PCS_PCS, "#{caseId}", "changeCaseState", "bodies/pcsBodies/PCSChangeState.json"))
	
