package com.xxx.redo.service;

import com.xxx.redo.model.BusinessException;
import com.xxx.redo.util.JsonUtil;
import com.xxx.redo.annotation.Redo;
import com.xxx.redo.dao.SysRedoMapper;
import com.xxx.redo.model.RedoException;
import com.xxx.redo.model.SysRedo;
import com.xxx.redo.util.RedoCheckUtils;
import com.xxx.redo.util.RedoConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * redo service
 */
@Slf4j
@Service
public class RedoService extends ApplicationObjectSupport {

    @Resource(name = "commonThreadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    // mork model，用来标记是否redo模式，告诉切面过滤处理
    private static final ThreadLocal<Boolean> WORK_MODEL = new ThreadLocal<Boolean>();

    @Resource
    private SysRedoMapper mapper;

    /**
     * 异步记录redo日志
     * @param redo
     * @param t
     * @param invokeClass
     * @param invokeMethod
     * @param args
     */
    public void addRedoItem(Redo redo, Throwable t, Class<?> invokeClass, MethodSignature invokeMethod, Object[] args) {
        final SysRedo redoItem = new SysRedo();
        redoItem.setAutoRedoAble(redo.autoRedoAble());
        redoItem.setAutoRedoLimit(redo.autoRedoLimit());
        redoItem.setRedoHandler(redo.handlerName());
        redoItem.setBizType(redo.type());
        if (invokeClass != null) {
            redoItem.setBizInvokeClazz(invokeClass.getName());
            redoItem.setBizInvokeMethod(invokeMethod.getName());
            String[] parameterTypes = getMethodActualParameterType(invokeMethod);
            redoItem.setBizInvokeMethodArgtype(JsonUtil.toJson(parameterTypes));
        }
        redoItem.setBizInvokeArgs(serialize(args));
        redoItem.setCreateTime(new Date());
        redoItem.setFailMessage(getFailMessage(t));
        // 异步保存
        taskExecutor.submit( () -> {
            try {
                mapper.insertSelective(redoItem);
            } catch (Exception e) {
                log.error("save redo item {} error", redoItem,e);
            }
        });
    }


    /**
     * 执行自动重做的操作
     */
    public void autoRedo() {
        // 查询可以自动执行的任务: 可自动自行 & 状态为 待处理
        List<SysRedo> redoList = mapper.findAutoRedoItem();
        // 处理任务
        redoList.stream().forEach( item -> {
            redo(item, "auto");
        });
    }

    /**
     * 执行手动重做
     */
    public void redoById(Long id, String operator) {
        SysRedo redoItem = mapper.selectByPrimaryKey(id);
        if (redoItem != null) {
            boolean result = redo(redoItem, operator);
            if (!result){
                throw new BusinessException("请求失败");
            }
        }
    }


    /**
     * 执行redo
     * @param redoItem
     */
    @Transactional
    public boolean redo(SysRedo redoItem, String executor) {
        SysRedo modify = new SysRedo();
        try {
            modify.setId(redoItem.getId());
            modify.setUpdateTime(new Date());
            modify.setUpdator(executor);
            if (RedoConstants.STATUS_NEW.equals(redoItem.getStatus())
                    || RedoConstants.STATUS_HANDLE_FAILED.equals(redoItem.getStatus())) {
                RedoHandler redoHandler = getUredoHandler(redoItem.getRedoHandler());
                RedoCheckUtils.checkNotNull(redoHandler);
                WORK_MODEL.set(true);
                redoHandler.redo(redoItem);
            }
            // 执行成功，设置状态
            modify.setStatus(RedoConstants.STATUS_HANDLE_SUCCESS);
            mapper.updateByPrimaryKeySelective(modify);
            return true;
        } catch (Exception e) {
            log.error("execute redo item {} error", redoItem, e);
            // 执行失败，重试次数加一，如果重试次数大于等于重试限制，状态设置为失败
            modify.setAutoRedoCount(redoItem.getAutoRedoCount() + 1);
            if (modify.getAutoRedoCount() >= redoItem.getAutoRedoLimit()) {
                modify.setStatus(RedoConstants.STATUS_HANDLE_FAILED);
            }
            mapper.updateByPrimaryKeySelective(modify);
            return false;
        } finally {
            WORK_MODEL.remove();
        }
    }

    /**
     * 取消重做
     * @param id
     * @param operator
     */
    @Transactional
    public void cancelById(Long id, String operator) {
        SysRedo redoItem = mapper.selectByPrimaryKey(id);
        if (redoItem != null){
            if (RedoConstants.STATUS_NEW.equals(redoItem.getStatus())
                    || RedoConstants.STATUS_HANDLE_FAILED.equals(redoItem.getStatus())) {
                redoItem.setStatus(RedoConstants.STATUS_NOT_NEED_HANDLE);
                redoItem.setUpdator(operator);
                redoItem.setUpdateTime(new Date());
                mapper.updateByPrimaryKeySelective(redoItem);
            }
        }


    }

    /**
     * 还有未处理的任务报警(近10分钟内)
     */
    public void warning() {
        // 获取执行失败 & 需要手动处理的任务
        int needRedoCount = mapper.countNeedRedoItem();
        if (needRedoCount > 0) {
            // 通知
            // messageChannel.sendAsync("【重试】有需要手动处理的任务【"+ needRedoCount + "】条，请及时查看处理");
        }
    }


    /**
     * 直接通过json序列化数据 <br>
     * 反序列化的时候根据方法参数类型做json反序列化
     * @see com.xxx.redo.service.DefaultRedoHandler#deserialize(String, String)
     * @return json array string
     */
    public String serialize(Object[] args) {
        return JsonUtil.toJson(args);
    }

    /**
     * 获取方法定义的参数类型
     * @param methodSignature aspectJ方法签名
     * @return
     */
    private String[] getMethodActualParameterType(MethodSignature methodSignature) {
        Method method = methodSignature.getMethod();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        String[] typeNames = new String[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            typeNames[i] = JsonUtil.toCanonical(genericParameterTypes[i]);
        }
        return typeNames;

    }


    public static void main(String[] args) throws NoSuchMethodException {
        String[] aa = new String[]{"1", "2"};
        List<String> bb = Arrays.asList(aa);
        System.out.println(JsonUtil.toJson(aa));
        System.out.println(JsonUtil.toJson(bb));
        JsonUtil.toList("[\"1\",\"2\"]", String.class).toArray();
        String aaa = JsonUtil.toJson(null);
        System.out.println(aaa);
        Method method = RedoService.class.getMethod("autoRedo");
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        String[] typeNames = new String[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            typeNames[i] = JsonUtil.toCanonical(genericParameterTypes[i]);
        }
        System.out.println(JsonUtil.toJson(typeNames));
        System.out.println(JsonUtil.toJson(null));
        //System.out.println(genericParameterTypes);
    }

    private String getFailMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String message = t.getMessage();
        String cause = t.toString();
        String failMessage = "message: " + message + "; cause: " + cause;
        return failMessage.substring(0, failMessage.length() < 2000 ? failMessage.length() : 2000);
    }

    private RedoHandler getUredoHandler(String handlerName) {
        RedoCheckUtils.checkNotBlank(handlerName);
        byte[] charArray = handlerName.getBytes();
        if (charArray[0] >= 'A' && charArray[0] < 'Z') {
            charArray[0] = (byte) ((char) charArray[0] + 'a' - 'A');
        }
        String handlerBeanName = new String(charArray);
        ApplicationContext applicationContext = getApplicationContext();
        try {
            Object bean = applicationContext.getBean(handlerBeanName);
            RedoCheckUtils.check(bean instanceof RedoHandler);
            return (RedoHandler) bean;
        } catch (NoSuchBeanDefinitionException e) {
            throw new RedoException("no redoHandler instance named " + handlerBeanName + " is defined", e);
        }
    }

    public static boolean isRedoWorkModel() {
        return WORK_MODEL.get() == null ? false : WORK_MODEL.get();
    }


}
