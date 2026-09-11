package scenarios

import ccd._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.api.payments
import utilities._
import utils.Environment

object PCSClaimant {

	val feedPCSUserData = csv("PCSSolicitorUserData.csv").circular
	val feedPCSCWUserData = csv("PCSCWUserData.csv").circular
	val feedPCSHousingUserData = csv("PCSHousingUserData.csv").circular

	val Create = {

		feed(feedPCSUserData)
			.exec(session => session
				.set("solicitorEmail", session("email").as[String])
				.set("solicitorPassword", session("password").as[String]))

		.feed(feedPCSHousingUserData)
			.exec(session => session
				.set("housingEmail", session("email").as[String])
				.set("housingPassword", session("password").as[String]))

		/*=============================================
		Create PCS Case as claimant
		===============================================*/

		.exec(CcdHelper.createCase("#{housingEmail}", "#{housingPassword}", CcdCaseTypes.PCS_PCS, "createPossessionClaim", "bodies/pcsBodies/PCSCreateCase.json"))
		.pause(Environment.thinkTime)

		.feed(feedPCSCWUserData)
			.exec(session => session
				.set("cwEmail", session("email").as[String])
				.set("cwPassword", session("password").as[String]))

		/*=============================================
		Upload Tenancy Document
		===============================================*/

		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("TenancyDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("TenancyDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Upload Notice Document
		===============================================*/

		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("NoticeDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("NoticeDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Upload Rent Arrears Document
		===============================================*/

		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("RentArrearsDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("RentArrearsDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Upload Rent Statement Document
		===============================================*/

		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("RentStatementDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("RentStatementDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Upload Rent Statement Document
		===============================================*/

		.exec(CcdHelper.uploadDocumentToCdam("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS.copy(microservice = "pcs_api"), "1MB.pdf", additionalChecks = Seq(
			jsonPath("$.documents[0]._links.self.href").saveAs("TenancyAgreementDocumentURL"),
			jsonPath("$.documents[0].hashToken").saveAs("TenancyAgreementDocumentHash")
		)))
		.pause(Environment.thinkTime)

		/*=============================================
		Submit Claim
		===============================================*/

		.exec(CcdHelper.addCaseEvent("#{housingEmail}", "#{housingPassword}", CcdCaseTypes.PCS_PCS, "#{caseId}", "resumePossessionClaim", "bodies/pcsBodies/PCSSubmitClaim.json"))
		.pause(Environment.thinkTime)

		/*=============================================
		Pay fee for claim
		===============================================*/
		.exec(payments.AddPCSPayment)
		.pause(Environment.thinkTime)

		.exec(_.set("pastDate", DateUtils.getDatePast("yyyy-MM-dd", days = 10)))

		/*=============================================
		Add Case Review Date case event
		===============================================*/
		.exec(CcdHelper.addCaseEvent("#{cwEmail}", "#{cwPassword}", CcdCaseTypes.PCS_PCS, "#{caseId}", "addCaseReviewDate", "bodies/pcsBodies/PCSAddReviewDate.json"))
		.pause(Environment.thinkTime)

	}	
}
