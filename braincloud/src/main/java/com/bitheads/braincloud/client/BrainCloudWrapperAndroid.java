package com.bitheads.braincloud.client;

import android.content.Context;
import android.content.SharedPreferences;

import com.bitheads.braincloud.services.AppStoreService;
import com.bitheads.braincloud.services.AsyncMatchService;
import com.bitheads.braincloud.services.AuthenticationService;
import com.bitheads.braincloud.services.ChatService;
import com.bitheads.braincloud.services.DataStreamService;
import com.bitheads.braincloud.services.EntityService;
import com.bitheads.braincloud.services.EventService;
import com.bitheads.braincloud.services.FileService;
import com.bitheads.braincloud.services.FriendService;
import com.bitheads.braincloud.services.GamificationService;
import com.bitheads.braincloud.services.GlobalAppService;
import com.bitheads.braincloud.services.GlobalEntityService;
import com.bitheads.braincloud.services.GlobalStatisticsService;
import com.bitheads.braincloud.services.GroupFileService;
import com.bitheads.braincloud.services.GroupService;
import com.bitheads.braincloud.services.IdentityService;
import com.bitheads.braincloud.services.LobbyService;
import com.bitheads.braincloud.services.MailService;
import com.bitheads.braincloud.services.MessagingService;
import com.bitheads.braincloud.services.MatchMakingService;
import com.bitheads.braincloud.services.OneWayMatchService;
import com.bitheads.braincloud.services.PlaybackStreamService;
import com.bitheads.braincloud.services.PlayerStateService;
import com.bitheads.braincloud.services.PlayerStatisticsEventService;
import com.bitheads.braincloud.services.PlayerStatisticsService;
import com.bitheads.braincloud.services.PresenceService;
import com.bitheads.braincloud.services.ProfanityService;
import com.bitheads.braincloud.services.PushNotificationService;
import com.bitheads.braincloud.services.RedemptionCodeService;
import com.bitheads.braincloud.services.RelayService;
import com.bitheads.braincloud.services.RTTService;
import com.bitheads.braincloud.services.S3HandlingService;
import com.bitheads.braincloud.services.ScriptService;
import com.bitheads.braincloud.services.SocialLeaderboardService;
import com.bitheads.braincloud.services.TimeService;
import com.bitheads.braincloud.services.TournamentService;
import com.bitheads.braincloud.services.GlobalFileService;
import com.bitheads.braincloud.services.CustomEntityService;
import com.bitheads.braincloud.services.VirtualCurrencyService;
import com.bitheads.braincloud.services.ItemCatalogService;
import com.bitheads.braincloud.services.UserItemsService;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;
import android.os.Build;

/**
 * The BrainCloudWrapper provides some convenience functionality to developers when they are
 * getting started with the authentication system.
 * <p>
 * By using the wrapper authentication methods, the anonymous and profile ids will be automatically
 * persisted upon successful authentication. When authenticating, any stored anonymous/profile ids will
 * be sent to the server. This strategy is useful when using anonymous authentication.
 */
public class BrainCloudWrapperAndroid implements IServerCallback, IBrainCloudWrapper {

    private static final String AUTHENTICATION_ANONYMOUS = "anonymous";
    private static final String _SHARED_PREFERENCES = "bcprefs";
    private static final String _DEFAULT_URL = "https://api.braincloudservers.com/dispatcherv2";

    private static BrainCloudWrapperAndroid _instance = null;

    private Context _context = null;

    private boolean _alwaysAllowProfileSwitch = true;
    private IServerCallback _authenticateCallback = null;

    private BrainCloudClient _client = null;
    private String _wrapperName = "";

    //get the release platform
    public Platform getReleasePlatform() {
        return _client.getReleasePlatform();
    }
    public void setReleasePlatform(Platform releasePlatform) {
        getClient().setReleasePlatform(releasePlatform);
    }

    /**
     * Returns a singleton instance of the BrainCloudClient, if this is the BrainCloudWrapper Singleton.
     * Otherwise, return an instance of the BrainCloudClient, if this is an instance of the BrainCloudWrapper.
     *
     * @return A singleton instance of the BrainCloudClient.
     */
    public BrainCloudClient getClient() {
        return _client;
    }

    public BrainCloudWrapperAndroid() {
        _client = new BrainCloudClient();
    }

