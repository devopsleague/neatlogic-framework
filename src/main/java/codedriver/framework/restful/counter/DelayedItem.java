package codedriver.framework.restful.counter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import codedriver.framework.asynchronization.threadlocal.TenantContext;

public class DelayedItem implements Delayed {

//	private long delayTime = TimeUnit.MINUTES.toMillis(5);
	private long delayTime = TimeUnit.SECONDS.toMillis(30);
	
	/** 缓存租户访问记录 **/
	private ConcurrentMap<String, Map<String, Integer>> tenantAccessTokenMap = new ConcurrentHashMap<>();
	
	public DelayedItem() {
	}

	@Override
	public int compareTo(Delayed o) {
		DelayedItem item = (DelayedItem) o;
		long diff = this.delayTime - item.delayTime;
		if(diff <= 0){
			return -1;
		} else{
			return 1;
		}
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return delayTime;
	}

	public void putToken(String token) {
		if(StringUtils.isNotBlank(token)) {
			String tenantUuid = TenantContext.get().getTenantUuid();
			Map<String, Integer> accessTokenCounterMap = tenantAccessTokenMap.get(tenantUuid);
			if(accessTokenCounterMap == null) {
				synchronized(DelayedItem.class){
					accessTokenCounterMap = tenantAccessTokenMap.get(tenantUuid);
					if(accessTokenCounterMap == null) {
						accessTokenCounterMap = new HashMap<>();
						tenantAccessTokenMap.put(tenantUuid, accessTokenCounterMap);
					}
				}
			}
			synchronized(accessTokenCounterMap){
				Integer counter = accessTokenCounterMap.get(token);
				if(counter == null) {
					counter = 0;
				}
				accessTokenCounterMap.put(token, counter + 1);
			}
		}		
	}

	public ConcurrentMap<String, Map<String, Integer>> getTenantAccessTokenMap() {
		return tenantAccessTokenMap;
	}

	public long getDelayTime() {
		return delayTime;
	}
}
