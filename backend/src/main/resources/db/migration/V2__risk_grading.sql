-- 入户评估风险分级增强

-- assessment：五维度现场记录 + 平台评分 + 高风险照护建议
ALTER TABLE assessment ADD COLUMN mobility_score        INT;          -- 行动能力评分
ALTER TABLE assessment ADD COLUMN wetness               VARCHAR(16);  -- 卫生间湿滑程度: 干燥/一般/较湿/积水
ALTER TABLE assessment ADD COLUMN wetness_score         INT;
ALTER TABLE assessment ADD COLUMN bed_difficulty_score  INT;          -- 床边起身难度评分(0-3)
ALTER TABLE assessment ADD COLUMN lighting_score        INT;          -- 夜间照明评分
ALTER TABLE assessment ADD COLUMN emergency_condition   VARCHAR(256); -- 紧急呼叫条件(同住人/手机/离邻居距离等)
ALTER TABLE assessment ADD COLUMN emergency_score       INT;
ALTER TABLE assessment ADD COLUMN risk_score            INT;          -- 平台综合评分
ALTER TABLE assessment ADD COLUMN risk_factors          VARCHAR(1024);-- 风险因子明细(平台生成)
ALTER TABLE assessment ADD COLUMN care_recommendation    VARCHAR(512); -- 高风险: 陪同/临时照护建议

-- construction_schedule：高风险优先 + 施工陪同/照护安排
ALTER TABLE construction_schedule ADD COLUMN priority       INT NOT NULL DEFAULT 0; -- 高风险=1 优先排期
ALTER TABLE construction_schedule ADD COLUMN care_required  VARCHAR(16);            -- NONE/COMPANION/TEMP_CARE
ALTER TABLE construction_schedule ADD COLUMN care_arrangement VARCHAR(512);         -- 陪同/临时照护安排说明

-- 家属对高风险改造项目的拒绝留痕（保留评估师说明与家属拒绝原因，供街道审核查看）
CREATE TABLE plan_item_rejection (
    id                BIGSERIAL PRIMARY KEY,
    application_id    BIGINT NOT NULL REFERENCES application(id),
    plan_item_id      BIGINT REFERENCES plan_item(id),
    item_name         VARCHAR(128) NOT NULL,
    category          VARCHAR(32),
    assessor_note     VARCHAR(512) NOT NULL,  -- 评估师说明（为什么建议，评估依据）
    family_reason     VARCHAR(512) NOT NULL,  -- 家属拒绝原因
    risk_level        VARCHAR(16),            -- 拒绝时的风险等级
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_rejection_app ON plan_item_rejection(application_id);
