/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.framework.exception.runner;

import neatlogic.framework.exception.core.ApiRuntimeException;

public class RunnerNotMatchException extends ApiRuntimeException {

    private static final long serialVersionUID = 3593220313941443951L;

    public RunnerNotMatchException(String ip, Long resourceId) {
        super("nfer.runnernotmatchexception.runnernotmatchexception.ipresourceid", ip, resourceId);
    }

    public RunnerNotMatchException(String ip, Long resourceId, String tag) {
        super("ip: {0}({1})找不到匹配的runner，请核对标签为{2}runner组配置", ip, resourceId, tag);
    }

    public RunnerNotMatchException() {
        super("nfer.runnernotmatchexception.runnernotmatchexception.noparam");
    }


}
