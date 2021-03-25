package codedriver.framework.transaction.core;

/**
 * @Title: ICommit
 * @Package: codedriver.framework.transaction.core
 * @Description: 去掉事务执行
 * @author: chenqiwei
 * @date: 2021/1/73:38 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public interface IEscapeTransaction {
    void execute();
}