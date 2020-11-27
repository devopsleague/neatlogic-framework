package codedriver.framework.lock.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.common.RootConfiguration;
import codedriver.framework.lock.dao.mapper.LockMapper;

@Service
@RootConfiguration
public class LockManager {

    @Autowired
    private LockMapper lockMapper;

    public String getLockById(String lock) {
        if (StringUtils.isBlank(lockMapper.getLockById(lock))) {
            lockMapper.insertLock(lock);
        }
        return lockMapper.getLockByIdForUpdate(lock);
    }

}