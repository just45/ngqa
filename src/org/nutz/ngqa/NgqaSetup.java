package org.nutz.ngqa;

import org.nutz.lang.Strings;
import org.nutz.lang.random.R;
import org.nutz.mongo.MongoDao;
import org.nutz.mvc.NutConfig;
import org.nutz.mvc.Setup;
import org.nutz.ngqa.api.Ngqa;
import org.nutz.ngqa.bean.Answer;
import org.nutz.ngqa.bean.App;
import org.nutz.ngqa.bean.Question;
import org.nutz.ngqa.bean.Role;
import org.nutz.ngqa.bean.SystemConfig;
import org.nutz.ngqa.bean.User;
import org.nutz.ngqa.service.CommonMongoService;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class NgqaSetup implements Setup {

	public void init(NutConfig config) {
		CommonMongoService commons = config.getIoc().get(CommonMongoService.class, "commons");
		MongoDao dao = commons.dao();
		
		//初始化集合
		dao.create(User.class, false);
		dao.create(Question.class, false);
		dao.create(Answer.class, false);
		dao.create(SystemConfig.class, false);
		dao.create(App.class, false);
		dao.create(Role.class, false);
		
		//创建匿名用户
		User anonymous = dao.findOne(User.class, new BasicDBObject("provider", "anonymous"));
		if (anonymous == null) {
			anonymous = new User();
			anonymous.setId(commons.seq("user"));
			anonymous.setProvider("anonymous");
			dao.save(anonymous);
		}
		
		//创建超级用户
		User root = dao.findOne(User.class, new BasicDBObject("provider", "root"));
		if (root == null) {
			root = new User();
			root.setId(commons.seq("user"));
			root.setProvider("root");
			dao.save(root);
		}
		
		//检查超级用户的密码
		DBObject dbo = commons.coll("systemconfig").findOne();
		if (dbo == null) {
			commons.coll("systemconfig").insert(new BasicDBObject("api_version", Ngqa.apiVersion()));
			dbo = commons.coll("systemconfig").findOne();
		}
		if (Strings.isBlank((String)dbo.get("root_password"))) {
			commons.coll("systemconfig").findAndModify(new BasicDBObject(), null, null, false, new BasicDBObject("$set", new BasicDBObject("root_password", R.sg(64).next())), true, true);
		}
	}
	
	public void destroy(NutConfig config) {
		
	}
}