    /**
     * Instantiate a copy of the brainCloud wrapper. Don't use getInstance if creating your own copy.
     *
     * @param wrapperName value used to differentiate saved wrapper data
     */
    public BrainCloudWrapperAndroid(String wrapperName) {
        _wrapperName = wrapperName;
        _client = new BrainCloudClient();
    }

    /**
     * Method returns a singleton instance of the BrainCloudWrapper.
     *
     * @return A singleton instance of the BrainCloudWrapper.
     *
     * @deprecated Use of the *singleton* has been deprecated. We recommend that you create your own *variable* to hold an instance of the brainCloudWrapper. Explanation here: http://getbraincloud.com/apidocs/release-3-6-5/
     */
    public static BrainCloudWrapperAndroid getInstance() {

        if (BrainCloudClient.EnableSingletonMode == false) {
            throw new AssertionError(BrainCloudClient.SingletonUseErrorMessage);
        }

        if (_instance == null) {
            _instance = new BrainCloudWrapperAndroid();
            BrainCloudClient.setInstance(_instance.getClient());
        }

        return _instance;
    }

    /**
     * Returns a singleton instance of the BrainCloudClient.
     *
     * @return A singleton instance of the BrainCloudClient.
     */
    public static BrainCloudClient getBC() {
        return getInstance().getClient();
    }

    /**
     * Sets the context required for saving anonymous and profile ids to the
     * private SharedPreferences file.
     *
     * @param ctx The application context
     */
    public void setContext(Context ctx) {
        _context = ctx;
    }

    private void detectPlatform()
    {
        if(Build.MANUFACTURER != null)
        {
            //detect amazon. This is needed to distinguish between google android devices and amazon android devices.
            if(Build.MANUFACTURER.equals("Amazon")) {
                setReleasePlatform(getReleasePlatform().detectGenericPlatform(Build.MANUFACTURER));
            }
            else
            {
                getClient().setReleasePlatform(Platform.GooglePlayAndroid);
            }
        }
        else
        {
            getClient().setReleasePlatform(Platform.GooglePlayAndroid);
        }
    }

    private class InitializeParams
    {
        public String appId = "";
        public String secretKey = "";
        public String appVersion = "";
        public String serverUrl = "";
        public Map<String, String> secretMap = null;
    }
    private InitializeParams m_initializeParams = new InitializeParams();

