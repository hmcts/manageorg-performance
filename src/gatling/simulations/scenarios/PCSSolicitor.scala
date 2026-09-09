package scenarios

import ccd._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.api.payments
import utilities._

object PCSSolicitor {

	val feedPCSUserData = csv("PCSSolicitorUserData.csv").circular
	val feedPCSCWUserData = csv("PCSCWUserData.csv").circular
	val feedPCSHousingUserData = csv("PCSHousingUserData.csv").circular

	val create = {

		feed(feedPCSUserData)
			.exec(session => session
				.set("solicitorEmail", session("email").as[String])
				.set("solicitorPassword", session("password").as[String]))

		.feed(feedPCSHousingUserData)
			.exec(session => session
				.set("housingEmail", session("email").as[String])
				.set("housingPassword", session("password").as[String]))

		.exec(CcdHelper.createCase("#{housingEmail}", "#{housingPassword}", CcdCaseTypes.PCS_PCS, "createPossessionClaim", "bodies/pcsBodies/PCSCreateCase.json"))
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
	}
		//.feed(feedPCSCWUserData)
//		.exec(CcdHelper.uploadDocumentToCdam("#{email}", "#{password}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
//			jsonPath("$.documents[0]._links.self.href").saveAs("GADocumentURL"),
//			jsonPath("$.documents[0].hashToken").saveAs("GADocumentHash")
//		)))
//		.exec(CcdHelper.addCaseEvent("#{email}", "#{password}", CcdCaseTypes.PCS_PCS, "#{caseId}", "enterGenApp", "pcsBodies/PCSEnterGeneralApplication.json"))
				//.exec(CcdHelper.addCaseEvent("#{email}", "#{password}", CcdCaseTypes.PCS_PCS, "#{caseId}", "changeCaseState", "bodies/pcsBodies/PCSChangeState.json"))
	}
