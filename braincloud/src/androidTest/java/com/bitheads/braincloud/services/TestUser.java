package com.bitheads.braincloud.services;

import org.json.JSONException;

import com.bitheads.braincloud.client.IBrainCloudWrapper;

/**
 * Created by prestonjennings on 15-09-01.
 */

/// <summary>
/// Holds data for a randomly generated user
/// </summary>
public class TestUser
{
    public String id = "";
    public String password = "";
    public String profileId = "";
    public String email = "";

    IBrainCloudWrapper _wrapper;


    public TestUser(IBrainCloudWrapper wrapper, String idPrefix, int suffix, boolean authWithEmail)
    {
        _wrapper = wrapper;
        id = idPrefix + suffix;
        password = id;
        email = id + "@test.getbraincloud.com";
        Authenticate(authWithEmail);
        System.out.println("authWithEmail post");
    }

	 @SuppressWarnings("deprecation")
    private void Authenticate(boolean withEmail)
    {
        TestResult tr = new TestResult(_wrapper);
        if(!withEmail) {
            _wrapper.authenticateUniversal(
                    id,
                    password,
                    true,
                    tr);
            tr.Run();
        }
        if(withEmail) {
            _wrapper.getClient().getAuthenticationService().authenticateEmailPassword(
                    email,
                    password,
                    true,
                    tr);
            tr.Run();
        }
        
        profileId = _wrapper.getClient().getAuthenticationService().getProfileId();

        try
        {
            if (tr.m_response.getJSONObject("data").getBoolean("newUser") == true)
            {
                _wrapper.getMatchMakingService().enableMatchMaking(tr);
                tr.Run();
                _wrapper.getPlayerStateService().updateName(id, tr);
                tr.Run();
                _wrapper.getPlayerStateService().updateContactEmail("braincloudunittest@gmail.com", tr);
                tr.Run();
            }

            _wrapper.getPlayerStateService().logout(tr);
            tr.Run();
        }
        catch(JSONException je)
        {
            je.printStackTrace();
        }
    }
}
