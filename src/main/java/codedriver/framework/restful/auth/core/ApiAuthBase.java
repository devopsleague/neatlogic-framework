package codedriver.framework.restful.auth.core;

import codedriver.framework.restful.dao.mapper.ApiMapper;
import codedriver.framework.restful.dto.ApiVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public abstract class ApiAuthBase implements IApiAuth {
    protected static ApiMapper apiMapper;

    @Autowired
    public void setApiMapper(ApiMapper _apiMapper) {
        apiMapper = _apiMapper;
    }

    @Override
    public int auth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException {
        
        return myAuth(interfaceVo,jsonParam,request);
    }

    public abstract int myAuth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException;

}