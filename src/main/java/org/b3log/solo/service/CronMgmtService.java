/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-present, b3log.org
 *
 * Solo is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *         http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.b3log.solo.service;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.solo.util.Solos;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cron management service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.7, Sep 9, 2021
 * @since 2.9.7
 */
@Service
public class CronMgmtService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(CronMgmtService.class);

    /**
     * Cron thread pool.
     */
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    /**
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * Article management service.
     */
    @Inject
    private ArticleMgmtService articleMgmtService;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * Export service.
     */
    @Inject
    private ExportService exportService;

    /**
     * User management service.
     */
    @Inject
    private UserMgmtService userMgmtService;

    /**
     * Start all cron tasks.
     */
    public void start() {
        long delay = 10000;

        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                /// 移除xx时间内无任何请求的IP（访问者）计数
                StatisticMgmtService.removeExpiredOnlineVisitor();
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Executes cron failed", e);
            } finally {
                Stopwatchs.release();
            }
        }, delay, 1000 * 60 * 10, TimeUnit.MILLISECONDS);
        delay += 2000;

        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                /// 获取链滴社区的IP黑名单
                Solos.reloadBlacklistIPs();
                /// 更新文章的随机值，用于快速查询随机文章列表，可能用于B3社区帖子推荐，或是随机文章浏览
                articleMgmtService.updateArticlesRandomValue(20);
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Executes cron failed", e);
            } finally {
                Stopwatchs.release();
            }
        }, delay, 1000 * 60 * 60, TimeUnit.MILLISECONDS);
        delay += 2000;

        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                articleMgmtService.refreshGitHub();
                /// 向B3社区推送、刷新博客端的站点信息
                userMgmtService.refreshUSite();
                /// 打包管理者所有文章向B3社区推送备份
                exportService.exportLianDi();
                exportService.exportGitHub();
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Executes cron failed", e);
            } finally {
                Stopwatchs.release();
            }
        }, delay, 1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS);
        delay += 2000;
    }

    /**
     * Stop all cron tasks.
     */
    public void stop() {
        SCHEDULED_EXECUTOR_SERVICE.shutdown();
    }
}
