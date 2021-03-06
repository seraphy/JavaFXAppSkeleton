package jp.seraphyware.javafxexam.jfxexam1.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackgroundTaskService implements Executor {

	/**
	 * ロガー.
	 */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * スレッドサービス.
	 */
	private ExecutorService executor;

	/**
	 * 初期化.
	 */
	@PostConstruct
	public void init() {
		log.info("★BackgroundTaskService#init");
		executor = Executors.newCachedThreadPool();
	}

	/**
	 * 破棄処理.
	 */
	@PreDestroy
	public void dispose() {
		log.info("★BackgroundTaskService#dispose");
		shutdown();
	}

	/**
	 * バックグラウンドジョブのキューに入れFutureを返す.
	 *
	 * @param <V>
	 *            データ型
	 * @param task
	 *            タスク
	 * @return Future
	 */
	public <V> Future<V> execute(final Callable<V> task) {
		Objects.requireNonNull(task);
		return executor.submit(task);
	}

	/**
	 * バックグラウンドジョブのキューに入れる.
	 *
	 * @param task
	 *            タスク
	 */
	@Override
	public void execute(final Runnable task) {
		Objects.requireNonNull(task);
		executor.execute(task);
	}

	/**
	 * 非同期完了可能フューチャを作成して返す.
	 * @param supplier
	 * @return
	 */
	public <U> CompletableFuture<U> createSupplyAsyncCompletableFuture(
			Supplier<U> supplier) {
		Objects.requireNonNull(supplier);
		return CompletableFuture.supplyAsync(supplier, executor);
	}

	/**
	 * 非同期完了可能フューチャを作成して返す.
	 * @param supplier
	 * @return
	 */
	public CompletableFuture<Void> createAsyncCompletableFuture(
			Runnable task) {
		Objects.requireNonNull(task);
		return CompletableFuture.runAsync(task, executor);
	}

	/**
	 * タスクを受け取り、そのタスクを開始して、完了可能フューチャーとして返す.<br>
	 * @param task 開始するタスク
	 * @return 完了可能なタスク.
	 */
	public <T> CompletableFuture<T> wrapCompletableFuture(FutureTask<T> task) {
		CompletableFuture<T> cf = new CompletableFuture<>();
		Runnable job = () -> {
			try {
				task.run();
				cf.complete(task.get());

			} catch (Throwable ex) {
				cf.completeExceptionally(ex);
			}
		};
		executor.submit(job);
		return cf;
	}

	/**
	 * サービスを停止する.
	 */
	public void shutdown() {
		if (!executor.isShutdown()) {
			log.info("shutdownNow");
			executor.shutdownNow();
			try {
				executor.awaitTermination(10, TimeUnit.SECONDS);
				log.info("shutdown complete");

			} catch (InterruptedException ex) {
				log.warn("サービス停止待機を割り込みにより解除しました。:" + ex, ex);
			}
		}
	}
}
