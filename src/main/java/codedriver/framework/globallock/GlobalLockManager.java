/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.globallock;

import codedriver.framework.common.RootConfiguration;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.globallock.core.GlobalLockHandlerFactory;
import codedriver.framework.globallock.dao.mapper.GlobalLockMapper;
import codedriver.framework.lock.core.LockManager;
import codedriver.framework.transaction.util.TransactionUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

@Service
@RootConfiguration
public class GlobalLockManager {
    private static final Logger logger = LoggerFactory.getLogger(LockManager.class);

    private static GlobalLockMapper globalLockMapper;

    @Autowired
    public GlobalLockManager(GlobalLockMapper _globalLockMapper) {
        globalLockMapper = _globalLockMapper;
    }

    /**
     * 加入锁队列
     *
     * @param globalLockVo 锁入参
     */
    public static void insertLock(GlobalLockVo globalLockVo) {
        TransactionStatus transactionStatus = TransactionUtil.openTx();
        globalLockMapper.insertLock(globalLockVo);
        try {
            //获取所有该key的锁和未上锁的队列 for update
            List<GlobalLockVo> globalLockVoList = globalLockMapper.getGlobalLockByUuidForUpdate(globalLockVo.getUuid());
            //执行mode 策略 验证是否允许上锁
            if (GlobalLockHandlerFactory.getHandler(globalLockVo.getHandler()).getIsCanInsertLock(globalLockVoList, globalLockVo)) {
                TransactionUtil.commitTx(transactionStatus);
            }else{
                TransactionUtil.rollbackTx(transactionStatus);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
            //如果wait lock timeout 则仅加入队列，等待notify
            TransactionUtil.rollbackTx(transactionStatus);
            throw new ApiRuntimeException(ex.getMessage());
        }
    }
    /**
     * 获取锁
     *
     * @param globalLockVo 锁入参
     * @return lockId
     */
    public static GlobalLockVo getLock(GlobalLockVo globalLockVo) {
        //预先加入锁队列
        insertLock(globalLockVo);
        lock(globalLockVo);
        return globalLockVo;
    }

    /**
     * 锁
     *
     * @param globalLockVo 锁入参
     * @return lockId
     */
    private static GlobalLockVo lock(GlobalLockVo globalLockVo){
        TransactionStatus transactionStatus = TransactionUtil.openTx();
        try {
            //获取所有该key的锁和未上锁的队列 for update
            List<GlobalLockVo> globalLockVoList = globalLockMapper.getGlobalLockByUuidForUpdate(globalLockVo.getUuid());
            //执行mode 策略 验证是否允许上锁
            if (GlobalLockHandlerFactory.getHandler(globalLockVo.getHandler()).getIsCanLock(globalLockVoList, globalLockVo)) {
                globalLockMapper.updateToLockById(globalLockVo.getId());
                globalLockVo.setIsLock(1);
            }
            TransactionUtil.commitTx(transactionStatus);
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
            //如果wait lock timeout 则仅加入队列，等待notify
            TransactionUtil.rollbackTx(transactionStatus);
            globalLockVo.setWaitReason(ex.getMessage());
        }
        return globalLockVo;
    }

    public static void cancelLock(Long lockId, JSONObject paramJson) {
        GlobalLockVo globalLockVo = globalLockMapper.getGlobalLockById(lockId);
        if (globalLockVo != null) {
            TransactionStatus transactionStatus = TransactionUtil.openTx();
            try {
                //获取所有该key的锁和未上锁的队列 for update
                globalLockMapper.getGlobalLockByUuidForUpdate(globalLockVo.getUuid());
                globalLockMapper.deleteLock(lockId);
                //获取对应uuid队列中下一个lockId notify
                GlobalLockVo nextGlobalLockVo = globalLockMapper.getNextGlobalLockByUuid(globalLockVo.getUuid());
                TransactionUtil.commitTx(transactionStatus);
                if(nextGlobalLockVo != null){
                    GlobalLockHandlerFactory.getHandler(globalLockVo.getHandler()).doNotify(nextGlobalLockVo,paramJson);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(),ex);
                TransactionUtil.rollbackTx(transactionStatus);
                throw new ApiRuntimeException(ex.getMessage());
            }
        }
    }

    /**
     * 重试获取锁
     * @param lockId 锁 id
     */
    public static GlobalLockVo retryLock(GlobalLockVo globalLockVo){
        GlobalLockVo globalLockTmp = globalLockMapper.getGlobalLockById(globalLockVo.getId());
        if (globalLockTmp == null) {
            insertLock(globalLockVo);
        }
        globalLockVo = globalLockTmp ;
        return lock(globalLockVo);
    }

}