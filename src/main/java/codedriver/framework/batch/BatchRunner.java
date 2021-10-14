/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.batch;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.transaction.util.TransactionUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Title: BatchRunner
 * @Package codedriver.framework.batch
 * @Description: 批量处理框架，支持根据列表按照指定并行度并发处理逻辑
 * @Author: chenqiwei
 * @Date: 2021/1/4 9:31 上午
 **/
public class BatchRunner<T> {
    private final static Logger logger = LoggerFactory.getLogger(BatchRunner.class);

    public static class State {
        private boolean isSucceed = false;
        private String error;

        public boolean isSucceed() {
            return isSucceed;
        }

        public void setSucceed(boolean succeed) {
            isSucceed = succeed;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * @param itemList 对象列表
     * @param parallel 并发度（多少个线程）
     * @param job      执行函数
     */
    public State execute(List<T> itemList, int parallel, BatchJob<T> job, String threadName) {
        return execute(itemList, parallel, false, job, threadName);
    }

    /**
     * @param itemList        对象列表
     * @param parallel        并发度（多少个线程）
     * @param needTransaction 每个对象的执行过程是否需要启用事务
     * @param job             执行函数
     */
    public State execute(List<T> itemList, int parallel, boolean needTransaction, BatchJob<T> job, String threadName) {
        State state = new State();
        if (CollectionUtils.isNotEmpty(itemList)) {
            //状态默认是成功状态，任意线程出现异常则置为失败
            state.setSucceed(true);
            parallel = Math.min(itemList.size(), parallel);
            CountDownLatch latch = new CountDownLatch(parallel);

            for (int i = 0; i < parallel; i++) {
                Runner<T> runner = new Runner<>(threadName, i, parallel, needTransaction, itemList, job, latch, state);
                CachedThreadPool.execute(runner);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return state;
    }

    static class Runner<T> extends CodeDriverThread {
        int index;
        int parallel;
        List<T> itemList;
        BatchJob<T> job;
        CountDownLatch latch;
        boolean needTransaction;
        State state;

        public Runner(String _threadName, int _index, int _parallel, boolean _needTransaction, List<T> _itemList, BatchJob<T> _job, CountDownLatch _latch, State _state) {
            super(_threadName);
            index = _index;
            parallel = _parallel;
            itemList = _itemList;
            job = _job;
            latch = _latch;
            needTransaction = _needTransaction;
            state = _state;
        }

        @Override
        protected void execute() {
            try {
                for (int i = index; i < itemList.size(); i += parallel) {
                    TransactionStatus ts = null;
                    if (needTransaction) {
                        ts = TransactionUtil.openTx();
                    }
                    try {
                        job.execute(itemList.get(i));
                        if (ts != null) {
                            TransactionUtil.commitTx(ts);
                        }
                    } catch (Exception e) {
                        state.setSucceed(false);
                        state.setError(e.getMessage());
                        logger.error(e.getMessage(), e);
                        if (ts != null) {
                            TransactionUtil.rollbackTx(ts);
                        }
                    }
                }
            } catch (Exception ex) {
                if (!(ex instanceof ApiRuntimeException)) {
                    logger.error(ex.getMessage(), ex);
                }
            } finally {
                latch.countDown();
            }
        }
    }

   /* public static void main(String[] a) {
        ModuleInitApplicationListener.getModuleinitphaser().register();
        ModuleInitApplicationListener.getModuleinitphaser().arrive();
        BatchRunner<Integer> runner = new BatchRunner<>();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            list.add(i);
        }
        int parallel = 4;
        runner.execute(list, parallel, System.out::println, "A");
        System.out.println("done");
    }*/
}
