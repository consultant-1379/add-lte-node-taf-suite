/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.cm.taf.testcases;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

import static com.ericsson.cifwk.taf.datasource.TafDataSources.fromCsv;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.ACTIVE;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.ADD_DOOZER;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.CM_FUNCTION_RDN;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.COMMA;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.CSV_EXTENSITON;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.DATA_DIR;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.DELETE_NRM_DATA_FROM_ENM;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.EQUALS;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.FM_ALARM_SUPERVISION_RDN;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.MECONTEXT;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.NETWORK_ELEMENT;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.NULL;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.OSS1_SUBNETWORK_FDN;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.OSS2_SUBNETWORK_FDN;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.OSS3_SUBNETWORK_FDN;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.OSS_PREFIX;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.SHROOT12;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.SUBNETWORK;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.USER_FILE_NAME;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.USER_PASSWORD_PROPERTY;
import static com.ericsson.oss.mediation.cm.taf.testcases.constants.TestCaseConstants.USER_PROPERTY;
import static com.ericsson.oss.testware.enm.cli.matchers.EnmCliResponseMatcher.hasLineContaining;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_CREATE;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_DELETE;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USER_TO_CLEAN_UP;
import static com.ericsson.oss.testware.security.gim.flows.GimCleanupFlows.EnmObjectType.USER;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.data.DataHandler;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarios;
import com.ericsson.cifwk.taf.scenario.impl.LoggingScenarioListener;
import com.ericsson.cifwk.taf.tools.http.HttpTool;
import com.ericsson.oss.mediation.cm.operator.api.exception.CmMediationOperatorException;
import com.ericsson.oss.mediation.cm.operator.impl.lte.WriteLteNodeOperatorImpl;
import com.ericsson.oss.testware.enm.cli.EnmCliResponse;
import com.ericsson.oss.testware.enmbase.data.NetworkNode;
import com.ericsson.oss.testware.nodeintegration.operators.impl.NodeIntegrationOperatorCpp;
import com.ericsson.oss.testware.security.authentication.operators.LoginLogoutRestOperator;
import com.ericsson.oss.testware.security.gim.flows.GimCleanupFlows;
import com.ericsson.oss.testware.security.gim.flows.UserManagementTestFlows;
import com.google.inject.Provider;

/**
 * This class contains the acceptance test cases for the 'AddNode' epic:
 * TORF-931 Create Data - Add Lte Node to TOR.
 */
public class AddNodeTest extends TafTestBase {

    @Inject
    private NodeIntegrationOperatorCpp addRemoveNodeOperator;
    @Inject
    private WriteLteNodeOperatorImpl writeLteNodeOperator;
    @Inject
    private GimCleanupFlows idmCleanupFlows;
    @Inject
    private UserManagementTestFlows userManagementFlows;
    @Inject
    private Provider<LoginLogoutRestOperator> loginOperatorProvider;
    @Inject
    private TestContext context;

    private static final Logger log = LoggerFactory.getLogger(AddNodeTest.class);

    private HttpTool httpTool;
    private boolean isUserLoggedIn = false;
    private final Set<String> networkElementIdTracker = new LinkedHashSet<>();

    @BeforeClass(groups = { "Acceptance", "RFA250" }, alwaysRun = true)
    private void setUp() {
        createUser();
        login();
        initOperators();
    }

    @AfterSuite(groups = { "Acceptance", "RFA250" }, alwaysRun = true)
    public void cleanUp() {
        try {
            cleanupTestData();
            logout();
        } finally {
            startScenario(deleteEnmUser());
        }
    }

    @TestId(id = "TORF-931-001", title = "FunctionalTests - Verify add and delete an ERBS node with user created ossPrefix MOs")
    @Test(groups = { "Acceptance", "RFA250" }, enabled = true)
    @DataDriven(name = "OssPrefixUserCreated")
    public void GIVEN_requestToaddAndRemoveNodes_WHEN_ossPrefixExists_THEN_success(@Input("OssPrefixUserCreated") final NetworkNode node) {

        // Setup
        networkElementIdTracker.add(node.getNetworkElementId());
        createNodeRoot(node);

        // Execution
        final EnmCliResponse response = addRemoveNodeOperator.addSingleNode(node, httpTool);

        // Verify
        verifyResponseHasNoErrors(response);
        verifyNodeWasAdded(node);

        // Cleanup
        addRemoveNodeOperator.removeSingleNode(node, httpTool);
        assertThat(writeLteNodeOperator.getMoFdn(NETWORK_ELEMENT, node.getNetworkElementId()), isEmptyString());
        networkElementIdTracker.remove(node.getNetworkElementId());
    }

