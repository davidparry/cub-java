package com.ivelum.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ivelum.Cub;
import com.ivelum.CubModelBaseTest;
import com.ivelum.exception.ApiException;
import com.ivelum.exception.BadRequestException;
import com.ivelum.exception.CubException;
import com.ivelum.exception.DeserializationException;
import com.ivelum.exception.InvalidRequestException;
import com.ivelum.exception.UnauthorizedException;
import com.ivelum.net.Params;
import java.util.List;

import org.junit.Test;


public class UserTest extends CubModelBaseTest {
  static final String test_username = "support@ivelum.com";
  static final String test_userpassword = "SJW8Gg";

  @Test
  public void testLogin() throws CubException {
    User user = User.login(test_username, test_userpassword);
    assertNotNull(user.dateJoined);
    assertEquals(user.email, test_username);
    assertNotNull(user.token);
    assertNotNull(user.lastLogin);
    assertEquals("", user.middleName);
    assertEquals(user.token, user.getApiKey());

    User userCopy = User.get(user.id, new Params(user.token));
    assertEquals(user.token, userCopy.getApiKey());

    assertEquals(userCopy.id, user.id);
    assertEquals(userCopy.username, user.username);
    assertEquals(userCopy.email, user.email);
  }

  @Test
  public void testGetUserAndReissueToken() throws CubException {
    // invalid user token
    try {
      User.getUserAndReissueToken("invalid token");
    } catch (ApiException e) {
      assertEquals("You did not provide a valid API key.", e.getMessage());
    }

    User user = User.login(test_username, test_userpassword);
    String token = user.getApiKey();
    // invalid application key
    try {
      User.getUserAndReissueToken(token, "invalid_app_key");
    } catch (BadRequestException e) {
      assertTrue(e.getApiError().description.contains("app_key"));
    }

    // success
    User userCopy = User.getUserAndReissueToken(token);

    assertEquals(userCopy.id, user.id);
    assertEquals(userCopy.email, user.email);
  }

  @Test
  public void testUpdateUserName() throws CubException {
    User user = User.login(test_username, test_userpassword);

    // invalid password
    Boolean failed = false;
    try {
      User.updateUsername("new_user_name", "invalid_password", new Params(user.getApiKey()));
    } catch (BadRequestException e) {
      assertTrue(e.getApiError().description.contains("password"));
      failed = true;
    }

    assertTrue(failed);

    failed = false;
    // invalid token
    try {
      User.updateUsername("new_user_name", "invalid_password", new Params("invalid token"));
    } catch (ApiException e) {
      assertEquals("You did not provide a valid API key.", e.getMessage());
      failed = true;
    }

    assertTrue(failed);
  }

  @Test
  public void testUpdateEmail() throws CubException {
    User user = User.login(test_username, test_userpassword);

    // invalid password
    Boolean failed = false;
    try {
      User.updateEmail(
              "some@mail.com",
              "invalid_password",
              "site_id",
              new Params(user.getApiKey()));
    } catch (BadRequestException e) {
      assertTrue(e.getApiError().description.contains("password"));
      failed = true;
    }

    assertTrue(failed);

    failed = false;
    // invalid token
    try {
      User.updateEmail(
              "new_user_email",
              "invalid_password",
              "site_id",
              new Params("invalid token"));
    } catch (ApiException e) {
      assertEquals("You did not provide a valid API key.", e.getMessage());
      failed = true;
    }

    assertTrue(failed);
  }

