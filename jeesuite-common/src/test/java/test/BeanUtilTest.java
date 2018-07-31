package test;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.BeanUtils;

public class BeanUtilTest {

	public static void main(String[] args) {
		User user = new User();
		user.setName("小伙子");
		user.setMobile("13800138000");
		user.setFather(new User(1000, "你爹"));
		
		BaseUser user2 = BeanUtils.copy(user, BaseUser.class);
		System.out.println(JsonUtils.toPrettyJson(user2));
	}
}