    @TestId(id = "TORF-931-004", title = "FunctionalTests - Verify add and delete an ERBS node with system created MeContext MO")
    @Test(groups = { "Acceptance" }, enabled = true)
    @DataDriven(name = "MeContextSystemCreated")
    public void GIVEN_requestToaddAndRemoveNodes_WHEN_meContextDoesNotExist_THEN_success(@Input("MeContextSystemCreated") final NetworkNode node) {

        // Setup
        networkElementIdTracker.add(node.getNetworkElementId());
        assertThat(writeLteNodeOperator.getMoFdn(MECONTEXT, node.getNetworkElementId()), isEmptyString());

        // Execution
        final EnmCliResponse response = addRemoveNodeOperator.addSingleNode(node, httpTool);

        // Verify
        verifyResponseHasNoErrors(response);
        verifyNodeWasAdded(node);

        // Cleanup
        addRemoveNodeOperator.removeSingleNode(node, httpTool);
        assertThat(writeLteNodeOperator.getMoFdn(NETWORK_ELEMENT, node.getNetworkElementId()), isEmptyString());
        networkElementIdTracker.remove(node.getNetworkElementId());
    }

    @TestId(id = "TORF-50815-001", title = "FunctionalTests - Verify node cannot be deleted when supervision is still active on single ERBS node")
    @Test(groups = { "Acceptance" }, enabled = true)
    @DataDriven(name = "SupervisionActive")
    public void GIVEN_requestToDeleteNode_WHEN_SupervisionIsNotDisabled_THEN_nodeIsNotDeleted(@Input("SupervisionActive") final NetworkNode node) {

        // Setup
        networkElementIdTracker.add(node.getNetworkElementId());
        addRemoveNodeOperator.addSingleNode(node, httpTool);
        verifyNodeWasAdded(node);
        writeLteNodeOperator.updateManagedObject(getNetworkElementFdn(node) + COMMA + FM_ALARM_SUPERVISION_RDN, ACTIVE, "true");
        assertThat(writeLteNodeOperator.getAttribute(getNetworkElementFdn(node) + COMMA + FM_ALARM_SUPERVISION_RDN, ACTIVE), equalTo("true"));

        // Execution.
        final EnmCliResponse response =
                writeLteNodeOperator.executeActionOnMo(getNetworkElementFdn(node) + COMMA + CM_FUNCTION_RDN, DELETE_NRM_DATA_FROM_ENM, NULL);

        // Verify
        assertThat(response, hasLineContaining("Error"));
        assertThat(response, hasLineContaining("Supervision or system functions are still active"));
        assertThat(writeLteNodeOperator.getMoFdn(MECONTEXT, node.getNetworkElementId()), equalTo(getMeContextFdn(node)));

        // Cleanup
        addRemoveNodeOperator.removeSingleNode(node, httpTool);
        assertThat(writeLteNodeOperator.getMoFdn(NETWORK_ELEMENT, node.getNetworkElementId()), isEmptyString());
        networkElementIdTracker.remove(node.getNetworkElementId());
    }

    @TestId(id = "TORF-50815-002", title = "FunctionalTests - Verify NetworkElement can be deleted when associated MeContext exists")
    @Test(groups = { "Acceptance" }, enabled = true)
    @DataDriven(name = "DeleteNode")
    public void GIVEN_requestToDeleteNode_WHEN_meContextExists_AND_nodeWasNeverSynched_THEN_success(@Input("DeleteNode") final NetworkNode node)
            throws CmMediationOperatorException {

        networkElementIdTracker.add(node.getNetworkElementId());
        addRemoveNodeOperator.addSingleNode(node, httpTool);
        verifyNodeWasAdded(node);

        // Execution
        writeLteNodeOperator.deleteManagedObject(getNetworkElementFdn(node));

        // Verify
        assertThat(writeLteNodeOperator.getMoFdn(NETWORK_ELEMENT, node.getNetworkElementId()), isEmptyString());

        // Cleanup
        networkElementIdTracker.remove(node.getNetworkElementId());
    }