  @Test
  public void testDeserialization() throws DeserializationException {
    String jsonStr = getFixture("user");
    User user = (User) Cub.factory.fromString(jsonStr);

    assertEquals("firstName", user.firstName);
    assertEquals("lastName", user.lastName);
    assertEquals("middleName", user.middleName);
    assertEquals("token", user.token);
    assertEquals("photo_large.png", user.photoLarge);
    assertEquals("photo_small.png", user.photoSmall);
    assertEquals("username", user.username);
    assertEquals((Integer) 0, user.invitationSentCount);

    assertNotNull(user.birthDate);
    assertFalse(user.emailConfirmed);
    assertFalse(user.emailSetRequired);
    assertEquals("male", user.gender);
    assertFalse(user.invalidEmail);
    assertNotNull(user.invitationLastSentOn);
    assertEquals("user", user.objectName);
    assertEquals("originalUsername", user.originalUsername);
    assertFalse(user.passwordChangeRequired);
    assertFalse(user.retired);
    assertFalse(user.purchasingRoleBuyForOrganization);
    assertFalse(user.purchasingRoleBuyForSelfOnly);
    assertFalse(user.purchasingRoleRecommend);
    assertFalse(user.purchasingRoleSpecifyForOrganization);
    assertEquals(1, user.verifiedTags.size());
    assertTrue(user.verifiedTags.contains("Law Enforcement"));

    List<ExpandableField<Member>> members = user.membership;
    assertEquals(1, members.size());
    ExpandableField<Member> member = members.get(0);
    assertFalse(member.isExpanded());
    assertEquals("mbr_123", member.getId());

    assertFalse(user.registrationSite.isExpanded());
    assertEquals("ste_123", user.registrationSite.getId());

    assertEquals(user.token, user.getApiKey());
    assertNull(user.deleted);
  }

  @Test
  public void testDeserializationDeletedObject() throws DeserializationException {
    String jsonStr = getFixture("user_deleted");
    User user = (User) Cub.factory.fromString(jsonStr);
    assertTrue(user.deleted);
  }

  @Test
  public void testTokenNotSerialized() throws InvalidRequestException {
    User user = new User();
    Params params = new Params();
    user.toParams(params);
    assertTrue(params.hasKey("email"));
    assertFalse(params.hasKey("token"));
    assertTrue(User.serializationIgnoreFields.contains("token"));
  }

  @Test
  public void testRegister() throws CubException {
    try {
      User.register("", "", "", "", "", new Params());
      fail("BadRequestException is expected");
    } catch (BadRequestException e) {
      ApiError apiError = e.getApiError();
      assert apiError.params.get("password").contains("required");
      assert apiError.params.get("registration_site").contains("required");
      assert apiError.params.get("last_name").contains("required");
      assert apiError.params.get("first_name").contains("required");
      assert apiError.params.get("email").contains("required");
    }

    try {
      User.register("fname", "laname", test_username, "any", "any", new Params());
      fail("BadRequestException is expected");
    } catch (BadRequestException e) {
      ApiError apiError = e.getApiError();
      assert apiError.params.get("password").contains("length must be");
      assert apiError.params.get("registration_site").contains("does not exist");
      assert apiError.params.get("email").contains("already used");
    }
  }
  
  @Test
  public void testRegisterSuccess() throws CubException {
    User fixtureUser = (User) Cub.factory.fromString(getFixture("user"));
    String apiKey = "apiKey";
    String endpoint = String.format("/%s/register", User.classUrl);
    mockPostToListEndpoint(endpoint, 200, "user", apiKey);
    // register user
    User user = User.register(
        fixtureUser.firstName,
        fixtureUser.lastName,
        fixtureUser.email,
        "123123123",
        fixtureUser.registrationSite.getId(),
        new Params(apiKey));
    // check just created users
    assertEquals("token", user.getApiKey());
    assertEquals(user.id, fixtureUser.id);
    assertEquals(user.firstName, fixtureUser.firstName);
  }
  
  @Test
  public void testRegisterWithoutPasswordRequiredParamss() throws CubException {
    try {
      User.registerWithoutPassword("", "", "", "", new Params());
      fail("BadRequestException is expected");
    } catch (BadRequestException e) {
      ApiError apiError = e.getApiError();
      assert apiError.params.get("registration_site").contains("required");
      assert apiError.params.get("email").contains("required");
    }
    
    try {
      User.registerWithoutPassword("fname", "laname", test_username, "any", new Params());
      fail("BadRequestException is expected");
    } catch (BadRequestException e) {
      ApiError apiError = e.getApiError();
      assert apiError.params.get("registration_site").contains("does not exist");
      assert apiError.params.get("email").contains("already used");
    }
  }
  
