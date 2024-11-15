ALTER TABLE `mq_topic`
    ADD COLUMN `handler` varchar(50) NOT NULL DEFAULT 'artemis' COMMENT '消息队列类型' AFTER `config`;

ALTER TABLE `mq_subscribe`
    ADD COLUMN `handler` varchar(50) NOT NULL DEFAULT 'artemis' COMMENT '消息队列类型' AFTER `server_id`;