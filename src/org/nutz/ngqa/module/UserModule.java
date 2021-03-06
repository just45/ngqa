package org.nutz.ngqa.module;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.util.SocialAuthUtil;
import org.nutz.ioc.annotation.InjectName;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Files;
import org.nutz.lang.stream.NullInputStream;
import org.nutz.lang.util.Callback;
import org.nutz.mongo.MongoDao;
import org.nutz.mongo.util.Moo;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.View;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.view.ServerRedirectView;
import org.nutz.ngqa.bean.User;
import org.nutz.ngqa.service.CommonMongoService;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

@IocBean(create="init")
@InjectName
@At("/user")
public class UserModule {

	private SocialAuthManager manager;
	
	@Inject("java:$commons.dao()")
	private MongoDao dao;
	
	@Inject
	private CommonMongoService commons;
	
	public void init() throws Exception {
		SocialAuthConfig config = new SocialAuthConfig();
		File devConfig = Files.findFile("oauth_consumer.properties_dev");
		if (devConfig == null)
			devConfig = Files.findFile("oauth_consumer.properties");
		if (devConfig == null)
			config.load(new NullInputStream());
		else
			config.load(new FileInputStream(devConfig));
		manager = new SocialAuthManager();
		manager.setSocialAuthConfig(config);
	}
	
	/*提供匿名登录*/
	@At("/login/anonymous")
	@Ok("void")
	public View anonymousLogin(HttpSession session) throws Exception {
		User user = dao.findOne(User.class, new BasicDBObject("provider", "anonymous"));
		session.setAttribute("me", user);
		return new ServerRedirectView("/index.jsp");
	}
	
	/*提供google登录*/
	@At("/login/?")
	@Ok("void")
	public void login(String provider, HttpSession session) throws Exception {
		String returnTo = Mvcs.getReq().getRequestURL().toString() + "/callback";
		String url = manager.getAuthenticationUrl(provider, returnTo);
		session.setAttribute("authManager", manager);
		Mvcs.getResp().setHeader("Location", url);
		Mvcs.getResp().setStatus(302);
	}
	
	/*登出*/
	@At("/logout")
	@Ok("void")
	public void logout(HttpSession session) {
		session.invalidate();
	}
	
	/*无需做链接,这是OpenID的回调地址*/
	@At("/login/?/callback")
	public View returnPoint(String providerId, HttpServletRequest request) throws Exception {
		Map<String, String> paramsMap = SocialAuthUtil.getRequestParametersMap(request); 
		AuthProvider provider = manager.connect(paramsMap);
		Profile p = provider.getUserProfile();
        BasicDBObject query = new BasicDBObject().append("validatedId", p.getValidatedId()).append("provider", providerId);
        User user = dao.findOne(User.class, query);
        if (user == null) {
        	user = new User();
        	user.setEmail(p.getEmail());
        	user.setProvider(providerId);
        	user.setValidatedId(p.getValidatedId());
        	user.setId(commons.seq("user"));
        	final User _u = user;
        	dao.runNoError(new Callback<DB>() {
    			public void invoke(DB arg0) {
    				dao.save(_u);
    			}
    		});
        }
        Moo moo = Moo.SET("lastLoginDate", new Date()).set("email", p.getEmail());
        dao.update(User.class, new BasicDBObject("_id", user.getId()), moo);
        request.getSession().setAttribute("me", user);
        return new ServerRedirectView("/index.jsp");
	}
}