-- 施工变更增强：现场照片、材料需求、材料费/人工费拆分、社区三分支决策
ALTER TABLE construction_change ADD COLUMN site_photos            VARCHAR(1024); -- 现场照片说明/档案编号
ALTER TABLE construction_change ADD COLUMN material_requirements  VARCHAR(1024); -- 新的材料需求
ALTER TABLE construction_change ADD COLUMN material_fee_delta     NUMERIC(10,2) DEFAULT 0;
ALTER TABLE construction_change ADD COLUMN labor_fee_delta        NUMERIC(10,2) DEFAULT 0;
-- 社区判断：是否影响补贴资格（APPROVED 时逐项核定）
ALTER TABLE construction_change ADD COLUMN subsidy_affected       BOOLEAN;
ALTER TABLE construction_change ADD COLUMN community_decision     VARCHAR(24); -- APPROVED/REJECTED/COORDINATING
ALTER TABLE construction_change ADD COLUMN coordination_note      VARCHAR(512);
ALTER TABLE construction_change ADD COLUMN resolved_at           TIMESTAMP;
-- 核准时重新生成的费用快照
ALTER TABLE construction_change ADD COLUMN new_material_fee       NUMERIC(10,2);
ALTER TABLE construction_change ADD COLUMN new_labor_fee          NUMERIC(10,2);
ALTER TABLE construction_change ADD COLUMN new_subsidy_amount     NUMERIC(10,2);
ALTER TABLE construction_change ADD COLUMN new_total_cost         NUMERIC(10,2);
ALTER TABLE construction_change ADD COLUMN reimbursement_delta    NUMERIC(10,2); -- 本次新增可报销金额

-- 变更涉及的项目清单（新增项目 / 材料替代），通过后并入改造方案
CREATE TABLE change_item (
    id                BIGSERIAL PRIMARY KEY,
    change_id         BIGINT NOT NULL REFERENCES construction_change(id),
    application_id    BIGINT NOT NULL REFERENCES application(id),
    item_type         VARCHAR(16)  NOT NULL, -- ADD 新增项目 / REPLACE 材料替代/工艺变更
    category          VARCHAR(32)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    spec              VARCHAR(256),
    unit              VARCHAR(16),
    quantity          INT NOT NULL DEFAULT 1,
    material_fee      NUMERIC(10,2) NOT NULL DEFAULT 0, -- 材料费
    labor_fee         NUMERIC(10,2) NOT NULL DEFAULT 0, -- 人工费
    reason            VARCHAR(512),                       -- 现场原因
    subsidy_eligible  BOOLEAN NOT NULL DEFAULT FALSE,      -- 社区核定是否计入可报销
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_change_item_change ON change_item(change_id);

-- 方案项目增加材料费/人工费拆分、来源（评估生成/家属新增/施工变更并入）
ALTER TABLE plan_item ADD COLUMN material_fee NUMERIC(10,2);
ALTER TABLE plan_item ADD COLUMN labor_fee    NUMERIC(10,2);
ALTER TABLE plan_item ADD COLUMN source       VARCHAR(16) NOT NULL DEFAULT 'ASSESSMENT';
-- 回填：综合单价按 80% 材料 / 20% 人工拆分（行金额=单价×数量），人工=总价-材料避免尾差
UPDATE plan_item SET material_fee = ROUND(unit_price * quantity * 0.8, 2) WHERE material_fee IS NULL;
UPDATE plan_item SET labor_fee = unit_price * quantity - material_fee WHERE labor_fee IS NULL;

-- 变更通过后并入的方案项目使用 status='CHANGE_ADDED'（varchar，无需建枚举）
