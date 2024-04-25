package com.bitheads.braincloud.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


import com.bitheads.braincloud.services.Helpers;
import com.bitheads.braincloud.services.TestFixtureNoAuth;
import com.bitheads.braincloud.services.TestResult;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;


@RunWith(AndroidJUnit4.class)
public class BrainCloudWrapperTest extends TestFixtureNoAuth {
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // this forces us to create a new anonymous account
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        _wrapper.setContext(ctx);
        _wrapper.setStoredAnonymousId("");
        _wrapper.setStoredProfileId("");
    }

    @Test
    public void testAuthenticateAnonymous() {
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateAnonymous(tr);
        tr.Run();

        String anonId = _wrapper.getStoredAnonymousId();
        String profileId = _wrapper.getStoredProfileId();

        Logout();

        // Logout() calls resetCommunication so must re-initialize to continue
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        _wrapper.getClient().getAuthenticationService().clearSavedProfileId();

        _wrapper.authenticateAnonymous(tr);
        tr.Run();

        Assert.assertEquals(anonId, _wrapper.getStoredAnonymousId());
        Assert.assertEquals(profileId, _wrapper.getStoredProfileId());

        Logout();
    }

    @Test
    public void testAuthenticateEmailPassword() {
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        String email = getUser(Users.UserA).email;

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateEmailPassword(email, getUser(Users.UserA).password, true, tr);
        tr.Run();

        Logout();
    }

    @Test
    public void testAuthenticateUniversal(){
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        String uid = getUser(Users.UserA).id;
        uid += "_wrapper";

        _wrapper.authenticateUniversal(uid, getUser(Users.UserA).password, true, tr);
        tr.Run();

        Logout();
    }

    @Test
    public void testAuthenticateHandoff() {
        String handoffId;
        String handoffToken;
        TestResult tr = new TestResult(_wrapper);
        String anonId = _client.getAuthenticationService().generateAnonymousId();

        _client.getAuthenticationService().authenticateAnonymous(anonId, true, tr);
        tr.Run();

        _client.getScriptService().runScript("createHandoffId",
                Helpers.createJsonPair("", ""),
                tr);
        tr.Run();

        try {
            handoffId = tr.m_response.getJSONObject("data").getJSONObject("response").getString("handoffId");
            handoffToken = tr.m_response.getJSONObject("data").getJSONObject("response").getString("securityToken");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        _wrapper.authenticateHandoff(handoffId, handoffToken, tr);
        tr.Run();
    }

    @Test
    public void testAuthenticateSettopHandoff() {
        String handoffCode;
        TestResult tr = new TestResult(_wrapper);
        String anonId = _client.getAuthenticationService().generateAnonymousId();

        _client.getAuthenticationService().authenticateAnonymous(anonId, true, tr);
        tr.Run();

        _client.getScriptService().runScript(
                "CreateSettopHandoffCode",
                Helpers.createJsonPair("", ""),
                tr);
        tr.Run();

        try {
            handoffCode = tr.m_response.getJSONObject("data").getJSONObject("response").getString("handoffCode");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        _wrapper.authenticateSettopHandoff(handoffCode, tr);
        tr.Run();
    }

    @Test
    public void testManualRedirect() throws Exception {
        _wrapper.initialize(m_redirectAppId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateAnonymous(tr);

        tr.Run();
    }

    @Test
    public void canReconnectTrue(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        // Authenticate
        _wrapper.authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr);
        tr.Run();

        // Log out
        _wrapper.logout(false, tr);
        tr.Run();

        // Check canReconnect()
        assertEquals(true, _wrapper.canReconnect());
    }

    @Test
    public void canReconnectFalse(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        // Authenticate
        _wrapper.authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr);
        tr.Run();

        // Log out
        _wrapper.logout(true, tr);
        tr.Run();

        // Check canReconnect()
        assertEquals(false, _wrapper.canReconnect());
    }

    @Test
    public void reconnectExpectSuccess(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        // Authenticate
        _wrapper.authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr);
        tr.Run();

        // Log out
        _wrapper.logout(false, tr);
        tr.Run();

        // Check canReconnect()
        if(_wrapper.canReconnect()){
            _wrapper.reconnect(tr);
            tr.Run();
        }
        else fail("canReconnect returned false but should have been true");
    }

    @Test
    public void reconnectExpectFail(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        // Authenticate
        _wrapper.authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr);
        tr.Run();

        // Log out
        _wrapper.logout(true, tr);
        tr.Run();

        // Check canReconnect()
        if (_wrapper.canReconnect()) {
            fail("canReconnect returned true but should have been false");
        }
        else {
            System.out.println("Attempting to reconnect to confirm that it shouldn't be possible. . .");
            _wrapper.reconnect(tr);
            tr.RunExpectFail(202, 40208);
        }
    }

    @Test
    public void testReInitWrapper() throws Exception {
        //case 1 Multiple init on client
        Map<String, String> originalAppSecretMap = new HashMap<String, String>();
        originalAppSecretMap.put(m_appId, m_secret);
        originalAppSecretMap.put(m_childAppId, m_childSecret);

        int initCounter = 1;
        _wrapper.initializeWithApps(m_serverUrl, m_appId, originalAppSecretMap, m_appVersion);
        Assert.assertTrue(initCounter == 1);
        initCounter++;

        _wrapper.initializeWithApps(m_serverUrl, m_appId, originalAppSecretMap, m_appVersion);
        Assert.assertTrue(initCounter == 2);
        initCounter++;

        _wrapper.initializeWithApps(m_serverUrl, m_appId, originalAppSecretMap, m_appVersion);
        Assert.assertTrue(initCounter == 3);

        //case 2
        //Auth
        TestResult tr1 = new TestResult(_wrapper);
        _wrapper.getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr1);
        tr1.Run();

        //Call
        TestResult tr2 = new TestResult(_wrapper);
        _wrapper.getTimeService().readServerTime(
                tr2);
        tr2.Run();

        //reinit
        _wrapper.initializeWithApps(m_serverUrl, m_appId, originalAppSecretMap, m_appVersion);

        //call without auth - expecting it to fail because we need to reauth after init
        TestResult tr3 = new TestResult(_wrapper);
        _wrapper.getTimeService().readServerTime(
                tr3);
        tr3.RunExpectFail(StatusCodes.FORBIDDEN, ReasonCodes.NO_SESSION);
    }

    public void testLogout(boolean forgetUser) {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.authenticateUniversal(
                getUser(Users.UserA).id,
                getUser(Users.UserA).password,
                true,
                tr);
        tr.Run();

        _wrapper.logout(forgetUser, tr);
        tr.Run();

        Assert.assertEquals(forgetUser, _wrapper.getStoredProfileId().equals(""));
    }

    @Test
    public void testLogoutRememberUser() {
        testLogout(false);
    }

    @Test
    public void testLogoutForgetUser() {
        testLogout(true);
    }

    @Test
    public void testResetEmailPassword(){
        TestResult tr = new TestResult(_wrapper);

        String email = "braincloudunittest@gmail.com";

        _wrapper.authenticateEmailPassword(email, email, true, tr);
        tr.Run();

        _wrapper.resetEmailPassword(email, tr);
        tr.Run();
    }

    @Test
    public void testResetEmailPasswordAdvanced() {
        TestResult tr = new TestResult(_wrapper);

        String email = "braincloudunittest@gmail.com";
        String serviceParams = "{\"fromAddress\": \"fromAddress\",\"fromName\": \"fromName\",\"replyToAddress\": \"replyToAddress\",\"replyToName\": \"replyToName\", \"templateId\": \"8f14c77d-61f4-4966-ab6d-0bee8b13d090\",\"subject\": \"subject\",\"body\": \"Body goes here\", \"substitutions\": { \":name\": \"John Doe\",\":resetLink\": \"www.dummuyLink.io\"}, \"categories\": [\"category1\",\"category2\" ]}";

        _wrapper.authenticateEmailPassword(email, email, true, tr);
        tr.Run();

        _wrapper.resetEmailPasswordAdvanced(email, serviceParams, tr);
        tr.RunExpectFail(StatusCodes.BAD_REQUEST, ReasonCodes.INVALID_FROM_ADDRESS);
    }

    @Test
    public void testSmartSwitchAnonToUniversal(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.authenticateAnonymous(tr);
        tr.Run();

        _wrapper.smartSwitchAuthenticateUniversal("testAuth", "testPass", true, tr);
        tr.Run();
    }

    @Test
    public void testSmartSwitchUniversalToEmail(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr);
        tr.Run();

        _wrapper.smartSwitchAuthenticateEmail("testAuth", "testPass", true, tr);
        tr.Run();
    }

    @Test
    public void testSmartSwitchNoAuthToEmail(){
        TestResult tr = new TestResult(_wrapper);

        _wrapper.smartSwitchAuthenticateEmail("testAuth", "testPass", true, tr);
        tr.Run();
    }

    @Test
    public void testVerifyAlwaysAllowProfileFalse(){
        String uid = getUser(Users.UserA).id;
        uid += "_wrapperVerifyAlwaysAllowProfileFalse";

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);
        _wrapper.setAlwaysAllowProfileSwitch(false);

        // this forces us to create a new anonymous account
        _wrapper.setStoredAnonymousId("");
        _wrapper.setStoredProfileId("");

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateAnonymous(tr);
        tr.Run();

        String anonId = _wrapper.getStoredAnonymousId();
        String profileId = _wrapper.getStoredProfileId();

        _wrapper.getIdentityService().attachUniversalIdentity(uid, uid, tr);
        tr.Run();

        Logout();

        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        _wrapper.authenticateUniversal(uid, uid, true, tr);
        tr.Run();

        Assert.assertEquals(anonId, _wrapper.getStoredAnonymousId());
        Assert.assertEquals(profileId, _wrapper.getStoredProfileId());

        _wrapper.setAlwaysAllowProfileSwitch(true);
        Logout();
    }
}