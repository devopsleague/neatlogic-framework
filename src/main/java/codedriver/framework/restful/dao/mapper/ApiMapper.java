package codedriver.framework.restful.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.framework.restful.dto.ApiAuditVo;
import codedriver.framework.restful.dto.ApiVo;

public interface ApiMapper {

	public ApiVo getApiByToken(String token);

	public List<String> getApiTokenList(ApiVo apiVo);

	public List<ApiVo> getAllApi();

	public List<ApiVo> getApiListByTokenList(List<String> tokenList);

	public int getApiAuditCount(ApiAuditVo apiAuditVo);

	public List<ApiAuditVo> getApiAuditList(ApiAuditVo apiAuditVo);

	public List<ApiVo> getApiVisitTimesListByTokenList(List<String> tokenList);

	public String getApiAuditDetailByHash(String hash);

	public List<ApiVo> getApiAccessCountByTokenList(List<String> tokenList);

	public String getApiAccessCountLockByToken(String token);

	public int replaceApi(ApiVo apiVo);

	public int insertApiAudit(ApiAuditVo apiAudit);

	public int updateApiComponentIdById(ApiVo apiVo);

	public int batchUpdate(ApiVo apiVo);

	public int replaceApiAuditDetail(@Param("hash") String hash, @Param("content") String content);

	public int insertApiAccessCount(@Param("token") String token, @Param("visitTimes") Integer visitTimes);
	
	/**
	 * 
	* @Time:2020年7月15日
	* @Description: 增加接口访问次数 
	* @param token
	* @param visitTimes
	* @return int
	 */
	public int increaseApiAccessCount(@Param("token") String token, @Param("visitTimes") Integer visitTimes);

	public int deleteApiByToken(String token);

}
