/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.framework.filter;

import neatlogic.framework.asynchronization.queue.NeatLogicUniqueBlockingQueue;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.dao.mapper.UserSessionContentMapper;
import neatlogic.framework.dao.mapper.UserSessionMapper;
import neatlogic.framework.dto.UserSessionContentVo;
import neatlogic.framework.dto.UserSessionVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class InsertUserSessionThread {
    @Resource
    UserSessionMapper userSessionMapper;
    @Resource
    UserSessionContentMapper userSessionContentMapper;
    private static final Logger logger = LoggerFactory.getLogger(InsertUserSessionThread.class);
    private static final NeatLogicUniqueBlockingQueue<UserSessionVo> blockingQueue = new NeatLogicUniqueBlockingQueue<>(50000);

    @PostConstruct
    public void init() {
        Thread t = new Thread(new NeatLogicThread("INSERT-USER-SESSION-MANAGER") {
            @Override
            protected void execute() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        UserSessionVo userSessionVo = blockingQueue.take();
                        userSessionMapper.insertUserSession(userSessionVo.getUserUuid(), userSessionVo.getTokenHash(), userSessionVo.getTokenCreateTime(), userSessionVo.getAuthInfoHash());
                        userSessionContentMapper.insertUserSessionContent(new UserSessionContentVo(userSessionVo.getTokenHash(), userSessionVo.getToken()));
                        if (StringUtils.isNotBlank(userSessionVo.getAuthInfoHash())) {
                            userSessionContentMapper.insertUserSessionContent(new UserSessionContentVo(userSessionVo.getAuthInfoHash(), userSessionVo.getAuthInfoStr()));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void addInsertUserSession(UserSessionVo userSessionVo) {
        blockingQueue.offer(userSessionVo);
    }
}
