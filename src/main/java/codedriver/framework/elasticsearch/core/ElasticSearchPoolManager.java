package codedriver.framework.elasticsearch.core;

public class ElasticSearchPoolManager {
//	static Logger logger = LoggerFactory.getLogger(ElasticSearchPoolManager.class);
//	private static Map<String,MultiAttrsObjectPool> poolMap = new HashMap<String,MultiAttrsObjectPool>();
//
//	public synchronized static MultiAttrsObjectPool getObjectPool(String poolName){
//	    if(!poolMap.containsKey(poolName)) {
//	        if (!Config.ES_ENABLE()) {
//                throw new IllegalStateException("ES未启用，请修改对应配置");
//            }
//            Map<String, String> esClusters = Config.ES_CLUSTERS();
//            if (esClusters.isEmpty()) {
//                throw new IllegalStateException("ES集群信息未配置，es.cluster.<cluster-name>=<ip:port>[,<ip:port>...]");
//            }
//
//            MultiAttrsSearchConfig config = new MultiAttrsSearchConfig();
//            config.setPoolName(poolName);
//
//            Map.Entry<String, String> cluster = esClusters.entrySet().iterator().next();
//            config.addCluster(cluster.getKey(), cluster.getValue());
//            if (esClusters.size() > 1) {
//                logger.warn("multiple clusters available, only cluster {} was used (picked randomly) for testing", cluster.getKey());
//            }
//            MultiAttrsObjectPool  workcenterObjectPool = MultiAttrsSearch.getObjectPool(config);
//            poolMap.put(poolName, workcenterObjectPool);
//	    }
//
//
//		 return poolMap.get(poolName);
//	}
}
