package com.cxlayui.manage.web;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cxlayui.manage.entity.ResponseResult;
import com.cxlayui.manage.entity.UserDTO;
import com.cxlayui.manage.entity.UserRolesVO;
import com.cxlayui.manage.entity.UserSearchDTO;
import com.cxlayui.manage.pojo.Role;
import com.cxlayui.manage.pojo.User;
import com.cxlayui.manage.service.AuthService;
import com.cxlayui.manage.service.UserService;
import com.cxlayui.manage.utils.DateUtil;
import com.cxlayui.manage.utils.IStatusMessage;
import com.cxlayui.manage.utils.PageDataResult;
import com.cxlayui.manage.utils.ValidateUtil;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;

/**
 * @author 陈翔
 * @ClassName: UserController
 * @Description: TODO
 * @date 2018年4月24日
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory
            .getLogger(UserController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private EhCacheManager ecm;

    @RequestMapping("/userList")
    public String toUserList() {
        return "/auth/userList";
    }

    /**
     * 分页查询用户列表
     *
     * @return ok/fail
     */
    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    @ResponseBody
    public PageDataResult getUsers(@RequestParam("page") Integer page, @RequestParam("limit") Integer limit, UserSearchDTO userSearch) {
        logger.debug("分页查询用户列表！搜索条件：userSearch：" + userSearch + ",page:" + page + ",每页记录数量limit:" + limit);
        PageDataResult pdr = new PageDataResult();
        try {
            if (null == page) {
                page = 1;
            }
            if (null == limit) {
                limit = 10;
            }
            //获取用户和角色列表
            pdr = userService.getUsers(userSearch, page, limit);
            logger.debug("用户列表查询=pdr:" + pdr);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("用户列表查询异常！", e);
        }
        return pdr;
    }

    /**
     * 设置用户是否离职
     *
     * @return ok/fail
     */
    @RequestMapping(value = "/setJobUser", method = RequestMethod.POST)
    @ResponseBody
    public String setJobUser(@RequestParam("id") Integer id, @RequestParam("job") Integer isJob) {
        logger.debug("设置用户是否离职！id:" + id + ",isJob:" + isJob);
        try {
            if (null == id || null == isJob) {
                return "请求参数有误，请您稍后再试";
            }
            //设置用户是否离职
            userService.setJobUser(id, isJob);
            logger.info("设置用户是否离职成功！userID=" + id + ",isJob:" + isJob);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设置用户是否离职异常！", e);
        }
        return "ok";
    }

    /**
     * 设置用户[新增或更新]
     *
     * @return ok/fail
     */
    @RequestMapping(value = "/setUser", method = RequestMethod.POST)
    @ResponseBody
    public String setUser(@RequestParam("roleIds") String roleIds, User user) {
        logger.debug("设置用户[新增或更新]！user:" + user + ",roleIds:" + roleIds);
        try {
            if (null == user || StringUtils.isEmpty(roleIds)) {
                return "请求参数有误，请您稍后再试";
            }
            //设置用户[新增或更新]
            logger.info("设置用户[新增或更新]成功！user=" + user + ",roleIds=" + roleIds);
            return userService.setUser(user, roleIds);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设置用户[新增或更新]异常！", e);
        }
        return "操作失败，请您稍后再试";
    }

    /**
     * 删除用户
     *
     * @return ok/fail
     */
    @RequestMapping(value = "/delUser", method = RequestMethod.POST)
    @ResponseBody
    public String delUser(@RequestParam("id") Integer id) {
        logger.debug("删除用户！id:" + id);
        try {
            if (null == id) {
                return "请求参数有误，请您稍后再试";
            }
            //删除用户
            userService.setDelUser(id, 1);
            logger.info("删除用户成功！userId=" + id);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("删除用户异常！", e);
        }
        return "ok";
    }

    /**
     * 查询用户数据
     *
     * @return map
     */
    @RequestMapping(value = "/getUserAndRoles", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getUserAndRoles(@RequestParam("id") Integer id) {
        logger.debug("查询用户数据！id:" + id);
        Map<String, Object> map = new HashMap<>();
        try {
            if (null == id) {
                logger.debug("查询用户数据==请求参数有误，请您稍后再试");
                map.put("msg", "请求参数有误，请您稍后再试");
                return map;
            }
            //查询用户
            UserRolesVO urvo = userService.getUserAndRoles(id);
            logger.debug("查询用户数据！urvo=" + urvo);
            if (null != urvo) {
                map.put("user", urvo);
                //获取全部角色数据
                List<Role> roles = this.authService.getRoles();
                logger.debug("查询角色数据！roles=" + roles);
                if (null != roles && roles.size() > 0) {
                    map.put("roles", roles);
                }
                map.put("msg", "ok");
            } else {
                map.put("msg", "查询用户信息有误，请您稍后再试");
            }
            logger.debug("查询用户数据成功！map=" + map);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            map.put("msg", "查询用户错误，请您稍后再试");
            logger.error("查询用户数据异常！", e);
        }
        return map;
    }

    /**
     * 发送短信验证码
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "sendMsg", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult sendMsg(UserDTO user) {
        logger.debug("发送短信验证码！user:" + user);
        ResponseResult responseResult = new ResponseResult();
        try {
            if (null == user) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("请求参数有误，请您稍后再试");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            if (!validatorRequestParam(user, responseResult)) {
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //送短信验证码
//			String msg=userService.sendMsg(user);
            String msg = "ok";
            if (msg != "ok") {
                responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
                responseResult.setMessage(msg == "no" ? "发送验证码失败，请您稍后再试" : msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
            responseResult.setMessage("发送短信验证码失败，请您稍后再试");
            logger.error("发送短信验证码异常！", e);
        }
        logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
        return responseResult;
    }

    /**
     * 登录【使用shiro中自带的HashedCredentialsMatcher结合ehcache（记录输错次数）配置进行密码输错次数限制】
     * </br>缺陷是，无法友好的在后台提供解锁用户的功能，当然，可以直接提供一种解锁操作，清除ehcache缓存即可，不记录在用户表中；
     * </br>
     *
     * @param user
     * @param rememberMe
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult login(UserDTO user, @RequestParam(value = "rememberMe", required = false) boolean rememberMe) {
        logger.debug("用户登录，请求参数=user:" + user + "，是否记住我：" + rememberMe);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
        if (null == user) {
            responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
            responseResult.setMessage("请求参数有误，请您稍后再试");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        if (!validatorRequestParam(user, responseResult)) {
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        //用户是否存在
        User existUser = this.userService.findUserByName(user.getUsername());
        if (existUser == null) {
            responseResult.setMessage("该用户不存在，请您联系管理员");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        } else {
            //是否离职
            if (existUser.getIsJob()) {
                responseResult.setMessage("登录用户已离职，请您联系管理员");
                logger.debug("用户登录，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //校验验证码
			/*if(!existUser.getMcode().equals(user.getSmsCode())){
				//不等
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("短信验证码输入有误");
				logger.debug("用户登录，结果=responseResult:"+responseResult);
				return responseResult;
			}
			//1分钟
			long beginTime = existUser.getSendTime().getTime();
			long endTime = new Date().getTime();
			if(((endTime-beginTime)-120000>0)){
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("短信验证码超时");
				logger.debug("用户登录，结果=responseResult:"+responseResult);
				return responseResult;
			}*/
        }
        //用户登录
        try {
            // 1、 封装用户名、密码、是否记住我到token令牌对象  [支持记住我]
            AuthenticationToken token = new UsernamePasswordToken(
                    user.getUsername(), DigestUtils.md5Hex(user.getPassword()), rememberMe);
            // 2、 Subject调用login
            Subject subject = SecurityUtils.getSubject();
            //在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            //每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            //所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.debug("用户登录，用户验证开始！user=" + user.getUsername());
            subject.login(token);
            responseResult.setCode(IStatusMessage.SystemStatus.SUCCESS.getCode());
            logger.info("用户登录，用户验证通过！user=" + user.getUsername());
        } catch (UnknownAccountException uae) {
            logger.error("用户登录，用户验证未通过：未知用户！user=" + user.getUsername(), uae);
            responseResult.setMessage("该用户不存在，请您联系管理员");
        } catch (IncorrectCredentialsException ice) {
            //获取输错次数
            logger.error("用户登录，用户验证未通过：错误的凭证，密码输入错误！user=" + user.getUsername(), ice);
            responseResult.setMessage("用户名或密码不正确");
        } catch (LockedAccountException lae) {
            logger.error("用户登录，用户验证未通过：账户已锁定！user=" + user.getUsername(), lae);
            responseResult.setMessage("账户已锁定");
        } catch (ExcessiveAttemptsException eae) {
            logger.error("用户登录，用户验证未通过：错误次数大于5次,账户已锁定！user=.getMobile()" + user, eae);
            responseResult.setMessage("用户名或密码错误次数大于5次,账户已锁定!</br><span style='color:red;font-weight:bold; '>2分钟后可再次登录，或联系管理员解锁</span>");
            //这里结合了，另一种密码输错限制的实现，基于redis或mysql的实现；也可以直接使用RetryLimitHashedCredentialsMatcher限制5次
        }/*catch (DisabledAccountException sae){
			logger.error("用户登录，用户验证未通过：帐号已经禁止登录！user=" + user.getMobile(),sae);
			responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
			responseResult.setMessage("帐号已经禁止登录");
		}*/ catch (AuthenticationException ae) {
            //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
            logger.error("用户登录，用户验证未通过：认证异常，异常信息如下！user=" + user.getUsername(), ae);
            responseResult.setMessage("用户名或密码不正确");
        } catch (Exception e) {
            logger.error("用户登录，用户验证未通过：操作异常，异常信息如下！user=" + user.getUsername(), e);
            responseResult.setMessage("用户登录失败，请您稍后再试");
        }
        Cache<String, AtomicInteger> passwordRetryCache = ecm.getCache("passwordRetryCache");
        if (null != passwordRetryCache) {
            int retryNum = (passwordRetryCache.get(existUser.getUsername()) == null ? 0 : passwordRetryCache.get(existUser.getUsername())).intValue();
            logger.debug("输错次数：" + retryNum);
            if (retryNum > 0 && retryNum < 6) {
                responseResult.setMessage("用户名或密码错误" + retryNum + "次,再输错" + (6 - retryNum) + "次账号将锁定");
            }
        }
        logger.debug("用户登录，user=" + user.getMobile() + ",登录结果=responseResult:" + responseResult);
        return responseResult;
    }

    /**
     * 登录【使用redis和mysql实现，用户密码输错次数限制，和锁定解锁用户的功能//TODO】
     * 该实现后续会提供！TODO
     *
     * @param user
     * @param rememberMe
     * @return
     */
    @RequestMapping(value = "logina", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult logina(UserDTO user, @RequestParam(value = "rememberMe", required = false) boolean rememberMe) {
        logger.debug("用户登录，请求参数=user:" + user + "，是否记住我：" + rememberMe);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
        if (null == user) {
            responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
            responseResult.setMessage("请求参数有误，请您稍后再试");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        if (!validatorRequestParam(user, responseResult)) {
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        }
        //用户是否存在
        User existUser = this.userService.findUserByMobile(user.getMobile());
        if (existUser == null) {
            responseResult.setMessage("该用户不存在，请您联系管理员");
            logger.debug("用户登录，结果=responseResult:" + responseResult);
            return responseResult;
        } else {
            //校验验证码
			/*if(!existUser.getMcode().equals(user.getSmsCode())){
				//不等
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("短信验证码输入有误");
				logger.debug("用户登录，结果=responseResult:"+responseResult);
				return responseResult;
			}
			//1分钟
			long beginTime = existUser.getSendTime().getTime();
			long endTime = new Date().getTime();
			if(((endTime-beginTime)-120000>0)){
				responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
				responseResult.setMessage("短信验证码超时");
				logger.debug("用户登录，结果=responseResult:"+responseResult);
				return responseResult;
			}*/
        }
        //是否锁定
        boolean flag = false;
        //用户登录
        try {
            // 1、 封装用户名和密码到token令牌对象  [支持记住我]
            AuthenticationToken token = new UsernamePasswordToken(
                    user.getMobile(), DigestUtils.md5Hex(user.getPassword()), rememberMe);
            // 2、 Subject调用login
            Subject subject = SecurityUtils.getSubject();
            //在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            //每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            //所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.debug("用户登录，用户验证开始！user=" + user.getMobile());
            subject.login(token);
            responseResult.setCode(IStatusMessage.SystemStatus.SUCCESS.getCode());
            logger.info("用户登录，用户验证通过！user=" + user.getMobile());
        } catch (UnknownAccountException uae) {
            logger.error("用户登录，用户验证未通过：未知用户！user=" + user.getMobile(), uae);
            responseResult.setMessage("该用户不存在，请您联系管理员");
        } catch (IncorrectCredentialsException ice) {
            //获取输错次数
            logger.error("用户登录，用户验证未通过：错误的凭证，密码输入错误！user=" + user.getMobile(), ice);
            responseResult.setMessage("用户名或密码不正确");
        } catch (LockedAccountException lae) {
            logger.error("用户登录，用户验证未通过：账户已锁定！user=" + user.getMobile(), lae);
            responseResult.setMessage("账户已锁定");
        } catch (ExcessiveAttemptsException eae) {
            logger.error("用户登录，用户验证未通过：错误次数大于5次,账户已锁定！user=.getMobile()" + user, eae);
            responseResult.setMessage("用户名或密码错误次数大于5次,账户已锁定，2分钟后可再次登录或联系管理员解锁");
            //这里结合了，另一种密码输错限制的实现，基于redis或mysql的实现；也可以直接使用RetryLimitHashedCredentialsMatcher限制5次
            flag = true;
        }/*catch (DisabledAccountException sae){
			logger.error("用户登录，用户验证未通过：帐号已经禁止登录！user=" + user.getMobile(),sae);
			responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
			responseResult.setMessage("帐号已经禁止登录");
		}*/ catch (AuthenticationException ae) {
            //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
            logger.error("用户登录，用户验证未通过：认证异常，异常信息如下！user=" + user.getMobile(), ae);
            responseResult.setMessage("用户名或密码不正确");
        } catch (Exception e) {
            logger.error("用户登录，用户验证未通过：操作异常，异常信息如下！user=" + user.getMobile(), e);
            responseResult.setMessage("用户登录失败，请您稍后再试");
        }
        if (flag) {
            //已经输错6次了，将进行锁定！【也可以使用redis记录密码输错次数，然后进行锁定//TODO】
            int num = this.userService.setUserLockNum(existUser.getId(), 1);
            if (num < 1) {
                logger.info("用户登录，用户名或密码错误次数大于5次,账户锁定失败！user=" + user.getMobile());
            }
        }
        logger.debug("用户登录，user=" + user.getMobile() + ",登录结果=responseResult:" + responseResult);
        return responseResult;
    }

    /**
     * 发送短信验证码
     *
     * @param mobile
     * @param picCode
     * @return
     */
    @RequestMapping(value = "sendMessage", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult sendMessage(@RequestParam("mobile") String mobile,
                                      @RequestParam("picCode") String picCode) {
        logger.debug("发送短信验证码！mobile:" + mobile + ",picCode=" + picCode);
        ResponseResult responseResult = new ResponseResult();
        try {
            if (!ValidateUtil.isMobilephone(mobile)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("手机号格式有误，请您重新填写");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            if (!ValidateUtil.isPicCode(picCode)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("图片验证码有误，请您重新填写");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //判断用户是否登录
            User existUser = (User) SecurityUtils.getSubject().getPrincipal();
            if (null == existUser) {
                responseResult.setCode(IStatusMessage.SystemStatus.NO_LOGIN.getCode());
                responseResult.setMessage("您未登录或登录超时，请您重新登录后再试");
                logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //送短信验证码
//			String msg=userService.sendMessage(existUser.getId(),mobile);
            String msg = "ok";
            if (msg != "ok") {
                responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
                responseResult.setMessage(msg == "no" ? "发送验证码失败，请您稍后再试" : msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
            responseResult.setMessage("发送短信验证码失败，请您稍后再试");
            logger.error("发送短信验证码异常！", e);
        }
        logger.debug("发送短信验证码，结果=responseResult:" + responseResult);
        return responseResult;
    }

    /**
     * 修改密码
     *
     * @param oldPassword
     * @param newPassword
     * @return
     */
    @RequestMapping(value = "setPwd", method = RequestMethod.POST)
    @ResponseBody
    public ResponseResult setPwd(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("secondNewPassword") String secondNewPassword) {
        logger.debug("修改密码！oldPassword:" + oldPassword + ",newPassword=" + newPassword);
        ResponseResult responseResult = new ResponseResult();
        try {
            if (!ValidateUtil.isSimplePassword(oldPassword) || !ValidateUtil.isSimplePassword(newPassword)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("密码格式有误，请您重新填写");
                logger.debug("修改密码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            if (!secondNewPassword.equals(newPassword)) {
                responseResult.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
                responseResult.setMessage("两次密码输入不一致，请您重新填写");
                logger.debug("发修改密码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //判断用户是否登录
            User existUser = (User) SecurityUtils.getSubject().getPrincipal();
            if (null == existUser) {
                responseResult.setCode(IStatusMessage.SystemStatus.NO_LOGIN.getCode());
                responseResult.setMessage("您未登录或登录超时，请您重新登录后再试");
                logger.debug("修改密码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            User user = userService.findUserById(existUser.getId());
            if (!StringUtils.equals(user.getPassword(), DigestUtils.md5Hex(oldPassword))) {
                responseResult.setCode(IStatusMessage.SystemStatus.PASSWORD_ERROR.getCode());
                responseResult.setMessage("你输入的原始密码不正确");
                logger.debug("修改密码，结果=responseResult:" + responseResult);
                return responseResult;
            }
            //修改密码
            int num = this.userService.updatePwd(existUser.getId(), DigestUtils.md5Hex(newPassword));
            if (num != 1) {
                responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
                responseResult.setMessage("操作失败，请您稍后再试");
                logger.debug("修改密码失败，已经离职或该用户被删除！结果=responseResult:" + responseResult);
                return responseResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseResult.setCode(IStatusMessage.SystemStatus.ERROR.getCode());
            responseResult.setMessage("操作失败，请您稍后再试");
            logger.error("修改密码异常！", e);
        }
        logger.debug("修改密码，结果=responseResult:" + responseResult);
        return responseResult;
    }

    public static void main(String[] args) throws ParseException {
        Date date = DateUtil.parse("2018-01-04 20:15:21");
        Date date1 = DateUtil.parse("2018-01-04 20:11:21");
        Date date2 = DateUtil.parse("2018-01-04 20:12:21");
        long beginTime = date.getTime();
        long beginTime1 = date1.getTime();
        long end1 = date2.getTime();
        long endTime = System.currentTimeMillis();
        //main
        long betweenDays = endTime - beginTime;
        long betweenDays1 = endTime - beginTime1;
        long eq = end1 - beginTime1;
        boolean flag = betweenDays - (60000) > 0;//超时
        boolean flag1 = betweenDays1 - (60000) > 0;//超时
        boolean flag2 = eq - (60000) == 0;//
        System.out.println(betweenDays);
        System.out.println(betweenDays1);
        System.out.println(eq);
        System.out.println(flag);
        System.out.println(flag1);
        System.out.println(flag2);
    }


    /**
     * @param obj
     * @param response
     * @return
     * @描述：校验请求参数
     */
    protected boolean validatorRequestParam(Object obj, ResponseResult response) {
        boolean flag = false;
        Validator validator = new Validator();
        List<ConstraintViolation> ret = validator.validate(obj);
        if (ret.size() > 0) {
            //校验参数有误
            response.setCode(IStatusMessage.SystemStatus.PARAM_ERROR.getCode());
            response.setMessage(ret.get(0).getMessageTemplate());
        } else {
            flag = true;
        }
        return flag;
    }
}
