package com.chuangjie.framework.banner.core;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.ClassUtils;

import java.util.concurrent.TimeUnit;

/**
 * 项目启动成功后，输出启动提示
 *
 * @author hhy
 */
@Slf4j
public class BannerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(1, TimeUnit.SECONDS); // 延迟 1 秒，保证输出到结尾
            log.info("\n----------------------------------------------------------\n\t" +
                    "项目启动成功！\n" +
                    "----------------------------------------------------------");

            // 数据报表
            if (isNotPresent("com.chuangjie.module.report.framework.security.config.SecurityConfiguration")) {
                System.out.println("[报表模块 chuangjie-module-report - 已禁用]");
            }
            // 工作流
            if (isNotPresent("com.chuangjie.module.bpm.framework.flowable.config.BpmFlowableConfiguration")) {
                System.out.println("[工作流模块 chuangjie-module-bpm - 已禁用]");
            }
            // 商城系统
            if (isNotPresent("com.chuangjie.module.trade.framework.web.config.TradeWebConfiguration")) {
                System.out.println("[商城系统 chuangjie-module-mall - 已禁用]");
            }
            // ERP 系统
            if (isNotPresent("com.chuangjie.module.erp.framework.web.config.ErpWebConfiguration")) {
                System.out.println("[ERP 系统 chuangjie-module-erp - 已禁用]");
            }
            // WMS 仓库管理系统
            if (isNotPresent("com.chuangjie.module.wms.framework.web.config.WmsWebConfiguration")) {
                System.out.println("[WMS 仓库管理系统 chuangjie-module-wms - 已禁用]");
            }
            // PMS 系统
            if (isNotPresent("com.chuangjie.module.pms.framework.web.config.PmsWebConfiguration")) {
                System.out.println("[PMS 项目管理系统 chuangjie-module-pms - 已禁用]");
            }
            // CRM 系统
            if (isNotPresent("com.chuangjie.module.crm.framework.web.config.CrmWebConfiguration")) {
                System.out.println("[CRM 系统 chuangjie-module-crm - 已禁用]");
            }
            // MES 系统
            if (isNotPresent("com.chuangjie.module.mes.framework.web.config.MesWebConfiguration")) {
                System.out.println("[MES 系统 chuangjie-module-mes - 已禁用]");
            }
            // 微信公众号
            if (isNotPresent("com.chuangjie.module.mp.framework.mp.config.MpConfiguration")) {
                System.out.println("[微信公众号 chuangjie-module-mp - 已禁用]");
            }
            // 支付平台
            if (isNotPresent("com.chuangjie.module.pay.framework.pay.config.PayConfiguration")) {
                System.out.println("[支付系统 chuangjie-module-pay - 已禁用]");
            }
            // AI 大模型
            if (isNotPresent("com.chuangjie.module.ai.framework.web.config.AiWebConfiguration")) {
                System.out.println("[AI 大模型 chuangjie-module-ai - 已禁用]");
            }
            // IoT 物联网
            if (isNotPresent("com.chuangjie.module.iot.framework.web.config.IotWebConfiguration")) {
                System.out.println("[IoT 物联网 chuangjie-module-iot - 已禁用]");
            }
            // IM 即时通讯
            if (isNotPresent("com.chuangjie.module.im.framework.web.config.ImWebConfiguration")) {
                System.out.println("[IM 即时通讯 chuangjie-module-im - 已禁用]");
            }
        });
    }

    private static boolean isNotPresent(String className) {
        return !ClassUtils.isPresent(className, ClassUtils.getDefaultClassLoader());
    }

}