  @Test
  public void testRegisterWithoutPasswordSuccess() throws CubException {
    User fixtureUser = (User) Cub.factory.fromString(getFixture("user"));
    String apiKey = "apiKey";
    String endpoint = String.format("/%s/register-without-password", User.classUrl);
    mockPostToListEndpoint(endpoint, 200, "user", apiKey);
    // register user
    Params params = new Params(apiKey);
    params.setValue("middle_name", fixtureUser.middleName);
    User user = User.registerWithoutPassword(
        fixtureUser.firstName,
        fixtureUser.lastName,
        fixtureUser.email,
        fixtureUser.registrationSite.getId(),
        params);
    // check just created users
    assertEquals(user.id, fixtureUser.id);
    assertEquals(user.firstName, fixtureUser.firstName);
  }

  @Test
  public void testConfirmEmail() throws CubException {
    String invalidConfirmToken = "invalid";
    try {
      User.confirmEmail(invalidConfirmToken, new Params(Cub.apiKey));
      fail("exception excpected");
    } catch (BadRequestException e) {
      e.getApiError().description.equals("Bad token");
    }
  }

  @Test
  public void testGetUserByRestorePasswordToken() throws CubException {
    String invalidRestorePasswordToken = "invalid";
    try {
      User.getUserByRestorePasswordToken(invalidRestorePasswordToken, new Params(Cub.apiKey));
    } catch (UnauthorizedException e) {
      assertEquals(e.getApiError().description,"Invalid token");
    }
  }

  @Test
  public void testResetPasswordWithToken() throws CubException {
    String invalidRestorePasswordToken = "invalid";
    try {
      User.resetPasswordWithToken(
          invalidRestorePasswordToken, "new password", new Params(Cub.apiKey));
    } catch (UnauthorizedException e) {
      assertEquals(e.getApiError().description,"Invalid token");
    }
  }
  
  @Test
  public void testLoginByTokenSuccess() throws CubException {
    User fixtureUser = (User) Cub.factory.fromString(getFixture("user"));
    String apiKey = "apiKey";
    String token = "temporary_token";
    String endpoint = String.format("/%s/login/%s", User.classUrl, token);
    mockPostToListEndpoint(endpoint, 200, "user", apiKey);
    User user = User.loginByToken(token, new Params(apiKey));
    assertEquals("token", user.getApiKey());
    assertEquals(user.id, fixtureUser.id);
    assertEquals(user.firstName, fixtureUser.firstName);
  }
  
  @Test(expected = UnauthorizedException.class)
  public void testLoginByTokenFailure() throws CubException {
    String apiKey = "api_key";
    String token = "temporary_token";
    String endpoint = String.format("/%s/login/%s", User.classUrl, token);
    mockPostToListEndpoint(endpoint, 401, "unautorized_error_invalid_token", apiKey);
    User user = User.loginByToken(token, new Params(apiKey));
  }
  
  @Test
  public void testSetPasswordSuccess() throws CubException {
    String currentPassword = "currentUserPassword";
    String newPassword = "newUserPassword";
    String userToken = "userToken";
    
    User fixtureUser = (User) Cub.factory.fromString(getFixture("user"));
    String endpoint = String.format("/%s/password/", User.classUrl);
    mockPostToListEndpoint(endpoint, 200, "user", userToken);
  
    User userAfter = User.setPassword(currentPassword, newPassword, new Params(userToken));
    
    assertEquals(userAfter.id, fixtureUser.id);
  }
  
  @Test
  public void testSetPasswordInvalidCurrentPassword() throws CubException {
    String token = "userToken";
    String endpoint = String.format("/%s/password/", User.classUrl);
    mockPostToListEndpoint(endpoint, 400, "set_password_error", token);
  
    try {
      User.setPassword("invalidCurrentPassword", "anyNewPass", new Params(token));
    } catch (BadRequestException e) {
      assertTrue(400 == e.getApiError().code);
      assertTrue(e.getApiError().params.containsKey("password"));
      assertEquals(
          e.getApiError().params.get("password"),
          "Your password was entered incorrectly. Please enter it again.");
    }
  }
}