    private void createNodeRoot(final NetworkNode node) {
        final String ossPrefix = node.getOssPrefix();
        if (ossPrefix != null && ossPrefix.contains(SUBNETWORK)) {
            addRemoveNodeOperator.createSubNetwork(ossPrefix, httpTool);
        }
    }

    private String getNetworkElementFdn(final NetworkNode node) {
        return NETWORK_ELEMENT + EQUALS + node.getNetworkElementId();
    }

    private String getMeContextFdn(final NetworkNode node) {
        final String ossPrefix = node.getOssPrefix();
        if (ossPrefix != null && ossPrefix.contains(SUBNETWORK)) {
            return ossPrefix.contains(MECONTEXT) ? ossPrefix : ossPrefix.concat(COMMA + MECONTEXT + EQUALS + node.getNetworkElementId());
        }
        return MECONTEXT + EQUALS + node.getNetworkElementId();
    }

    private void verifyResponseHasNoErrors(final EnmCliResponse response) {
        assertThat(response, not(hasLineContaining("Error")));
        assertThat(response, not(hasLineContaining("matching object already exists")));
    }

    private void verifyNodeWasAdded(final NetworkNode node) {
        assertThat(addRemoveNodeOperator.confirmSingleNodeAdded(node, httpTool), equalTo(true));

        final String ossPrefix = writeLteNodeOperator.getAttribute(getNetworkElementFdn(node), OSS_PREFIX);

        final int waitIntervalSecs = 3;
        final int maxRetries = 5;
        final String fdn = writeLteNodeOperator.getMoFdn(MECONTEXT, node.getNetworkElementId(), waitIntervalSecs, maxRetries);

        assertThat(fdn, equalTo(ossPrefix));
    }

    private void login() {
        final String userName = (String) DataHandler.getAttribute(USER_PROPERTY);
        final String password = (String) DataHandler.getAttribute(USER_PASSWORD_PROPERTY);
        log.info("Logging in with user [{}], and password [{}]", userName, password);
        httpTool = loginOperatorProvider.get().login(userName, password);
        isUserLoggedIn = true;
    }

    private void logout() {
        if (isUserLoggedIn) {
            loginOperatorProvider.get().logout(httpTool);
        }
    }

    private void initOperators() {
        writeLteNodeOperator.setHttpTool(httpTool);
    }

    private void createUser() {
        context.addDataSource(USERS_TO_CREATE, fromCsv(DATA_DIR + USER_FILE_NAME + CSV_EXTENSITON));
        context.addDataSource(USER_TO_CLEAN_UP, fromCsv(DATA_DIR + USER_FILE_NAME + CSV_EXTENSITON));
        if (DataHandler.getAttribute(USER_PROPERTY) == null) {
            startScenario(creteEnmUser());
            DataHandler.setAttribute(USER_PROPERTY, ADD_DOOZER);
            DataHandler.setAttribute(USER_PASSWORD_PROPERTY, SHROOT12);
        }
    }

    private TestScenario creteEnmUser() {
        return TestScenarios.scenario("Create Users")
                .addFlow(idmCleanupFlows.cleanUp(USER))
                .addFlow(userManagementFlows.createUserWithoutRoleVerification())
                .build();
    }

    private void startScenario(final TestScenario scenario) {
        TestScenarios.runner()
                .withListener(new LoggingScenarioListener())
                .build()
                .start(scenario);
    }

    private TestScenario deleteEnmUser() {
        context.addDataSource(USERS_TO_DELETE, fromCsv(DATA_DIR + USER_FILE_NAME + CSV_EXTENSITON));

        return TestScenarios.scenario()
                .addFlow(userManagementFlows.deleteUser())
                .build();
    }

    private void cleanupTestData() {
        if (isUserLoggedIn) {
            deleteNodesLeftDueToFailedTests();
            writeLteNodeOperator.deleteManagedObject(OSS1_SUBNETWORK_FDN);
            writeLteNodeOperator.deleteManagedObject(OSS2_SUBNETWORK_FDN);
            writeLteNodeOperator.deleteManagedObject(OSS3_SUBNETWORK_FDN);
        }
    }

    private void deleteNodesLeftDueToFailedTests() {
        try {
            for (final String id : networkElementIdTracker) {
                writeLteNodeOperator.deleteManagedObject(NETWORK_ELEMENT + EQUALS + id);
            }
        } catch (final Exception e) {
            log.warn("Failed to cleanup nodes after failed test cases.");
        }
    }

}