    /**
     * Method initializes the BrainCloudClient.
     *
     * @param ctx        The application context
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     */
    public void initialize(Context ctx, String appId, String secretKey, String appVersion) {
        setContext(ctx);

        m_initializeParams.appId = appId;
        m_initializeParams.secretKey = secretKey;
        m_initializeParams.appVersion = appVersion;
        m_initializeParams.serverUrl = _DEFAULT_URL;
        m_initializeParams.secretMap = null;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }

        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initialize(_DEFAULT_URL,appId, secretKey, appVersion);
    }

    /**
     * Method initializes the BrainCloudClient.
     *
     * @param ctx        The application context
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     * @param serverUrl  The url to the brainCloud server
     */
    public void initialize(Context ctx, String appId, String secretKey, String appVersion, String serverUrl) {
        setContext(ctx);

        m_initializeParams.appId = appId;
        m_initializeParams.secretKey = secretKey;
        m_initializeParams.appVersion = appVersion;
        m_initializeParams.serverUrl = serverUrl;
        m_initializeParams.secretMap = null;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }

        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initialize(serverUrl, appId, secretKey, appVersion);
    }


    /**
     * Method initializes the BrainCloudClient. Make sure to
     * set the context via setContext() if you're using this method.
     *
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     * @param serverUrl  The url to the brainCloud server
     */
    public void initialize(String appId, String secretKey, String appVersion, String serverUrl) {

        m_initializeParams.appId = appId;
        m_initializeParams.secretKey = secretKey;
        m_initializeParams.appVersion = appVersion;
        m_initializeParams.serverUrl = serverUrl;
        m_initializeParams.secretMap = null;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }
        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initialize(serverUrl, appId, secretKey, appVersion);
    }

    /**
     * Method initializes the BrainCloudClient. Make sure to
     * set the context via setContext() if you're using this method.
     *
     * @param appId      The app id
     * @param secretKey  The secret key for your app
     * @param appVersion The app version
     */
    public void initialize(String appId, String secretKey, String appVersion) {

        m_initializeParams.appId = appId;
        m_initializeParams.secretKey = secretKey;
        m_initializeParams.appVersion = appVersion;
        m_initializeParams.serverUrl = _DEFAULT_URL;
        m_initializeParams.secretMap = null;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }
        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initialize(appId, secretKey, appVersion);
    }

    public void initializeWithApps(String url, String appId, Map<String, String> secretMap, String version, String companyName, String appName)
    {
        m_initializeParams.appId = appId;
        m_initializeParams.secretKey = "";
        m_initializeParams.appVersion = version;
        m_initializeParams.serverUrl = url;
        m_initializeParams.secretMap = secretMap;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }

        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initializeWithApps(url, appId, secretMap, version);
    }

    /**
     * Method initializes the BrainCloudClient. - Note this is here for toggling purposes to testdifferent initializations.
     *
     * @param url The URL to the brainCloud server
     * @param defaultAppId The default app ID
     * @param secretMap All app ids to secret keys used by this application
     * @param version The app version
     */
    public void initializeWithApps(String url, String defaultAppId, Map<String, String> secretMap, String version)
    {
        m_initializeParams.appId = defaultAppId;
        m_initializeParams.secretKey = "";
        m_initializeParams.appVersion = version;
        m_initializeParams.serverUrl = url;
        m_initializeParams.secretMap = secretMap;

        if(_client == null)
        {
            _client = new BrainCloudClient();
        }

        //need to do detection in the wrapper because java doesn't recognize defines or precompiler statements...
        //Both java_desktop and java_android have lib specific ways of detecting platforms and they are not cross compatible.
        detectPlatform();

        getClient().initializeWithApps(url, defaultAppId, secretMap, version);
    }

    protected void initializeIdentity(boolean isAnonymousAuth) {

        // check if we already have saved IDs
        String profileId = getStoredProfileId();
        String anonymousId = getStoredAnonymousId();

        // create an anonymous ID if necessary
        if ((!anonymousId.isEmpty() && profileId.isEmpty()) || anonymousId.isEmpty()) {
            anonymousId = getClient().getAuthenticationService().generateAnonymousId();
            profileId = "";
            setStoredAnonymousId(anonymousId);
            setStoredProfileId(profileId);
        }

        String profileIdToAuthenticateWith = profileId;
        if (!isAnonymousAuth && _alwaysAllowProfileSwitch) {
            profileIdToAuthenticateWith = "";
        }
        setStoredAuthenticationType(isAnonymousAuth ? AUTHENTICATION_ANONYMOUS : "");

        // send our IDs to brainCloud
        getClient().initializeIdentity(profileIdToAuthenticateWith, anonymousId);
    }


    /**
     * Combines the wrapperName and the _SHARED_PREFERENCES to create a unique save name
     *
     * ie. userone_bcprefs
     *
     * @return unique save name
     */
    private String getSaveName() {
        String prefix = _wrapperName.isEmpty() ? "" : "_" + _wrapperName;
        return prefix + _SHARED_PREFERENCES;
    }

    /**
     * Returns the stored profile id
     *
     * @return The stored profile id
     */
    public String getStoredProfileId() {
        String saveName = getSaveName();

        SharedPreferences sharedPref = _context.getSharedPreferences(saveName, Context.MODE_PRIVATE);
        return sharedPref.getString("profileId", "");
    }

    /**
     * Sets the stored profile id
     *
     * @param profileId The profile id to set
     */
    public void setStoredProfileId(String profileId) {
        String saveName = getSaveName();

        SharedPreferences sharedPref = _context.getSharedPreferences(saveName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("profileId", profileId);
        editor.commit();
    }


    /**
     * Resets the profile id to empty string
     */
    public void resetStoredProfileId() {
        setStoredProfileId("");
    }

    /**
     * Returns the stored anonymous id
     *
     * @return The stored anonymous id
     */
    String getStoredAnonymousId() {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getString("anonymousId", "");
    }

    /**
     * Sets the stored anonymous id
     *
     * @param anonymousId The anonymous id to set
     */
    public void setStoredAnonymousId(String anonymousId) {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("anonymousId", anonymousId);
        editor.commit();
    }

    /**
     * Resets the anonymous id to empty string
     */
    public void resetStoredAnonymousId() {
        setStoredAnonymousId("");
    }

    /**
     * For non-anonymous authentication methods, a profile id will be passed in
     * when this value is set to false. This will generate an error on the server
     * if the profile id passed in does not match the profile associated with the
     * authentication credentials. By default, this value is true.
     *
     * @param alwaysAllow Controls whether the profile id is passed in with
     *                    non-anonymous authentications.
     */
    public void setAlwaysAllowProfileSwitch(boolean alwaysAllow) {
        _alwaysAllowProfileSwitch = alwaysAllow;
    }

    /**
     * Returns the value for always allow profile switch
     *
     * @return Whether to always allow profile switches
     */
    public boolean getAlwaysAllowProfileSwitch() {
        return _alwaysAllowProfileSwitch;
    }

    // these methods are not really used
    protected String getStoredAuthenticationType() {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getString("authenticationType", "");
    }

    protected void setStoredAuthenticationType(String authenticationType) {
        SharedPreferences sharedPref = _context.getSharedPreferences(_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("authenticationType", authenticationType);
        editor.commit();
    }

    protected void resetStoredAuthenticationType() {
        setStoredAuthenticationType("");
    }

    /**
     * Authenticate a user anonymously with brainCloud - used for apps that
     * don't want to bother the user to login, or for users who are sensitive to
     * their privacy
     *
     * @param callback The callback handler
     */
    public void authenticateAnonymous(IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(true);

        getClient().getAuthenticationService().authenticateAnonymous(true, this);
    }

    /**
     * Authenticate the user using a handoffId and an authentication token.
     *
     * @param handoffId   braincloud handoffId generated frim cloud script
     * @param securityToken The authentication token
     * @param callback   The callback handler
     */
    public void authenticateHandoff(String handoffId, String securityToken, IServerCallback callback) {
        _authenticateCallback = callback;

        getClient().getAuthenticationService().authenticateHandoff(handoffId, securityToken, this);
    }

    /**
     * Authenticate the user using a handoffId and an authentication token.
     *
     * @param handoffCode generate in cloud code
     * @param callback   The callback handler
     */
    public void authenticateSettopHandoff(String handoffCode, IServerCallback callback) {
        _authenticateCallback = callback;

        getClient().getAuthenticationService().authenticateSettopHandoff(handoffCode, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email       The e-mail address of the user
     * @param password    The password of the user
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateEmailPassword(String email,
                                          String password,
                                          boolean forceCreate,
                                          IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateEmailPassword(email, password, forceCreate, this);
    }

    /**
     * Authenticate the user via cloud code (which in turn validates the supplied credentials against an external system).
     * This allows the developer to extend brainCloud authentication to support other backend authentication systems.
     * <p>
     * Service Name - Authenticate
     * Server Operation - Authenticate
     *
     * @param userId           The user id
     * @param token            The user token (password etc)
     * @param externalAuthName The name of the cloud script to call for external authentication
     * @param forceCreate      Should a new profile be created for this user if the account
     *                         does not exist?
     */
    public void authenticateExternal(String userId,
                                     String token,
                                     String externalAuthName,
                                     boolean forceCreate,
                                     IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateExternal(userId, token, externalAuthName, forceCreate, this);
    }

    /**
     * Authenticate the user with brainCloud using their Facebook Credentials
     *
     * @param fbUserId    The facebook id of the user
     * @param fbAuthToken The validated token from the Facebook SDK (that will be
     *                    further validated when sent to the bC service)
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateFacebook(String fbUserId,
                                     String fbAuthToken,
                                     boolean forceCreate,
                                     IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateFacebook(fbUserId, fbAuthToken, forceCreate, this);
    }

    /**
     * Authenticate the user with brainCloud using their FacebookLimited Credentials
     *
     * @param fbLimitedUserId    The facebookLimited id of the user
     * @param fbAuthToken The validated token from the Facebook SDK (that will be
     *                    further validated when sent to the bC service)
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateFacebookLimited(String fbLimitedUserId,
                                            String fbAuthToken,
                                            boolean forceCreate,
                                            IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateFacebookLimited(fbLimitedUserId, fbAuthToken, forceCreate, this);
    }

    /**
     * Authenticate the user with brainCloud using their Oculus Credentials
     *
     * @param oculusUserId    The oculus id of the user
     * @param oculusNonce from the Oculus SDK (that will be
     *                    further validated when sent to the bC service)
     * @param forceCreate Should a new profile be created for this user if the account
     *                    does not exist?
     * @param callback    The callback handler
     */
    public void authenticateOculus(String oculusUserId,
                                   String oculusNonce,
                                   boolean forceCreate,
                                   IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateOculus(oculusUserId, oculusNonce, forceCreate, this);
    }

    /**
     *Authenticate the user using an apple id
     *
     * @param appleUserId  This can be the user id OR the email of the user for the account
     * @param identityToken The token confirming the user's identity
     * @param forceCreate     Should a new profile be created for this user if the account
     *                        does not exist?
     * @param callback        The callback handler
     */
    public void authenticateApple(String appleUserId, String identityToken, boolean forceCreate, IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateApple(appleUserId, identityToken, forceCreate, this);
    }

    /**
     * Authenticate the user using a google userid(email address) and google
     * authentication token.
     *
     * @param googleUserId    String representation of google+ userId. Gotten with calls like RequestUserId
     * @param serverAuthCode The server authentication token derived via the google apis. Gotten with calls like RequestServerAuthCode
     * @param forceCreate     Should a new profile be created for this user if the account
     *                        does not exist?
     * @param callback        The callback handler
     */
    public void authenticateGoogle(String googleUserId,
                                   String serverAuthCode,
                                   boolean forceCreate,
                                   IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateGoogle(googleUserId, serverAuthCode, forceCreate, this);
    }

    /**
     * Authenticate the user using a google openId
     *
     * @param googleUserAccountEmail    The email associated with the google user
     * @param IdToken The id token of the google account. Can get with calls like requestIdToken
     * @param forceCreate     Should a new profile be created for this user if the account
     *                        does not exist?
     * @param callback        The callback handler
     */
    public void authenticateGoogleOpenId(String googleUserAccountEmail,
                                         String IdToken,
                                         boolean forceCreate,
                                         IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateGoogleOpenId(googleUserAccountEmail, IdToken, forceCreate, this);
    }


    /**
     * Authenticate the user using a steam userid and session ticket (without
     * any validation on the userid).
     *
     * @param steamUserId        String representation of 64 bit steam id
     * @param steamSessionTicket The session ticket of the user (hex encoded)
     * @param forceCreate        Should a new profile be created for this user if the account
     *                           does not exist?
     * @param callback           The callback handler
     */
    public void authenticateSteam(String steamUserId,
                                  String steamSessionTicket,
                                  boolean forceCreate,
                                  IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateSteam(steamUserId, steamSessionTicket, forceCreate, this);
    }

    /**
     * Authenticate the user for Ultra.
     *
     * @param ultraUsername      it's what the user uses to log into the Ultra endpoint initially
     * @param ultraIdToken       The "id_token" taken from Ultra's JWT.
     * @param forceCreate        Should a new profile be created for this user if the account
     *                           does not exist?
     * @param callback           The callback handler
     */
    public void authenticateUltra(String ultraUsername,
                                  String ultraIdToken,
                                  boolean forceCreate,
                                  IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateUltra(ultraUsername, ultraIdToken, forceCreate, this);
    }

    /**
     * Authenticate the user using a Twitter userid, authentication token, and secret from Twitter.
     * <p>
     * Service Name - Authenticate
     * Service Operation - Authenticate
     *
     * @param userId      String representation of Twitter userid
     * @param token       The authentication token derived via the Twitter apis.
     * @param secret      The secret given when attempting to link with Twitter
     * @param forceCreate Should a new profile be created for this user if the account does not exist?
     * @param callback    The callback handler
     */
    public void authenticateTwitter(String userId,
                                    String token,
                                    String secret,
                                    boolean forceCreate,
                                    IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateTwitter(userId, token, secret, forceCreate, this);
    }


    /**
     * Authenticate the user using a userid and password (without any validation
     * on the userid). Similar to AuthenticateEmailPassword - except that that
     * method has additional features to allow for e-mail validation, password
     * resets, etc.
     *
     * @param userId       The e-mail address of the user
     * @param userPassword The password of the user
     * @param forceCreate  Should a new profile be created for this user if the account
     *                     does not exist?
     * @param callback     The callback handler
     */
    public void authenticateUniversal(String userId,
                                      String userPassword,
                                      boolean forceCreate,
                                      IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateUniversal(userId, userPassword, forceCreate, this);
    }

    /*
     * A generic Authenticate method that translates to the same as calling a specific one, except it takes an extraJson
     * that will be passed along to pre- or post- hooks.
     *
     * Service Name - Authenticate
     * Service Operation - Authenticate
     *
     * @param authenticationType Universal, Email, Facebook, etc
     * @param ids Auth IDs object
     * @param forceCreate Should a new profile be created for this user if the account does not exist?
     * @param extraJson Additional to piggyback along with the call, to be picked up by pre- or post- hooks. Leave empty string for no extraJson.
     * @param callback The method to be invoked when the server response is received
     */
    public void authenticateAdvanced(AuthenticationType authenticationType, AuthenticationIds ids, boolean forceCreate, String extraJson, IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(false);

        getClient().getAuthenticationService().authenticateAdvanced(authenticationType, ids, forceCreate, extraJson, this);
    }

    public void smartSwitchAuthenticateEmail(String email, String password, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateEmailPassword(email, password, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateExternal(String userId, String token, String externalAuthName, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateExternal(userId, token, externalAuthName, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateFacebook(String fbUserId, String fbAuthToken, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateFacebook(fbUserId, fbAuthToken, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateOculus(String oculusUserId, String oculusNonce, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateOculus(oculusUserId, oculusNonce, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateGoogle(String googleUserId, String serverAuthCode, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateGoogle(googleUserId, serverAuthCode, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateGoogleOpenId(String googleUserAccountEmail, String idToken, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateGoogleOpenId(googleUserAccountEmail, idToken, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateApple(String appleUserId, String token, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateApple(appleUserId, token, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateSteam(String steamUserId, String sessionTicket, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateSteam(steamUserId, sessionTicket, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateTwitter(String userId, String token, String secret, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateTwitter(userId, token, secret, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateUniversal(String userId, String password, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateUniversal(userId, password, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateUltra(String ultraUserId, String ultraIdToken, boolean forceCreate, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateUltra(ultraUserId, ultraIdToken, forceCreate, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void smartSwitchAuthenticateAdvanced(AuthenticationType authenticationType, AuthenticationIds ids, boolean forceCreate, String extraJson, IServerCallback callback)
    {
        getIdentitiesCallback(new IServerCallback() {
            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {

                // Clear IDs
                resetStoredAnonymousId();
                resetStoredProfileId();
                getClient().getAuthenticationService().clearSavedProfileId();

                authenticateAdvanced(authenticationType, ids, forceCreate, extraJson, callback);
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                callback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
            }
        });
    }

    public void logout(boolean forgetUser, IServerCallback callback){
        if(forgetUser){
            resetStoredProfileId();
        }

        getClient().getPlayerStateService().logout(callback);
    }

    private void getIdentitiesCallback(IServerCallback success)
    {
        IdentityCallback identityCallback = new IdentityCallback(this, success);

        if (getClient().isAuthenticated())
        {
            getClient().getIdentityService().getIdentities(identityCallback);
        }
        else
        {
            success.serverCallback(ServiceName.authenticationV2, ServiceOperation.AUTHENTICATE, null);
        }
    }

    /**
     * Check if there are stored anonymous and profile IDs. If there are, reconnect authentication is possible.
     * @return False if the anonymous and/or profile IDs are empty
     */
    public boolean canReconnect(){
        return !getStoredAnonymousId().equals("") && !getStoredProfileId().equals("");
    }

    /**
     * Re-authenticates the user with brainCloud
     *
     * @param callback The callback handler
     */
    public void reconnect(IServerCallback callback) {
        _authenticateCallback = callback;

        initializeIdentity(true);

        getClient().getAuthenticationService().authenticateAnonymous(false, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email    The e-mail address of the user
     * @param callback The callback handler
     */
    public void resetEmailPassword(String email, IServerCallback callback) {
        getClient().getAuthenticationService().resetEmailPassword(email, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email         The e-mail address of the user
     * @param serviceParams Parameters to send to the email service. see documentation for full
     *                      list. http://getbraincloud.com/apidocs/apiref/#capi-mail
     * @param callback      The callback handler
     */
    public void resetEmailPasswordAdvanced(String email, String serviceParams, IServerCallback callback) {
        getClient().getAuthenticationService().resetEmailPasswordAdvanced(email, serviceParams, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email             The e-mail address of the user
     * @param tokenTtlInMinutes Token expiry time
     * @param callback          The callback handler
     */
    public void resetEmailPasswordWithExpiry(String email, int tokenTtlInMinutes, IServerCallback callback) {
        getClient().getAuthenticationService().resetEmailPasswordWithExpiry(email, tokenTtlInMinutes, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param email             The e-mail address of the user
     * @param serviceParams     Parameters to send to the email service. see documentation for full
     *                          list. http://getbraincloud.com/apidocs/apiref/#capi-mail
     * @param tokenTtlInMinutes Token expiry time
     * @param callback          The callback handler
     */
    public void resetEmailPasswordAdvancedWithExpiry(String email, String serviceParams, Integer tokenTtlInMinutes, IServerCallback callback) {
        getClient().getAuthenticationService().resetEmailPasswordAdvancedWithExpiry(email, serviceParams, tokenTtlInMinutes, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param universalId The e-mail address of the user
     * @param callback    The callback handler
     */
    public void resetUniversalIdPassword(String universalId, IServerCallback callback) {
        getClient().getAuthenticationService().resetUniversalIdPassword(universalId, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param universalId   The e-mail address of the user
     * @param serviceParams Parameters to send to the email service. see documentation for full
     *                      list. http://getbraincloud.com/apidocs/apiref/#capi-mail
     * @param callback      The callback handler
     */
    public void resetUniversalIdPasswordAdvanced(String universalId, String serviceParams, IServerCallback callback) {
        getClient().getAuthenticationService().resetUniversalIdPasswordAdvanced(universalId, serviceParams, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param universalId       The e-mail address of the user
     * @param tokenTtlInMinutes Token expiry time
     * @param callback          The callback handler
     */
    public void resetUniversalIdPasswordWithExpiry(String universalId, int tokenTtlInMinutes, IServerCallback callback) {
        getClient().getAuthenticationService().resetUniversalIdPasswordWithExpiry(universalId, tokenTtlInMinutes, this);
    }

    /**
     * Authenticate the user with a custom Email and Password. Note that the
     * client app is responsible for collecting (and storing) the e-mail and
     * potentially password (for convenience) in the client data. For the
     * greatest security, force the user to re-enter their * password at each
     * login. (Or at least give them that option).
     * <p>
     * Note that the password sent from the client to the server is protected
     * via SSL.
     *
     * @param universalId       The e-mail address of the user
     * @param serviceParams     parameters to send to the email service. see documentation for full
     *                          list. http://getbraincloud.com/apidocs/apiref/#capi-mail
     * @param tokenTtlInMinutes Token expiry time
     * @param callback          The callback handler
     */
    public void resetUniversalIdPasswordAdvancedWithExpiry(String universalId, String serviceParams, Integer tokenTtlInMinutes, IServerCallback callback) {
        getClient().getAuthenticationService().resetUniversalIdPasswordAdvancedWithExpiry(universalId, serviceParams, tokenTtlInMinutes, this);
    }

    /**
     * Run callbacks, to be called once per frame from your main thread
     */
    public void runCallbacks() {
        getClient().runCallbacks();
    }

    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param serviceName      - name of the requested service
     * @param serviceOperation - requested operation
     * @param jsonData         - returned data from the server
     */
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
        if (serviceName.equals(ServiceName.authenticationV2) && serviceOperation.equals(ServiceOperation.AUTHENTICATE)) {
            try {
                String profileId = jsonData.getJSONObject("data").getString("profileId");
                if (!profileId.isEmpty()) {
                    setStoredProfileId(profileId);
                }
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }

        if (_authenticateCallback != null) {
            _authenticateCallback.serverCallback(serviceName, serviceOperation, jsonData);
        }
    }

    /**
     * Errors are returned back to the layer which is interfacing with the
     * BrainCloud library through the serverError() callback.
     * <p>
     * A server error might indicate a failure of the client to communicate
     * with the server after N retries.
     *
     * @param serviceName      - name of the requested service
     * @param serviceOperation - requested operation
     * @param statusCode       The error status return code (400, 403, 500, etc)
     * @param reasonCode       The brainCloud reason code (see reason codes on apidocs site)
     * @param jsonError        The error json string
     */
    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {

        if (statusCode == 202 && reasonCode == ReasonCodes.MANUAL_REDIRECT) // This should only happen on auth calls
        {
            try
            {
                // Manual redirection
                JSONObject data = new JSONObject(jsonError);

                m_initializeParams.serverUrl = data.has("redirect_url") ? data.getString("redirect_url") : m_initializeParams.serverUrl;
                String newAppId = data.has("redirect_appid") ? data.getString("redirect_appid") : null;

                // re-initialize the client with our app info
                if (m_initializeParams.secretMap == null)
                {
                    if (newAppId != null) m_initializeParams.appId = newAppId;
                    getClient().initialize(m_initializeParams.serverUrl,
                            m_initializeParams.appId,
                            m_initializeParams.secretKey,
                            m_initializeParams.appVersion);
                }
                else
                {
                    // For initialize with apps, we ignore the app id
                    getClient().initializeWithApps(m_initializeParams.serverUrl,
                            m_initializeParams.appId,
                            m_initializeParams.secretMap,
                            m_initializeParams.appVersion);
                }

                initializeIdentity(true);
                getClient().getAuthenticationService().retryPreviousAuthenticate(this);

                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (_authenticateCallback != null) {
            _authenticateCallback.serverError(serviceName, serviceOperation, statusCode, reasonCode, jsonError);
        }
    }


    // brainCloud Services
    public AppStoreService getAppStoreService() {
        return _client.getAppStoreService();
    }

    public AsyncMatchService getAsyncMatchService() {
        return _client.getAsyncMatchService();
    }

    public AuthenticationService getAuthenticationService() {
        return _client.getAuthenticationService();
    }

    public ChatService getChatService() {
        return _client.getChatService();
    }

    public DataStreamService getDataStreamService() {
        return _client.getDataStreamService();
    }

    public EntityService getEntityService() {
        return _client.getEntityService();
    }

    public EventService getEventService() {
        return _client.getEventService();
    }

    public FileService getFileService() {
        return _client.getFileService();
    }

    public FriendService getFriendService() {
        return _client.getFriendService();
    }

    public GamificationService getGamificationService() {
        return _client.getGamificationService();
    }

    public GlobalAppService getGlobalAppService() {
        return _client.getGlobalAppService();
    }

    public GlobalEntityService getGlobalEntityService() {
        return _client.getGlobalEntityService();
    }

    public GlobalStatisticsService getGlobalStatisticsService() {
        return _client.getGlobalStatisticsService();
    }

    public GroupService getGroupService() {
        return _client.getGroupService();
    }

    @Override
    public GroupFileService getGroupFileService() {
        return _client.getGroupFileService();
    }

    public IdentityService getIdentityService() {
        return _client.getIdentityService();
    }

    public LobbyService getLobbyService() {
        return _client.getLobbyService();
    }

    public MailService getMailService() {
        return _client.getMailService();
    }

    public MessagingService getMessagingService() {
        return _client.getMessagingService();
    }

    public MatchMakingService getMatchMakingService() {
        return _client.getMatchMakingService();
    }

    public OneWayMatchService getOneWayMatchService() {
        return _client.getOneWayMatchService();
    }

    public PlaybackStreamService getPlaybackStreamService() {
        return _client.getPlaybackStreamService();
    }

    public PlayerStateService getPlayerStateService() {
        return _client.getPlayerStateService();
    }

    public PlayerStatisticsService getPlayerStatisticsService() {
        return _client.getPlayerStatisticsService();
    }

    public PlayerStatisticsEventService getPlayerStatisticsEventService() {
        return _client.getPlayerStatisticsEventService();
    }

    public PresenceService getPresenceService() {
        return _client.getPresenceService();
    }

    public VirtualCurrencyService getVirtualCurrencyService() {
        return _client.getVirtualCurrencyService();
    }

    public ProfanityService getProfanityService() {
        return _client.getProfanityService();
    }

    public PushNotificationService getPushNotificationService() {
        return _client.getPushNotificationService();
    }

    public RedemptionCodeService getRedemptionCodeService() {
        return _client.getRedemptionCodeService();
    }

    public RelayService getRelayService() {
        return _client.getRelayService();
    }

    public RTTService getRTTService() {
        return _client.getRTTService();
    }

    public S3HandlingService getS3HandlingService() {
        return _client.getS3HandlingService();
    }

    public ScriptService getScriptService() {
        return _client.getScriptService();
    }

    public SocialLeaderboardService getSocialLeaderboardService() {
        return _client.getSocialLeaderboardService();
    }

    public SocialLeaderboardService getLeaderboardService() {
        return _client.getSocialLeaderboardService();
    }

    public TimeService getTimeService() {
        return _client.getTimeService();
    }

    public TournamentService getTournamentService() {
        return _client.getTournamentService();
    }

    public GlobalFileService getGlobalFileService() {
        return _client.getGlobalFileService();
    }

    public CustomEntityService getCustomEntityService() {
        return _client.getCustomEntityService();
    }

    public ItemCatalogService getItemCatalogService() {
        return _client.getItemCatalogService();
    }

    public UserItemsService getUserItemsService() {
        return _client.getUserItemsService();
    }
}