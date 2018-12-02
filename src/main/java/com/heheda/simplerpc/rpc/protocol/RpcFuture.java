package com.heheda.simplerpc.rpc.protocol;

import lombok.extern.log4j.Log4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

@Log4j
public class RpcFuture implements Future<RpcResponse> {

    private RpcRequest request;
    private RpcResponse response;
    private RpcSync sync;


    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.sync = new RpcSync();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.idDone();
    }

    @Override
    public RpcResponse get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        return response;
    }

    @Override
    public RpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            return response;
        }
        return null;
    }

    public void done(RpcResponse response) {
        this.response = response;
        log.info("done");
        sync.release(1);
    }

    static class RpcSync extends AbstractQueuedSynchronizer {

        private static final int done = 1;  // 已经执行完了 1
        private static final int pending = 0; // 初始状态就是 0, 开始就是未完成状态, 处理完时会更新为 1

        @Override
        protected boolean tryAcquire(int arg) {
//            return compareAndSetState(done, pending);
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);  // 执行完时把状态更新为 1
            } else {
                return true;
            }
        }


        public boolean idDone() {
            return getState() == done;
        }
    }
}
