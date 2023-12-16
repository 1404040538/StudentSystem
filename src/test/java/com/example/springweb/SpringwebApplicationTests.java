package com.example.springweb;
import com.example.springweb.dao.loginMapper;
import com.example.springweb.pojo.Login;
import com.example.springweb.service.EmailService;
import com.example.springweb.service.loginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SpringwebApplicationTests {

    @Mock
    private loginMapper loginMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private loginService loginServiceUnderTest;

    private Login testLogin;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testLogin = new Login();
        testLogin.setEmail("testemail@test.com");
        testLogin.setPassword("testpassword");
        testLogin.setSalt("testsalt");
        testLogin.setConfirm_code("testconfirmcode");
        testLogin.setValidation_time(LocalDateTime.now().plusDays(1));
        testLogin.setIs_valid(0);
    }

    @Test
    public void testLoginInValidEmail() {
        // Setup
        Login invalidLogin = new Login();
        invalidLogin.setEmail("invalidemail@test.com");
        invalidLogin.setPassword("testpassword");

        when(loginMapper.queryByEmail(invalidLogin.getEmail())).thenReturn(new ArrayList<>());

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.loginIn(invalidLogin, session);

        // Verify the results
        assertEquals("邮箱未注册", result.get("message"));
    }

    @Test
    public void testLoginInAccountException() {
        // Setup
        List<Login> loginList = new ArrayList<>();
        loginList.add(testLogin);

        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(loginList);
        when(loginMapper.queryByEmail(any())).thenReturn(loginList);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.loginIn(testLogin, session);

        // Verify the results
        assertEquals("账号信息异常，请联系管理员", result.get("message"));
    }

    @Test
    public void testLoginInUnactivatedAccount() {
        // Setup
        testLogin.setIs_valid(0);

        List<Login> loginList = new ArrayList<>();
        loginList.add(testLogin);

        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(loginList);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.loginIn(testLogin, session);

        // Verify the results
        assertEquals("账号未激活", result.get("message"));
    }

    @Test
    public void testLoginInIncorrectPassword() {
        // Setup
        List<Login> loginList = new ArrayList<>();
        loginList.add(testLogin);

        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(loginList);

        Login wrongPasswordLogin = new Login();
        wrongPasswordLogin.setEmail(testLogin.getEmail());
        wrongPasswordLogin.setPassword("wrongpassword");

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.loginIn(wrongPasswordLogin, session);

        // Verify the results
        assertEquals("密码错误", result.get("message"));
    }

    @Test
    public void testLoginInSuccess() {
        // Setup
        List<Login> loginList = new ArrayList<>();
        loginList.add(testLogin);

        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(loginList);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.loginIn(testLogin, session);

        // Verify the results
        assertEquals("登录成功", result.get("message"));
        verify(session).setAttribute("USER", testLogin);
    }

    @Test
    public void testAddUserEmptyEmail() {
        // Setup
        Login emptyEmailLogin = new Login();
        emptyEmailLogin.setEmail("");
        emptyEmailLogin.setPassword("testpassword");

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.addUser(emptyEmailLogin);

        // Verify the results
        assertEquals("邮件不能为空", result.get("message"));
    }

    @Test
    public void testAddUserEmptyPassword() {
        // Setup
        Login emptyPasswordLogin = new Login();
        emptyPasswordLogin.setEmail("testemail@test.com");
        emptyPasswordLogin.setPassword("");

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.addUser(emptyPasswordLogin);

        // Verify the results
        assertEquals("密码不能为空", result.get("message"));
    }

    @Test
    public void testAddUserExistingEmail() {
        // Setup
        List<Login> loginList = new ArrayList<>();
        loginList.add(testLogin);

        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(loginList);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.addUser(testLogin);

        // Verify the results
        assertEquals("该邮件已注册", result.get("message"));
    }

    @Test
    public void testAddUserSuccess() throws MessagingException {
        // Setup
        when(loginMapper.queryByEmail(testLogin.getEmail())).thenReturn(new ArrayList<>());
        when(loginMapper.addUser(testLogin)).thenReturn(1);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.addUser(testLogin);

        // Verify the results
        assertEquals("注册成功,请前往邮件验证", result.get("message"));
        verify(emailService).sendEmail(anyString(), eq(testLogin.getEmail()));
    }

    @Test
    public void testValidMailExpired() {
        // Setup
        testLogin.setValidation_time(LocalDateTime.now().minusDays(1));

        when(loginMapper.queryTime(testLogin.getConfirm_code())).thenReturn(testLogin);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.validMail(testLogin.getConfirm_code());

        // Verify the results
        assertEquals(500, result.get("code"));
        assertEquals("该邮件的激活时间已失效，请重新接受邮件激活", result.get("message"));
    }

    @Test
    public void testValidMailSuccess() {
        // Setup
        when(loginMapper.queryTime(testLogin.getConfirm_code())).thenReturn(testLogin);
        when(loginMapper.updateValid(testLogin.getConfirm_code())).thenReturn(1);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.validMail(testLogin.getConfirm_code());

        // Verify the results
        assertEquals(200, result.get("code"));
        assertEquals("恭喜你，激活成功", result.get("message"));
    }

    @Test
    public void testValidMailFailure() {
        // Setup
        when(loginMapper.queryTime(testLogin.getConfirm_code())).thenReturn(testLogin);
        when(loginMapper.updateValid(testLogin.getConfirm_code())).thenReturn(0);

        // Run the test
        final Map<String, Object> result = loginServiceUnderTest.validMail(testLogin.getConfirm_code());

        // Verify the results
        assertEquals(500, result.get("code"));
        assertEquals("激活失败", result.get("message"));
    }
}