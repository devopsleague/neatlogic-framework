package codedriver.framework.globallock.dao.mapper;

import codedriver.framework.dto.globallock.GlobalLockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GlobalLockMapper {

    List<GlobalLockVo> getGlobalLockByUuidForUpdate(String id);

    GlobalLockVo getGlobalLockById(Long lockId);

    GlobalLockVo getNextGlobalLockByUuid(String uuid);

    List<String> getGlobalLockUuidByKey(String jobId);

    void insertLock(GlobalLockVo globalLockVo);

    Integer updateToLockById(Long id);

    void deleteLock(Long id);

    List<GlobalLockVo> searchLock(GlobalLockVo globalLockVo);

    List<GlobalLockVo> getLockListByKeyListAndHandler(@Param("keyList") List<String> keyList,@Param("handler") String handler);

    Integer getLockCount(GlobalLockVo globalLockVo);

}
