package com.bitheads.braincloud.client;

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
public class BrainCloudWrapperTest extends TestFixtureNoAuth
{
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        // this forces us to create a new anonymous account
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        _wrapper.setContext(ctx);
        _wrapper.setStoredAnonymousId("");
        _wrapper.setStoredProfileId("");
    }

    @Test
    public void testAuthenticateAnonymous()
    {
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
    public void testManualRedirect() throws Exception
    {
        _wrapper.initialize(m_redirectAppId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateAnonymous(tr);

        tr.Run();
    }

    @Test
    public void testAuthenticateEmailPassword()
    {
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        String email = getUser(Users.UserA).email;
        email += "_wrapper";

        TestResult tr = new TestResult(_wrapper);
        _wrapper.authenticateEmailPassword(email, getUser(Users.UserA).password, true, tr);
        tr.Run();

        Logout();
    }

    @Test
    public void testAuthenticateUniversal()
    {
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        String uid = getUser(Users.UserA).id;
        uid += "_wrapper";
        _wrapper.authenticateUniversal(uid, getUser(Users.UserA).password, true, tr);
        tr.Run();

        Logout();
    }

    @Test
    public void testAuthenticateHandoff(){
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
    public void testAuthenticateSettopHandoff(){
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
    public void testReconnect()
    {
        _wrapper.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);

        TestResult tr = new TestResult(_wrapper);
        String uid = getUser(Users.UserA).id;
        uid += "_wrapper";
        _wrapper.authenticateUniversal(uid, getUser(Users.UserA).password, true, tr);
        tr.Run();

        _wrapper.getClient().getPlayerStateService().logout(tr);
        tr.Run();

        _wrapper.reconnect(tr);
        tr.Run();

        Logout();
    }
    @Test
    public void testReInitWrapper() throws Exception
    {
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

    public void testLogout(boolean forgetUser){
        TestResult tr = new TestResult(_wrapper);

        Log.d("brainCloudWrapper test", "Authenticating . . .");
        _wrapper.authenticateUniversal(
                getUser(Users.UserA).id,
                getUser(Users.UserA).password,
                true,
                tr);
        tr.Run();

        Log.d("brainCloudWrapper test", "Logging out . . .");
        _wrapper.logout(forgetUser, tr);
        tr.Run();

        Assert.assertEquals(forgetUser, _wrapper.getStoredProfileId().equals(""));
    }

    @Test
    public void testLogoutRememberUser(){
        testLogout(false);
    }

    @Test
    public void testLogoutForgetUser(){
        testLogout(true);
    }
/*
    @Test
    public void testVerifyAlwaysAllowProfileFalse()
    {
        BrainCloudWrapper bcw = BrainCloudWrapper.getInstance();
        bcw.initialize(m_appId, m_secret, m_appVersion, m_serverUrl);
        bcw.setAlwaysAllowProfileSwitch(false);

        // this forces us to create a new anonymous account
        bcw.setStoredAnonymousId("");
        bcw.setStoredProfileId("");

        TestResult tr = new TestResult(_wrapper);
        bcw.authenticateAnonymous(tr);
        tr.Run();

        String anonId = bcw.getStoredAnonymousId();
        String profileId = bcw.getStoredProfileId();
        String uid = "aaa";//getUser(Users.UserA).id;
        uid += "_wrapperVerifyAlwaysAllowProfileFalse";

        BrainCloudWrapper.getBC().getIdentityService().attachUniversalIdentity(uid, uid, tr);
        tr.Run();

        Logout();
        BrainCloudWrapper.getBC().getAuthenticationService().clearSavedProfileId();

        bcw.authenticateUniversal(uid, uid, true, tr);
        tr.Run();

        Assert.assertEquals(anonId, bcw.getStoredAnonymousId());
        Assert.assertEquals(profileId, bcw.getStoredProfileId());

        bcw.setAlwaysAllowProfileSwitch(true);
        Logout();
    }*/
}